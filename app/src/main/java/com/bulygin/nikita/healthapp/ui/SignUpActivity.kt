package com.bulygin.nikita.healthapp.ui

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.healthapp.datasender.HealthAppDataSender

private const val AUTH_TAG = "_AUTH_"

class SignUpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HealthAppDataSender.schedule()
        val intent = Intent(this, MainActivity::class.java)
        this.startActivity(intent)
    }


}