import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.lightscout.candybox.auth.AuthService
import org.lightscout.candybox.auth.SocialCredential
import org.lightscout.candybox.auth.SocialSignIn

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
    val isSignUpSheetVisible: Boolean = false,
    val isLoginSheetVisible: Boolean = false,
    val googleAvailable: Boolean = false,
    val appleAvailable: Boolean = false,
)

sealed class SignInNavEvent {
    data object NavigateToBrowse : SignInNavEvent()
}

data class SignInCallbacks(
    val onEmailChange: (String) -> Unit = {},
    val onPasswordChange: (String) -> Unit = {},
    val clearError: () -> Unit = {},
    val onLogin: () -> Unit = {},
    val onRegister: () -> Unit = {},
    val onGoogleSignIn: () -> Unit = {},
    val onAppleSignIn: () -> Unit = {},
    val onContinueAsGuest: () -> Unit = {},
    val onResetPassword: () -> Unit = {},
    val onOpenSignUpSheet: () -> Unit = {},
    val onDismissSignUpSheet: () -> Unit = {},
    val onOpenLoginSheet: () -> Unit = {},
    val onDismissLoginSheet: () -> Unit = {},
)

class SignInViewModel(
    private val authService: AuthService,
    private val socialSignIn: SocialSignIn,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SignInUiState(googleAvailable = socialSignIn.googleAvailable, appleAvailable = socialSignIn.appleAvailable),
    )
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    private val _navEvents = Channel<SignInNavEvent>(Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

    val currentUser get() = authService.currentUser

    fun setSignUpSheetVisible(visible: Boolean) = _uiState.update {
        if (visible) it.copy(isSignUpSheetVisible = true, errorMessage = null, statusMessage = null)
        else it.copy(isSignUpSheetVisible = false)
    }

    fun setLoginSheetVisible(visible: Boolean) = _uiState.update {
        if (visible) it.copy(isLoginSheetVisible = true, errorMessage = null, statusMessage = null)
        else it.copy(isLoginSheetVisible = false)
    }

    fun onEmailChange(email: String) = _uiState.update { it.copy(email = email) }
    fun onPasswordChange(password: String) = _uiState.update { it.copy(password = password) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null, statusMessage = null) }

    fun onLogin() = submit(isRegister = false)
    fun onRegister() = submit(isRegister = true)
    fun onGoogleSignInRequested() = providerSignIn(isGoogle = true)
    fun onAppleSignInRequested() = providerSignIn(isGoogle = false)

    fun onContinueAsGuest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { authService.signInAnonymously() }
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    _navEvents.send(SignInNavEvent.NavigateToBrowse)
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.message ?: "Sign-in failed")
                    }
                }
        }
    }

    private fun submit(isRegister: Boolean) {
        val current = _uiState.value
        if (current.email.isBlank() || current.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email and password must not be empty") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, statusMessage = null) }
            runCatching {
                if (isRegister) {
                    authService.registerWithEmail(current.email.trim(), current.password)
                } else {
                    authService.signInWithEmail(current.email.trim(), current.password)
                }
            }.onSuccess {
                _uiState.update {
                    it.copy(isLoading = false, isSignUpSheetVisible = false, isLoginSheetVisible = false)
                }
                _navEvents.send(SignInNavEvent.NavigateToBrowse)
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Authentication failed")
                }
            }
        }
    }

    private fun providerSignIn(isGoogle: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, statusMessage = null) }
            runCatching {
                val credential = (if (isGoogle) socialSignIn.google() else socialSignIn.apple())
                    ?: return@runCatching false
                if (isGoogle) {
                    authService.signInWithGoogle(credential.idToken)
                } else {
                    authService.signInWithApple(credential.idToken, credential.requireNonce())
                }
                true
            }.onSuccess { signedIn ->
                if (!signedIn) {
                    // The user closed the sheet; nothing to report.
                    _uiState.update { it.copy(isLoading = false) }
                    return@onSuccess
                }
                _uiState.update {
                    it.copy(isLoading = false, isSignUpSheetVisible = false, isLoginSheetVisible = false)
                }
                _navEvents.send(SignInNavEvent.NavigateToBrowse)
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Sign-in failed. Please try again.")
                }
            }
        }
    }

    private fun SocialCredential.requireNonce(): String =
        rawNonce ?: error("Apple sign-in returned no nonce")

    fun onResetPassword() {
        val email = _uiState.value.email.trim()
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter your email to reset your password.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, statusMessage = null) }
            runCatching { authService.resetPassword(email) }
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, statusMessage = "Password reset email sent") }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.message ?: "Failed to send reset email")
                    }
                }
        }
    }
}
