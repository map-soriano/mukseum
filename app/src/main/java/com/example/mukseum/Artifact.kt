package com.example.mukseum

data class Artifact(
    var artifactOwner: String? = null,
    var artifactImage: String? = null,
    var artifactName: String? = null,
    var artifactTags: String? = null,
    var artifactDescription: String? = null,
    var artifactDidYouKnow: String? = null,
    var artifactMap: String? = null,
    var artifactComments: Any? = null
)