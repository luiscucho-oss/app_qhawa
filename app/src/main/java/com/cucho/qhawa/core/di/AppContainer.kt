package com.cucho.qhawa.core.di

import android.content.Context
import com.cucho.qhawa.data.auth.AuthRepository
import com.cucho.qhawa.data.auth.DemoAuthRepository
import com.cucho.qhawa.data.auth.FirebaseAuthRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

/**
 * Repositorios que usa la app.
 * Cada integrante agrega aquí los repositorios de lo suyo (mediciones, hábitos, consejos...).
 */
interface AppContainer {
    val authRepository: AuthRepository
    val environmentRepository: com.cucho.qhawa.data.environment.EnvironmentRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val environmentRepository by lazy { com.cucho.qhawa.data.environment.MqttEnvironmentRepository(context.applicationContext) }

    override val authRepository: AuthRepository by lazy {
        // Sin google-services.json usamos una sesión local para recorrer las vistas de demostración.
        if (FirebaseApp.getApps(context).isEmpty()) {
            DemoAuthRepository()
        } else {
            FirebaseAuthRepository(FirebaseAuth.getInstance())
        }
    }
}
