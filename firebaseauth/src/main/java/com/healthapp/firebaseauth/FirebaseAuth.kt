package com.healthapp.firebaseauth

import android.app.Activity
import androidx.fragment.app.Fragment
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser


const val RC_SIGN_IN = 121

object FirebaseAuth {


    fun signIn(fromFragment: androidx.fragment.app.Fragment) {
        val providers = arrayListOf(AuthUI.IdpConfig.GoogleBuilder().build())
        fromFragment.startActivityForResult(AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(), RC_SIGN_IN)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int): FirebaseUser? {
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                return FirebaseAuth.getInstance().currentUser
            }
        }
        return null
    }

    fun getCurrentUser(): FirebaseUser? = FirebaseAuth.getInstance().currentUser
}
