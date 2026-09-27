/**
 * Главный класс Application: инициализирует SSL-провайдер Conscrypt и SDK LiveKit при старте приложения.
 */
package com.livekit.meetkit

import android.app.Application
import io.livekit.android.LiveKit
import org.conscrypt.Conscrypt
import java.security.Security

class MeetKitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Security.insertProviderAt(Conscrypt.newProvider(), 1)
        LiveKit.create(this)
    }
}
