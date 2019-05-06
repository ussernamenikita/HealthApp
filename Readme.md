### Modules for HealthApp application

#### Firebaseauth module include authentication with FirebaseSdk.

   For using authentication with Firebase do folowing steps:

1. Create firebase project https://firebase.google.com/docs/android/setup#step_1_create_a_firebase_project

2. Add firebase to app https://firebase.google.com/docs/android/setup#console

1. Check if user authenticate com.healthapp.firebaseauth.FirebaseAuth.getCurrentUser(). This method return FirebaseUser(https://firebase.google.com/docs/reference/android/com/google/firebase/auth/FirebaseUser.html). If this method return null, then user not authorizaed now and you should open AuthFragment.
2. For authorization by gogle account show AuthFragment (see SignUpActivity) :

    supportFragmentManager.beginTransaction().add(AuthFragment(), AUTH_TAG).commit()

