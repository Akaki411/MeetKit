/**
 * Набор векторных иконок Tabler Icons, используемых в интерфейсе.
 */
package com.livekit.meetkit.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

object Tabler {
    val User: ImageVector by lazy {
        outline("User", listOf(
        "M8 7a4 4 0 1 0 8 0a4 4 0 0 0 -8 0",
        "M6 21v-2a4 4 0 0 1 4 -4h4a4 4 0 0 1 4 4v2",
        ))
    }

    val Login: ImageVector by lazy {
        outline("Login", listOf(
        "M15 8v-2a2 2 0 0 0 -2 -2h-7a2 2 0 0 0 -2 2v12a2 2 0 0 0 2 2h7a2 2 0 0 0 2 -2v-2",
        "M21 12h-13l3 -3",
        "M11 15l-3 -3",
        ))
    }

    val Logout: ImageVector by lazy {
        outline("Logout", listOf(
        "M14 8v-2a2 2 0 0 0 -2 -2h-7a2 2 0 0 0 -2 2v12a2 2 0 0 0 2 2h7a2 2 0 0 0 2 -2v-2",
        "M9 12h12l-3 -3",
        "M18 15l3 -3",
        ))
    }

    val LayoutDashboard: ImageVector by lazy {
        outline("LayoutDashboard", listOf(
        "M5 4h4a1 1 0 0 1 1 1v6a1 1 0 0 1 -1 1h-4a1 1 0 0 1 -1 -1v-6a1 1 0 0 1 1 -1",
        "M5 16h4a1 1 0 0 1 1 1v2a1 1 0 0 1 -1 1h-4a1 1 0 0 1 -1 -1v-2a1 1 0 0 1 1 -1",
        "M15 12h4a1 1 0 0 1 1 1v6a1 1 0 0 1 -1 1h-4a1 1 0 0 1 -1 -1v-6a1 1 0 0 1 1 -1",
        "M15 4h4a1 1 0 0 1 1 1v2a1 1 0 0 1 -1 1h-4a1 1 0 0 1 -1 -1v-2a1 1 0 0 1 1 -1",
        ))
    }

    val DeviceFloppy: ImageVector by lazy {
        outline("DeviceFloppy", listOf(
        "M6 4h10l4 4v10a2 2 0 0 1 -2 2h-12a2 2 0 0 1 -2 -2v-12a2 2 0 0 1 2 -2",
        "M10 14a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
        "M14 4l0 4l-6 0l0 -4",
        ))
    }

    val Lock: ImageVector by lazy {
        outline("Lock", listOf(
        "M5 13a2 2 0 0 1 2 -2h10a2 2 0 0 1 2 2v6a2 2 0 0 1 -2 2h-10a2 2 0 0 1 -2 -2v-6",
        "M11 16a1 1 0 1 0 2 0a1 1 0 0 0 -2 0",
        "M8 11v-4a4 4 0 1 1 8 0v4",
        ))
    }

    val LockOpen2: ImageVector by lazy {
        outline("LockOpen2", listOf(
        "M3 13a2 2 0 0 1 2 -2h10a2 2 0 0 1 2 2v6a2 2 0 0 1 -2 2h-10a2 2 0 0 1 -2 -2l0 -6",
        "M9 16a1 1 0 1 0 2 0a1 1 0 0 0 -2 0",
        "M13 11v-4a4 4 0 1 1 8 0v4",
        ))
    }

    val X: ImageVector by lazy {
        outline("X", listOf(
        "M18 6l-12 12",
        "M6 6l12 12",
        ))
    }

    val Eye: ImageVector by lazy {
        outline("Eye", listOf(
        "M10 12a2 2 0 1 0 4 0a2 2 0 0 0 -4 0",
        "M21 12c-2.4 4 -5.4 6 -9 6c-3.6 0 -6.6 -2 -9 -6c2.4 -4 5.4 -6 9 -6c3.6 0 6.6 2 9 6",
        ))
    }

