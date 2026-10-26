package com.cucho.qhawa.feature.auth.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cucho.qhawa.R
import com.cucho.qhawa.core.designsystem.component.QhawaLogo
import com.cucho.qhawa.core.designsystem.component.QhawaPrimaryButton
import com.cucho.qhawa.core.designsystem.component.QhawaSecondaryButton
import com.cucho.qhawa.core.designsystem.theme.QhawaTheme
import com.cucho.qhawa.core.designsystem.theme.qhawaScreenBackground

@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Scaffold(
        modifier = modifier.qhawaScreenBackground(),
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            QhawaLogo()
            Spacer(Modifier.height(24.dp))

            Image(painterResource(R.drawable.qhawa_home_air), "El aire y el hogar", Modifier.fillMaxWidth().height(210.dp))

            Spacer(Modifier.height(28.dp))
            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineLarge,
                color = colors.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.welcome_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(32.dp))
            QhawaPrimaryButton(
                text = stringResource(R.string.welcome_login),
                onClick = onLoginClick,
            )
            Spacer(Modifier.height(12.dp))
            QhawaSecondaryButton(
                text = stringResource(R.string.welcome_create_account),
                onClick = onCreateAccountClick,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomeScreenPreview() {
    QhawaTheme {
        WelcomeScreen(onLoginClick = {}, onCreateAccountClick = {})
    }
}
