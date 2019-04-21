package com.healthapp.firebaseauth

import android.app.Activity
import android.content.Intent
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth

const val RC_SIGN_IN = 121

object FirebaseAuth {

    fun signIn(fromActivity: Activity) {
        val providers = arrayListOf(
                AuthUI.IdpConfig.EmailBuilder().build(),
                AuthUI.IdpConfig.PhoneBuilder().build(),
                AuthUI.IdpConfig.GoogleBuilder().build())
        fromActivity.startActivityForResult(AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(), RC_SIGN_IN)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): User? {

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                return getCurrentUser()
            }
        }
        return null
    }

    public fun getCurrentUser(): User? {
        val user = FirebaseAuth.getInstance().currentUser
        return user?.let { User(it.uid, it.email) }
    }

    class User(val id: String, val email: String?)
}
