package com.example.mukseum

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mukseum.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivityMainBinding
    private lateinit var recyclerView: RecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        recyclerView = binding.recyclerView

        // To determine what UI elements are available for access
        // to the role assigned to the user
        getUserRole()

        // Get Name of User and Display on the userNameButton
        setUserNameButton()

        // Display museum gallery
        viewGallery()

        // Sign In
        setSignInButton()

        // Add Artifact
        curatorAddArtifact()

        // Manage Comments
        showComments()

        // Show Saved Artifacts
        binding.showSavedButton.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    SavedArtifactsActivity::class.java
                )
            )
        }

        // Log Out
        setLogOutButton()
    }

    private fun getUserRole() {
        var userRole: String?
        Firebase.database.reference.child("Registered Users")
            .child(firebaseAuth.currentUser?.uid.toString()).get().addOnSuccessListener {
                for (detail in it.children) {
                    if (detail.key == "userRole") {
                        userRole = detail.value.toString()

                        when (userRole) {
                            "admin" -> {
                                binding.addArtifactButton.visibility = View.VISIBLE
                                binding.addArtifactButton.isClickable = true
                                binding.manageCommentsButton.visibility = View.VISIBLE
                                binding.manageCommentsButton.isClickable = true
                                binding.showSavedButton.visibility = View.VISIBLE
                                binding.showSavedButton.isClickable = true
                                binding.userNameButton.visibility = View.VISIBLE
                                binding.userNameButton.isClickable = true
                                binding.logOutButton.visibility = View.VISIBLE
                                binding.logOutButton.isClickable = true
                                binding.mainSignInButton.visibility = View.GONE
                                binding.mainSignInButton.isClickable = false
                            }

                            /*
                            Admin and curator roles may seem like they have the same buttons, but
                            separation is necessary for future buttons that may only be visible for
                            admins.
                             */

                            "curator" -> {
                                binding.addArtifactButton.visibility = View.VISIBLE
                                binding.addArtifactButton.isClickable = true
                                binding.manageCommentsButton.visibility = View.VISIBLE
                                binding.manageCommentsButton.isClickable = true
                                binding.showSavedButton.visibility = View.VISIBLE
                                binding.showSavedButton.isClickable = true
                                binding.userNameButton.visibility = View.VISIBLE
                                binding.userNameButton.isClickable = true
                                binding.logOutButton.visibility = View.VISIBLE
                                binding.logOutButton.isClickable = true
                                binding.mainSignInButton.visibility = View.GONE
                                binding.mainSignInButton.isClickable = false
                            }

                            "user" -> {
                                binding.apply {
                                    // Change the constraints of a View
                                    val layoutParams =
                                        showSavedButton.layoutParams as ConstraintLayout.LayoutParams
                                    layoutParams.startToStart =
                                        ConstraintLayout.LayoutParams.PARENT_ID
                                    layoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                                    layoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                                    layoutParams.bottomToBottom =
                                        ConstraintLayout.LayoutParams.PARENT_ID
                                    layoutParams.verticalBias = 0.955F
                                    showSavedButton.layoutParams = layoutParams
                                    /*
                                    This is because the showSavedButton is constrained to the other
                                    two buttons present on the admin/curator view, so readjustment
                                    of constraints is needed for user who has no access to said
                                    buttons.
                                     */
                                }

                                binding.showSavedButton.visibility = View.VISIBLE
                                binding.showSavedButton.isClickable = true
                                binding.logOutButton.visibility = View.VISIBLE
                                binding.logOutButton.isClickable = true
                                binding.userNameButton.visibility = View.VISIBLE
                                binding.userNameButton.isClickable = true
                                binding.mainSignInButton.visibility = View.GONE
                                binding.mainSignInButton.isClickable = false
                            }

                            else -> {
                                binding.mainSignInButton.visibility = View.VISIBLE
                                binding.mainSignInButton.isClickable = true
                                Snackbar.make(
                                    binding.root,
                                    "You are currently not logged in",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
    }

    private fun setUserNameButton() {
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.reference.child("Registered Users")
        val uid = firebaseAuth.currentUser?.uid.toString()
        val userRef = usersRef.child(uid)

        userRef.child("name").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.getValue(String::class.java)
                binding.userNameButton.text = name
            }

            override fun onCancelled(error: DatabaseError) {
                Snackbar.make(binding.root, "Failed to retrieve user name", Snackbar.LENGTH_SHORT)
                    .show()
            }
        })

        binding.userNameButton.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    ProfileActivity::class.java
                )
            )
        }
    }

    private fun viewGallery() {
        val artifactList = mutableListOf<String>()

        // Fetches all artifacts currently in storage and lists their URLS
        Firebase.database.reference.child("Artifacts").get().addOnSuccessListener {
            for (artifact in it.children) {
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

    private fun setSignInButton() {
        binding.mainSignInButton.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    SignInActivity::class.java
                )
            )
        }
    }

    private fun curatorAddArtifact() {
        binding.addArtifactButton.setOnClickListener {
            startActivity(Intent(this, UploadArtifactActivity::class.java))
        }
    }

    private fun setLogOutButton() {
        binding.logOutButton.setOnClickListener {
            binding.addArtifactButton.visibility = View.GONE
            binding.addArtifactButton.isClickable = false
            binding.logOutButton.visibility = View.GONE
            binding.logOutButton.isClickable = false
            firebaseAuth.signOut()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }

    private fun showComments() {
        binding.manageCommentsButton.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    CommentsActivity::class.java
                ).putExtra("origin", "main")
            )
        }
    }
}