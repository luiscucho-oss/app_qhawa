package com.cucho.qhawa.feature.auth.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cucho.qhawa.R
import com.cucho.qhawa.core.designsystem.component.QhawaPasswordField
import com.cucho.qhawa.core.designsystem.component.QhawaPrimaryButton
import com.cucho.qhawa.core.designsystem.component.QhawaTextField
import com.cucho.qhawa.core.designsystem.theme.QhawaTheme
import com.cucho.qhawa.feature.auth.AuthBanner
import com.cucho.qhawa.feature.auth.AuthScreenLayout

/** Conecta la pantalla de login con su ViewModel. */
@Composable
fun LoginRoute(
    onBackClick: () -> Unit,
    onLoginSuccess: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoginSuccess()
    }

    LoginScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLoginClick = viewModel::onLoginClick,
        onForgotPasswordClick = onForgotPasswordClick,
        onCreateAccountClick = onCreateAccountClick,
    )
}

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onBackClick: () -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onLoginClick()
    }

    AuthScreenLayout(
        title = stringResource(R.string.login_title),
        subtitle = stringResource(R.string.login_subtitle),
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        QhawaTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.field_email),
            leadingIcon = R.drawable.ic_email,
            errorMessage = uiState.emailError?.let { stringResource(it) },
            enabled = !uiState.isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        Spacer(Modifier.height(16.dp))
        QhawaPasswordField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.field_password),
            leadingIcon = R.drawable.ic_lock,
            errorMessage = uiState.passwordError?.let { stringResource(it) },
            enabled = !uiState.isLoading,
            onDone = submit,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onForgotPasswordClick, enabled = !uiState.isLoading) {
                Text(stringResource(R.string.login_forgot_password))
            }
        }

        uiState.errorMessage?.let { message ->
            Spacer(Modifier.height(8.dp))
            AuthBanner(message = stringResource(message))
        }

        Spacer(Modifier.height(20.dp))
        QhawaPrimaryButton(
            text = stringResource(R.string.login_button),
            onClick = submit,
            isLoading = uiState.isLoading,
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.login_no_account),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onCreateAccountClick, enabled = !uiState.isLoading) {
                Text(stringResource(R.string.login_create_account))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    QhawaTheme {
        LoginScreen(
            uiState = LoginUiState(
                email = "usuario@correo.com",
                errorMessage = R.string.error_invalid_credentials,
            ),
            onBackClick = {},
            onEmailChange = {},
            onPasswordChange = {},
            onLoginClick = {},
            onForgotPasswordClick = {},
            onCreateAccountClick = {},
        )
    }
}
