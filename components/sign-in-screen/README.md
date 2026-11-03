# Firebase Auth

Multiplatform auth flow: email/password, anonymous, Google on Android and Apple on iOS. Firebase Auth sits behind a shared AuthService interface and the platform sign-in sheets behind a SocialSignIn seam, so the same ViewModel and UI run everywhere and tests use fakes.

Tags: `auth`, `firebase`, `form`, `bottom-sheet`, `kmp`

## Files

| File | Platform |
|---|---|
| [common/AuthUser.kt](common/AuthUser.kt) | common |
| [common/AuthService.kt](common/AuthService.kt) | common |
| [common/SocialSignIn.kt](common/SocialSignIn.kt) | common |
| [android-ios/FirebaseAuthService.kt](android-ios/FirebaseAuthService.kt) | android + ios |
| [android/AndroidSocialSignIn.kt](android/AndroidSocialSignIn.kt) | android |
| [android/MainActivity.kt](android/MainActivity.kt) | android |
| [ios/IosSocialSignIn.kt](ios/IosSocialSignIn.kt) | ios |
| [ios/AppleSignIn.swift](ios/AppleSignIn.swift) | ios |
| [ios/MainViewController.kt](ios/MainViewController.kt) | ios |
| [common/SignInViewModel.kt](common/SignInViewModel.kt) | common |
| [common/SignInScreen.kt](common/SignInScreen.kt) | common |

## Setup

### 1. Create a Firebase project

*Firebase Console*

```
Go to console.firebase.google.com → Add project.
Enable Google Analytics is optional.
```

### 2. Enable Authentication providers

*Firebase Console*

```
Authentication → Sign-in method → enable:
• Anonymous
• Email/Password
• Google
• Apple

For Apple: enter your Apple Team ID, create a Service ID
(reverse-DNS bundle ID), generate an Auth Key (.p8 file),
and paste the Key ID + key contents into Firebase.
```

### 3. Register your Android app and download google-services.json

*Firebase Console*

```
Project Settings → Add app → Android.
Package name must match applicationId in build.gradle.kts.
Download google-services.json and place it in androidApp/.
```

### 4. Add the SHA-1 fingerprint for Google Sign-In

*Firebase Console*

```
Project Settings → Your apps → Android app → Add fingerprint.

Get your debug SHA-1:
  ./gradlew signingReport

Without this, Google Sign-In silently cancels with no UI shown.
```

### 5. Enable Sign In with Apple capability

*Apple Developer*

```
Certificates, Identifiers & Profiles → Identifiers → your App ID.
Enable 'Sign In with Apple' and save.
```

### 6. Create a Services ID for Apple OAuth (Firebase web flow)

*Apple Developer*

```
Identifiers → + → Services IDs.
Enable 'Sign In with Apple', configure domains and redirect URL
using the callback URL from Firebase (Authentication → Apple → Setup).
```

### 7. Generate an Auth Key

*Apple Developer*

```
Keys → + → enable 'Sign In with Apple' → Configure → select your app.
Download the .p8 key file (only available once).
Note your Key ID and Team ID — both are needed in Firebase.
```

### 8. Add the Firebase BOM and google-services plugin

*Android*

```
In root build.gradle.kts, inside the outer dependencies {} block
(not the KMP DSL), add:

  dependencies {
    classpath("com.google.gms:google-services:4.4.2")
  }

In androidApp/build.gradle.kts:
  plugins { id("com.google.gms.google-services") }

Note: Kotlin 2.3 removed platform() from the KMP DSL.
Use the outer dependencies block for the BOM.
```

### 9. Add Credential Manager dependencies (Google Sign-In)

*Android*

```
In androidApp/build.gradle.kts dependencies:

  implementation("androidx.credentials:credentials:1.3.0")
  implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
  implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

AndroidSocialSignIn lives in the app module, next to the Activity it needs.
```

### 10. Pass Firebase auth and Google sign-in from MainActivity

