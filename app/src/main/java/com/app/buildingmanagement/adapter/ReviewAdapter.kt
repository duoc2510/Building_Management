package com.app.buildingmanagement.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.buildingmanagement.model.Review
import com.app.buildingmanagement.R
class ReviewAdapter(private val reviews: List<Review>) :
    RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    inner class ReviewViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userName: TextView = view.findViewById(R.id.userName)
        val comment: TextView = view.findViewById(R.id.comment)
        val ratingBar: RatingBar = view.findViewById(R.id.reviewRatingBar)
        val reviewTime: TextView = view.findViewById(R.id.reviewTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]
        holder.userName.text = review.userName
        holder.comment.text = review.comment
        holder.ratingBar.rating = review.rating
        holder.reviewTime.text = getRelativeTimeString(review.timestamp)
    }

    override fun getItemCount(): Int = reviews.size

    private fun getRelativeTimeString(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val days = diff / (1000 * 60 * 60 * 24)
        return when {
            days == 0L -> "Hôm nay"
            days == 1L -> "Hôm qua"
            days in 2..6 -> "$days ngày trước"
            else -> "${days / 7} tuần trước"
        }
    }
}
