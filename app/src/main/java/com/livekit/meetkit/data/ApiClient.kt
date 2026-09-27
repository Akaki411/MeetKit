/**
 * REST API клиент для работы с сервером MeetKit: авторизация, управление комнатами, профилем и админкой.
 */
package com.livekit.meetkit.data

import android.content.Context
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.livekit.meetkit.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.InetAddress
import java.net.Socket
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

class ApiClient(context: Context) {
    private val prefs = ServerPreferences(context)
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val cookieStore = HashMap<String, MutableList<Cookie>>()

    init {
        loadCookiesFromPrefs()
    }

    private fun loadCookiesFromPrefs() {
        val json = prefs.getCookiesJson() ?: return
        try {
            val type = object : TypeToken<List<String>>() {}.type
            val strings: List<String> = gson.fromJson(json, type) ?: emptyList()
            val httpUrl = baseUrl.toHttpUrlOrNull() ?: return
            val host = httpUrl.host
            val list = cookieStore.getOrPut(host) { mutableListOf() }
            for (str in strings) {
                val cookie = Cookie.parse(httpUrl, str)
                if (cookie != null) {
                    list.removeAll { it.name == cookie.name }
                    list.add(cookie)
                }
            }
        } catch (_: Exception) {}
    }

    private fun saveCookiesToPrefs() {
        val allCookies = cookieStore.values.flatten().map { it.toString() }
        val json = gson.toJson(allCookies)
        prefs.saveCookiesJson(json)
    }

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val host = url.host
            val list = cookieStore.getOrPut(host) { mutableListOf() }
            for (cookie in cookies) {
                list.removeAll { it.name == cookie.name }
                list.add(cookie)
            }
            saveCookiesToPrefs()
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val host = url.host
            val direct = cookieStore[host]
            if (!direct.isNullOrEmpty()) return direct
            return cookieStore.values.flatten()
        }
    }

    val client: OkHttpClient = createUnsafeOkHttpClient()

    private fun createUnsafeOkHttpClient(): OkHttpClient {
        return try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val original = chain.request()
                    val appOrigin = baseUrl.trimEnd('/').let {
                        when {
                            it.startsWith("http://") -> it.replace("http://", "https://")
                            it.startsWith("https://") -> it
                            it.isNotEmpty() -> "https://$it"
                            else -> it
                        }
                    }

                    val builder = original.newBuilder()
                    if (original.header("Origin") == null && appOrigin.isNotEmpty()) {
                        builder.header("Origin", appOrigin)
                    }
                    if (original.header("User-Agent") == null) {
                        builder.header("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    }

                    chain.proceed(builder.build())
                }
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .cookieJar(cookieJar)
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()
        } catch (e: Exception) {
            OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()
        }
    }

    val baseUrl: String
        get() = prefs.serverUrl

    private fun buildUrl(path: String): String {
        var base = baseUrl.trimEnd('/')
        if (base.isNotEmpty()) {
            if (!base.startsWith("http://") && !base.startsWith("https://")) {
                base = "https://$base"
            } else if (base.startsWith("http://") && !base.substringAfter("://").contains(":")) {
                base = base.replace("http://", "https://")
            }
        }
        val p = if (path.startsWith("/")) path else "/$path"
        return "$base$p"
    }

    suspend fun testServerUrl(url: String): Result<Pair<UserMe, String>> = withContext(Dispatchers.IO) {
        val trimmed = url.trim().trimEnd('/')
        if (trimmed.isEmpty()) return@withContext Result.failure(Exception("Пустой адрес сервера"))

        val candidates = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            listOf("https://$trimmed", "http://$trimmed")
        } else {
            listOf(trimmed)
        }

        var lastException: Exception? = null
        for (candidate in candidates) {
            try {
                val request = Request.Builder()
                    .url("$candidate/api/me")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val me = gson.fromJson(body, UserMe::class.java) ?: UserMe()
                    return@withContext Result.success(Pair(me, candidate))
                } else {
                    lastException = Exception("HTTP ${response.code} (${response.message})")
                }
            } catch (e: Exception) {
                android.util.Log.e("ApiClient", "Candidate $candidate failed", e)
                lastException = e
            }
        }
        Result.failure(lastException ?: Exception("Не удалось подключиться к серверу"))
    }

    suspend fun getMe(): Result<UserMe> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(buildUrl("/api/me"))
                .get()
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val me = gson.fromJson(body, UserMe::class.java) ?: UserMe()
                Result.success(me)
            } else {
                Result.failure(Exception("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(username: String, password: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JsonObject().apply {
                addProperty("username", username)
                addProperty("password", password)
            }
            val request = Request.Builder()
                .url(buildUrl("/api/auth/login"))
                .post(json.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(buildUrl("/api/auth/logout"))
                .post("{}".toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            cookieStore.clear()
            prefs.saveCookiesJson("")
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            cookieStore.clear()
            prefs.saveCookiesJson("")
            Result.failure(e)
        }
    }

    suspend fun updateLogin(newLogin: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JsonObject().apply {
                addProperty("login", newLogin)
            }
            val request = Request.Builder()
                .url(buildUrl("/api/account/login"))
                .post(json.toString().toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception(if (response.code == 409) "login_taken" else "error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePassword(currentPass: String, newPass: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JsonObject().apply {
                addProperty("currentPassword", currentPass)
                addProperty("newPassword", newPass)
            }
            val request = Request.Builder()
                .url(buildUrl("/api/account/password"))
                .post(json.toString().toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("wrong_password"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadAvatar(bytes: ByteArray, mimeType: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val json = JsonObject().apply {
                addProperty("type", mimeType)
                addProperty("dataBase64", base64)
            }
            val request = Request.Builder()
                .url(buildUrl("/api/account/avatar"))
                .post(json.toString().toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchApiBlob(path: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(buildUrl(path)).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
            val json = gson.fromJson(body, JsonObject::class.java)
            Result.success(Base64.decode(json.get("dataBase64").asString, Base64.DEFAULT))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun moderate(action: String, roomName: String, identity: String): Result<Boolean> = withContext(Dispatchers.IO) {
        postJson("/api/room/$action", JsonObject().apply {
            addProperty("roomName", roomName)
            addProperty("identity", identity)
        })
    }

    suspend fun endRoom(roomName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        postJson("/api/room/end", JsonObject().apply { addProperty("roomName", roomName) })
    }

    suspend fun record(action: String, roomName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = buildUrl("/api/record/$action?roomName=${java.net.URLEncoder.encode(roomName, "UTF-8")}")
            val response = client.newCall(Request.Builder().url(url).get().build()).execute()
            if (response.isSuccessful) Result.success(true) else Result.failure(Exception("HTTP ${response.code}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadRoomFileChunk(
        uploadId: String,
        index: Int,
        totalChunks: Int,
        name: String,
        type: String,
        chunk: ByteArray,
    ): Result<Attachment?> = withContext(Dispatchers.IO) {
        try {
            val json = JsonObject().apply {
                addProperty("uploadId", uploadId)
                addProperty("index", index)
                addProperty("totalChunks", totalChunks)
                addProperty("name", name)
                addProperty("type", type)
                addProperty("chunkBase64", Base64.encodeToString(chunk, Base64.NO_WRAP))
            }
            val request = Request.Builder()
                .url(buildUrl("/api/room-files"))
                .post(json.toString().toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            when {
                response.code == 413 -> Result.failure(Exception("too_large"))
                !response.isSuccessful -> Result.failure(Exception("HTTP ${response.code}"))
                index == totalChunks - 1 -> Result.success(gson.fromJson(body, Attachment::class.java))
                else -> Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun postJson(path: String, json: JsonObject): Result<Boolean> = try {
        val request = Request.Builder()
            .url(buildUrl(path))
            .post(json.toString().toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).execute()
        if (response.isSuccessful) Result.success(true) else Result.failure(Exception("HTTP ${response.code}"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun roomLink(roomName: String): String =
        "${buildUrl("/rooms/")}${java.net.URLEncoder.encode(roomName, "UTF-8").replace("+", "%20")}"

    suspend fun createRoom(name: String, password: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JsonObject().apply {
                addProperty("name", name)
                addProperty("password", password)
            }
            val request = Request.Builder()
                .url(buildUrl("/api/rooms"))
                .post(json.toString().toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception(if (response.code == 409) "room_exists" else "error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRoomInfo(roomName: String): Result<RoomInfoResponse> = withContext(Dispatchers.IO) {
        try {
            val url = buildUrl("/api/room-info?roomName=${java.net.URLEncoder.encode(roomName, "UTF-8")}")
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val info = gson.fromJson(body, RoomInfoResponse::class.java)
                Result.success(info)
            } else {
                Result.failure(Exception("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getConnectionDetails(
        roomName: String,
        participantName: String,
        password: String? = null,
        region: String? = null
    ): Result<ConnectionDetailsResponse> = withContext(Dispatchers.IO) {
        try {
            var url = "${buildUrl("/api/connection-details")}?roomName=${java.net.URLEncoder.encode(roomName, "UTF-8")}&participantName=${java.net.URLEncoder.encode(participantName, "UTF-8")}"
            if (!password.isNullOrBlank()) {
                url += "&password=${java.net.URLEncoder.encode(password, "UTF-8")}"
            }
            if (!region.isNullOrBlank()) {
                url += "&region=${java.net.URLEncoder.encode(region, "UTF-8")}"
            }

            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val details = gson.fromJson(body, ConnectionDetailsResponse::class.java)
                Result.success(details)
            } else {
                var errStr = "HTTP ${response.code}"
                try {
                    val jsonObj = gson.fromJson(body, JsonObject::class.java)
                    if (jsonObj.has("error")) errStr = jsonObj.get("error").asString
                } catch (_: Exception) {}
                Result.failure(Exception(errStr))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAdminUsers(): Result<List<AdminUser>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(buildUrl("/api/admin/users")).get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: "[]"
                val type = object : TypeToken<List<AdminUser>>() {}.type
                val list: List<AdminUser> = gson.fromJson(body, type)
                Result.success(list)
            } else {
                Result.failure(Exception("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createAdminUser(login: String, pass: String, isAdmin: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JsonObject().apply {
                addProperty("login", login)
                addProperty("password", pass)
                addProperty("isAdmin", isAdmin)
            }
            val request = Request.Builder()
                .url(buildUrl("/api/admin/users"))
                .post(json.toString().toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) Result.success(true) else Result.failure(Exception("HTTP ${response.code}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changeUserRole(userId: Int, makeAdmin: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JsonObject().apply {
                addProperty("isAdmin", makeAdmin)
            }
            val request = Request.Builder()
                .url(buildUrl("/api/admin/users/$userId"))
                .patch(json.toString().toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAdminUser(userId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(buildUrl("/api/admin/users/$userId"))
                .delete()
                .build()
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAdminRooms(): Result<List<AdminRoom>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(buildUrl("/api/admin/rooms")).get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: "[]"
                val type = object : TypeToken<List<AdminRoom>>() {}.type
                val list: List<AdminRoom> = gson.fromJson(body, type)
                Result.success(list)
            } else {
                Result.failure(Exception("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createAdminRoom(name: String, pass: String?, adminOnly: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JsonObject().apply {
                addProperty("name", name)
                if (!pass.isNullOrBlank()) addProperty("password", pass)
                addProperty("adminOnly", adminOnly)
            }
            val request = Request.Builder()
                .url(buildUrl("/api/admin/rooms"))
                .post(json.toString().toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) Result.success(true) else Result.failure(Exception("HTTP ${response.code}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAdminRoom(roomId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(buildUrl("/api/admin/rooms/$roomId"))
                .delete()
                .build()
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAdminActiveRooms(): Result<List<ActiveRoom>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(buildUrl("/api/admin/participants")).get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: "[]"
                val type = object : TypeToken<List<ActiveRoom>>() {}.type
                val list: List<ActiveRoom> = gson.fromJson(body, type)
                Result.success(list)
            } else {
                Result.failure(Exception("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAdminParticipants(roomName: String): Result<List<Participant>> = withContext(Dispatchers.IO) {
        try {
            val url = buildUrl("/api/admin/participants?roomName=${java.net.URLEncoder.encode(roomName, "UTF-8")}")
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: "[]"
                val type = object : TypeToken<List<Participant>>() {}.type
                val list: List<Participant> = gson.fromJson(body, type)
                Result.success(list)
            } else {
                Result.failure(Exception("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun kickParticipant(roomName: String, identity: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JsonObject().apply {
                addProperty("roomName", roomName)
                addProperty("identity", identity)
            }
            val request = Request.Builder()
                .url(buildUrl("/api/admin/participants"))
                .post(json.toString().toRequestBody(jsonMediaType))
                .build()
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAdminBans(): Result<List<Ban>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(buildUrl("/api/admin/bans")).get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: "[]"
                val type = object : TypeToken<List<Ban>>() {}.type
                val list: List<Ban> = gson.fromJson(body, type)
                Result.success(list)
            } else {
                Result.failure(Exception("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unban(banId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(buildUrl("/api/admin/bans/$banId"))
                .delete()
                .build()
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
