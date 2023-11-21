package com.example.mukseum

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mukseum.databinding.ActivitySavedArtifactsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class SavedArtifactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavedArtifactsBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedArtifactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewGallery()

        // Cancel Viewing Saved Artifacts
        binding.savedCancelButton.setOnClickListener { finish() }
    }

    private fun viewGallery() {
        firebaseAuth = FirebaseAuth.getInstance()
        val uid = firebaseAuth.currentUser?.uid.toString()
        val recyclerView = binding.savedRecyclerView
        val savedList = mutableListOf<String>()

        // Fetches artifacts that are saved by the user
        Firebase.database.reference.child("Registered Users").child(uid).child("savedList")
            .get().addOnSuccessListener {
                for (artifact in it.children) {
                    artifact.getValue(String::class.java)
                        ?.let { it1 -> savedList.add(it1) }
                }

                // Setting up RecyclerView
                val layoutManager = GridLayoutManager(this, 3)
                recyclerView.layoutManager = layoutManager

                val adapter = ImageAdapter(savedList)
                recyclerView.adapter = adapter

            }.addOnFailureListener {
                Log.e("firebase", "Error getting data", it)
            }
    }
}