package com.sju18001.petmanagement.ui.community.post

import android.content.Context
import android.view.LayoutInflater
import androidx.recyclerview.widget.RecyclerView
import com.sju18001.petmanagement.restapi.dao.Post
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.restapi.dao.Account
import com.sju18001.petmanagement.ui.community.CommunityUtil

interface PostListAdapterInterface {
    fun startCommentActivity(postId: Long)
    fun createLike(postId: Long, holder: PostListAdapter.ViewHolder, position: Int)
    fun deleteLike(postId: Long, holder: PostListAdapter.ViewHolder, position: Int)
    fun onClickPostFunctionButton(post: Post, position: Int)
    fun setAccountPhoto(id: Long, holder: PostListAdapter.ViewHolder)
    fun setAccountDefaultPhoto(holder: PostListAdapter.ViewHolder)
    fun setPostMedia(holder: PostMediaAdapter.ViewPagerHolder, postId: Long, index: Int, url: String, dummyImageView: ConstraintLayout)
    fun startGeneralFilesActivity(postId: Long, fileAttachments: String)
    fun getContext(): Context
}

class PostListAdapter(private var dataSet: ArrayList<Post>, private var likedCounts: ArrayList<Long>, private var isPostLiked: ArrayList<Boolean>) : RecyclerView.Adapter<PostListAdapter.ViewHolder>() {
    lateinit var postListAdapterInterface: PostListAdapterInterface

    class ViewHolder(view: View): RecyclerView.ViewHolder(view){
        val accountPhotoImage: ImageView = view.findViewById(R.id.account_photo)
        val nicknameTextView: TextView = view.findViewById(R.id.nickname)
        val petNameTextView: TextView = view.findViewById(R.id.pet_name)
        val layoutUserInfo: ConstraintLayout = view.findViewById(R.id.layout_user_info)
        val dialogButton: ImageButton = view.findViewById(R.id.dialog_button)

        val dummyLayout: ConstraintLayout = view.findViewById(R.id.layout_dummy)
        val viewPager: ViewPager2 = view.findViewById(R.id.view_pager)
        val contentsTextView: TextView = view.findViewById(R.id.text_contents)
        val viewMoreTextView: TextView = view.findViewById(R.id.view_more)
        val tagRecyclerView: RecyclerView = view.findViewById(R.id.recycler_view_tag)

        val createLikeButton: ImageButton = view.findViewById(R.id.create_like_button)
        val deleteLikeButton: ImageButton = view.findViewById(R.id.delete_like_button)
        val commentButton: ImageButton = view.findViewById(R.id.comment_button)
        val generalFilesButton: ImageButton = view.findViewById(R.id.general_files_button)
        val likeCountTextView: TextView = view.findViewById(R.id.like_count)
        val timestampTextView: TextView = view.findViewById(R.id.text_timestamp)
    }

