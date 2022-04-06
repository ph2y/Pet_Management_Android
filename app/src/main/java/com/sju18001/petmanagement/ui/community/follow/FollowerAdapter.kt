package com.sju18001.petmanagement.ui.community.follow

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.restapi.RetrofitBuilder
import com.sju18001.petmanagement.restapi.ServerUtil
import com.sju18001.petmanagement.controller.SessionManager
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.restapi.dao.Account
import com.sju18001.petmanagement.restapi.dto.*
import com.sju18001.petmanagement.ui.community.CommunityUtil
import de.hdodenhof.circleimageview.CircleImageView

class FollowerAdapter(val context: Context, private val followUnfollowButtonInterface: FollowUnfollowButtonInterface):
    RecyclerView.Adapter<FollowerAdapter.HistoryListViewHolder>() {

    private var resultList = mutableListOf<FollowListItem>()

    private var isViewDestroyed = false

    class HistoryListViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val mainLayout: View = view.findViewById(R.id.main_layout)
        val accountPhoto: CircleImageView = view.findViewById(R.id.account_photo)
        val accountNickname: TextView = view.findViewById(R.id.account_nickname)
        val followUnfollowButton: Button = view.findViewById(R.id.follow_unfollow_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowerAdapter.HistoryListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.follow_list_item, parent, false)

        val holder = FollowerAdapter.HistoryListViewHolder(view)
        setListenerOnView(holder)

        return holder
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBindViewHolder(holder: FollowerAdapter.HistoryListViewHolder, position: Int) {
        // set account photo
        if(resultList[position].getHasPhoto()) {
            if(resultList[position].getPhoto() == null) {
                setAccountPhoto(resultList[position].getId(), holder, position)
            }
            else {
                holder.accountPhoto.setImageBitmap(resultList[position].getPhoto())
            }
        }
        else {
            holder.accountPhoto.setImageDrawable(context.getDrawable(R.drawable.ic_baseline_account_circle_24))
        }

        // set account nickname
        val nicknameText = resultList[position].getNickname() + '님'
        holder.accountNickname.text = nicknameText

        // for follow/unfollow button
        if(resultList[position].getIsFollowing()) {
            holder.followUnfollowButton.setBackgroundColor(context.getColor(R.color.border_line))
            holder.followUnfollowButton.setTextColor(context.resources.getColor(R.color.black))
            holder.followUnfollowButton.text = context.getText(R.string.unfollow_button)
        }
        else {
            holder.followUnfollowButton.setBackgroundColor(context.getColor(R.color.carrot))
            holder.followUnfollowButton.setTextColor(context.resources.getColor(R.color.white))
            holder.followUnfollowButton.text = context.getText(R.string.follow_button)
        }
    }

    private fun setListenerOnView(holder: FollowerAdapter.HistoryListViewHolder){
        // start pet profile
        holder.mainLayout.setOnClickListener {
            val position = holder.absoluteAdapterPosition
            CommunityUtil.fetchRepresentativePetAndStartPetProfile(context, Account(
                resultList[position].getId(), resultList[position].getUsername(), "", "", null,
                null, resultList[position].getNickname(), if (resultList[position].getHasPhoto()) "true" else null,
                "", resultList[position].getRepresentativePetId(), null, null, 0.0), isViewDestroyed)
        }

        holder.followUnfollowButton.setOnClickListener {
            val position = holder.absoluteAdapterPosition

            // set button to loading
            holder.followUnfollowButton.isEnabled = false

            // API call
            if(resultList[position].getIsFollowing()) {
                deleteFollow(resultList[position].getId(), holder, position)
            }
            else {
                createFollow(resultList[position].getId(), holder, position)
            }
        }
    }

    private fun createFollow(id: Long, holder: HistoryListViewHolder, position: Int) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(context)!!)
            .createFollowReq(CreateFollowReqDto(id))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, context, {
            // update isFollowing and button state
            val currentItem = resultList[position]
            currentItem.setValues(
                currentItem.getHasPhoto(), currentItem.getPhoto(), currentItem.getId(), currentItem.getUsername(),
                currentItem.getNickname(), true, currentItem.getRepresentativePetId()
            )
            notifyItemChanged(position)

            holder.followUnfollowButton.isEnabled = true

            followUnfollowButtonInterface.updateFollowUnfollowButton()
        }, {
            holder.followUnfollowButton.isEnabled = true
        }, {
            holder.followUnfollowButton.isEnabled = true
        })
    }

    private fun deleteFollow(id: Long, holder: HistoryListViewHolder, position: Int) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(context)!!)
            .deleteFollowReq(DeleteFollowReqDto(id))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, context, {
            // update isFollowing and button state
            val currentItem = resultList[position]
            currentItem.setValues(
                currentItem.getHasPhoto(), currentItem.getPhoto(), currentItem.getId(), currentItem.getUsername(),
                currentItem.getNickname(), false, currentItem.getRepresentativePetId()
            )
            notifyItemChanged(position)

            holder.followUnfollowButton.isEnabled = true

            followUnfollowButtonInterface.updateFollowUnfollowButton()
        }, {
            holder.followUnfollowButton.isEnabled = true
        }, {
            holder.followUnfollowButton.isEnabled = true
        })
    }

    private fun setAccountPhoto(id: Long, holder: HistoryListViewHolder, position: Int) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(context)!!)
            .fetchAccountPhotoReq(FetchAccountPhotoReqDto(id))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, context, { response ->
            val photoBitmap = Util.getBitmapFromInputStream(response.body()!!.byteStream())
            holder.accountPhoto.setImageBitmap(photoBitmap)

            val currentItem = resultList[position]
            currentItem.setValues(
                currentItem.getHasPhoto(), photoBitmap, currentItem.getId(), currentItem.getUsername(),
                currentItem.getNickname(), currentItem.getIsFollowing(), currentItem.getRepresentativePetId()
            )
        }, {}, {})
    }

    override fun getItemCount() = resultList.size

    public fun setResult(result: MutableList<FollowListItem>){
        this.resultList = result
        notifyDataSetChanged()
    }

    public fun onDestroy() {
        isViewDestroyed = true
    }
}