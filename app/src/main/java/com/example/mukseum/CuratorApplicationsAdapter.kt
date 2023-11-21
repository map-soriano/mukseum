package com.example.mukseum

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CuratorApplicationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val nameTextView: TextView = itemView.findViewById(R.id.apply_for_curator_name_text_view)
    val acceptButton: Button = itemView.findViewById(R.id.curator_approve_button)
    val declineButton: Button = itemView.findViewById(R.id.curator_deny_button)
}

class CuratorApplicationsAdapter(private val parentView: View, private val curatorApplications: List<String>) :
    RecyclerView.Adapter<CuratorApplicationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CuratorApplicationViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_curator, parent, false)
        return CuratorApplicationViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: CuratorApplicationViewHolder, position: Int) {
        val curatorApplication = curatorApplications[position]

        // Setting the Name
        val query = FirebaseDatabase.getInstance().getReference("Registered Users")
            .orderByKey()
            .equalTo(curatorApplication)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (itemSnapshot in snapshot.children) {
                    val name = itemSnapshot.child("name").getValue(String::class.java)
                    // Bind the data to the views
                    holder.nameTextView.text = name
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Snackbar.make(holder.itemView, "Failed to retrieve name", Snackbar.LENGTH_SHORT)
                    .show()
            }
        })



        // Set click listeners for accept and decline buttons
        holder.acceptButton.setOnClickListener {
            updateUserRole(1, curatorApplication)
            parentView.context.startActivity(Intent(parentView.context, CuratorManagementActivity::class.java))
        }

        holder.declineButton.setOnClickListener {
            updateUserRole(0, curatorApplication)
            parentView.context.startActivity(Intent(parentView.context, CuratorManagementActivity::class.java))
        }
    }

    override fun getItemCount(): Int {
        return curatorApplications.size
    }

    private fun updateUserRole(mode: Int, uid: String) { // Mode here refers to whether the application was accepted [1] or rejected [0]
        val databaseReference = FirebaseDatabase.getInstance().reference.child("Registered Users").child(uid)

        val updatedValue = "curator"

        val updates = HashMap<String, Any>()
        if (mode == 1)
            updates["userRole"] = updatedValue

        updates["applyCurator"] = false

        databaseReference.updateChildren(updates)
            .addOnSuccessListener {
                Snackbar.make(parentView, "User role updated", Snackbar.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Snackbar.make(parentView, "Failed to update user role", Snackbar.LENGTH_SHORT).show()
            }

    }
}