    val EyeOff: ImageVector by lazy {
        outline("EyeOff", listOf(
        "M10.585 10.587a2 2 0 0 0 2.829 2.828",
        "M16.681 16.673a8.717 8.717 0 0 1 -4.681 1.327c-3.6 0 -6.6 -2 -9 -6c1.272 -2.12 2.712 -3.678 4.32 -4.674m2.86 -1.146a9.055 9.055 0 0 1 1.82 -.18c3.6 0 6.6 2 9 6c-.666 1.11 -1.379 2.067 -2.138 2.87",
        "M3 3l18 18",
        ))
    }

    val Microphone: ImageVector by lazy {
        outline("Microphone", listOf(
        "M9 5a3 3 0 0 1 3 -3a3 3 0 0 1 3 3v5a3 3 0 0 1 -3 3a3 3 0 0 1 -3 -3l0 -5",
        "M5 10a7 7 0 0 0 14 0",
        "M8 21l8 0",
        "M12 17l0 4",
        ))
    }

    val MicrophoneOff: ImageVector by lazy {
        outline("MicrophoneOff", listOf(
        "M3 3l18 18",
        "M9 5a3 3 0 0 1 6 0v5a3 3 0 0 1 -.13 .874m-2 2a3 3 0 0 1 -3.87 -2.872v-1",
        "M5 10a7 7 0 0 0 10.846 5.85m2 -2a6.967 6.967 0 0 0 1.152 -3.85",
        "M8 21l8 0",
        "M12 17l0 4",
        ))
    }

    val Video: ImageVector by lazy {
        outline("Video", listOf(
        "M15 10l4.553 -2.276a1 1 0 0 1 1.447 .894v6.764a1 1 0 0 1 -1.447 .894l-4.553 -2.276v-4",
        "M3 8a2 2 0 0 1 2 -2h8a2 2 0 0 1 2 2v8a2 2 0 0 1 -2 2h-8a2 2 0 0 1 -2 -2l0 -8",
        ))
    }

    val VideoOff: ImageVector by lazy {
        outline("VideoOff", listOf(
        "M3 3l18 18",
        "M15 11v-1l4.553 -2.276a1 1 0 0 1 1.447 .894v6.764a1 1 0 0 1 -.675 .946",
        "M10 6h3a2 2 0 0 1 2 2v3m0 4v1a2 2 0 0 1 -2 2h-8a2 2 0 0 1 -2 -2v-8a2 2 0 0 1 2 -2h1",
        ))
    }

    val PhoneOff: ImageVector by lazy {
        outline("PhoneOff", listOf(
        "M3 21l18 -18",
        "M5.831 14.161a15.946 15.946 0 0 1 -2.831 -8.161a2 2 0 0 1 2 -2h4l2 5l-2.5 1.5c.108 .22 .223 .435 .345 .645m1.751 2.277c.843 .84 1.822 1.544 2.904 2.078l1.5 -2.5l5 2v4a2 2 0 0 1 -2 2a15.963 15.963 0 0 1 -10.344 -4.657",
        ))
    }

    val Settings: ImageVector by lazy {
        outline("Settings", listOf(
        "M10.325 4.317c.426 -1.756 2.924 -1.756 3.35 0a1.724 1.724 0 0 0 2.573 1.066c1.543 -.94 3.31 .826 2.37 2.37a1.724 1.724 0 0 0 1.065 2.572c1.756 .426 1.756 2.924 0 3.35a1.724 1.724 0 0 0 -1.066 2.573c.94 1.543 -.826 3.31 -2.37 2.37a1.724 1.724 0 0 0 -2.572 1.065c-.426 1.756 -2.924 1.756 -3.35 0a1.724 1.724 0 0 0 -2.573 -1.066c-1.543 .94 -3.31 -.826 -2.37 -2.37a1.724 1.724 0 0 0 -1.065 -2.572c-1.756 -.426 -1.756 -2.924 0 -3.35a1.724 1.724 0 0 0 1.066 -2.573c-.94 -1.543 .826 -3.31 2.37 -2.37c1 .608 2.296 .07 2.572 -1.065",
        "M9 12a3 3 0 1 0 6 0a3 3 0 0 0 -6 0",
        ))
    }

