package com.app.buildingmanagement

import android.content.Context
import com.app.buildingmanagement.model.CartItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object CartManager {
    private const val PREF_NAME = "cart_prefs"
    private const val KEY_CART_LIST = "cart_list"
    private val gson = Gson()

    /** Lấy giỏ hàng hiện tại từ SharedPreferences */
    fun getCart(context: Context): MutableList<CartItem> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CART_LIST, null)
        return if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<MutableList<CartItem>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
    }

    /** Lưu giỏ hàng mới vào SharedPreferences (PUBLIC để các màn hình có thể gọi) */
    fun saveCart(context: Context, cartItems: List<CartItem>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CART_LIST, gson.toJson(cartItems)).apply()
    }

    /** Thêm sản phẩm vào giỏ (tăng số lượng nếu đã tồn tại) */
    fun addToCart(context: Context, product: com.app.buildingmanagement.model.Product) {
        val cartItems = getCart(context)
        val existing = cartItems.find { it.product.id == product.id }
        if (existing != null) {
            existing.quantity++
        } else {
            cartItems.add(CartItem(product, 1))
        }
        saveCart(context, cartItems)
    }

    /** Xóa 1 sản phẩm khỏi giỏ */
    fun removeFromCart(context: Context, productId: String) {
        val cartItems = getCart(context).filter { it.product.id != productId }
        saveCart(context, cartItems)
    }

    /** Xóa toàn bộ giỏ */
    fun clearCart(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_CART_LIST).apply()
    }
}
