package com.cucho.qhawa.core.di

import android.content.Context
import com.cucho.qhawa.data.auth.AuthRepository
import com.cucho.qhawa.data.auth.DemoAuthRepository
import com.cucho.qhawa.data.auth.FirebaseAuthRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

/**
 * Repositorios que usa la app.
 * Cada integrante agrega aquí los repositorios de lo suyo (mediciones, mascotas, consejos...).
 */
interface AppContainer {
    val authRepository: AuthRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val authRepository: AuthRepository by lazy {
        // Sin google-services.json usamos una sesión local para recorrer las vistas de demostración.
        if (FirebaseApp.getApps(context).isEmpty()) {
            DemoAuthRepository()
        } else {
            FirebaseAuthRepository(FirebaseAuth.getInstance())
        }
    }
}