    val Share: ImageVector by lazy {
        outline("Share", listOf(
        "M3 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0",
        "M15 6a3 3 0 1 0 6 0a3 3 0 1 0 -6 0",
        "M15 18a3 3 0 1 0 6 0a3 3 0 1 0 -6 0",
        "M8.7 10.7l6.6 -3.4",
        "M8.7 13.3l6.6 3.4",
        ))
    }

    val Users: ImageVector by lazy {
        outline("Users", listOf(
        "M5 7a4 4 0 1 0 8 0a4 4 0 1 0 -8 0",
        "M3 21v-2a4 4 0 0 1 4 -4h4a4 4 0 0 1 4 4v2",
        "M16 3.13a4 4 0 0 1 0 7.75",
        "M21 21v-2a4 4 0 0 0 -3 -3.85",
        ))
    }

    val Message: ImageVector by lazy {
        outline("Message", listOf(
        "M8 9h8",
        "M8 13h6",
        "M18 4a3 3 0 0 1 3 3v8a3 3 0 0 1 -3 3h-5l-5 3v-3h-2a3 3 0 0 1 -3 -3v-8a3 3 0 0 1 3 -3h12",
        ))
    }

    val Send: ImageVector by lazy {
        outline("Send", listOf(
        "M10 14l11 -11",
        "M21 3l-6.5 18a.55 .55 0 0 1 -1 0l-3.5 -7l-7 -3.5a.55 .55 0 0 1 0 -1l18 -6.5",
        ))
    }

    val Paperclip: ImageVector by lazy {
        outline("Paperclip", listOf(
        "M15 7l-6.5 6.5a1.5 1.5 0 0 0 3 3l6.5 -6.5a3 3 0 0 0 -6 -6l-6.5 6.5a4.5 4.5 0 0 0 9 9l6.5 -6.5",
        ))
    }

    val Loader2: ImageVector by lazy {
        outline("Loader2", listOf(
        "M12 3a9 9 0 1 0 9 9",
        ))
    }

    val MoodSmile: ImageVector by lazy {
        outline("MoodSmile", listOf(
        "M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0",
        "M9 10l.01 0",
        "M15 10l.01 0",
        "M9.5 15a3.5 3.5 0 0 0 5 0",
        ))
    }

    val ArrowBackUp: ImageVector by lazy {
        outline("ArrowBackUp", listOf(
        "M9 14l-4 -4l4 -4",
        "M5 10h11a4 4 0 1 1 0 8h-1",
        ))
    }

    val File: ImageVector by lazy {
        outline("File", listOf(
        "M14 3v4a1 1 0 0 0 1 1h4",
        "M17 21h-10a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2h7l5 5v11a2 2 0 0 1 -2 2",
        ))
    }

    val Crown: ImageVector by lazy {
        outline("Crown", listOf(
        "M12 6l4 6l5 -4l-2 10h-14l-2 -10l5 4l4 -6",
        ))
    }

    val Shield: ImageVector by lazy {
        outline("Shield", listOf(
        "M12 3a12 12 0 0 0 8.5 3a12 12 0 0 1 -8.5 15a12 12 0 0 1 -8.5 -15a12 12 0 0 0 8.5 -3",
        ))
    }

    val ShieldOff: ImageVector by lazy {
        outline("ShieldOff", listOf(
        "M17.67 17.667a12 12 0 0 1 -5.67 3.333a12 12 0 0 1 -8.5 -15c.794 .036 1.583 -.006 2.357 -.124m3.128 -.926a11.997 11.997 0 0 0 3.015 -1.95a12 12 0 0 0 8.5 3a12 12 0 0 1 -1.116 9.376",
        "M3 3l18 18",
        ))
    }

    val Trash: ImageVector by lazy {
        outline("Trash", listOf(
        "M4 7l16 0",
        "M10 11l0 6",
        "M14 11l0 6",
        "M5 7l1 12a2 2 0 0 0 2 2h8a2 2 0 0 0 2 -2l1 -12",
        "M9 7v-3a1 1 0 0 1 1 -1h4a1 1 0 0 1 1 1v3",
        ))
    }

    val Plus: ImageVector by lazy {
        outline("Plus", listOf(
        "M12 5l0 14",
        "M5 12l14 0",
        ))
    }

