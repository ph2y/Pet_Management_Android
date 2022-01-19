package com.sju18001.petmanagement.ui.community.post.createUpdatePost

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.databinding.FragmentCreateUpdatePostBinding
import java.io.File

class MediaListAdapter(private val createUpdatePostViewModel: CreateUpdatePostViewModel,
                       private val context: Context, private val binding: FragmentCreateUpdatePostBinding,
                       private val confirmButtonAndUsageInterface: ConfirmButtonAndUsageInterface) :
        RecyclerView.Adapter<MediaListAdapter.HistoryListViewHolder>() {

    private var resultList = mutableListOf<MediaListItem>()

    class HistoryListViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.photos_thumbnail)
        val deleteButton: ImageView = view.findViewById(R.id.delete_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryListViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.uploaded_media_list_item, parent, false)

        val holder = HistoryListViewHolder(view)
        setListenerOnView(holder)

        return holder
    }

    override fun onBindViewHolder(holder: HistoryListViewHolder, position: Int) {
        if (!resultList[position].isVideo) { // if photo
            Glide.with(context).load(File(createUpdatePostViewModel.photoPathList[resultList[position].indexInList])).into(holder.thumbnail)
        } else { // if video
            Glide.with(context)
                .load(createUpdatePostViewModel.videoPathList[resultList[position].indexInList])
                .placeholder(R.drawable.ic_baseline_video_library_36)
                .into(holder.thumbnail)
        }
    }

    override fun getItemCount() = resultList.size

    private fun setListenerOnView(holder: HistoryListViewHolder) {
        holder.deleteButton.setOnClickListener {
            val position = holder.absoluteAdapterPosition

            if (!resultList[position].isVideo) { // if photo
                deletePhoto(position)
                confirmButtonAndUsageInterface.updatePhotoUsage()
            } else { // if video
                deleteVideo(position)
                confirmButtonAndUsageInterface.updateVideoUsage()
            }
        }
    }

    private fun deletePhoto(position: Int) {
        // delete file
        File(createUpdatePostViewModel.photoPathList[resultList[position].indexInList]).delete()

        // delete ViewModel values + RecyclerView list
        createUpdatePostViewModel.photoPathList.removeAt(resultList[position].indexInList)
        createUpdatePostViewModel.mediaList.removeAt(position)

        // for item remove animation
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, this.resultList.size)

        // re-index mediaList values for photos
        var newIndex = 0
        for (i in createUpdatePostViewModel.mediaList.indices) {
            if (!createUpdatePostViewModel.mediaList[i].isVideo) {
                createUpdatePostViewModel.mediaList[i].indexInList = newIndex++
            }
        }

        confirmButtonAndUsageInterface.verifyAndEnableConfirmButton()
    }

    private fun deleteVideo(position: Int) {
        // delete file
        File(createUpdatePostViewModel.videoPathList[resultList[position].indexInList]).delete()

        // delete ViewModel values + RecyclerView list
        createUpdatePostViewModel.videoPathList.removeAt(resultList[position].indexInList)
        createUpdatePostViewModel.mediaList.removeAt(position)

        // for item remove animation
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, this.resultList.size)

        // re-index mediaList values for videos
        var newIndex = 0
        for (i in createUpdatePostViewModel.mediaList.indices) {
            if (createUpdatePostViewModel.mediaList[i].isVideo) {
                createUpdatePostViewModel.mediaList[i].indexInList = newIndex++
            }
        }

        confirmButtonAndUsageInterface.verifyAndEnableConfirmButton()
    }

    public fun setResult(result: MutableList<MediaListItem>){
        this.resultList = result
        notifyDataSetChanged()
    }
}