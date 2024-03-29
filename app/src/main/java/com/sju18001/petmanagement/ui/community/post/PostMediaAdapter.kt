package com.sju18001.petmanagement.ui.community.post

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.VideoView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.restapi.global.FileMetaData

class PostMediaAdapter(
    private var communityPostAdapterInterface: PostAdapterInterface,
    private val id: Long,
    private val mediaAttachments: Array<FileMetaData>,
    private val parentHolder: PostAdapter.ViewHolder
): RecyclerView.Adapter<PostMediaAdapter.ViewPagerHolder>() {
    private val viewPager = parentHolder.viewPager

    inner class ViewPagerHolder(parent: ViewGroup): RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_postmedia, parent, false)
    ){
        val postMediaImage: ImageView = itemView.findViewById(R.id.imageview_postmedia)
        val postMediaVideo: VideoView = itemView.findViewById(R.id.videoview_postmedia)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewPagerHolder{
        val holder = ViewPagerHolder(parent)
        setListenerOnView(holder)

        return holder
    }

    override fun onBindViewHolder(holder: ViewPagerHolder, position: Int) {
        communityPostAdapterInterface.setPostMedia(holder, id, position, mediaAttachments[position].url, parentHolder.dummyLayout)
    }

    override fun getItemCount(): Int = mediaAttachments.size

    private fun setListenerOnView(holder: ViewPagerHolder) {
        viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                if(state == ViewPager2.SCROLL_STATE_IDLE) {
                    // 페이지가 넘어간 뒤에 이미지 리사이징을 요청해야합니다.
                    if(holder.postMediaImage.isVisible){
                        holder.postMediaImage.requestLayout()
                    }
                }
            }
        })

        // 동영상 클릭 -> 재생 / 정지
        val postMediaVideo = holder.postMediaVideo
        postMediaVideo.setOnClickListener {
            if(postMediaVideo.isVisible){
                if(postMediaVideo.isPlaying){
                    postMediaVideo.pause()
                }else{
                    postMediaVideo.start()
                }
            }
        }
    }
}