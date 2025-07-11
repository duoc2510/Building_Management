package com.app.buildingmanagement.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class  Product(
    var id: String = "",
    val name: String = "",
    val description: String = "",
    val type: String = "",
    val price: Int = 0,
    val quantity: Int = 0,
    val status: String = "",
    val imageUrl: String = ""
) : Parcelable