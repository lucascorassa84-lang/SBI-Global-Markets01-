# Authentication

This Android starter uses Firebase Authentication with email/password.

- Sign in: FirebaseAuth.signInWithEmailAndPassword
- Registration: FirebaseAuth.createUserWithEmailAndPassword
- Password reset: FirebaseAuth.sendPasswordResetEmail
- Session check: FirebaseAuth.currentUser
- Sign out: FirebaseAuth.signOut

Enable Email/Password in Firebase Console > Authentication > Sign-in method.
Register Android package `com.sbi.globalmarkets`.
Do not commit private credentials or service-account keys.
Store the Android Firebase configuration as GitHub Actions secret `FIREBASE_CONFIG_BASE64` (base64 of google-services.json).
Firebase SDK manages its session tokens; never log tokens or passwords.
The app is a starter: market data, signals and notifications are not yet connected.
