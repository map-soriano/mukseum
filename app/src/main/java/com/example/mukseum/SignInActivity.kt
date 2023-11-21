package com.example.mukseum

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.mukseum.databinding.ActivitySignInBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class SignInActivity : AppCompatActivity() {

    // Firebase Setup
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivitySignInBinding

    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        // Redirect to Sign Up
        binding.signInNotRegisteredTextView.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        // Cancel Sign In
        binding.signInCancelButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // Sign In Button
        binding.signInButton.setOnClickListener {
            toggleProgressAndButtons(1)
            // Get Email
            val email = binding.signInEditTextEmailAddress.text.toString()

            // Get Password
            val password = binding.signInEditTextPassword.text.toString()

            // Check if all fields are filled
            if (email.isNotEmpty() && password.isNotEmpty()) {
                firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener {

                    if (it.isSuccessful) {
                        binding.signInProgressBar.visibility = View.GONE
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        toggleProgressAndButtons(0)
                        Snackbar.make(
                            binding.root,
                            it.exception.toString().split(": ")[1],
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                toggleProgressAndButtons(0)
                Snackbar.make(binding.root, "All fields must be filled", Snackbar.LENGTH_SHORT)
                    .show()
            }
        }
    }

    // Keeps the user logged in instead of starting at SignInActivity
    override fun onStart() {
        super.onStart()

        if (firebaseAuth.currentUser != null)
            startActivity(Intent(this, MainActivity::class.java))
    }

    // 1 for ON Progress Bar and OFF Cancel Button, else OFF Progress Bar and ON Cancel Button
    private fun toggleProgressAndButtons(mode: Int) {
        when (mode) {
            1 -> {
                binding.signInProgressBar.visibility = View.VISIBLE
                binding.signInCancelButton.visibility = View.GONE
                binding.signInCancelButton.isClickable = false
            }

            else -> {
                binding.signInProgressBar.visibility = View.GONE
                binding.signInCancelButton.visibility = View.VISIBLE
                binding.signInCancelButton.isClickable = true
            }
        }
    }
}