*Android*

```
In MainActivity.onCreate():

  val auth = FirebaseAuthService()
  val social = AndroidSocialSignIn(this, AndroidSocialSignIn.webClientId(this))
  setContent { App(auth = auth, social = social) }

Credential Manager needs an Activity context, so the Activity is passed in directly.
webClientId reads default_web_client_id, which the google-services plugin writes once
Google sign-in is enabled in Firebase. Without it the Google button is hidden.
```

### 11. Add GoogleService-Info.plist to Xcode

*iOS*

```
Download from Firebase Console → Project Settings → iOS app.
Drag into iosApp/iosApp/ in Xcode — make sure it's added to the target.
In Xcode 16+ with PBXFileSystemSynchronizedRootGroup the file is auto-
included; you only need to add FIREBASE_APP_ID to your xcconfig.
```

### 12. Add FirebaseAuth via Swift Package Manager

*iOS*

```
In Xcode: File → Add Package Dependencies.
URL: https://github.com/firebase/firebase-ios-sdk
Add FirebaseAuth to the iosApp target.
```

### 13. Initialise Firebase in AppDelegate

*iOS*

```
Create AppDelegate.swift (or add to iOSApp.swift):

  import FirebaseCore
  class AppDelegate: NSObject, UIApplicationDelegate {
    func application(_ app: UIApplication,
      didFinishLaunchingWithOptions ...) -> Bool {
      FirebaseApp.configure()
      return true
    }
  }
  @main struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    var body: some Scene { WindowGroup { ContentView() } }
  }

Using init() on the SwiftUI App struct is unreliable for Firebase init.
```

### 14. Add Sign In with Apple entitlement

*iOS*

```
Create iosApp/iosApp/iosApp.entitlements:

  <dict>
    <key>com.apple.developer.applesignin</key>
    <array><string>Default</string></array>
  </dict>

In project.pbxproj add to both Debug and Release build configs:
  CODE_SIGN_ENTITLEMENTS = iosApp/iosApp.entitlements;

Without this entitlement Apple Sign-In fails with AKAuthenticationError -7026.
```

### 15. Add AppleSignIn.swift and pass it to MainViewController

*iOS*

```
Add AppleSignIn.swift to iosApp/iosApp/. It owns ASAuthorizationController and the
nonce, and implements the Kotlin IosAppleSignIn interface through callbacks, because
Swift can call Kotlin suspend functions but can't implement them.

In ContentView.swift:
  MainViewControllerKt.MainViewController(appleSignIn: AppleSignIn())
```

### 16. Add firebase-auth in a source set shared by Android and iOS

*KMP*

```
The web targets have no Firebase, so in shared/build.gradle.kts group Android and
iOS under a firebaseMain source set:

  applyDefaultHierarchyTemplate {
    common {
      group("firebase") {
        withCompilations { it.platformType == KotlinPlatformType.androidJvm }
        group("ios") { withIos() }
      }
    }
  }

  getByName("firebaseMain").dependencies {
    implementation("dev.gitlive:firebase-auth:2.4.0")
  }
```

### 17. Define AuthService and FirebaseAuthService

*KMP*

```
AuthService is a plain interface in commonMain, so no Firebase types reach shared code.
FirebaseAuthService in firebaseMain implements it once for both Android and iOS.
View models take AuthService as a constructor argument, so tests pass a fake.
```

### 18. Define the SocialSignIn seam

*KMP*

```
SocialSignIn opens the platform sheet and returns a SocialCredential for AuthService.
Android: AndroidSocialSignIn offers Google through Credential Manager.
iOS: IosSocialSignIn offers Apple through the Swift AppleSignIn class.
Web: UnavailableSocialSignIn, so the sign-in screen shows no social buttons.
```

---

Run it live and tweak the parameters in [Glaze](https://glaze.lightscout.co.uk) · [Google Play](https://play.google.com/store/apps/details?id=org.lightscout.candybox)
