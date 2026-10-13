package com.cucho.qhawa.feature.auth.register

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cucho.qhawa.R
import com.cucho.qhawa.core.common.MIN_PASSWORD_LENGTH
import com.cucho.qhawa.core.designsystem.component.QhawaPasswordField
import com.cucho.qhawa.core.designsystem.component.QhawaPrimaryButton
import com.cucho.qhawa.core.designsystem.component.QhawaTextField
import com.cucho.qhawa.core.designsystem.theme.QhawaTheme
import com.cucho.qhawa.feature.auth.AuthBanner
import com.cucho.qhawa.feature.auth.AuthScreenLayout

/** Conecta la pantalla de registro con su ViewModel. */
@Composable
fun RegisterRoute(
    onBackClick: () -> Unit,
    onRegistered: () -> Unit,
    onLoginClick: () -> Unit,
    viewModel: RegisterViewModel = viewModel(factory = RegisterViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isRegistered) {
        if (uiState.isRegistered) onRegistered()
    }

    RegisterScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onRegisterClick = viewModel::onRegisterClick,
        onLoginClick = onLoginClick,
    )
}

@Composable
fun RegisterScreen(
    uiState: RegisterUiState,
    onBackClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onRegisterClick()
    }

    AuthScreenLayout(
        title = stringResource(R.string.register_title),
        subtitle = stringResource(R.string.register_subtitle),
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        QhawaTextField(
            value = uiState.name,
            onValueChange = onNameChange,
            label = stringResource(R.string.field_name),
            leadingIcon = R.drawable.ic_person,
            errorMessage = uiState.nameError?.let { stringResource(it) },
            enabled = !uiState.isLoading,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
        )
        Spacer(Modifier.height(16.dp))
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
            helperText = stringResource(R.string.register_password_hint, MIN_PASSWORD_LENGTH),
            enabled = !uiState.isLoading,
            imeAction = ImeAction.Next,
        )
        Spacer(Modifier.height(16.dp))
        QhawaPasswordField(
            value = uiState.confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = stringResource(R.string.field_confirm_password),
            leadingIcon = R.drawable.ic_lock,
            errorMessage = uiState.confirmPasswordError?.let { stringResource(it) },
            enabled = !uiState.isLoading,
            onDone = submit,
        )

        uiState.errorMessage?.let { message ->
            Spacer(Modifier.height(16.dp))
            AuthBanner(message = stringResource(message))
        }

        Spacer(Modifier.height(24.dp))
        QhawaPrimaryButton(
            text = stringResource(R.string.register_button),
            onClick = submit,
            isLoading = uiState.isLoading,
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.register_have_account),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onLoginClick, enabled = !uiState.isLoading) {
                Text(stringResource(R.string.register_login))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterScreenPreview() {
    QhawaTheme {
        RegisterScreen(
            uiState = RegisterUiState(
                name = "Luis Cucho",
                email = "luis@correo.com",
                confirmPasswordError = R.string.error_password_mismatch,
            ),
            onBackClick = {},
            onNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onRegisterClick = {},
            onLoginClick = {},
        )
    }
}
