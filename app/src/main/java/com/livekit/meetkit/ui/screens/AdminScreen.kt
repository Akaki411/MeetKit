/**
 * Экран администратора: управление пользователями, комнатами, активными вызовами и банами.
 */
package com.livekit.meetkit.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import com.livekit.meetkit.data.ApiClient
import com.livekit.meetkit.data.models.*
import com.livekit.meetkit.ui.components.*
import com.livekit.meetkit.ui.icons.Tabler
import com.livekit.meetkit.ui.theme.*
import kotlinx.coroutines.launch

private val ItemShape = RoundedCornerShape(9.6.dp)
private val AdminFieldPadding = PaddingValues(horizontal = 14.4.dp, vertical = 14.dp)

@Composable
fun AdminScreen(
    userMe: UserMe,
    apiClient: ApiClient
) {
    val scope = rememberCoroutineScope()
    val tabs = listOf("Пользователи", "Комнаты", "Активные", "Баны")
    val pagerState = rememberPagerState(pageCount = { tabs.size })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        SecondaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent,
            contentColor = TextPrimary,
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(pagerState.currentPage),
                    height = 2.dp,
                    color = TextPrimary
                )
            },
            divider = { HorizontalDivider(thickness = 1.dp, color = DarkCardBorder) }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                ) {
                    Text(
                        title,
                        fontSize = 13.sp,
                        maxLines = 1,
                        color = if (pagerState.currentPage == index) TextPrimary else TextLabel,
                        modifier = Modifier.padding(vertical = 14.dp)
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) { page ->
            when (page) {
                0 -> AdminUsersTab(userMe = userMe, apiClient = apiClient)
                1 -> AdminRoomsTab(apiClient = apiClient)
                2 -> AdminActiveRoomsTab(apiClient = apiClient)
                3 -> AdminBansTab(apiClient = apiClient)
            }
        }
    }
}

@Composable
fun RoleBadge(role: String) {
    when (role.lowercase()) {
        "owner" -> WebPill("Владелец", OwnerPillContainer, OwnerPillText, icon = Tabler.Crown)
        "admin" -> WebPill("Админ", AdminPillContainer, AdminPillText)
        "user" -> WebPill("Юзер", UserPillContainer, UserPillText)
        else -> WebPill("Гость", GuestPillContainer, GuestPillText)
    }
}

@Composable
private fun AdminItem(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glass(ItemShape, Color(0x0AFFFFFF))
            .padding(horizontal = 15.2.dp, vertical = 12.dp),
        content = content
    )
}

