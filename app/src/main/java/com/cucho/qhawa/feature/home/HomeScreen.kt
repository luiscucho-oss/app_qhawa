package com.cucho.qhawa.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cucho.qhawa.R
import com.cucho.qhawa.core.designsystem.component.QhawaTopBar

/**
 * TEMPORAL: quien haga la pantalla de Inicio la reemplaza por la real, con las métricas.
 * Por ahora sirve para comprobar que el login funciona y que "Cerrar sesión" (menú del perfil) cierra la sesión.
 */
@Composable
fun HomeScreen(
    userName: String?,
    userEmail: String?,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            QhawaTopBar(
                screenTitle = stringResource(R.string.nav_home),
                isDeviceOnline = true,
                userName = userName,
                userEmail = userEmail,
                onNotificationsClick = {},
                onSignOutClick = onSignOutClick,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.home_placeholder_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            if (userEmail != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = userEmail,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
