import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/**
 * Google sign-in on Android, through Credential Manager's "Sign in with Google" sheet. The ID
 * token it returns is minted for the Firebase project's web client, which is what Firebase
 * checks it against. Apple isn't offered on Android.
 */
class AndroidSocialSignIn(
    private val activity: Activity,
    /** The Firebase project's web OAuth client id; null until Google sign-in is enabled. */
    private val webClientId: String?,
) : SocialSignIn {

    override val googleAvailable: Boolean get() = webClientId != null
    override val appleAvailable: Boolean = false

    override suspend fun google(): SocialCredential? {
        val clientId = webClientId ?: return null
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).build())
            .build()
        return try {
            val credential = CredentialManager.create(activity).getCredential(activity, request).credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                SocialCredential(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                null
            }
        } catch (_: GetCredentialCancellationException) {
            null
        } catch (e: NoCredentialException) {
            throw SocialSignInException("There's no Google account on this phone to sign in with.", e)
        } catch (e: GetCredentialException) {
            throw SocialSignInException("Google sign-in didn't work: ${e.message ?: e.type}", e)
        } catch (e: GoogleIdTokenParsingException) {
            throw SocialSignInException("Google sign-in didn't work. Please try again.", e)
        }
    }

    override suspend fun apple(): SocialCredential? = null

    companion object {
        /**
         * The web client id the google-services plugin writes into resources once Google
         * sign-in is enabled in Firebase. Looked up by name so a build without it still
         * compiles and simply offers no Google sign-in.
         */
        fun webClientId(activity: Activity): String? {
            val id = activity.resources.getIdentifier("default_web_client_id", "string", activity.packageName)
            return if (id == 0) null else activity.getString(id).takeIf { it.isNotBlank() }
        }
    }
}
