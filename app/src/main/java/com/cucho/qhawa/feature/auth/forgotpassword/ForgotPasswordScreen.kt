package com.cucho.qhawa.feature.auth.forgotpassword

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.cucho.qhawa.core.designsystem.component.QhawaPrimaryButton
import com.cucho.qhawa.core.designsystem.component.QhawaSecondaryButton
import com.cucho.qhawa.core.designsystem.component.QhawaTextField
import com.cucho.qhawa.core.designsystem.theme.QhawaTheme
import com.cucho.qhawa.feature.auth.AuthBanner
import com.cucho.qhawa.feature.auth.AuthScreenLayout

/** Conecta la pantalla de recuperar contraseña con su ViewModel. */
@Composable
fun ForgotPasswordRoute(
    onBackClick: () -> Unit,
    viewModel: ForgotPasswordViewModel = viewModel(factory = ForgotPasswordViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ForgotPasswordScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onEmailChange = viewModel::onEmailChange,
        onSendClick = viewModel::onSendClick,
    )
}

@Composable
fun ForgotPasswordScreen(
    uiState: ForgotPasswordUiState,
    onBackClick: () -> Unit,
    onEmailChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onSendClick()
    }

    AuthScreenLayout(
        title = stringResource(R.string.forgot_title),
        subtitle = stringResource(R.string.forgot_subtitle),
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        if (uiState.emailSent) {
            AuthBanner(message = stringResource(R.string.forgot_sent), isError = false)
            Spacer(Modifier.height(24.dp))
            QhawaSecondaryButton(
                text = stringResource(R.string.forgot_back_to_login),
                onClick = onBackClick,
            )
        } else {
            QhawaTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                label = stringResource(R.string.field_email),
                leadingIcon = R.drawable.ic_email,
                errorMessage = uiState.emailError?.let { stringResource(it) },
                enabled = !uiState.isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )

            uiState.errorMessage?.let { message ->
                Spacer(Modifier.height(16.dp))
                AuthBanner(message = stringResource(message))
            }

            Spacer(Modifier.height(24.dp))
            QhawaPrimaryButton(
                text = stringResource(R.string.forgot_button),
                onClick = submit,
                isLoading = uiState.isLoading,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ForgotPasswordScreenPreview() {
    QhawaTheme {
        ForgotPasswordScreen(
            uiState = ForgotPasswordUiState(email = "usuario@correo.com"),
            onBackClick = {},
            onEmailChange = {},
            onSendClick = {},
        )
    }
}
