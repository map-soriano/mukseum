package com.example.mukseum

import android.content.Intent
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.mukseum.databinding.ActivityArtifactInformationBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage

class ArtifactInformationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArtifactInformationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArtifactInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get User Role to establish accessible content
        getUserRole()

        // Set Artifact Information
        setArtifactInformation()

        // Show/Hide Did You Know
        toggleDidYouKnowButton()

        // Show Comments
        showComments()

        // Add/Remove to/from Favorites
        toggleFavorite()

        // Update Artifact
        binding.manageArtifactsButton2.setOnClickListener {
            val imgUrl = intent.getStringExtra("image_url")
            val intent = Intent(this, UpdateArtifactActivity::class.java)
            intent.putExtra("image_url", imgUrl)
            startActivity(intent)
        }

        // Delete Artifact
        toggleDelete()

        // Cancel Viewing Artifact Information
        binding.artifactInformationCancelButton.setOnClickListener { finish() }
    }

    private fun setArtifactInformation() {

        val imageUrl = intent.getStringExtra("image_url")

        // Setting the Image
        Glide.with(this)
            .load(imageUrl)
            .into(binding.artifactImageView)

        // Allow image to be clicked to view full image
        binding.artifactImageView.setOnClickListener {
            val intent = Intent(this, FullImageActivity::class.java)
            intent.putExtra("image_url", imageUrl)
            startActivity(intent)
        }

        // Allow scrolling in case it is long
        binding.artifactNameTextView.movementMethod = ScrollingMovementMethod.getInstance()
        binding.artifactDescriptionTextView.movementMethod = ScrollingMovementMethod.getInstance()
        binding.artifactTagsTextView.movementMethod = ScrollingMovementMethod.getInstance()
        binding.artifactDidYouKnowTextView.movementMethod = ScrollingMovementMethod.getInstance()


        // Setting the Name, Description, Tag, and Did You Know
        val query = FirebaseDatabase.getInstance().getReference("Artifacts")
            .orderByChild("artifactImage")
            .equalTo(imageUrl)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (itemSnapshot in snapshot.children) {
                    val name = itemSnapshot.child("artifactName").getValue(String::class.java)
                    val owner = itemSnapshot.child("artifactOwner").getValue(String::class.java)
                    val desc =
                        itemSnapshot.child("artifactDescription").getValue(String::class.java)
                    val tags = itemSnapshot.child("artifactTags").getValue(String::class.java)
                    val dyk = itemSnapshot.child("artifactDidYouKnow").getValue(String::class.java)
                    // Handle the matched item with the URL
                    binding.artifactNameTextView.text = name
                    getOwnerName(owner)
                    binding.artifactDescriptionTextView.text = getString(R.string.description, desc)
                    binding.artifactTagsTextView.text = getString(R.string.tags, tags)
                    binding.artifactDidYouKnowTextView.text =
                        dyk ?: "No extra information available"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Snackbar.make(binding.root, "Failed to artifact information", Snackbar.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun getOwnerName(uid: String?) {
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.reference.child("Registered Users")
        val userRef = uid?.let { usersRef.child(it) }

        userRef?.child("name")?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.artifactOwnerTextView.text =
                    getString(R.string.owner, snapshot.getValue(String::class.java))
                binding.artifactOwnerTextView.setOnClickListener {
                    val intent =
                        Intent(binding.artifactOwnerTextView.context, ProfileActivity::class.java)
                    intent.putExtra("target_uid", uid)
                    intent.putExtra("mode", "visit")
                    startActivity(intent)

                }

            }

            override fun onCancelled(error: DatabaseError) {
                Snackbar.make(binding.root, "Failed to retrieve user name", Snackbar.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun getUserRole() {
        val firebaseAuth = FirebaseAuth.getInstance()
        var userRole: String?
        Firebase.database.reference.child("Registered Users")
            .child(firebaseAuth.currentUser?.uid.toString()).get().addOnSuccessListener {
                for (detail in it.children) {
                    if (detail.key == "userRole") {
                        userRole = detail.value.toString()

                        when (userRole) {
                            "admin", "curator", "user" -> {
                                binding.addFavoritesButton.visibility = View.VISIBLE
                                binding.addFavoritesButton.isClickable = true

                                /*
                                Add/Remove to/from Saved
                                Placed here to prevent not logged in users from accessing save
                                feature
                                */

                                toggleSaved()
                            }

                            else -> {
                                binding.addFavoritesButton.visibility = View.GONE
                                binding.addFavoritesButton.isClickable = false
                                binding.deleteArtifactButton.visibility = View.GONE
                                binding.deleteArtifactButton.isClickable = false
                                binding.manageArtifactsButton2.visibility = View.GONE
                                binding.manageArtifactsButton2.isClickable = false
                            }
                        }
                    }
                }
            }
    }

    private fun toggleDidYouKnowButton() {
        var didYouKnowIsShown = false
        binding.didYouKnowButton.setOnClickListener {
            when (didYouKnowIsShown) {
                true -> {
                    binding.artifactDidYouKnowTextView.visibility = View.GONE
                    didYouKnowIsShown = false
                }

                false -> {
                    binding.artifactDidYouKnowTextView.visibility = View.VISIBLE
                    didYouKnowIsShown = true
                }
            }
        }
    }

    private fun toggleFavorite() {
        var isFavorite = false
        val favoritesList = mutableListOf<String>()
        val userUID = FirebaseAuth.getInstance().currentUser?.uid.toString()

        // Fetch the favorites list from the database
        Firebase.database.reference.child("Registered Users").child(userUID).child("favoritesList")
            .get()
            .addOnSuccessListener { dataSnapshot ->
                // Clear the existing favorites list
                favoritesList.clear()

                // Populate the favorites list with the retrieved data
                for (snapshot in dataSnapshot.children) {
                    val favorite = snapshot.getValue(String::class.java)
                    favorite?.let { favoritesList.add(it) }
                }

                // Check if the current image URL is in the favorites list
                val imageUrl = intent.getStringExtra("image_url")
                isFavorite = imageUrl != null && imageUrl in favoritesList

                // Update the star button's image resource based on the isFavorite flag
                if (isFavorite) {
                    binding.addFavoritesButton.setImageResource(android.R.drawable.btn_star_big_on)
                } else {
                    binding.addFavoritesButton.setImageResource(android.R.drawable.btn_star_big_off)
                }
            }
            .addOnFailureListener {
                Snackbar.make(
                    binding.root,
                    "Failed to access Favorites list",
                    Snackbar.LENGTH_SHORT
                ).show()
            }

        binding.addFavoritesButton.setOnClickListener {
            when (isFavorite) {
                true -> {
                    removeFromFavorites(userUID)
                    binding.addFavoritesButton.setImageResource(android.R.drawable.btn_star_big_off)
                    isFavorite = false
                    Snackbar.make(
                        binding.root,
                        "Removed from favorites",
                        Snackbar.LENGTH_SHORT
                    )
                        .show()
                }

                false -> {
                    addToFavorites(userUID)
                    binding.addFavoritesButton.setImageResource(android.R.drawable.btn_star_big_on)
                    isFavorite = true
                    Snackbar.make(binding.root, "Added to favorites", Snackbar.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun addToFavorites(userUID: String) {
        // Assume we have a Firebase database reference to a specific location
        val databaseReference =
            FirebaseDatabase.getInstance().reference.child("Registered Users")
                .child(userUID).child("favoritesList")

        // Create a new child with an automatically generated key
        val childReference = databaseReference.push()

        // Set the values for the child
        childReference.setValue(intent.getStringExtra("image_url"))
    }

    private fun removeFromFavorites(userUID: String) {
        val databaseReference =
            FirebaseDatabase.getInstance().reference.child("Registered Users")
                .child(userUID).child("favoritesList")

        // Define the value to match for deletion
        val valueToDelete = intent.getStringExtra("image_url")

        // Query the database to find the matching data
        val query = databaseReference.orderByValue().equalTo(valueToDelete)

        // Add a listener to handle the query result
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Iterate through the matching data
                for (childSnapshot in snapshot.children) {
                    // Remove the matching data
                    childSnapshot.ref.removeValue()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Snackbar.make(
                    binding.root,
                    "Failed to remove from Favorites",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun toggleSaved() {
        var isSaved = false
        var doubleTapCtr = 2
        val savedList = mutableListOf<String>()
        val userUID = FirebaseAuth.getInstance().currentUser?.uid.toString()

        // Fetch the saved list from the database
        Firebase.database.reference.child("Registered Users").child(userUID).child("savedList")
            .get()
            .addOnSuccessListener { dataSnapshot ->
                // Clear the existing saved list
                savedList.clear()

                // Populate the saved list with the retrieved data
                for (snapshot in dataSnapshot.children) {
                    val saved = snapshot.getValue(String::class.java)
                    saved?.let { savedList.add(it) }
                }

                // Check if the current image URL is in the saved list
                val imageUrl = intent.getStringExtra("image_url")
                isSaved = imageUrl != null && imageUrl in savedList
            }
            .addOnFailureListener {
                Snackbar.make(
                    binding.root,
                    "Failed to access saved list",
                    Snackbar.LENGTH_SHORT
                ).show()
            }

        binding.artifactDescriptionTextView.setOnClickListener {
            if (doubleTapCtr < 1) {
                when (isSaved) {
                    true -> {
                        removeFromSaved(userUID)
                        isSaved = false
                        Snackbar.make(
                            binding.root,
                            "Removed from saved list",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }

                    false -> {
                        addToSaved(userUID)
                        isSaved = true
                        Snackbar.make(binding.root, "Added to saved list", Snackbar.LENGTH_SHORT)
                            .show()
                    }
                }
                doubleTapCtr = 2
            } else {
                val addOrRemove = if (isSaved) "remove from" else "add to"
                Snackbar.make(
                    binding.root,
                    "Tap description $doubleTapCtr more time/s to $addOrRemove Saved",
                    Snackbar.LENGTH_SHORT
                ).show()
                doubleTapCtr--
            }
        }
    }

    private fun addToSaved(userUID: String) {
        // Assume we have a Firebase database reference to a specific location
        val databaseReference =
            FirebaseDatabase.getInstance().reference.child("Registered Users")
                .child(userUID).child("savedList")

        // Create a new child with an automatically generated key
        val childReference = databaseReference.push()

        // Set the values for the child
        childReference.setValue(intent.getStringExtra("image_url"))
    }

    private fun removeFromSaved(userUID: String) {
        val databaseReference =
            FirebaseDatabase.getInstance().reference.child("Registered Users")
                .child(userUID).child("savedList")

        // Define the value to match for deletion
        val valueToDelete = intent.getStringExtra("image_url")

        // Query the database to find the matching data
        val query = databaseReference.orderByValue().equalTo(valueToDelete)

        // Add a listener to handle the query result
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Iterate through the matching data
                for (childSnapshot in snapshot.children) {
                    // Remove the matching data
                    childSnapshot.ref.removeValue()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Snackbar.make(
                    binding.root,
                    "Failed to remove from Saved",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun toggleDelete() {
        val firebaseAuth = FirebaseAuth.getInstance()
        val uid = firebaseAuth.currentUser?.uid.toString()
        var userRole: String?

        // Perform check if user is admin
        Firebase.database.reference.child("Registered Users")
            .child(uid).get().addOnSuccessListener {
                for (detail in it.children) {
                    if (detail.key == "userRole") {
                        userRole = detail.value.toString()

                        when (userRole) {
                            "admin" -> {
                                binding.deleteArtifactButton.visibility = View.VISIBLE
                                binding.deleteArtifactButton.isClickable = true
                                binding.manageArtifactsButton2.visibility = View.VISIBLE
                                binding.manageArtifactsButton2.isClickable = true
                            }

                            else -> {
                                binding.deleteArtifactButton.visibility = View.GONE
                                binding.deleteArtifactButton.isClickable = false
                            }
                        }
                    }
                }
            }

        // Perform check if user owns the artifact
        val artifactList = mutableListOf<String>()

        // Fetches all artifacts currently in storage that belongs to user and lists their URLs
        Firebase.database.reference.child("Artifacts").get().addOnSuccessListener {
            for (artifact in it.children) {
                if (artifact.child("artifactOwner").getValue(String::class.java) == uid)
                    artifact.child("artifactImage").getValue(String::class.java)
                        ?.let { it1 -> artifactList.add(it1) }
            }

            if (intent.getStringExtra("image_url").toString() in artifactList) {
                binding.deleteArtifactButton.visibility = View.VISIBLE
                binding.deleteArtifactButton.isClickable = true
                binding.manageArtifactsButton2.visibility = View.VISIBLE
                binding.manageArtifactsButton2.isClickable = true
            }

        }.addOnFailureListener {
            Log.e("firebase", "Error getting data", it)
        }

        var ctr = 6
        binding.deleteArtifactButton.setOnClickListener {
            if (ctr > 1) {
                ctr--
                Snackbar.make(
                    binding.root,
                    "Tap DELETE icon $ctr more times to confirm DELETION OF ARTIFACT",
                    Snackbar.LENGTH_SHORT
                ).show()
            } else {
                deleteArtifact()
                ctr = 6
                Snackbar.make(
                    binding.root,
                    "Artifact deleted",
                    Snackbar.LENGTH_SHORT
                ).show()
                startActivity(Intent(this, MainActivity::class.java))
            }
        }
    }

    private fun deleteArtifact() {
        val imageUrl = intent.getStringExtra("image_url")
        val query = FirebaseDatabase.getInstance().getReference("Artifacts")
            .orderByChild("artifactImage")
            .equalTo(imageUrl)

        // Add a listener to handle the query result
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Iterate through the matching data
                for (childSnapshot in snapshot.children) {
                    // Remove the matching data
                    childSnapshot.ref.removeValue()
                    deleteArtifactInStorage()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Snackbar.make(
                    binding.root,
                    "Failed to delete artifact",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun deleteArtifactInStorage() {
        val imageUrl = intent.getStringExtra("image_url").toString()

        // Get the storage reference from the image URL
        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)

        // Delete the file from Firebase Storage
        storageReference.delete()
            .addOnSuccessListener {
                Snackbar.make(
                    binding.root,
                    "Artifact file deleted from the cloud",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { exception ->
                Snackbar.make(
                    binding.root,
                    "Failed to delete artifact from the cloud",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
    }

    private fun showComments() {
        binding.viewCommentsButton.setOnClickListener {
            val imageUrl = intent.getStringExtra("image_url")
            val intent = Intent(this, CommentsActivity::class.java)
            intent.putExtra("image_url", imageUrl)
            intent.putExtra("origin", "artifact")
            startActivity(intent)
        }
    }
}
