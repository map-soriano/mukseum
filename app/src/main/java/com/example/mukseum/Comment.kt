package com.example.mukseum

data class Comment(
    var commentId: String? = null,
    var postID: String? = null,
    var userID: String? = null,
    var author: String? = null,
    var content: String? = null,
    var likes: Int? = null,
    var isApproved: Boolean? = false
)
