package com.example.mukseum

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.mukseum.databinding.ActivitySignUpBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : AppCompatActivity() {

    // Firebase and Binding Setup
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var binding: ActivitySignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Firebase Instances
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()

        // Redirect to Sign In
        binding.signUpAlreadyRegisteredTextView.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }

        // Cancel Sign Up
        binding.signUpCancelButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // Sign Up Button
        binding.signUpButton.setOnClickListener { createUser() }
    }

    private fun createUser() {
        val name = binding.signUpEditTextName.text.toString()
        val email = binding.signUpEditTextEmailAddress.text.toString()
        val password = binding.signUpEditTextPassword.text.toString()
        val confirmPassword = binding.signUpEditTextConfirmPassword.text.toString()
        val applyForCurator = binding.applyForCuratorSwitch.isChecked

        if (!validateFields(name, email, password, confirmPassword)) {
            return
        }

        toggleProgressAndButtons(1)
        registerUser(name, email, password, applyForCurator)
    }

    private fun validateFields(name: String, email: String, password: String, confirmPassword: String): Boolean {
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            toggleProgressAndButtons(0)
            Snackbar.make(binding.root, "All fields must be filled", Snackbar.LENGTH_SHORT).show()
            return false
        }

        val emailRegex = "^[A-Za-z](.*)([@]{1})(.{1,})(\\.)(.{1,})"
        if (!email.matches(emailRegex.toRegex())) {
            toggleProgressAndButtons(0)
            Snackbar.make(binding.root, "Invalid email", Snackbar.LENGTH_SHORT).show()
            return false
        }

        if (password != confirmPassword) {
            toggleProgressAndButtons(0)
            Snackbar.make(binding.root, "Passwords do not match", Snackbar.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun registerUser(name: String, email: String, password: String, applyForCurator: Boolean) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (!it.isSuccessful) {
                    toggleProgressAndButtons(0)
                    Snackbar.make(
                        binding.root,
                        it.exception.toString().split(": ")[1],
                        Snackbar.LENGTH_SHORT
                    ).show()
                    return@addOnCompleteListener
                }

                val userProfile = firebaseDatabase.getReference("Registered Users")
                val userUID = firebaseAuth.currentUser?.uid
                val user = User(name = name, applyCurator = applyForCurator)

                if (userUID == null) {
                    toggleProgressAndButtons(0)
                    Snackbar.make(
                        binding.root,
                        "Registration failed. Please try again.",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    return@addOnCompleteListener
                }

                userProfile.child(userUID)
                    .setValue(user)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            binding.signUpProgressBar.visibility = View.GONE
                            Snackbar.make(
                                binding.root,
                                "User registered successfully",
                                Snackbar.LENGTH_SHORT
                            )
                                .show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            toggleProgressAndButtons(0)
                            Snackbar.make(
                                binding.root,
                                "Registration failed. Please try again.",
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
    }

    // 1 for ON Progress Bar and OFF Cancel Button, else OFF Progress Bar and ON Cancel Button
    private fun toggleProgressAndButtons(mode: Int) {
        when (mode) {
            1 -> {
                binding.signUpProgressBar.visibility = View.VISIBLE
                binding.signUpCancelButton.visibility = View.GONE
                binding.signUpCancelButton.isClickable = false
            }

            else -> {
                binding.signUpProgressBar.visibility = View.GONE
                binding.signUpCancelButton.visibility = View.VISIBLE
                binding.signUpCancelButton.isClickable = true
            }
        }

    }
}