package com.sju18001.petmanagement.ui.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.databinding.BookmarkTreeItemBinding

class BookmarkTreeAdapter(
    private var dataSet: ArrayList<BookmarkTreeItem>
    ): RecyclerView.Adapter<BookmarkTreeAdapter.ViewHolder>() {
    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val bookmarkImage: ImageView = view.findViewById(R.id.image_bookmark)
        val folderImage: ImageView = view.findViewById(R.id.image_folder)
        val nameText: TextView = view.findViewById(R.id.text_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.bookmark_tree_item, parent, false)

        val holder = ViewHolder(view)
        setListenerOnView(holder)

        return holder
    }

    private fun setListenerOnView(holder: ViewHolder) {

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        updateViewHolderByDataSet(holder, dataSet[position])
    }

    override fun getItemCount(): Int = dataSet.size

    private fun updateViewHolderByDataSet(holder: ViewHolder, data: BookmarkTreeItem) {
        when(data.isBookmark){
            true -> {
                holder.nameText.text = data.bookmark!!.place.name
                holder.bookmarkImage.visibility = View.VISIBLE
                holder.folderImage.visibility = View.GONE
            }
            false -> {
                holder.nameText.text = data.folder
                holder.bookmarkImage.visibility = View.GONE
                holder.folderImage.visibility = View.VISIBLE
            }
        }
    }

    fun addItem(item: BookmarkTreeItem) {
        dataSet.add(item)
    }
}