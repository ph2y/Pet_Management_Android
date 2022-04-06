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
    fun onClickReviewFunctionButton(review: Review, position: Int)
    fun startPetProfile(author: Account)
}

class ReviewListAdapter(
    private var dataSet: ArrayList<Review>,
    private val context: Context
): RecyclerView.Adapter<ReviewListAdapter.ViewHolder>() {
    lateinit var reviewListAdapterInterface: ReviewListAdapterInterface

    class ViewHolder(view: View): RecyclerView.ViewHolder(view){
        val accountPhoto: ImageView = view.findViewById(R.id.circleimageview_review_accountphoto)
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
        holder.accountPhoto.setOnClickListener {
            val position = holder.absoluteAdapterPosition
            reviewListAdapterInterface.startPetProfile(dataSet[position].author)
        }
        holder.nicknameText.setOnClickListener {
            val position = holder.absoluteAdapterPosition
            reviewListAdapterInterface.startPetProfile(dataSet[position].author)
        }

        holder.dialogButton.setOnClickListener {
            val position = holder.absoluteAdapterPosition
            val item = dataSet[position]
            reviewListAdapterInterface.onClickReviewFunctionButton(item, position)
        }
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        updateViewHolderByDataSet(holder, position)
    }

    private fun updateViewHolderByDataSet(holder: ViewHolder, position: Int) {
        val data = dataSet[position]

        holder.nicknameText.text = data.author.nickname
        holder.contentsText.text = data.contents
        holder.timestampText.text = Util.getTimestampForDisplay(data.timestamp) + " 전"

        setAccountPhoto(holder, data.author)
        Util.setViewMore(holder.contentsText, holder.viewMoreText, 3)
        Util.setRatingStars(getStarImages(holder), data.rating.toDouble(), context)
    }

    private fun setAccountPhoto(holder: ViewHolder, author: Account) {
        if(!author.photoUrl.isNullOrEmpty()){
            reviewListAdapterInterface.setAccountPhoto(author.id, holder)
        }else{
            reviewListAdapterInterface.setAccountDefaultPhoto(holder)
        }
    }

    private fun getStarImages(holder: ViewHolder) = arrayListOf(holder.starImage1, holder.starImage2, holder.starImage3, holder.starImage4, holder.starImage5)

    override fun getItemCount(): Int = dataSet.size


    fun addItem(review: Review) {
        dataSet.add(review)
    }

    fun addItemToTop(review: Review) {
        dataSet.add(0, review)
    }

    fun resetItem() {
        dataSet = arrayListOf()
    }

    fun removeItem(index: Int) {
        dataSet.removeAt(index)
    }

    fun setItem(position: Int, review: Review) {
        dataSet[position] = review
    }

    fun getItem(position: Int): Review {
        return dataSet[position]
    }
}