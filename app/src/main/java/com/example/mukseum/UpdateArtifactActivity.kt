package com.example.mukseum

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.mukseum.databinding.ActivityUploadArtifactBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UpdateArtifactActivity : AppCompatActivity() {

    // Uses same layout as Upload Artifact
    private lateinit var binding: ActivityUploadArtifactBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadArtifactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setArtifactInformation()
        binding.artifactUploadButton.text = getString(R.string.update)

        // Cancel Update
        binding.uploadCancelButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun setArtifactInformation() {

        val imageUrl = intent.getStringExtra("image_url")

        // Setting the Name, Description, Tag, and Did You Know
        val query = FirebaseDatabase.getInstance().getReference("Artifacts")
            .orderByChild("artifactImage")
            .equalTo(imageUrl)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (itemSnapshot in snapshot.children) {
                    val name = itemSnapshot.child("artifactName").getValue(String::class.java)
                    val desc =
                        itemSnapshot.child("artifactDescription").getValue(String::class.java)
                    val tags = itemSnapshot.child("artifactTags").getValue(String::class.java)
                    val dyk = itemSnapshot.child("artifactDidYouKnow").getValue(String::class.java)
                    // Handle the matched item with the URL
                    binding.artifactImageUploadButton.visibility = View.INVISIBLE
                    binding.artifactImageUploadButton.isClickable = false
                    binding.artifactNameEditText.setText(name)
                    binding.artifactDescriptionEditText.setText(desc)
                    binding.artifactTagsEditText.setText(tags)
                    binding.artifactDidYouKnowEditText.setText(
                        dyk ?: "No extra information available"
                    )

                    binding.artifactUploadButton.setOnClickListener {
                        val updates = HashMap<String, Any>()
                        updates["artifactName"] = binding.artifactNameEditText.text.toString()
                        updates["artifactDescription"] =
                            binding.artifactDescriptionEditText.text.toString()
                        updates["artifactTags"] = binding.artifactTagsEditText.text.toString()
                        updates["artifactDidYouKnow"] =
                            binding.artifactDidYouKnowEditText.text.toString()

                        var reference = snapshot.value.toString().split("{")
                        reference = reference[1].split("=")
                        snapshot.ref.child(reference[0]).updateChildren(updates)
                        finish()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Snackbar.make(binding.root, "Failed to retrieve name", Snackbar.LENGTH_SHORT)
                    .show()
            }
        })
    }

}