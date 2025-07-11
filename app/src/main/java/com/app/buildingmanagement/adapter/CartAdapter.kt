package com.app.buildingmanagement.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.buildingmanagement.R
import com.app.buildingmanagement.model.Product
import java.text.NumberFormat
import java.util.*

class CartAdapter(private val cartList: List<Product>) :
    RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart_product, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val product = cartList[position]
        holder.bind(product)
    }

    override fun getItemCount(): Int = cartList.size

    class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.cartProductName)
        private val priceText: TextView = itemView.findViewById(R.id.cartProductPrice)

        fun bind(product: Product) {
            nameText.text = product.name
            priceText.text = formatVND(product.price)
        }

        private fun formatVND(price: Int): String {
            val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
            return formatter.format(price) + " đ"
        }
    }
}
