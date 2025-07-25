package com.app.buildingmanagement

import ImagePagerAdapter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.app.buildingmanagement.adapter.FullReviewAdapter
import com.app.buildingmanagement.adapter.ReviewAdapter
import androidx.appcompat.widget.Toolbar
import com.app.buildingmanagement.model.Product
import com.app.buildingmanagement.model.Review

import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.NumberFormat
import java.util.*

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var btnAddToCart: MaterialButton
    private lateinit var product: Product

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)
        actionBar?.hide()
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Chi tiết sản phẩm"

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Lấy product từ Intent


        product = intent.getParcelableExtra<Product>("product") ?: return

        // Gán dữ liệu sản phẩm vào view
        findViewById<TextView>(R.id.productName).text = product.name
        findViewById<TextView>(R.id.productDescription).text = product.description
        findViewById<TextView>(R.id.productPrice).text = formatVND(product.price)
        findViewById<TextView>(R.id.productQuantity).text = product.quantity.toString()
        findViewById<TextView>(R.id.productType).text = product.type
        findViewById<TextView>(R.id.productBrand).text = "N/A"
        findViewById<TextView>(R.id.productSku).text = "N/A"
        findViewById<Chip>(R.id.statusChip).text = product.status

        // Hiển thị ảnh
        val imageUrls = listOf(product.imageUrl)
        val viewPager = findViewById<ViewPager2>(R.id.imageViewPager)
        viewPager.adapter = ImagePagerAdapter(imageUrls)

        // Load review
        val reviewRecyclerView = findViewById<RecyclerView>(R.id.reviewRecyclerView)
        reviewRecyclerView.layoutManager = LinearLayoutManager(this)
        loadReviews(product.id)

        findViewById<MaterialButton>(R.id.btnWriteReview).setOnClickListener {
            showWriteReviewDialog(product.id)
        }

        // Nút thêm giỏ hàng (dùng CartManager chung)
        btnAddToCart = findViewById(R.id.btnAddToCart)
        btnAddToCart.setOnClickListener {
            CartManager.addToCart(this, product)
            Toast.makeText(this, "Đã thêm ${product.name} vào giỏ hàng!", Toast.LENGTH_SHORT).show()
        }
        val btnShowAllReviews = findViewById<MaterialButton>(R.id.btnShowAllReviews)
        btnShowAllReviews.setOnClickListener {
            loadAllReviewsForDialog(product.id)
        }
    }

    private fun formatVND(amount: Int): String {
        val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
        return formatter.format(amount) + " đ"
    }

    // Dialog viết đánh giá
    private fun showWriteReviewDialog(productId: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_write_review, null)
        val builder = AlertDialog.Builder(this)
            .setTitle("Viết đánh giá")
            .setView(dialogView)
            .setNegativeButton("Huỷ", null)
            .setPositiveButton("Gửi", null)
        val dialog = builder.create()
        dialog.show()

        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)
        val edtComment = dialogView.findViewById<EditText>(R.id.edtReviewContent)

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val comment = edtComment.text.toString().trim()
            val rating = ratingBar.rating

            if (comment.isEmpty() || rating == 0f) {
                Toast.makeText(this, "Vui lòng nhập đánh giá và chọn số sao", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                Toast.makeText(this, "Không xác định được người dùng", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Lấy số điện thoại từ users/{uid}
            val userRef = FirebaseDatabase.getInstance().getReference("user").child(uid)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val rawPhone = snapshot.child("phone").getValue(String::class.java)
                    val phoneNumber = if (rawPhone != null && rawPhone.startsWith("+84")) {
                        rawPhone.replaceFirst("+84", "0")
                    } else {
                        rawPhone
                    }

                    if (phoneNumber == null) {
                        Toast.makeText(this@ProductDetailActivity, "Không tìm thấy số điện thoại người dùng", Toast.LENGTH_SHORT).show()
                        return
                    }
                    Log.d("DEBUG", "Phone number from user: $phoneNumber")
                    // Truy cập phone_to_room/{phone}
                    val phoneRef = FirebaseDatabase.getInstance().getReference("phone_to_room").child(phoneNumber)
                    phoneRef.addListenerForSingleValueEvent(object : ValueEventListener {

                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                val userName = snapshot.child("name").getValue(String::class.java) ?: "Người dùng"

                                val reviewId = FirebaseDatabase.getInstance().reference.push().key!!
                                val review = Review(
                                    id = reviewId,
                                    userName = userName,
                                    rating = rating,
                                    comment = comment
                                )

                                FirebaseDatabase.getInstance()
                                    .getReference("reviews")
                                    .child(productId)
                                    .child(reviewId)
                                    .setValue(review)
                                    .addOnSuccessListener {
                                        Toast.makeText(this@ProductDetailActivity, "Đã gửi đánh giá", Toast.LENGTH_SHORT).show()
                                        dialog.dismiss()
                                        loadReviews(productId)
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this@ProductDetailActivity, "Gửi thất bại", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Toast.makeText(this@ProductDetailActivity, "Không tìm thấy tên người dùng", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(this@ProductDetailActivity, "Lỗi khi truy cập phone_to_room", Toast.LENGTH_SHORT).show()
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ProductDetailActivity, "Lỗi khi truy cập user", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }



    // Load danh sách đánh giá và cập nhật thống kê sao
    private fun loadReviews(productId: String) {
        val reviewRef = FirebaseDatabase.getInstance().getReference("reviews").child(productId)
        reviewRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val fullReviewList = mutableListOf<Review>()
                val starCountMap = mutableMapOf<Int, Int>()
                var totalRating = 0f

                for (i in 1..5) starCountMap[i] = 0

                for (child in snapshot.children) {
                    val review = child.getValue(Review::class.java)
                    if (review != null) {
                        fullReviewList.add(review)
                        val star = review.rating.toInt().coerceIn(1, 5)
                        starCountMap[star] = starCountMap[star]!! + 1
                        totalRating += review.rating
                    }
                }

                findViewById<RecyclerView>(R.id.reviewRecyclerView).adapter = ReviewAdapter(fullReviewList)

                val count = fullReviewList.size
                val average = if (count > 0) totalRating / count else 0f

                findViewById<TextView>(R.id.tvAverageRating).text = String.format("%.1f", average)
                findViewById<RatingBar>(R.id.ratingBarAverage).rating = average
                findViewById<TextView>(R.id.tvTotalRatingCount).text = "$count đánh giá"

                val total = count.takeIf { it > 0 } ?: 1
                for (star in 1..5) {
                    val countStar = starCountMap[star] ?: 0
                    val progress = (countStar * 100) / total
                    findViewById<ProgressBar>(resources.getIdentifier("progressBar$star", "id", packageName)).progress = progress
                    findViewById<TextView>(resources.getIdentifier("tvCount$star", "id", packageName)).text = countStar.toString()
                }
                val recentReviews = fullReviewList.sortedByDescending { it.timestamp }.take(4)
                findViewById<RecyclerView>(R.id.reviewRecyclerView).adapter = ReviewAdapter(recentReviews)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
    private fun loadAllReviewsForDialog(productId: String) {
        val reviewRef = FirebaseDatabase.getInstance()
            .getReference("reviews")
            .child(productId)

        reviewRef.orderByChild("timestamp")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val fullReviewList = mutableListOf<Review>()

                    for (child in snapshot.children) {
                        val review = child.getValue(Review::class.java)
                        if (review != null) {
                            fullReviewList.add(review)
                        }
                    }

                    fullReviewList.sortByDescending { it.timestamp }

                    // 👉 Gọi dialog ở đây
                    showFullReviewDialog(fullReviewList)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ProductDetailActivity, "Lỗi tải tất cả đánh giá", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showFullReviewDialog(reviewList: List<Review>) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_viewall_review, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.reviewsRecyclerView)
        val emptyStateLayout = dialogView.findViewById<LinearLayout>(R.id.emptyState)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btnClose)
        val averageText = dialogView.findViewById<TextView>(R.id.averageRating)
        val averageBar = dialogView.findViewById<RatingBar>(R.id.averageRatingBar)
        val totalReviews = dialogView.findViewById<TextView>(R.id.totalReviews)

        btnClose.setOnClickListener { dialog.dismiss() }

        if (reviewList.isEmpty()) {
            emptyStateLayout.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE

            // Gán mặc định khi không có đánh giá
            averageText.text = "0.0"
            averageBar.rating = 0f
            totalReviews.text = "Dựa trên 0 đánh giá"
            for (star in 1..5) {
                val progressBarId = dialogView.resources.getIdentifier("progressBar$star", "id", packageName)
                val tvCountId = dialogView.resources.getIdentifier("tvCount$star", "id", packageName)
                dialogView.findViewById<ProgressBar>(progressBarId)?.progress = 0
                dialogView.findViewById<TextView>(tvCountId)?.text = "0"
            }

        } else {
            emptyStateLayout.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE

            // Setup recycler
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = FullReviewAdapter(this, reviewList)

            // Tính trung bình
            val avg = reviewList.map { it.rating }.average().toFloat()
            averageText.text = String.format("%.1f", avg)
            averageBar.rating = avg
            totalReviews.text = "Dựa trên ${reviewList.size} đánh giá"

            // Đếm số lượt đánh giá theo từng sao
            val starCounts = mutableMapOf<Int, Int>().apply {
                for (i in 1..5) this[i] = 0
            }
            for (review in reviewList) {
                val rating = review.rating.toInt().coerceIn(1, 5)
                starCounts[rating] = starCounts[rating]!! + 1
            }

            val total = reviewList.size

            for (star in 5 downTo 1) {
                val count = starCounts[star] ?: 0
                val percent = if (total > 0) ((count * 100.0) / total).toInt() else 0

                val progressBarId = dialogView.resources.getIdentifier("progressBar$star", "id", packageName)
                val tvCountId = dialogView.resources.getIdentifier("tvCount$star", "id", packageName)

                val progressBar = dialogView.findViewById<ProgressBar>(progressBarId)
                val tvCount = dialogView.findViewById<TextView>(tvCountId)

                progressBar?.apply {
                    max = 100
                    progress = percent
                }
                tvCount?.text = count.toString()

                Log.d("ReviewDialog", "Star $star: count=$count, percent=$percent")
            }
        }

        dialog.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()  // Quay lại trang trước
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

