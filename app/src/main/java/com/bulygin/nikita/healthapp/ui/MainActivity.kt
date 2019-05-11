package com.bulygin.nikita.healthapp.ui

import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import com.bulygin.nikita.healthapp.R
import com.bulygin.nikita.healthapp.di.AppModule

class MainActivity : AppCompatActivity() {

    lateinit var module: AppModule



    override fun onCreate(savedInstanceState: Bundle?) {
        this.inject()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val viewPager = findViewById<androidx.viewpager.widget.ViewPager>(R.id.viewpager)
        viewPager.adapter = adapter
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        tabLayout.setupWithViewPager(viewPager)
    }


    private lateinit var adapter: HealthPagerAdapter

    private fun inject() {
        module = AppModule(this)
        this.adapter = module.createHealthPagerAdapter()
    }
}
