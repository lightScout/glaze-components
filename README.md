# Glaze components

The source of every component in [Glaze](https://glaze.lightscout.co.uk), a gallery of Compose Multiplatform components that run live on your phone.

Each folder here is exactly what the app shows in its Code tab: the Kotlin (and where needed, Swift and Gradle) you need to use the component in your own project, plus the setup steps. Copy what you want.

<!-- components:start -->
| Component | What it is | Platforms |
|---|---|---|
| [Firebase Auth](components/sign-in-screen/) | Multiplatform auth flow: email/password, anonymous, Google on Android and Apple on iOS. | android, common, ios |
| [Multiplatform Settings](components/multiplatform-settings/) | Persist key-value preferences across Android (SharedPreferences) and iOS (NSUserDefaults) from shared KMP code. | android, common, ios |
| [Android ProGuard](components/android-proguard/) | R8/ProGuard setup for Android KMP projects: minification, resource shrinking, and keep rules for Compose, Kotlin Serialization, Firebase Auth, AndroidX Credentials, and Multiplatform Settings. | android |
| [Morph Text](components/morph-text/) | Morphing text transition using blur + threshold compositing. | common |
<!-- components:end -->

## How this repo is kept in sync

The app's component registry is the source of truth. This repo is generated from it, so the code here and the code in the app are always the same text. New components land here when they land in the app.

## Using a component

Copy the files for the platforms you target into your project and adjust the package line. Each component README lists the files by platform and any setup the platform needs (Firebase config, Gradle changes, Info.plist keys).

## Get the app

- Android: [Google Play](https://play.google.com/store/apps/details?id=org.lightscout.candybox)
- iOS: coming soon
- Web: coming soon

## Requests and bugs

Open an issue. Component requests are welcome, and a bug in one of these files is a bug in the app, so it gets fixed in both.

## Licence

[Apache 2.0](LICENSE). Where a component started from someone else's work, its README says so and links to the original.
