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
import androidx.recyclerview.widget.RecyclerView
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.restapi.dao.Account
import com.sju18001.petmanagement.restapi.dao.Comment

interface CommentListAdapterInterface{
    fun getActivity(): Activity
    fun onClickReply(id: Long, nickname: String)
    fun onLongClickComment(authorId: Long, commentId: Long, commentContents: String, position: Int)
    fun setAccountPhoto(id: Long, holder: CommentListAdapter.ViewHolder)
    fun setAccountDefaultPhoto(holder: CommentListAdapter.ViewHolder)
    fun fetchReplyComment(pageIndex: Int, topReplyId: Long?, parentCommentId: Long, position: Int)
    fun startPetProfile(author: Account)
}

class CommentListAdapter(
    private var dataSet: ArrayList<Comment>,
    private var pageIndices: ArrayList<Int>,
    private var topReplyIdList: ArrayList<Long?> // -1: 답글 없음, NULL: 답글 있으나 불러온 적 없음, N+: 답글 있으며 불러온 적 있음
    ) : RecyclerView.Adapter<CommentListAdapter.ViewHolder>()  {
    lateinit var commentListAdapterInterface: CommentListAdapterInterface

    class ViewHolder(view: View): RecyclerView.ViewHolder(view){
        val constraintLayout: ConstraintLayout = view.findViewById(R.id.constraintlayout_comment)
        val profileCircleImageView: ImageView = view.findViewById(R.id.circleimageview_comment_profile)
        val nicknameTextView: TextView = view.findViewById(R.id.textview_comment_nickname)
        val contentsTextView: TextView = view.findViewById(R.id.textview_comment_contents)
        val timestampTextView: TextView = view.findViewById(R.id.textview_comment_timestamp)
        val replyTextView: TextView = view.findViewById(R.id.textview_comment_reply)
        val loadReplyTextView: TextView = view.findViewById(R.id.textview_comment_loadreply)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)

        val holder = ViewHolder(view)
        setListenerOnView(holder)

        return holder
    }

    private fun setListenerOnView(holder: ViewHolder) {
        // start pet profile
        holder.profileCircleImageView.setOnClickListener {
            val position = holder.absoluteAdapterPosition
            commentListAdapterInterface.startPetProfile(dataSet[position].author)
        }
        holder.nicknameTextView.setOnClickListener {
            val position = holder.absoluteAdapterPosition
            commentListAdapterInterface.startPetProfile(dataSet[position].author)
        }

        holder.replyTextView.setOnClickListener {
            val position = holder.absoluteAdapterPosition
            dataSet[position].author.nickname?.let {
                commentListAdapterInterface.onClickReply(dataSet[position].id, it)
            }
        }

        holder.constraintLayout.setOnLongClickListener { _ ->
            val position = holder.absoluteAdapterPosition
            commentListAdapterInterface.onLongClickComment(dataSet[position].author.id, dataSet[position].id, dataSet[position].contents, position)
            true
        }

        holder.loadReplyTextView.setOnClickListener {
            val position = holder.absoluteAdapterPosition
            commentListAdapterInterface.fetchReplyComment(pageIndices[position], null, dataSet[position].id, position)
            pageIndices[position] += 1

            // 답글 불러오기 -> 이전 답글 불러오기
            holder.loadReplyTextView.text = commentListAdapterInterface.getActivity().getString(R.string.load_prev_reply_title)
        }
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        updateViewHolderByDataSet(holder, dataSet[position], position)
        
        // 댓글 내용에 indent 추가
        setSpanToContent(holder.nicknameTextView, holder.contentsTextView)
    }

    override fun getItemCount(): Int = dataSet.size

    private fun updateViewHolderByDataSet(holder: CommentListAdapter.ViewHolder, data: Comment, position: Int){
        holder.nicknameTextView.text = data.author.nickname
        holder.contentsTextView.text = data.contents
        holder.timestampTextView.text = Util.getTimestampForDisplay(data.timestamp)

        // Set photo
        if(!data.author.photoUrl.isNullOrEmpty()){
            commentListAdapterInterface.setAccountPhoto(data.author.id, holder)
        }else{
            commentListAdapterInterface.setAccountDefaultPhoto(holder)
        }

        // 답글 불러오기 버튼 세팅
        if(topReplyIdList[position] == (-1).toLong()){
            holder.loadReplyTextView.visibility = View.GONE
        }else{
            holder.loadReplyTextView.visibility = View.VISIBLE
        }

        // Comment, Reply를 구분하여 이에 따라 뷰 세팅
        setViewDependingOnCommentOrReply(holder, position)
    }

    private fun setSpanToContent(nicknameTextView: TextView, contentTextView: TextView){
        contentTextView.post{
            nicknameTextView.post{
                // contents의 첫줄에 닉네임만큼의 indent를 주기 위함
                val spannable = SpannableString(contentTextView.text.toString())
                val span = LeadingMarginSpan.Standard(nicknameTextView.width + 10, 0)
                spannable.setSpan(span, 0, spannable.count(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                contentTextView.text = spannable
            }
        }
    }

    private fun setViewDependingOnCommentOrReply(holder: ViewHolder, position: Int){
        val isReply = dataSet[position].parentCommentId != null
        
        // 답글일 시 Margin 추가
        val layoutParams = holder.constraintLayout.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.leftMargin = if(isReply) 96 else 0
        holder.constraintLayout.layoutParams = layoutParams

        // 답글일 시 답글 달기 제거
        holder.replyTextView.visibility = if(isReply) View.GONE else View.VISIBLE
    }

    fun addItem(item: Comment){
        dataSet.add(item)

        // 기본값으로 추가
        pageIndices.add(0)
        topReplyIdList.add(-1)
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
        topReplyIdList.add(position, -1)
    }

    fun setTopReplyIdList(topCommentId: Long?, position: Int){
        topReplyIdList[position] = topCommentId
    }

    fun resetDataSet(){
        dataSet = arrayListOf()
        pageIndices = arrayListOf()
        topReplyIdList = arrayListOf()
    }
}