    val Refresh: ImageVector by lazy {
        outline("Refresh", listOf(
        "M20 11a8.1 8.1 0 0 0 -15.5 -2m-.5 -4v4h4",
        "M4 13a8.1 8.1 0 0 0 15.5 2m.5 4v-4h-4",
        ))
    }

    val UserX: ImageVector by lazy {
        outline("UserX", listOf(
        "M8 7a4 4 0 1 0 8 0a4 4 0 0 0 -8 0",
        "M6 21v-2a4 4 0 0 1 4 -4h3.5",
        "M22 22l-5 -5",
        "M17 22l5 -5",
        ))
    }

    val Ban: ImageVector by lazy {
        outline("Ban", listOf(
        "M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0",
        "M5.7 5.7l12.6 12.6",
        ))
    }

    val Volume: ImageVector by lazy {
        outline("Volume", listOf(
        "M15 8a5 5 0 0 1 0 8",
        "M17.7 5a9 9 0 0 1 0 14",
        "M6 15h-2a1 1 0 0 1 -1 -1v-4a1 1 0 0 1 1 -1h2l3.5 -4.5a.8 .8 0 0 1 1.5 .5v14a.8 .8 0 0 1 -1.5 .5l-3.5 -4.5",
        ))
    }

    val VolumeOff: ImageVector by lazy {
        outline("VolumeOff", listOf(
        "M15 8a5 5 0 0 1 1.912 4.934m-1.377 2.602a5 5 0 0 1 -.535 .464",
        "M17.7 5a9 9 0 0 1 2.362 11.086m-1.676 2.299a9 9 0 0 1 -.686 .615",
        "M9.069 5.054l.431 -.554a.8 .8 0 0 1 1.5 .5v2m0 4v8a.8 .8 0 0 1 -1.5 .5l-3.5 -4.5h-2a1 1 0 0 1 -1 -1v-4a1 1 0 0 1 1 -1h2l1.294 -1.664",
        "M3 3l18 18",
        ))
    }

    val CameraRotate: ImageVector by lazy {
        outline("CameraRotate", listOf(
        "M5 7h1a2 2 0 0 0 2 -2a1 1 0 0 1 1 -1h6a1 1 0 0 1 1 1a2 2 0 0 0 2 2h1a2 2 0 0 1 2 2v9a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-9a2 2 0 0 1 2 -2",
        "M11.245 15.904a3 3 0 0 0 3.755 -2.904m-2.25 -2.905a3 3 0 0 0 -3.75 2.905",
        "M14 13h2v2",
        "M10 13h-2v-2",
        ))
    }

    val ChevronUp: ImageVector by lazy {
        outline("ChevronUp", listOf(
        "M6 15l6 -6l6 6",
        ))
    }

    val Server: ImageVector by lazy {
        outline("Server", listOf(
        "M3 7a3 3 0 0 1 3 -3h12a3 3 0 0 1 3 3v2a3 3 0 0 1 -3 3h-12a3 3 0 0 1 -3 -3v-2",
        "M3 15a3 3 0 0 1 3 -3h12a3 3 0 0 1 3 3v2a3 3 0 0 1 -3 3h-12a3 3 0 0 1 -3 -3l0 -2",
        "M7 8l0 .01",
        "M7 16l0 .01",
        ))
    }

    val Download: ImageVector by lazy {
        outline("Download", listOf(
        "M4 17v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2 -2v-2",
        "M7 11l5 5l5 -5",
        "M12 4l0 12",
        ))
    }

    val Phone: ImageVector by lazy {
        outline("Phone", listOf(
        "M5 4h4l2 5l-2.5 1.5a11 11 0 0 0 5 5l1.5 -2.5l5 2v4a2 2 0 0 1 -2 2a16 16 0 0 1 -15 -15a2 2 0 0 1 2 -2",
        ))
    }

    val ChevronDown: ImageVector by lazy {
        outline("ChevronDown", listOf(
        "M6 9l6 6l6 -6",
        ))
    }

    val CircleCheck: ImageVector by lazy {
        outline("CircleCheck", listOf(
        "M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0",
        "M9 12l2 2l4 -4",
        ))
    }

