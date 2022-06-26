package com.sju18001.petmanagement.ui.community.comment

import android.app.Activity
import android.text.Spannable
import android.text.SpannableString
import android.text.style.LeadingMarginSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.ItemCommentBinding
import com.sju18001.petmanagement.restapi.dao.Account
import com.sju18001.petmanagement.restapi.dao.Comment

interface CommentAdapterInterface{
    fun getActivity(): Activity
    fun onClickReply(id: Long, nickname: String)
    fun onLongClickComment(authorId: Long, comment: Comment, commentContents: String, position: Int)
    fun onClickLoadReply(pageIndex: Int, topReplyId: Long?, parentCommentId: Long, position: Int)
    fun startPetProfile(author: Account)
}

class CommentAdapter(
    private var dataSet: ArrayList<Comment>,
    private var pageIndices: ArrayList<Int>,
    private var topReplyIdList: ArrayList<Long?>, // NO_REPLY(-1), REPLY_NOT_FETCHED(null), N+: 답글 있으며 불러온 적 있음
    private val commentAdapterInterface: CommentAdapterInterface
    ) : RecyclerView.Adapter<CommentAdapter.ViewHolder>()  {
    class ViewHolder(
        private val adapter: CommentAdapter,
        private val binding: ItemCommentBinding
    ): RecyclerView.ViewHolder(binding.root){
        fun bind(comment: Comment, topReplyId: Long?) {
            binding.adapter = adapter
            binding.holder = this
            binding.comment = comment
            binding.topReplyId = topReplyId

            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DataBindingUtil.inflate<ItemCommentBinding>(LayoutInflater.from(parent.context),
            R.layout.item_comment, parent, false)
        return ViewHolder(this, binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataSet[position], topReplyIdList[position])
    }

    override fun getItemCount(): Int = dataSet.size


    fun addItem(item: Comment){
        dataSet.add(item)

        // 기본값으로 추가
        pageIndices.add(0)
        topReplyIdList.add(NO_REPLY)
    }

    fun removeItem(position: Int){
        dataSet.removeAt(position)
        pageIndices.removeAt(position)
        topReplyIdList.removeAt(position)
    }

    fun updateCommentContents(newContents: String, position: Int){
        dataSet[position].contents = newContents
    }

    fun addItemOnPosition(item: Comment, position: Int){
        dataSet.add(position, item)

        // 기본값으로 추가
        pageIndices.add(position, 0)
        topReplyIdList.add(position, NO_REPLY)
    }

    fun setTopReplyIdList(topCommentId: Long?, position: Int){
        topReplyIdList[position] = topCommentId
    }

    fun addPageIndices(position: Int){
        pageIndices[position] += 1
    }

    fun resetDataSet(){
        dataSet = arrayListOf()
        pageIndices = arrayListOf()
        topReplyIdList = arrayListOf()
    }


    /** Databinding functions */
    companion object {
        const val NO_REPLY = -1L
        val REPLY_NOT_FETCHED = null
    }

    fun startPetProfile(holder: ViewHolder) {
        val position = holder.absoluteAdapterPosition
        commentAdapterInterface.startPetProfile(dataSet[position].author)
    }

    fun onClickReply(holder: ViewHolder) {
        val position = holder.absoluteAdapterPosition
        commentAdapterInterface.onClickReply(dataSet[position].id, dataSet[position].author.nickname!!)
    }

    fun onLongClickComment(holder: ViewHolder): Boolean {
        val position = holder.absoluteAdapterPosition
        commentAdapterInterface.onLongClickComment(dataSet[position].author.id, dataSet[position], dataSet[position].contents, position)
        return true
    }

    fun onClickLoadReply(loadReplyTextView: TextView, holder: ViewHolder) {
        val position = holder.absoluteAdapterPosition
        commentAdapterInterface.onClickLoadReply(pageIndices[position], null, dataSet[position].id, position)
        loadReplyTextView.text = commentAdapterInterface.getActivity().getString(R.string.load_prev_reply_title)
    }
}