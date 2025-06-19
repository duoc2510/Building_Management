package com.app.buildingmanagement.model

data class Product(
    val name: String = "",
    val description: String = "",
    val type: String = "",
    val price: Double = 0.0,
    val quantity: Int = 0,
    val imageUrl: String = "", // Path to image in Firebase Storage
    val status: String = ""    // e.g. "available", "out_of_stock"
)