package com.sju18001.petmanagement.ui.community.follow.following

import android.app.AlertDialog
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
import com.sju18001.petmanagement.restapi.dto.DeleteFollowReqDto
import com.sju18001.petmanagement.restapi.dto.FetchAccountPhotoReqDto
import com.sju18001.petmanagement.ui.community.CommunityUtil
import com.sju18001.petmanagement.ui.community.follow.FollowItem
import com.sju18001.petmanagement.ui.community.follow.FollowUnfollowButtonInterface
import com.sju18001.petmanagement.ui.community.follow.FollowViewModel
import de.hdodenhof.circleimageview.CircleImageView

class FollowingAdapter(
    private val context: Context,
    private val followViewModel: FollowViewModel,
    private val buttonInterface: FollowUnfollowButtonInterface
    ): RecyclerView.Adapter<FollowingAdapter.HistoryListViewHolder>() {
    private var resultList = mutableListOf<FollowItem>()
    private var isViewDestroyed = false

    class HistoryListViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val cardView: View = view.findViewById(R.id.cardview_follow)
        val accountPhoto: CircleImageView = view.findViewById(R.id.circleimageview_accountphoto)
        val accountNickname: TextView = view.findViewById(R.id.textview_accountnickname)
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
        setAccountPhoto(holder, position)

        holder.accountNickname.text = resultList[position].nickname + '님'
        holder.button.setBackgroundColor(context.getColor(R.color.border_line))
        holder.button.setTextColor(context.resources.getColor(R.color.black))
        holder.button.text = context.getText(R.string.unfollow_button)
    }

    private fun setAccountPhoto(holder: HistoryListViewHolder, position: Int) {
        if(resultList[position].hasPhoto) {
            if(resultList[position].photo == null){
                setAccountPhoto(resultList[position].id, holder, position)
            }else{
                holder.accountPhoto.setImageBitmap(resultList[position].photo)
            }
        }else{
            holder.accountPhoto.setImageDrawable(context.getDrawable(R.drawable.ic_baseline_account_circle_24))
        }
    }

    private fun setListenerOnView(holder: HistoryListViewHolder) {
        holder.cardView.setOnClickListener {
            val position = holder.absoluteAdapterPosition

            CommunityUtil.fetchRepresentativePetAndStartPetProfile(context, Account(
                resultList[position].id, resultList[position].username, "", "", null,
                null, resultList[position].nickname, if (resultList[position].hasPhoto) "true" else null,
                "", resultList[position].representativePetId, null, null, 0.0), isViewDestroyed)
        }

        holder.button.setOnClickListener {
            val position = holder.absoluteAdapterPosition
            val messageText = resultList[position].nickname + context.getString(R.string.unfollow_confirm_dialog_message)
            AlertDialog.Builder(context).setMessage(messageText)
                .setPositiveButton(
                    R.string.confirm
                ) { _, _ ->
                    deleteFollow(resultList[position].id, holder, position)
                }
                .setNegativeButton(
                    R.string.cancel
                ) { dialog, _ ->
                    dialog.cancel()
                }
                .create().show()
        }
    }

    private fun deleteFollow(id: Long, holder: HistoryListViewHolder, position: Int) {
        holder.button.isEnabled = false
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(context)!!)
            .deleteFollowReq(DeleteFollowReqDto(id))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, context, {
            resultList.removeAt(position)
            followViewModel.followingTitle.value =
                    "${context.getText(R.string.following_fragment_title)} ${resultList.size}"

            notifyItemRemoved(position)
            notifyItemRangeChanged(position, resultList.size)

            buttonInterface.updateFollowUnfollowButton()

            holder.button.isEnabled = true
        }, { holder.button.isEnabled = true }, { holder.button.isEnabled = true })
    }

    private fun setAccountPhoto(id: Long, holder: HistoryListViewHolder, position: Int) {
        val call = RetrofitBuilder.getServerApiWithToken(SessionManager.fetchUserToken(context)!!)
            .fetchAccountPhotoReq(FetchAccountPhotoReqDto(id))
        ServerUtil.enqueueApiCall(call, {isViewDestroyed}, context, { response ->
            val photoBitmap = Util.getBitmapFromInputStream(response.body()!!.byteStream())
            holder.accountPhoto.setImageBitmap(photoBitmap)

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