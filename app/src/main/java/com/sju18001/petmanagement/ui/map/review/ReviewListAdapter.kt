package com.sju18001.petmanagement.ui.map.review

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.restapi.dao.Review

class ReviewListAdapter(
    private var dataSet: ArrayList<Review>
): RecyclerView.Adapter<ReviewListAdapter.ViewHolder>() {
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

    private fun updateDataSetToViewHolder(holder: ViewHolder, position: Int){
        val data = dataSet[position]

        // profileImage
        holder.nicknameText.text = data.author.nickname
        // starImage
        holder.contentsText.text = data.contents
        Util.setViewMore(holder.contentsText, holder.viewMoreText, 3)
        holder.timestampText.text = Util.getTimestampForDisplay(data.timestamp) + " 전"
    }

    override fun getItemCount(): Int = dataSet.size
}