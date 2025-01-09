package com.example.testapplication.Activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.testapplication.databinding.ActivityImagePreviewSendBinding
import java.io.File
import java.io.FileOutputStream

class ImagePreviewSendActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImagePreviewSendBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImagePreviewSendBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        val imageUri = intent.getParcelableExtra<Uri>("imageUrlDisplay")
        val file = imageUri?.let { uriToFile(it) }

        if (file != null) {
            Glide.with(this).load(file.absoluteFile).into(binding.imagePreview)
        }
        binding.btnClose.setOnClickListener{
            finish()
        }
        binding.ivSend.setOnClickListener {
            val intent = Intent()
            intent.putExtra("imageUrlRetrieve", imageUri)
            setResult(RESULT_OK, intent)
            finish()
        }
        binding.tvSenderName.text = intent.getStringExtra("senderName").orEmpty()
    }

    private fun uriToFile(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        val file = File(cacheDir, "image_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return file
    }

}