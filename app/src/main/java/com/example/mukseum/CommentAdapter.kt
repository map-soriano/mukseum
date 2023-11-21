package com.example.mukseum

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val authorTextView: TextView = itemView.findViewById(R.id.comment_author_text_view)
    val contentTextView: TextView = itemView.findViewById(R.id.comment_content_text_view)
}

class CommentsAdapter(private val comments: MutableList<Comment>) :
    RecyclerView.Adapter<CommentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.authorTextView.text = comment.author
        holder.contentTextView.text = comment.content
    }

    override fun getItemCount(): Int {
        return comments.size
    }
}