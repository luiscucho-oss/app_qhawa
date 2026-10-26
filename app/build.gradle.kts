plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Firebase se activa solo si existe app/google-services.json (ese archivo no se sube a GitHub).
// Sin él la app compila y abre, pero iniciar sesión muestra un aviso.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.cucho.qhawa"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.cucho.qhawa"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["appLabel"] = "@string/app_name"
    }

    buildTypes {
        debug {
            if (providers.gradleProperty("qhawa.notificationCheck").orNull == "true") {
                applicationIdSuffix = ".notificationcheck"
                manifestPlaceholders["appLabel"] = "Qhawa · Verificación"
            } else if (providers.gradleProperty("qhawa.mqttPreview").orNull == "true") {
                applicationIdSuffix = ".mqtt"
                manifestPlaceholders["appLabel"] = "Qhawa MQTT"
            }
        }
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.mqtt.paho)
    implementation(libs.gson)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.kotlinx.coroutines.play.services)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// Live MQTT checks are opt-in so regular builds never require a running simulator.
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    systemProperty("qhawa.pauseTest", providers.gradleProperty("qhawa.pauseTest").getOrElse("false"))
    systemProperty("qhawa.liveMqtt", providers.gradleProperty("qhawa.liveMqtt").getOrElse("false"))
    systemProperty("qhawa.expectedPmMin", providers.gradleProperty("qhawa.expectedPmMin").getOrElse("0"))
    systemProperty("qhawa.expectedTemperature", providers.gradleProperty("qhawa.expectedTemperature").getOrElse(""))
    systemProperty("qhawa.expectedHumidity", providers.gradleProperty("qhawa.expectedHumidity").getOrElse(""))
}
