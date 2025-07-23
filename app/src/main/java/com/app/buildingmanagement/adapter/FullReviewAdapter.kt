package com.app.buildingmanagement.adapter
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import com.app.buildingmanagement.R
import com.app.buildingmanagement.model.Review // hoặc đúng đường dẫn model Review của bạn

class FullReviewAdapter(private val context: Context, private val reviews: List<Review>) :
    RecyclerView.Adapter<FullReviewAdapter.ReviewViewHolder>() {

    inner class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvReviewerName: TextView = itemView.findViewById(R.id.userName)
        val ratingBar: RatingBar = itemView.findViewById(R.id.reviewRatingBar)
        val tvComment: TextView = itemView.findViewById(R.id.comment)
        val tvTimestamp: TextView = itemView.findViewById(R.id.reviewTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]
        holder.tvReviewerName.text = review.userName
        holder.ratingBar.rating = review.rating
        holder.tvComment.text = review.comment

        // Format thời gian nếu có
        val dateText = review.timestamp?.let {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.format(Date(it))
        } ?: ""
        holder.tvTimestamp.text = dateText
    }

    override fun getItemCount(): Int = reviews.size
}