@Composable
private fun SectionHead(title: String, onRefresh: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WebSectionTitle(title)
        if (onRefresh != null) {
            WebIconButton(
                icon = Tabler.Refresh,
                contentDescription = "Обновить",
                onClick = onRefresh,
                size = 34.dp,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
private fun AdminPrimaryButton(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    WebButton(
        onClick = onClick,
        modifier = modifier,
        height = 44.dp,
        contentPadding = PaddingValues(horizontal = 19.2.dp),
        pressedFill = Color(0x1FFFFFFF)
    ) {
        Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
        Text(text, color = TextPrimary, fontSize = 14.4.sp)
    }
}

@Composable
private fun AdminField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    password: Boolean = false
) {
    WebField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        password = password,
        fontSize = 14.4.sp,
        contentPadding = AdminFieldPadding,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun MutedText(text: String) {
    Text(text, color = Color(0x73FFFFFF), fontSize = 14.4.sp)
}

@Composable
fun AdminUsersTab(userMe: UserMe, apiClient: ApiClient) {
    var users by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    var uLogin by remember { mutableStateOf("") }
    var uPassword by remember { mutableStateOf("") }
    var uIsAdmin by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val loadUsers = {
        scope.launch {
            val res = apiClient.getAdminUsers()
            if (res.isSuccess) users = res.getOrDefault(emptyList())
        }
    }

    LaunchedEffect(Unit) { loadUsers() }

    val isViewerOwner = userMe.role == "owner"

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            WebSectionTitle("Пользователи", modifier = Modifier.padding(bottom = 8.dp))
        }

        items(users) { u ->
            AdminItem {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                u.login,
                                color = TextPrimary,
                                fontSize = 14.4.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(Modifier.width(8.dp))
                            RoleBadge(u.role)
                        }
                        if (!u.nickname.isNullOrBlank()) {
                            Text("Ник: ${u.nickname}", fontSize = 12.8.sp, color = TextLabel)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.4.dp)) {
                        if (isViewerOwner && u.role != "owner") {
                            WebIconButton(
                                icon = if (u.role == "admin") Tabler.ShieldOff else Tabler.Shield,
                                contentDescription = if (u.role == "admin") "Разжаловать" else "Назначить админом",
                                onClick = {
                                    scope.launch {
                                        apiClient.changeUserRole(u.id, u.role != "admin")
                                        loadUsers()
                                    }
                                }
                            )
                        }

                        if (u.role != "owner" && u.login != userMe.login) {
                            WebIconButton(
                                icon = Tabler.Trash,
                                contentDescription = "Удалить",
                                danger = true,
                                onClick = {
                                    scope.launch {
                                        apiClient.deleteAdminUser(u.id)
                                        loadUsers()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            WebCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                padding = PaddingValues(16.dp),
                spacing = 9.6.dp
            ) {
                Text("Создать пользователя", color = TextLabel, fontSize = 13.sp)
                AdminField(uLogin, { uLogin = it }, "Логин")
                AdminField(uPassword, { uPassword = it }, "Пароль", password = true)

                if (isViewerOwner) {
                    WebCheck(checked = uIsAdmin, onCheckedChange = { uIsAdmin = it }, label = "Сделать администратором")
                }

                if (errorMsg != null) {
                    Text(errorMsg!!, color = ErrorRed, fontSize = 14.4.sp)
                }

                AdminPrimaryButton(
                    text = "Добавить",
                    icon = Tabler.Plus,
                    modifier = Modifier.align(Alignment.End),
                    onClick = {
                        if (uLogin.isNotBlank() && uPassword.isNotBlank()) {
                            scope.launch {
                                val res = apiClient.createAdminUser(uLogin.trim(), uPassword, uIsAdmin)
                                if (res.isSuccess) {
                                    uLogin = ""
                                    uPassword = ""
                                    uIsAdmin = false
                                    errorMsg = null
                                    loadUsers()
                                } else {
                                    errorMsg = "Ошибка создания пользователя"
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AdminRoomsTab(apiClient: ApiClient) {
    var rooms by remember { mutableStateOf<List<AdminRoom>>(emptyList()) }
    var rName by remember { mutableStateOf("") }
    var rPassword by remember { mutableStateOf("") }
    var rAdminOnly by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val loadRooms = {
        scope.launch {
            val res = apiClient.getAdminRooms()
            if (res.isSuccess) rooms = res.getOrDefault(emptyList())
        }
    }

    LaunchedEffect(Unit) { loadRooms() }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            WebSectionTitle("Комнаты", modifier = Modifier.padding(bottom = 8.dp))
        }

        items(rooms) { r ->
            AdminItem {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(r.name, color = TextPrimary, fontSize = 14.4.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(5.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(5.6.dp)) {
                            if (r.adminOnly) WebPill("Только админы", Fill08, TextSecondary)
                            if (r.hasPassword) WebPill("По паролю", Fill08, TextSecondary)
                            if (!r.adminOnly && !r.hasPassword) WebPill("Открытая", Fill08, TextSecondary)
                        }
                    }

                    WebIconButton(
                        icon = Tabler.Trash,
                        contentDescription = "Удалить",
                        danger = true,
                        onClick = {
                            scope.launch {
                                apiClient.deleteAdminRoom(r.id)
                                loadRooms()
                            }
                        }
                    )
                }
            }
        }

        item {
            WebCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                padding = PaddingValues(16.dp),
                spacing = 9.6.dp
            ) {
                Text("Создать комнату", color = TextLabel, fontSize = 13.sp)
                AdminField(rName, { rName = it }, "Название комнаты")
                AdminField(rPassword, { rPassword = it }, "Пароль (необязательно)", password = true)
                WebCheck(checked = rAdminOnly, onCheckedChange = { rAdminOnly = it }, label = "Только для админов")

                AdminPrimaryButton(
                    text = "Создать",
                    icon = Tabler.Plus,
                    modifier = Modifier.align(Alignment.End),
                    onClick = {
                        if (rName.isNotBlank()) {
                            scope.launch {
                                val res = apiClient.createAdminRoom(rName.trim(), rPassword.ifBlank { null }, rAdminOnly)
                                if (res.isSuccess) {
                                    rName = ""
                                    rPassword = ""
                                    rAdminOnly = false
                                    loadRooms()
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AdminActiveRoomsTab(apiClient: ApiClient) {
    var activeRooms by remember { mutableStateOf<List<ActiveRoom>>(emptyList()) }
    var participantsMap by remember { mutableStateOf<Map<String, List<Participant>>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    val loadActive = {
        scope.launch {
            val res = apiClient.getAdminActiveRooms()
            if (res.isSuccess) activeRooms = res.getOrDefault(emptyList())
        }
    }

    LaunchedEffect(Unit) { loadActive() }

    val loadParticipants = { roomName: String ->
        scope.launch {
            val res = apiClient.getAdminParticipants(roomName)
            if (res.isSuccess) {
                participantsMap = participantsMap + (roomName to res.getOrDefault(emptyList()))
            }
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Box(Modifier.padding(bottom = 8.dp)) {
                SectionHead("Активные комнаты", onRefresh = { loadActive() })
            }
        }

        if (activeRooms.isEmpty()) {
            item { MutedText("Нет активных звонков") }
        }

        items(activeRooms) { room ->
            AdminItem(modifier = Modifier.clickable { loadParticipants(room.name) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(room.name, color = TextPrimary, fontSize = 14.4.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text("Участников: ${room.numParticipants}", fontSize = 13.sp, color = Color(0x73FFFFFF))
                    }
                    Icon(Tabler.ChevronDown, contentDescription = null, tint = TextLabel, modifier = Modifier.size(18.dp))
                }

                val list = participantsMap[room.name]
                if (list != null) {
                    Spacer(Modifier.height(4.dp))
                    list.forEach { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 7.2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(p.name, fontSize = 14.4.sp, color = TextPrimary)
                                if (p.isAdmin) {
                                    Spacer(Modifier.width(6.4.dp))
                                    RoleBadge(role = "admin")
                                }
                            }

                            if (!p.isAdmin) {
                                WebIconButton(
                                    icon = Tabler.UserX,
                                    contentDescription = "Исключить",
                                    danger = true,
                                    onClick = {
                                        scope.launch {
                                            apiClient.kickParticipant(room.name, p.identity)
                                            loadParticipants(room.name)
                                            loadActive()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminBansTab(apiClient: ApiClient) {
    var bans by remember { mutableStateOf<List<Ban>>(emptyList()) }
    val scope = rememberCoroutineScope()

    val loadBans = {
        scope.launch {
            val res = apiClient.getAdminBans()
            if (res.isSuccess) bans = res.getOrDefault(emptyList())
        }
    }

    LaunchedEffect(Unit) { loadBans() }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Box(Modifier.padding(bottom = 8.dp)) {
                SectionHead("Заблокированные IP", onRefresh = { loadBans() })
            }
        }

        if (bans.isEmpty()) {
            item { MutedText("Нет банов") }
        }

        items(bans) { b ->
            AdminItem {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(b.name ?: b.login ?: "Неизвестно", color = TextPrimary, fontSize = 14.4.sp)
                        Text("IP: ${b.ip}", fontSize = 12.8.sp, color = TextLabel)
                        if (!b.roomName.isNullOrBlank()) {
                            Text("Комната: ${b.roomName}", fontSize = 12.8.sp, color = Color(0x73FFFFFF))
                        }
                    }

                    WebIconButton(
                        icon = Tabler.Trash,
                        contentDescription = "Разблокировать",
                        danger = true,
                        onClick = {
                            scope.launch {
                                apiClient.unban(b.id)
                                loadBans()
                            }
                        }
                    )
                }
            }
        }
    }
}
