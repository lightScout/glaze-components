/**
 * A credential from a platform sign-in sheet: the provider's ID token, plus the raw nonce it
 * was signed against (Apple needs it; Google doesn't).
 */
data class SocialCredential(
    val idToken: String,
    val rawNonce: String? = null,
)

/**
 * The platform half of social sign-in: opening the Google or Apple sheet and returning a
 * credential for [AuthService] to hand to Firebase. Android offers Google through Credential
 * Manager; iOS offers Apple through `ASAuthorization`. The real implementations are passed in
 * at each app's entry point; [UnavailableSocialSignIn] stands in everywhere else.
 */
interface SocialSignIn {
    val googleAvailable: Boolean
    val appleAvailable: Boolean

    /** Open the Google sheet; null if cancelled or unavailable, [SocialSignInException] if it failed. */
    suspend fun google(): SocialCredential?

    /** Open the Apple sheet; null if cancelled or unavailable, [SocialSignInException] if it failed. */
    suspend fun apple(): SocialCredential?
}

/** The platform sheet failed for a reason worth showing (not a cancel). */
class SocialSignInException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** No platform sign-in wired: both providers report unavailable and the UI hides their buttons. */
object UnavailableSocialSignIn : SocialSignIn {
    override val googleAvailable: Boolean = false
    override val appleAvailable: Boolean = false
    override suspend fun google(): SocialCredential? = null
    override suspend fun apple(): SocialCredential? = null
}
