package com.example.mukseum

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mukseum.databinding.ActivityProfileBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUserNameAndRoleText()

        // Show Favorites
        viewFavorites()

        // Shows Artifacts of the Curator
        viewGallery()

        if (intent.getStringExtra("mode") != "visit") {
            binding.profileViewCommentsButton.visibility = View.VISIBLE
            binding.profileViewCommentsButton.isClickable = true
            showComments()
        }

        // Refresh Profile
        binding.profileRefreshButton.setOnClickListener {
            recreate()
        }

        // Manage Curators Button
        binding.manageCuratorsButton.setOnClickListener {
            startActivity(Intent(this, CuratorManagementActivity::class.java))
        }

        // Go Back
        binding.profileCancelButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun setUserNameAndRoleText() {
        val firebaseAuth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.reference.child("Registered Users")
        val uid = when (intent.getStringExtra("mode")) {
            "visit" -> intent.getStringExtra("target_uid").toString()
            else -> firebaseAuth.currentUser?.uid.toString()
        }
        val userRef = usersRef.child(uid)

        userRef.child("name").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.getValue(String::class.java)
                binding.profileNameTextView.text = name
            }

            override fun onCancelled(error: DatabaseError) {
                Snackbar.make(binding.root, "Failed to retrieve user name", Snackbar.LENGTH_SHORT)
                    .show()
            }
        })

        userRef.child("userRole").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val role = snapshot.getValue(String::class.java)
                binding.profileRoleTextView.text = role

                when (role) {
                    "admin" -> {
                        binding.profileArtifactsRecyclerView.visibility = View.VISIBLE
                        binding.profileArtifactsRecyclerView.isClickable = true
                        binding.profileArtifactsTextView.visibility = View.VISIBLE

                        if (intent.getStringExtra("mode") != "visit") {
                            binding.manageCuratorsButton.visibility = View.VISIBLE
                            binding.manageCuratorsButton.isClickable = true
                        }
                    }

                    "curator" -> {
                        binding.profileArtifactsRecyclerView.visibility = View.VISIBLE
                        binding.profileArtifactsRecyclerView.isClickable = true
                        binding.profileArtifactsTextView.visibility = View.VISIBLE
                        binding.manageCuratorsButton.visibility = View.GONE
                        binding.manageCuratorsButton.isClickable = false
                    }

                    "user" -> {
                        binding.profileArtifactsRecyclerView.visibility = View.GONE
                        binding.profileArtifactsRecyclerView.isClickable = false
                        binding.profileArtifactsTextView.visibility = View.GONE
                        binding.manageCuratorsButton.visibility = View.GONE
                        binding.manageCuratorsButton.isClickable = false
                    }

                    else -> {
                        binding.profileArtifactsRecyclerView.visibility = View.GONE
                        binding.profileArtifactsRecyclerView.isClickable = false
                        binding.profileArtifactsTextView.visibility = View.GONE
                        binding.manageCuratorsButton.visibility = View.GONE
                        binding.manageCuratorsButton.isClickable = false
                        Snackbar.make(
                            binding.root,
                            "User role not defined",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Snackbar.make(binding.root, "Failed to retrieve user role", Snackbar.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun viewFavorites() {
        val firebaseAuth = FirebaseAuth.getInstance()
        val uid = when (intent.getStringExtra("mode")) {
            "visit" -> intent.getStringExtra("target_uid").toString()
            else -> firebaseAuth.currentUser?.uid.toString()
        }
        val recyclerView = binding.profileFavoritesRecyclerView
        val favoritesList = mutableListOf<String>()

        // Fetches all artifacts in the favoritesList of target user
        Firebase.database.reference.child("Registered Users").child(uid).child("favoritesList")
            .get().addOnSuccessListener {
                for (artifact in it.children) {
                    artifact.getValue(String::class.java)
                        ?.let { it1 -> favoritesList.add(it1) }
                }

                // Setting up RecyclerView
                val layoutManager = GridLayoutManager(this, 3)
                recyclerView.layoutManager = layoutManager

                val adapter = ImageAdapter(favoritesList)
                recyclerView.adapter = adapter

            }.addOnFailureListener {
                Log.e("firebase", "Error getting data", it)
            }
    }

    private fun viewGallery() {
        val firebaseAuth = FirebaseAuth.getInstance()
        val uid = when (intent.getStringExtra("mode")) {
            "visit" -> intent.getStringExtra("target_uid").toString()
            else -> firebaseAuth.currentUser?.uid.toString()
        }
        val recyclerView = binding.profileArtifactsRecyclerView
        val artifactList = mutableListOf<String>()

        // Fetches all artifacts posted by the user
        Firebase.database.reference.child("Artifacts").get().addOnSuccessListener {
            for (artifact in it.children) {
                if (artifact.child("artifactOwner").getValue(String::class.java) == uid)
                    artifact.child("artifactImage").getValue(String::class.java)
                        ?.let { it1 -> artifactList.add(it1) }
            }

            // Setting up RecyclerView
            val layoutManager = GridLayoutManager(this, 3)
            recyclerView.layoutManager = layoutManager

            val adapter = ImageAdapter(artifactList)
            recyclerView.adapter = adapter

        }.addOnFailureListener {
            Log.e("firebase", "Error getting data", it)
        }
    }

    private fun showComments() {
        binding.profileViewCommentsButton.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    CommentsActivity::class.java
                ).putExtra("origin", "profile")
            )
        }
    }
}
