import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.OAuthProvider
import dev.gitlive.firebase.auth.auth

/**
 * The real account layer over the GitLive KMP Firebase SDK, one class shared by Android and
 * iOS. It needs a configured Firebase project (`google-services.json` on Android,
 * `GoogleService-Info.plist` plus `FirebaseApp.configure()` on iOS). Social credentials arrive
 * already minted by [SocialSignIn]; this class only talks to Firebase.
 */
class FirebaseAuthService : AuthService {
    private val auth by lazy { Firebase.auth }

    override val currentUser: AuthUser?
        get() = auth.currentUser?.let { user ->
            AuthUser(uid = user.uid, email = user.email, isAnonymous = user.isAnonymous)
        }

    override suspend fun signInAnonymously() {
        auth.signInAnonymously()
    }

    override suspend fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
    }

    override suspend fun registerWithEmail(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
    }

    override suspend fun signInWithGoogle(idToken: String) {
        auth.signInWithCredential(GoogleAuthProvider.credential(idToken, null))
    }

    override suspend fun signInWithApple(idToken: String, rawNonce: String) {
        auth.signInWithCredential(
            OAuthProvider.credential(providerId = "apple.com", idToken = idToken, rawNonce = rawNonce),
        )
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email)
    }
}
