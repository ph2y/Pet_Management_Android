package com.sju18001.petmanagement.ui.community.follow.follower

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.restapi.dao.Account
import com.sju18001.petmanagement.restapi.dto.*
import com.sju18001.petmanagement.ui.community.CommunityUtil
import com.sju18001.petmanagement.ui.community.follow.FollowItem
import com.sju18001.petmanagement.ui.community.follow.FollowUnfollowButtonInterface
import de.hdodenhof.circleimageview.CircleImageView

class FollowerAdapter(
    private val context: Context,
    private val buttonInterface: FollowUnfollowButtonInterface
    ): RecyclerView.Adapter<FollowerAdapter.HistoryListViewHolder>() {
    private var resultList = mutableListOf<FollowItem>()
    private var isViewDestroyed = false

    class HistoryListViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val cardView: View = view.findViewById(R.id.cardview_follow)
        val accountPhotoCircleImageView: CircleImageView = view.findViewById(R.id.circleimageview_follow_accountphoto)
        val accountNicknameTextView: TextView = view.findViewById(R.id.textview_follow_accountnickname)
        val button: Button = view.findViewById(R.id.button_follow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_follow, parent, false)

        val holder = HistoryListViewHolder(view)
        setListenerOnView(holder)

        return holder
    }

    override fun onBindViewHolder(holder: HistoryListViewHolder, position: Int) {
        // set account photo
        if(resultList[position].hasPhoto) {
            if(resultList[position].photo == null) {
                setAccountPhoto(resultList[position].id, holder, position)
            }
            else {
                holder.accountPhotoCircleImageView.setImageBitmap(resultList[position].photo)
            }
        }
        else {
            holder.accountPhotoCircleImageView.setImageDrawable(context.getDrawable(R.drawable.ic_baseline_account_circle_24))
        }

        // set account nickname
        val nicknameText = resultList[position].nickname + '님'
        holder.accountNicknameTextView.text = nicknameText

        // for follow/unfollow button
        if(resultList[position].isFollowing) {
            holder.button.setBackgroundColor(context.getColor(R.color.border_line))
            holder.button.setTextColor(context.resources.getColor(R.color.black))
            holder.button.text = context.getText(R.string.unfollow_button)
        }
        else {
            holder.button.setBackgroundColor(context.getColor(R.color.carrot))
            holder.button.setTextColor(context.resources.getColor(R.color.white))
            holder.button.text = context.getText(R.string.follow_button)
        }
    }

    private fun setListenerOnView(holder: HistoryListViewHolder){
        // start pet profile
        holder.cardView.setOnClickListener {
            val position = holder.absoluteAdapterPosition
            CommunityUtil.fetchRepresentativePetAndStartPetProfile(context, Account(
                resultList[position].id, resultList[position].username, "", "", null,
                null, resultList[position].nickname, if (resultList[position].hasPhoto) "true" else null,
                "", resultList[position].representativePetId, null, null, 0.0), isViewDestroyed)
        }

        holder.button.setOnClickListener {
            val position = holder.absoluteAdapterPosition

            // set button to loading
            holder.button.isEnabled = false

            // API call
            if(resultList[position].isFollowing) {
                deleteFollow(resultList[position].id, holder, position)
            }
            else {
                createFollow(resultList[position].id, holder, position)
            }
        }
    }

    private fun createFollow(id: Long, holder: HistoryListViewHolder, position: Int) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(context)!!)
            .createFollowReq(CreateFollowReqDto(id))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, context, {
            // update isFollowing and button state
            val currentItem = resultList[position]
            resultList[position] = FollowItem(
                currentItem.hasPhoto, currentItem.photo, currentItem.id, currentItem.username,
                currentItem.nickname, true, currentItem.representativePetId
            )
            notifyItemChanged(position)

            holder.button.isEnabled = true

            buttonInterface.updateFollowUnfollowButton()
        }, {
            holder.button.isEnabled = true
        }, {
            holder.button.isEnabled = true
        })
    }

    private fun deleteFollow(id: Long, holder: HistoryListViewHolder, position: Int) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(context)!!)
            .deleteFollowReq(DeleteFollowReqDto(id))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, context, {
            // update isFollowing and button state
            val currentItem = resultList[position]
            resultList[position] = FollowItem(
                currentItem.hasPhoto, currentItem.photo, currentItem.id, currentItem.username,
                currentItem.nickname, false, currentItem.representativePetId
            )
            notifyItemChanged(position)

            holder.button.isEnabled = true

            buttonInterface.updateFollowUnfollowButton()
        }, {
            holder.button.isEnabled = true
        }, {
            holder.button.isEnabled = true
        })
    }

    private fun setAccountPhoto(id: Long, holder: HistoryListViewHolder, position: Int) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(context)!!)
            .fetchAccountPhotoReq(FetchAccountPhotoReqDto(id))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, context, { response ->
            val photoBitmap = Util.getBitmapFromInputStream(response.body()!!.byteStream())
            holder.accountPhotoCircleImageView.setImageBitmap(photoBitmap)

            val currentItem = resultList[position]
            resultList[position] = FollowItem(
                currentItem.hasPhoto, photoBitmap, currentItem.id, currentItem.username,
                currentItem.nickname, currentItem.isFollowing, currentItem.representativePetId
            )
        }, {}, {})
    }

    override fun getItemCount() = resultList.size

    fun setResult(result: MutableList<FollowItem>){
        this.resultList = result
        notifyDataSetChanged()
    }

    fun onDestroy() {
        isViewDestroyed = true
    }
}