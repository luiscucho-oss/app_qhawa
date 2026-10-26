package com.cucho.qhawa.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cucho.qhawa.data.auth.AuthRepository
import com.cucho.qhawa.feature.auth.forgotpassword.ForgotPasswordRoute
import com.cucho.qhawa.feature.auth.login.LoginRoute
import com.cucho.qhawa.feature.auth.register.RegisterRoute
import com.cucho.qhawa.feature.auth.welcome.WelcomeScreen
import com.cucho.qhawa.feature.home.HomeScreen

/** Nombres de las pantallas. Cada integrante agrega aquí las suyas. */
object QhawaRoutes {
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val HOME = "home"
}

@Composable
fun QhawaNavHost(
    authRepository: AuthRepository,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    // Si ya hay una sesión abierta, la app entra directo al inicio.
    val startDestination = remember {
        if (authRepository.isLoggedIn) QhawaRoutes.HOME else QhawaRoutes.WELCOME
    }

    // Después de entrar (login o registro) se borran las pantallas de acceso:
    // "atrás" ya no vuelve al login.
    val goToHome: () -> Unit = {
        navController.navigate(QhawaRoutes.HOME) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(QhawaRoutes.WELCOME) {
            WelcomeScreen(
                onLoginClick = { navController.navigate(QhawaRoutes.LOGIN) },
                onCreateAccountClick = { navController.navigate(QhawaRoutes.REGISTER) },
            )
        }

        composable(QhawaRoutes.LOGIN) {
            LoginRoute(
                onBackClick = { navController.popBackStack() },
                onLoginSuccess = goToHome,
                onForgotPasswordClick = { navController.navigate(QhawaRoutes.FORGOT_PASSWORD) },
                // Cambia entre login y registro sin acumular pantallas: "atrás" vuelve a la bienvenida.
                onCreateAccountClick = {
                    navController.navigate(QhawaRoutes.REGISTER) { popUpTo(QhawaRoutes.WELCOME) }
                },
            )
        }

        composable(QhawaRoutes.REGISTER) {
            RegisterRoute(
                onBackClick = { navController.popBackStack() },
                onRegistered = goToHome,
                onLoginClick = {
                    navController.navigate(QhawaRoutes.LOGIN) { popUpTo(QhawaRoutes.WELCOME) }
                },
            )
        }

        composable(QhawaRoutes.FORGOT_PASSWORD) {
            ForgotPasswordRoute(onBackClick = { navController.popBackStack() })
        }

        composable(QhawaRoutes.HOME) {
            HomeScreen(
                userId = authRepository.currentUserId ?: "local",
                isDemoSession = authRepository.isDemo,
                environmentRepository = (androidx.compose.ui.platform.LocalContext.current.applicationContext as com.cucho.qhawa.QhawaApplication).container.environmentRepository,
                userName = authRepository.currentUserName,
                userEmail = authRepository.currentUserEmail,
                onSignOutClick = {
                    authRepository.signOut()
                    navController.navigate(QhawaRoutes.WELCOME) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }
    }
}
