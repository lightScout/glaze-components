/**
 * The account layer as shared code sees it. No Firebase types cross this line, so view models
 * take it as a constructor argument: [FirebaseAuthService] on Android and iOS,
 * [UnavailableAuthService] on the web, [StubAuthService] for previews.
 *
 * Calls throw on failure, and the message is shown to the user.
 */
interface AuthService {
    val currentUser: AuthUser?
    suspend fun signInAnonymously()
    suspend fun signInWithEmail(email: String, password: String)
    suspend fun registerWithEmail(email: String, password: String)

    /** Sign in with a Google ID token obtained from the platform (see [SocialSignIn]). */
    suspend fun signInWithGoogle(idToken: String)

    /** Sign in with an Apple ID token and the raw nonce the request was signed with. */
    suspend fun signInWithApple(idToken: String, rawNonce: String)
    suspend fun signOut()
    suspend fun resetPassword(email: String)
}
