import androidx.compose.ui.window.ComposeUIViewController
import org.lightscout.candybox.auth.FirebaseAuthService
import org.lightscout.candybox.auth.IosAppleSignIn
import org.lightscout.candybox.auth.IosSocialSignIn
import platform.UIKit.UIViewController

fun MainViewController(appleSignIn: IosAppleSignIn): UIViewController {
    val auth = FirebaseAuthService()
    val social = IosSocialSignIn(appleSignIn)
    return ComposeUIViewController { App(auth = auth, social = social) }
}