    override fun getItemCount(): Int = dataSet.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.post_item, parent, false)

        val holder = ViewHolder(view)
        setListenerOnView(holder)

        return holder
    }

    private fun setListenerOnView(holder: ViewHolder){
        // 프로필 이동
        holder.accountPhotoImage.setOnClickListener {
            val item = dataSet[holder.absoluteAdapterPosition]
            CommunityUtil.startPetProfileFragmentFromCommunity(holder.itemView.context, item.pet, item.author)
        }
        holder.layoutUserInfo.setOnClickListener {
            val item = dataSet[holder.absoluteAdapterPosition]
            CommunityUtil.startPetProfileFragmentFromCommunity(holder.itemView.context, item.pet, item.author)
        }

        // 댓글 버튼
        holder.commentButton.setOnClickListener {
            val item = dataSet[holder.absoluteAdapterPosition]
            postListAdapterInterface.startCommentActivity(item.id)
        }

        // 좋아요 버튼
        holder.createLikeButton.setOnClickListener {
            val position = holder.absoluteAdapterPosition
            val item = dataSet[position]
            postListAdapterInterface.createLike(item.id, holder, position)
        }

        // 좋아요 취소 버튼
        holder.deleteLikeButton.setOnClickListener {
            val position = holder.absoluteAdapterPosition
            val item = dataSet[position]
            postListAdapterInterface.deleteLike(item.id, holder, position)
        }

        // general files button
        holder.generalFilesButton.setOnClickListener {
            val item = dataSet[holder.absoluteAdapterPosition]
            postListAdapterInterface.startGeneralFilesActivity(item.id, item.fileAttachments!!)
        }

        // ... 버튼
        holder.dialogButton.setOnClickListener {
            val position = holder.absoluteAdapterPosition
            val item = dataSet[position]
            postListAdapterInterface.onClickPostFunctionButton(item, position)
        }
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val safePosition = holder.absoluteAdapterPosition
        updateViews(holder, dataSet[safePosition], likedCounts[safePosition], safePosition)
    }


    private fun updateViews(holder: ViewHolder, post: Post, likedCount: Long, position: Int){
        setTextViews(holder, post, likedCount)
        setAccountPhoto(holder, post.author)
        setGeneralFilesButton(holder, post.fileAttachments)
        setLikeButton(holder, position)
        setViewPager(holder, post)
        setTag(holder, post.serializedHashTags)
        Util.setViewMore(holder.contentsTextView, holder.viewMoreTextView, 5)
    }

    private fun setTextViews(holder: ViewHolder, post: Post, likedCount: Long) {
        holder.nicknameTextView.text = post.author.nickname
        holder.petNameTextView.text = post.pet.name
        holder.contentsTextView.text = post.contents
        holder.likeCountTextView.text = likedCount.toString()
        holder.timestampTextView.text = Util.getTimestampForDisplay(post.timestamp) + " 전"
    }

    private fun setAccountPhoto(holder: ViewHolder, author: Account) {
        if(!author.photoUrl.isNullOrEmpty()){
            postListAdapterInterface.setAccountPhoto(author.id, holder)
        }else{
            postListAdapterInterface.setAccountDefaultPhoto(holder)
        }
    }

    private fun setGeneralFilesButton(holder: ViewHolder, fileAttachments: String?) {
        holder.generalFilesButton.visibility =
            if(fileAttachments.isNullOrEmpty()) View.GONE
            else View.VISIBLE
    }

    fun setLikeButton(holder: ViewHolder, position: Int) {
        if(isPostLiked[position]){
            holder.createLikeButton.visibility = View.GONE
            holder.deleteLikeButton.visibility = View.VISIBLE
        }else{
            holder.deleteLikeButton.visibility = View.GONE
            holder.createLikeButton.visibility = View.VISIBLE
        }
    }

    private fun setViewPager(holder: ViewHolder, post: Post){
        if(!post.imageAttachments.isNullOrEmpty() || !post.videoAttachments.isNullOrEmpty()){
            val imageAttachments = Util.getArrayFromMediaAttachments(post.imageAttachments)
            val videoAttachments = Util.getArrayFromMediaAttachments(post.videoAttachments)
            val mediaAttachments = imageAttachments + videoAttachments

            holder.dummyLayout.visibility = View.VISIBLE

            holder.viewPager.visibility = View.VISIBLE
            holder.viewPager.adapter = PostMediaAdapter(postListAdapterInterface, post.id, mediaAttachments, holder)
            holder.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        }else{
            holder.dummyLayout.visibility = View.GONE

            holder.viewPager.visibility = View.GONE
            holder.viewPager.adapter = PostMediaAdapter(postListAdapterInterface, 0, arrayOf(), holder)
        }
    }

    private fun setTag(holder: ViewHolder, serializedHashTags: String?){
        if(!serializedHashTags.isNullOrEmpty()){
            holder.tagRecyclerView.apply{
                visibility = View.VISIBLE
                adapter = PostTagListAdapter(ArrayList(serializedHashTags.split(',')))

                layoutManager = LinearLayoutManager(postListAdapterInterface.getContext())
                (layoutManager as LinearLayoutManager).orientation = LinearLayoutManager.HORIZONTAL
            }
        }else{
            holder.tagRecyclerView.apply {
                visibility = View.GONE
                adapter = PostTagListAdapter(arrayListOf())
            }
        }
    }


    fun addItem(post: Post){
        dataSet.add(post)

        // 기본값으로 추가
        likedCounts.add(0)
        isPostLiked.add(false)
    }

    fun addItemToTop(post: Post){
        dataSet.add(0, post)

        // 기본값으로 추가
        likedCounts.add(0, 0)
        isPostLiked.add(0, false)
    }

    fun removeItem(index: Int){
        dataSet.removeAt(index)
        likedCounts.removeAt(index)
        isPostLiked.removeAt(index)
    }

    fun resetItem(){
        dataSet = arrayListOf()
        likedCounts = arrayListOf()
        isPostLiked = arrayListOf()
    }

    fun getLikedCount(position: Int): Long {
        return likedCounts[position]
    }

    fun setLikedCount(position: Int, value: Long){
        likedCounts[position] = value
    }

    fun setIsPostLiked(position: Int, flag: Boolean){
        isPostLiked[position] = flag
    }

    fun setPost(position: Int, post: Post){
        dataSet[position] = post
    }
}