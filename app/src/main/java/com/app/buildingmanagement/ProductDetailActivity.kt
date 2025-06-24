package com.app.buildingmanagement

import ImagePagerAdapter
import android.os.Bundle
import android.widget.TextView
import android.widget.RatingBar
import com.google.android.material.chip.Chip
import androidx.appcompat.app.AppCompatActivity
import com.app.buildingmanagement.model.Product
import android.widget.ImageView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
class ProductDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        val product = intent.getParcelableExtra<Product>("product") ?: return

        findViewById<TextView>(R.id.productName).text = product.name
        findViewById<TextView>(R.id.productDescription).text = product.description
        findViewById<TextView>(R.id.productPrice).text = "${product.price} VND"
        findViewById<TextView>(R.id.productQuantity).text = product.quantity.toString()
        findViewById<TextView>(R.id.productType).text = product.type
        findViewById<TextView>(R.id.productBrand).text = "N/A" // Update if you have brand info
        findViewById<TextView>(R.id.productSku).text = "N/A"   // Update if you have SKU info

        // Set status chip
        findViewById<Chip>(R.id.statusChip).text = product.status

        // Set rating if you have it in Product (currently not present)
        findViewById<RatingBar>(R.id.ratingBar).rating = 4.5f // Or use product.rating if available
        findViewById<TextView>(R.id.ratingText).text = "4.5 (128 đánh giá)" // Update if dynamic
        val imageUrls = listOf(product.imageUrl) // Wrap single URL in a list
        val viewPager = findViewById<ViewPager2>(R.id.imageViewPager)
        viewPager.adapter = ImagePagerAdapter(imageUrls)

    }
}