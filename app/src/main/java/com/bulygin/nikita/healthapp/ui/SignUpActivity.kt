package com.bulygin.nikita.healthapp.ui

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.google.firebase.auth.FirebaseUser
import com.healthapp.firebaseauth.AuthFragment
import com.healthapp.firebaseauth.FirebaseAuth

private const val AUTH_TAG = "_AUTH_"

class SignUpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val currentUser = FirebaseAuth.getCurrentUser()
            if (currentUser == null) {
                supportFragmentManager.beginTransaction().add(AuthFragment(), AUTH_TAG).commit()
            } else {
                showSignInSuccess()
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val user = FirebaseAuth.getCurrentUser()
        if (user == null) {
            showSignInFailed()
        } else {
            showSignInSuccess()
        }
    }

    private fun showSignInSuccess() {
        val intent = Intent(this, MainActivity::class.java)
        this.startActivity(intent)
    }

    private fun showSignInFailed() {
        Toast.makeText(this, "Sign in failed try again", Toast.LENGTH_LONG).show()
    }
}