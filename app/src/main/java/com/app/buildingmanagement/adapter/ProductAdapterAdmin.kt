package com.app.buildingmanagement.adapter

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.buildingmanagement.ProductDetailActivity
import com.app.buildingmanagement.R
import com.app.buildingmanagement.model.Product
import com.bumptech.glide.Glide

class ProductAdapterAdmin(
    private val products: List<Product>
) : RecyclerView.Adapter<ProductAdapterAdmin.ProductViewHolder>() {

    var onUpdateClick: ((Product) -> Unit)? = null
    var onDeleteClick: ((Product) -> Unit)? = null

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.productName)
        val description: TextView = itemView.findViewById(R.id.productDescription)
        val type: TextView = itemView.findViewById(R.id.productType)
        val price: TextView = itemView.findViewById(R.id.productPrice)
        val quantity: TextView = itemView.findViewById(R.id.productQuantity)
        val status: TextView = itemView.findViewById(R.id.statusChip)
        val image: ImageView = itemView.findViewById(R.id.productImage)
        val btnUpdate: View = itemView.findViewById(R.id.btnUpdate)
        val btnDelete: View = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_admin, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.name.text = product.name
        holder.description.text = product.description
        holder.type.text = product.type
        holder.price.text = "${product.price} VND"
        holder.quantity.text = "Quantity: ${product.quantity}"
        holder.status.text = product.status
        Log.d("ProductAdapter", "Image URL: ${product.imageUrl}")

        Glide.with(holder.itemView.context)
            .load(product.imageUrl.replace("http://", "https://"))
            .placeholder(R.drawable.ic_launcher_background)
            .into(holder.image)
//        holder.itemView.setOnClickListener {
//            val context = holder.itemView.context
//            val intent = Intent(context, ProductDetailActivity::class.java)
//            intent.putExtra("product", product)
//            context.startActivity(intent)
//        }
        holder.btnUpdate.setOnClickListener {
            onUpdateClick?.invoke(product)
        }
        holder.btnDelete.setOnClickListener {
            onDeleteClick?.invoke(product)
        }
    }

    override fun getItemCount() = products.size
}