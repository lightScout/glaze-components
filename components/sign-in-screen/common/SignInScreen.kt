import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

// Replace with your own theme colours
private val AppBackground = Color(0xFF0A0A0A)
private val SheetBackground = Color(0xFF1A1A1A)
private val AccentColor = Color(0xFFFF6600)

@Composable
fun SignInScreen(auth: AuthService, social: SocialSignIn, onSignedIn: () -> Unit) {
    val viewModel = viewModel { SignInViewModel(auth, social) }
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { if (viewModel.currentUser != null) onSignedIn() }
    LaunchedEffect(Unit) { viewModel.navEvents.collect { onSignedIn() } }
    SignInContent(
        uiState = uiState,
        callbacks = SignInCallbacks(
            onEmailChange = viewModel::onEmailChange,
            onPasswordChange = viewModel::onPasswordChange,
            clearError = viewModel::clearError,
            onLogin = viewModel::onLogin,
            onRegister = viewModel::onRegister,
            onGoogleSignIn = viewModel::onGoogleSignInRequested,
            onAppleSignIn = viewModel::onAppleSignInRequested,
            onContinueAsGuest = viewModel::onContinueAsGuest,
            onResetPassword = viewModel::onResetPassword,
            onOpenSignUpSheet = { viewModel.setSignUpSheetVisible(true) },
            onDismissSignUpSheet = { viewModel.setSignUpSheetVisible(false) },
            onOpenLoginSheet = { viewModel.setLoginSheetVisible(true) },
            onDismissLoginSheet = { viewModel.setLoginSheetVisible(false) },
        ),
    )
}

private data class SocialButton(val label: String, val onClick: () -> Unit)

@Composable
private fun SignInContent(uiState: SignInUiState, callbacks: SignInCallbacks) {
    val socialButtons = buildList {
        if (uiState.appleAvailable) add(SocialButton("Apple", callbacks.onAppleSignIn))
        if (uiState.googleAvailable) add(SocialButton("Google", callbacks.onGoogleSignIn))
    }

    if (uiState.isSignUpSheetVisible) {
        AuthBottomSheet(
            title = "Create account",
            submitLabel = "Sign up with Email",
            uiState = uiState,
            socialButtons = socialButtons,
            onDismiss = callbacks.onDismissSignUpSheet,
            onEmailChange = callbacks.onEmailChange,
            onPasswordChange = callbacks.onPasswordChange,
            clearError = callbacks.clearError,
            onSubmit = callbacks.onRegister,
            onResetPassword = null,
        )
    }

    if (uiState.isLoginSheetVisible) {
        AuthBottomSheet(
            title = "Welcome back",
            submitLabel = "Log in with Email",
            uiState = uiState,
            socialButtons = socialButtons,
            onDismiss = callbacks.onDismissLoginSheet,
            onEmailChange = callbacks.onEmailChange,
            onPasswordChange = callbacks.onPasswordChange,
            clearError = callbacks.clearError,
            onSubmit = callbacks.onLogin,
            onResetPassword = callbacks.onResetPassword,
        )
    }

    Box(
        modifier = Modifier.fillMaxSize().background(AppBackground),
        contentAlignment = Alignment.Center,
    ) {
        SignInLanding(uiState, socialButtons, callbacks)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthBottomSheet(
    title: String,
    submitLabel: String,
    uiState: SignInUiState,
    socialButtons: List<SocialButton>,
    onDismiss: () -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    clearError: () -> Unit,
    onSubmit: () -> Unit,
    onResetPassword: (() -> Unit)?,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        sheetMaxWidth = Dp.Unspecified,
        containerColor = SheetBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White)

            OutlinedTextField(
                value = uiState.email,
                onValueChange = { onEmailChange(it); if (uiState.errorMessage != null) clearError() },
                placeholder = {
                    Text("Email", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email, imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = authFieldColors(),
            )

            OutlinedTextField(
                value = uiState.password,
                onValueChange = { onPasswordChange(it); if (uiState.errorMessage != null) clearError() },
                placeholder = {
                    Text("Password", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password, imeAction = ImeAction.Done,
                ),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = authFieldColors(),
            )

            uiState.errorMessage?.let { Text(it, color = Color(0xFFFF6B6B), fontSize = 13.sp) }
            uiState.statusMessage?.let { Text(it, color = Color.White.copy(0.6f), fontSize = 13.sp) }

            AuthButton(label = submitLabel, enabled = !uiState.isLoading, onClick = onSubmit)
            socialButtons.forEach { button ->
                AuthButton(
                    label = "Continue with ${button.label}",
                    enabled = !uiState.isLoading,
                    containerColor = Color.White.copy(alpha = 0.08f),
                    onClick = button.onClick,
                )
            }

            if (onResetPassword != null) {
                Row(
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onResetPassword,
                    ),
                ) {
                    Text("Forgot password? ", fontSize = 12.sp, color = Color.White.copy(0.5f))
                    Text("Reset it", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SignInLanding(
    uiState: SignInUiState,
    socialButtons: List<SocialButton>,
    callbacks: SignInCallbacks,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Place your app branding here
        Spacer(Modifier.height(40.dp))

        AuthButton(
            label = "Sign up",
            enabled = !uiState.isLoading,
            onClick = callbacks.onOpenSignUpSheet,
        )
        socialButtons.forEach { button ->
            Spacer(Modifier.height(12.dp))
            AuthButton(
                label = "Continue with ${button.label}",
                enabled = !uiState.isLoading,
                containerColor = Color.White.copy(alpha = 0.18f),
                contentColor = Color.White,
                onClick = button.onClick,
            )
        }
        Spacer(Modifier.height(12.dp))
        AuthButton(
            label = "Continue as Guest",
            enabled = !uiState.isLoading,
            containerColor = Color.Transparent,
            contentColor = Color.White.copy(alpha = 0.7f),
            onClick = callbacks.onContinueAsGuest,
        )
        Spacer(Modifier.height(24.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(
                enabled = !uiState.isLoading,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = callbacks.onOpenLoginSheet,
            ),
        ) {
            Text("Already have an account? ", fontSize = 14.sp, color = Color.White.copy(0.5f))
            Text("Log in", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }

        uiState.errorMessage?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = Color(0xFFFF6B6B), fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun AuthButton(
    label: String,
    enabled: Boolean = true,
    containerColor: Color = AccentColor,
    contentColor: Color = Color.White,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.3f),
            disabledContentColor = contentColor.copy(alpha = 0.3f),
        ),
    ) {
        Text(label, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedPlaceholderColor = Color.White.copy(0.4f),
    unfocusedPlaceholderColor = Color.White.copy(0.4f),
    focusedBorderColor = Color.White.copy(0.6f),
    unfocusedBorderColor = Color.White.copy(0.15f),
    cursorColor = Color.White,
    focusedContainerColor = Color.White.copy(0.05f),
    unfocusedContainerColor = Color.White.copy(0.03f),
)
