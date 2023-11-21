package com.example.mukseum

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mukseum.databinding.ActivityCommentsBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class CommentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCommentsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getUserRole()

        getComments()

        binding.commentsCancelButton.setOnClickListener { finish() }
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
                                if (intent.getStringExtra("origin").toString() == "artifact") {
                                    binding.commentEditText.visibility = View.VISIBLE
                                    binding.commentEditText.isClickable = true
                                    binding.commentsSubmitButton.visibility = View.VISIBLE
                                    binding.commentsSubmitButton.isClickable = true
                                    binding.commentsSubmitButton.setOnClickListener { submitComment() }
                                }
                            }

                            else -> {
                                binding.commentEditText.visibility = View.GONE
                                binding.commentEditText.isClickable = false
                                binding.commentsSubmitButton.visibility = View.GONE
                                binding.commentsSubmitButton.isClickable = false
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

    private fun getComments() {
        var firebaseAuth = FirebaseAuth.getInstance()
        val imageUrl = intent.getStringExtra("image_url")
        val origin = intent.getStringExtra("origin")
        val commentList = mutableListOf<Comment>()


        when (origin) {
            "artifact" -> { // When checking comments for each artifact
                Firebase.database.reference.child("Comments").get().addOnSuccessListener {
                    for (comment in it.children) {
                        if (comment.child("postID").getValue(String::class.java) == imageUrl) {
                            val author = comment.child("author").getValue(String::class.java)
                            val content = comment.child("content").getValue(String::class.java)
                            commentList.add(Comment(author = author, content = content))
                        }
                    }

                    val commentsRecyclerView: RecyclerView =
                        findViewById(R.id.comments_recycler_view)
                    val layoutManager = LinearLayoutManager(this)
                    val adapter = CommentsAdapter(commentList)
                    layoutManager.reverseLayout = true
                    layoutManager.stackFromEnd = true
                    commentsRecyclerView.layoutManager = layoutManager
                    commentsRecyclerView.adapter = adapter

                }.addOnFailureListener {
                    Log.e("firebase", "Error getting data", it)
                }
            }

            "profile" -> { // When checking comments made by user
                Firebase.database.reference.child("Registered Users")
                    .child(firebaseAuth.currentUser?.uid.toString()).child("madeComments").get()
                    .addOnSuccessListener {
                        for (comment in it.children) {
                            val author = comment.child("author").getValue(String::class.java)
                            val content = comment.child("content").getValue(String::class.java)
                            commentList.add(Comment(author = author, content = content))
                        }

                        val commentsRecyclerView: RecyclerView =
                            findViewById(R.id.comments_recycler_view)
                        val layoutManager = LinearLayoutManager(this)
                        val adapter = CommentsAdapter(commentList)
                        layoutManager.reverseLayout = true
                        layoutManager.stackFromEnd = true
                        commentsRecyclerView.layoutManager = layoutManager
                        commentsRecyclerView.adapter = adapter
                    }
            }

            else -> { // When checking comments made by other users on current curator's artifacts
                firebaseAuth = FirebaseAuth.getInstance()
                val uid = firebaseAuth.currentUser?.uid.toString()
                val artifactList = mutableListOf<String>()

                // Get artifacts owned by user
                Firebase.database.reference.child("Artifacts").get()
                    .addOnSuccessListener { dataSnapshot ->
                        for (artifact in dataSnapshot.children) {
                            if (artifact.child("artifactOwner").getValue(String::class.java) == uid)
                                artifact.child("artifactImage").getValue(String::class.java)
                                    ?.let { it1 -> artifactList.add(it1) }
                        }

                        // Get comments on each artifact
                        Firebase.database.reference.child("Comments").get().addOnSuccessListener {
                            for (comment in it.children) {
                                if (comment.child("postID")
                                        .getValue(String::class.java) in artifactList
                                ) {
                                    val author =
                                        comment.child("author").getValue(String::class.java)
                                    val content =
                                        comment.child("content").getValue(String::class.java)
                                    commentList.add(Comment(author = author, content = content))
                                }
                            }

                            val commentsRecyclerView: RecyclerView =
                                findViewById(R.id.comments_recycler_view)
                            val layoutManager = LinearLayoutManager(this)
                            val adapter = CommentsAdapter(commentList)
                            layoutManager.reverseLayout = true
                            layoutManager.stackFromEnd = true
                            commentsRecyclerView.layoutManager = layoutManager
                            commentsRecyclerView.adapter = adapter
                        }


                    }.addOnFailureListener {
                    Log.e("firebase", "Error getting data", it)
                }
            }
        }
    }

    private fun submitComment() {
        val firebaseAuth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance()
        val uid = firebaseAuth.currentUser?.uid.toString()
        val usersRef = database.reference.child("Registered Users")
        val userRef = uid.let { usersRef.child(it) }

        userRef.child("name").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val content = binding.commentEditText.text.toString()

                if (content.isBlank() || content.isEmpty()) {
                    Snackbar.make(binding.root, "Please enter a comment", Snackbar.LENGTH_SHORT)
                        .show()
                    return
                }

                val commentAuthor = snapshot.getValue(String::class.java)
                val comment = Comment(
                    postID = intent.getStringExtra("image_url").toString(),
                    author = commentAuthor,
                    content = content
                )
                // Putting Comment into Comment Node
                var databaseReference =
                    FirebaseDatabase.getInstance().reference.child("Comments")
                // Create a new child with an automatically generated key
                var childReference = databaseReference.push()
                // Set the values for the child
                childReference.setValue(comment)


                // Putting Comment into User Node
                databaseReference =
                    FirebaseDatabase.getInstance().reference.child("Registered Users")
                        .child(uid).child("madeComments")
                // Create a new child with an automatically generated key
                childReference = databaseReference.push()
                // Set the values for the child
                childReference.setValue(comment)

                binding.commentEditText.setText("")
                recreate()
            }

            override fun onCancelled(error: DatabaseError) {
                Snackbar.make(binding.root, "Failed to add comment", Snackbar.LENGTH_SHORT)
                    .show()
            }
        })
    }
}