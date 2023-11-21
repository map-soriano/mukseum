package com.example.mukseum

data class User(
    var name: String? = null,
    var userRole: String = "user",
    var applyCurator: Boolean? = null,
    var favoritesList: Any? = null,
    var savedList: Any? = null,
    var madeComments: Any? = null
)