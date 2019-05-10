package com.healthapp.datasender

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.healthapp.firebaseauth.AuthFragment

const val ATUH_FRAGMENT_TAG = "AuthFragment"

class DataSenderAuthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.send_data_auth_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().add(AuthFragment(), ATUH_FRAGMENT_TAG).commit()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        HealthAppDataSender.schedule()
    }
}
