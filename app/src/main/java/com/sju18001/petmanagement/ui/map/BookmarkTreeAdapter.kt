package com.sju18001.petmanagement.ui.map

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.databinding.BookmarkTreeItemBinding
import com.sju18001.petmanagement.restapi.dao.Bookmark
import com.sju18001.petmanagement.restapi.dao.Place

interface BookmarkTreeAdapterInterface {
    fun addBookmarkPOIItemAndMoveCamera(place: Place)
    fun closeDrawer()
}

class BookmarkTreeAdapter(
    private var dataSet: ArrayList<BookmarkTreeItem>,
    private var bookmarkTreeAdapterInterface: BookmarkTreeAdapterInterface
    ): RecyclerView.Adapter<BookmarkTreeAdapter.ViewHolder>() {
    var folderToBookmarks: HashMap<String, ArrayList<Bookmark>> = hashMapOf()

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val itemLayout: LinearLayout = view.findViewById(R.id.layout_item)
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
        holder.itemLayout.setOnClickListener {
            val position = holder.absoluteAdapterPosition
            if(dataSet[position].isBookmark){
                bookmarkTreeAdapterInterface.closeDrawer()
                bookmarkTreeAdapterInterface.addBookmarkPOIItemAndMoveCamera(dataSet[position].bookmark!!.place)
            }
            else{
                val bookmarkCount = folderToBookmarks[holder.nameText.text]!!.count()

                when(dataSet[position].isOpened){
                    true -> {
                        for(i in 1..bookmarkCount){
                            dataSet.removeAt(position + 1)
                        }
                        notifyItemRangeRemoved(position + 1, bookmarkCount)
                    }
                    false -> {
                        folderToBookmarks[holder.nameText.text]?.mapIndexed{ i, bookmark ->
                            dataSet.add(position + i + 1, BookmarkTreeItem(true, bookmark, null, false))
                        }
                        notifyItemRangeInserted(position + 1, bookmarkCount)
                    }
                }

                dataSet[position].isOpened = !dataSet[position].isOpened
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        updateViewHolderByDataSet(holder, dataSet[position])
    }

    override fun getItemCount(): Int = dataSet.size

    private fun updateViewHolderByDataSet(holder: ViewHolder, data: BookmarkTreeItem) {
        when(data.isBookmark){
            true -> {
                holder.nameText.text = data.bookmark!!.name
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

    fun resetDataSet() {
        dataSet = arrayListOf()
        folderToBookmarks = hashMapOf()
    }
}