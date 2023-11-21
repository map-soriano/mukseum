package com.example.mukseum

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mukseum.databinding.ActivityCuratorManagementBinding
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class CuratorManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCuratorManagementBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCuratorManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewCuratorApplicationsList()

        // Go Back
        binding.curatorManagementCancelButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }

    private fun viewCuratorApplicationsList() {
        val recyclerView = binding.curatorManagementRecycler
        val curatorUIDList = mutableListOf<String>()

        // Fetches all curator applications from registered users
        Firebase.database.reference.child("Registered Users").get().addOnSuccessListener {
            for (curator in it.children) {
                if (curator.child("applyCurator").getValue(Boolean::class.java) == true) {
                    curatorUIDList.add(curator.key.toString())
                }
            }


            val layoutManager = LinearLayoutManager(this)
            val adapter = CuratorApplicationsAdapter(binding.root, curatorUIDList)

            recyclerView.layoutManager = layoutManager
            recyclerView.adapter = adapter

        }.addOnFailureListener {
            Log.e("firebase", "Error getting data", it)
        }
    }
}