package com.app.buildingmanagement

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.app.buildingmanagement.databinding.ActivityAdminBinding

import com.google.android.material.bottomnavigation.BottomNavigationView

class AdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Default fragment

        }
    }
