package com.example.mukseum

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.mukseum.databinding.ActivityFullImageBinding

class FullImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFullImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cancel Full Image View
        binding.fullImageViewCancelButton.setOnClickListener { finish() }

        // View The Image
        val imageUrl = intent.getStringExtra("image_url")
        val imageView: ImageView = findViewById(R.id.full_image_view)

        Glide.with(this)
            .load(imageUrl)
            .into(imageView)

        // Cancel Full Image View by Clicking on the Image again
        binding.fullImageView.setOnClickListener { finish() }
    }
}