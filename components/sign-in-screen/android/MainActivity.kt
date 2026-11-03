import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.lightscout.candybox.auth.AndroidSocialSignIn
import org.lightscout.candybox.auth.FirebaseAuthService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuthService()
        val social = AndroidSocialSignIn(this, AndroidSocialSignIn.webClientId(this))

        setContent {
            App(auth = auth, social = social)
        }
    }
}
