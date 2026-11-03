import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Sign in with Apple, in a shape Swift can implement: Kotlin/Native lets Swift call a suspend
 * function but not implement one, so the Swift class answers through callbacks and
 * [IosSocialSignIn] adapts it back. The Swift side owns `ASAuthorizationController` and the
 * nonce: it sends the nonce's SHA-256 to Apple and hands back the raw one for Firebase.
 */
interface IosAppleSignIn {
    fun signIn(
        onCredential: (idToken: String, rawNonce: String) -> Unit,
        onCancel: () -> Unit,
        onError: (message: String) -> Unit,
    )
}

/** iOS offers Apple only; Google on iOS would need the GoogleSignIn SDK. */
class IosSocialSignIn(private val appleSignIn: IosAppleSignIn) : SocialSignIn {
    override val googleAvailable: Boolean = false
    override val appleAvailable: Boolean = true

    override suspend fun google(): SocialCredential? = null

    override suspend fun apple(): SocialCredential? = suspendCancellableCoroutine { cont ->
        appleSignIn.signIn(
            onCredential = { token, nonce ->
                if (cont.isActive) cont.resume(SocialCredential(token, nonce))
            },
            onCancel = { if (cont.isActive) cont.resume(null) },
            onError = { message -> if (cont.isActive) cont.resumeWithException(SocialSignInException(message)) },
        )
    }
}
