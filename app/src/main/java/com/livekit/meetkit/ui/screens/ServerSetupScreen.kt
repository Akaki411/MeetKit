/**
 * Экран первоначальной настройки и проверки подключения к серверу MeetKit.
 */
package com.livekit.meetkit.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livekit.meetkit.data.ApiClient
import com.livekit.meetkit.data.ServerPreferences
import com.livekit.meetkit.ui.components.*
import com.livekit.meetkit.ui.icons.Logo
import com.livekit.meetkit.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ServerSetupScreen(
    apiClient: ApiClient,
    prefs: ServerPreferences,
    onSuccess: () -> Unit
) {
    var urlText by remember { mutableStateOf(prefs.serverUrl) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val connect = {
        if (urlText.isNotBlank()) {
            isLoading = true
            errorMessage = null
            scope.launch {
                val result = apiClient.testServerUrl(urlText)
                isLoading = false
                if (result.isSuccess) {
                    val (_, workingUrl) = result.getOrThrow()
                    prefs.serverUrl = workingUrl
                    onSuccess()
                } else {
                    val ex = result.exceptionOrNull()
                    errorMessage = "Не удалось подключиться: ${ex?.localizedMessage ?: "Ошибка сети"}"
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        WebCard(
            horizontalAlignment = Alignment.CenterHorizontally,
            padding = PaddingValues(horizontal = 24.dp, vertical = 36.dp),
            spacing = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                imageVector = Logo.LiveKitMeet,
                contentDescription = "LiveKit Meet",
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .widthIn(max = 280.dp)
                    .fillMaxWidth()
                    .aspectRatio(Logo.ASPECT_RATIO)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Подключение к серверу",
                fontSize = 20.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Укажите адрес сервера провайдера для начала работы с приложением",
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))

            WebField(
                value = urlText,
                onValueChange = { urlText = it },
                placeholder = "https://example.com",
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { connect() }),
                enabled = !isLoading
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage!!,
                    color = ErrorRed,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(21.6.dp))

            WebButton(
                onClick = { connect() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && urlText.isNotBlank()
            ) {
                WebButtonText(if (isLoading) "Подключение…" else "Подключиться")
            }
        }
    }
}
