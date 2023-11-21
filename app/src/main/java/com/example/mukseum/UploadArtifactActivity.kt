package com.example.mukseum

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.mukseum.databinding.ActivityUploadArtifactBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class UploadArtifactActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var firebaseStorage: FirebaseStorage
    private lateinit var selectedImage: Uri
    private lateinit var binding: ActivityUploadArtifactBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadArtifactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()

        // Cancel Upload
        binding.uploadCancelButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.artifactImageUploadButton.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "image/*"

            resultLauncher.launch(intent)
        }

        binding.artifactUploadButton.setOnClickListener {
            toggleProgressAndButtons(1)
            val artifactName = binding.artifactNameEditText.text.toString()
            val artifactTags = binding.artifactTagsEditText.text.toString()
            val artifactDescription = binding.artifactDescriptionEditText.text.toString()
            val artifactDidYouKnow = binding.artifactDidYouKnowEditText.text.toString()

            // Created separate cases to let user know which fields are missing
            if (artifactName.isEmpty()) {
                toggleProgressAndButtons(0)
                Snackbar.make(
                    binding.root,
                    "Please enter the artifact's name",
                    Snackbar.LENGTH_SHORT
                ).show()
            } else if (artifactTags.isEmpty()) {
                toggleProgressAndButtons(0)
                Snackbar.make(
                    binding.root,
                    "Please enter artifact's tag",
                    Snackbar.LENGTH_SHORT
                ).show()
            } else if (artifactDescription.isEmpty()) {
                toggleProgressAndButtons(0)
                Snackbar.make(
                    binding.root,
                    "Please enter artifact's description",
                    Snackbar.LENGTH_SHORT
                ).show()
            } else if (binding.artifactImageUploadButton.drawable == null || binding.artifactImageUploadButton.drawable.constantState?.hashCode() == ContextCompat.getDrawable(
                    this,
                    android.R.drawable.ic_menu_gallery
                )?.constantState?.hashCode()
            ) {
                toggleProgressAndButtons(0)
                Snackbar.make(
                    binding.root,
                    "Please select an artifact image",
                    Snackbar.LENGTH_SHORT
                ).show()
            } else uploadArtifactImage(
                artifactName,
                artifactTags,
                artifactDescription,
                artifactDidYouKnow
            )
        }
    }

    private fun uploadArtifactImage( // Uploads the artifact image to the Firebase Storage
        artifactName: String,
        artifactTags: String,
        artifactDescription: String,
        artifactDidYouKnow: String
    ) {
        val userUID = firebaseAuth.currentUser?.uid
        val referenceArtifact = firebaseStorage.reference.child("Artifacts")
        if (userUID != null) {
            referenceArtifact.child(userUID).child(artifactName).putFile(selectedImage)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        Snackbar.make(
                            binding.root,
                            "Image uploaded successfully",
                            Snackbar.LENGTH_SHORT
                        )
                            .show()
                        referenceArtifact.child(userUID)
                            .child(artifactName).downloadUrl.addOnSuccessListener { task ->
                                uploadArtifact(
                                    userUID,
                                    task.toString(),
                                    artifactName,
                                    artifactTags,
                                    artifactDescription,
                                    artifactDidYouKnow
                                )
                            }
                    } else {
                        toggleProgressAndButtons(0)
                        Snackbar.make(binding.root, "Image upload failed", Snackbar.LENGTH_SHORT)
                            .show()
                    }
                }
        }
    }

    private fun uploadArtifact( // Uploads the artifact details (e.g., name, tags) to the Firebase Database
        artifactOwner: String,
        artifactImgUrl: String,
        artifactName: String,
        artifactTags: String,
        artifactDescription: String,
        artifactDidYouKnow: String
    ) {
        val artifact =
            Artifact(
                artifactOwner = artifactOwner,
                artifactImage = artifactImgUrl,
                artifactName = artifactName,
                artifactTags = artifactTags,
                artifactDescription = artifactDescription,
                artifactDidYouKnow = artifactDidYouKnow
            )
        val artifactReference = FirebaseDatabase.getInstance().getReference("Artifacts")
        artifactReference.child(artifactName).setValue(artifact).addOnSuccessListener {
            binding.uploadArtifactProgressBar.visibility = View.GONE
            Snackbar.make(binding.root, "Artifact uploaded successfully", Snackbar.LENGTH_SHORT)
                .show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    // Loads the image selected by the user to the UI
    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                if (data != null) {
                    if (data.data != null) {
                        selectedImage = data.data!!

                        binding.artifactImageUploadButton.setImageURI(selectedImage)
                    }
                }
            }
        }

    // 1 for ON Progress Bar and OFF Cancel Button, else OFF Progress Bar and ON Cancel Button
    private fun toggleProgressAndButtons(mode: Int) {
        when (mode) {
            1 -> {
                binding.uploadArtifactProgressBar.visibility = View.VISIBLE
                binding.uploadCancelButton.visibility = View.GONE
                binding.uploadCancelButton.isClickable = false
            }

            else -> {
                binding.uploadArtifactProgressBar.visibility = View.GONE
                binding.uploadCancelButton.visibility = View.VISIBLE
                binding.uploadCancelButton.isClickable = true
            }
        }

    }
}