    val Pencil: ImageVector by lazy {
        outline("Pencil", listOf(
        "M4 20h4l10.5 -10.5a2.828 2.828 0 1 0 -4 -4l-10.5 10.5v4",
        "M13.5 6.5l4 4",
        ))
    }

    val ScreenShare: ImageVector by lazy {
        outline("ScreenShare", listOf(
        "M21 12v3a1 1 0 0 1 -1 1h-16a1 1 0 0 1 -1 -1v-10a1 1 0 0 1 1 -1h9",
        "M7 20l10 0",
        "M9 16l0 4",
        "M15 16l0 4",
        "M17 4h4v4",
        "M16 9l5 -5",
        ))
    }

    val ScreenShareOff: ImageVector by lazy {
        outline("ScreenShareOff", listOf(
        "M21 12v3a1 1 0 0 1 -1 1h-16a1 1 0 0 1 -1 -1v-10a1 1 0 0 1 1 -1h9",
        "M7 20l10 0",
        "M9 16l0 4",
        "M15 16l0 4",
        "M17 8l4 -4m-4 0l4 4",
        ))
    }

    val NoiseReduction: ImageVector by lazy {
        outline("NoiseReduction", listOf(
        "M21 12a9 9 0 1 1 -18 0a9 9 0 0 1 18 0",
        "M10.01 18h-.01",
        "M14.01 14h-.01",
        "M16.01 12h-.01",
        "M18.01 10h-.01",
        "M16.01 16h-.01",
        "M14.01 18h-.01",
        "M18.01 14h-.01",
        "M12.01 16h-.01",
        ))
    }

    val ZoomIn: ImageVector by lazy {
        outline("ZoomIn", listOf(
        "M3 10a7 7 0 1 0 14 0a7 7 0 1 0 -14 0",
        "M7 10l6 0",
        "M10 7l0 6",
        "M21 21l-6 -6",
        ))
    }

    val ZoomOut: ImageVector by lazy {
        outline("ZoomOut", listOf(
        "M3 10a7 7 0 1 0 14 0a7 7 0 1 0 -14 0",
        "M7 10l6 0",
        "M21 21l-6 -6",
        ))
    }

    val Check: ImageVector by lazy {
        outline("Check", listOf(
        "M5 12l5 5l10 -10",
        ))
    }

    val Upload: ImageVector by lazy {
        outline("Upload", listOf(
        "M4 17v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2 -2v-2",
        "M7 9l5 -5l5 5",
        "M12 4l0 12",
        ))
    }

    val CameraFilled: ImageVector by lazy {
        filled("CameraFilled", listOf(
        "M15 3a2 2 0 0 1 1.995 1.85l.005 .15a1 1 0 0 0 .883 .993l.117 .007h1a3 3 0 0 1 2.995 2.824l.005 .176v9a3 3 0 0 1 -2.824 2.995l-.176 .005h-14a3 3 0 0 1 -2.995 -2.824l-.005 -.176v-9a3 3 0 0 1 2.824 -2.995l.176 -.005h1a1 1 0 0 0 1 -1a2 2 0 0 1 1.85 -1.995l.15 -.005h6zm-3 7a3 3 0 0 0 -2.985 2.698l-.011 .152l-.004 .15l.004 .15a3 3 0 1 0 2.996 -3.15z",
        ))
    }

    val ChevronDownFilled: ImageVector by lazy {
        filled("ChevronDownFilled", listOf(
        "M18.707 8.293a1 1 0 0 1 0 1.414l-6 6a1 1 0 0 1 -1.414 0l-6 -6a1 1 0 0 1 1.414 -1.414l5.293 5.293l5.293 -5.293a1 1 0 0 1 1.414 0",
        ))
    }
}

private fun outline(name: String, paths: List<String>): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        paths.forEach { d ->
            addPath(
                pathData = PathParser().parsePathString(d).toNodes(),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

private fun filled(name: String, paths: List<String>): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        paths.forEach { d ->
            addPath(pathData = PathParser().parsePathString(d).toNodes(), fill = SolidColor(Color.Black))
        }
    }.build()
