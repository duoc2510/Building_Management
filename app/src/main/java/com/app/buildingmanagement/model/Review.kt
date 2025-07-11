package com.app.buildingmanagement.model

data class Review(
    val id: String = "",
    val userName: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
)