package com.sju18001.petmanagement.ui.map.review

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.restapi.dao.Account
import com.sju18001.petmanagement.restapi.dao.Review

interface ReviewListAdapterInterface {
    fun setAccountPhoto(id: Long, holder: ReviewListAdapter.ViewHolder)
    fun setAccountDefaultPhoto(holder: ReviewListAdapter.ViewHolder)
}

class ReviewListAdapter(
    private var dataSet: ArrayList<Review>,
    private val context: Context
): RecyclerView.Adapter<ReviewListAdapter.ViewHolder>() {
    lateinit var reviewListAdapterInterface: ReviewListAdapterInterface

    class ViewHolder(view: View): RecyclerView.ViewHolder(view){
        val profileImage: ImageView = view.findViewById(R.id.image_profile)
        val nicknameText: TextView = view.findViewById(R.id.text_nickname)
        val starImage1: ImageView = view.findViewById(R.id.image_star1)
        val starImage2: ImageView = view.findViewById(R.id.image_star2)
        val starImage3: ImageView = view.findViewById(R.id.image_star3)
        val starImage4: ImageView = view.findViewById(R.id.image_star4)
        val starImage5: ImageView = view.findViewById(R.id.image_star5)
        val dialogButton: ImageButton = view.findViewById(R.id.dialog_button)
        val contentsText: TextView = view.findViewById(R.id.text_contents)
        val viewMoreText: TextView = view.findViewById(R.id.view_more)
        val timestampText: TextView = view.findViewById(R.id.text_timestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.review_item, parent, false)

        val holder = ViewHolder(view)
        setListenerOnView(holder)

        return holder
    }

    private fun setListenerOnView(holder:ViewHolder) {
        holder.profileImage.setOnClickListener {

        }
        holder.nicknameText.setOnClickListener {

        }

        holder.dialogButton.setOnClickListener {

        }

        holder.viewMoreText.setOnClickListener {

        }
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        updateDataSetToViewHolder(holder, position)
    }

    private fun updateDataSetToViewHolder(holder: ViewHolder, position: Int) {
        val data = dataSet[position]

        holder.nicknameText.text = data.author.nickname
        holder.contentsText.text = data.contents
        holder.timestampText.text = Util.getTimestampForDisplay(data.timestamp) + " 전"

        setAccountPhoto(holder, data.author)
        Util.setViewMore(holder.contentsText, holder.viewMoreText, 3)
        setRatingStars(data.rating.toFloat(), holder)
    }

    private fun setAccountPhoto(holder: ViewHolder, author: Account) {
        if(!author.photoUrl.isNullOrEmpty()){
            reviewListAdapterInterface.setAccountPhoto(author.id, holder)
        }else{
            reviewListAdapterInterface.setAccountDefaultPhoto(holder)
        }
    }

    private fun setRatingStars(rating: Float, holder: ViewHolder) {
        val starImages = getStarImages(holder)
        for(i in 0 until starImages.size){
            val drawableId = getDrawableIdOfStarImage(rating, i)
            val drawable = context.resources.getDrawable(drawableId, context.theme)
            starImages[i].setImageDrawable(drawable)
        }
    }

    private fun getStarImages(holder: ViewHolder) = arrayListOf(holder.starImage1, holder.starImage2, holder.starImage3, holder.starImage4, holder.starImage5)

    private fun getDrawableIdOfStarImage(rating: Float, index: Int): Int{
        return if(rating > index+0.75){
            R.drawable.ic_baseline_star_16
        }else if(rating > index+0.25){
            R.drawable.ic_baseline_star_half_16
        }else{
            R.drawable.ic_baseline_star_border_16
        }
    }

    override fun getItemCount(): Int = dataSet.size
}