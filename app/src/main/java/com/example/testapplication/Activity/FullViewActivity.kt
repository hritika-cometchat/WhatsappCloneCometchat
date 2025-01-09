package com.example.testapplication.Activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.testapplication.databinding.ActivityFullViewBinding

class FullViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFullViewBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        val imageUrl = intent.getStringExtra("imageUrl")

        Glide.with(this).load(imageUrl).into(binding.fullImageView)
        binding.closeButton.setOnClickListener{
            finish()
        }
    }
}
