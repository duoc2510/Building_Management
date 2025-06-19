package com.app.buildingmanagement.model

data class Product(
    var id: String = "",
    val name: String = "",
    val description: String = "",
    val type: String = "",
    val price: Double = 0.0,
    val quantity: Int = 0,
    val imageUrl: String = "",
    val status: String = ""
)
