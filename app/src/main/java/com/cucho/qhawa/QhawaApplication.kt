package com.cucho.qhawa

import android.app.Application
import com.cucho.qhawa.core.di.AppContainer
import com.cucho.qhawa.core.di.DefaultAppContainer

class QhawaApplication : Application() {

    /** Repositorios de la app. Los ViewModel los toman de aquí. */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
