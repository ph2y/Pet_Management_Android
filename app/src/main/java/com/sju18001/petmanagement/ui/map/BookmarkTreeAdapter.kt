package com.sju18001.petmanagement.ui.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sju18001.petmanagement.R
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
        val linearLayout: LinearLayout = view.findViewById(R.id.linearlayout_bookmarktree)
        val bookmarkImageView: ImageView = view.findViewById(R.id.imageview_bookmarktree_bookmark)
        val folderImageView: ImageView = view.findViewById(R.id.imageview_bookmarktree_folder)
        val nameTextView: TextView = view.findViewById(R.id.textview_bookmarktree_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bookmarktree, parent, false)

        val holder = ViewHolder(view)
        setListenerOnView(holder)

        return holder
    }

    private fun setListenerOnView(holder: ViewHolder) {
        holder.linearLayout.setOnClickListener {
            val position = holder.absoluteAdapterPosition
            if(dataSet[position].isBookmark){
                bookmarkTreeAdapterInterface.closeDrawer()
                bookmarkTreeAdapterInterface.addBookmarkPOIItemAndMoveCamera(dataSet[position].bookmark!!.place)
            }
            else{
                val bookmarkCount = folderToBookmarks[holder.nameTextView.text]!!.count()

                when(dataSet[position].isOpened){
                    true -> {
                        for(i in 1..bookmarkCount){
                            dataSet.removeAt(position + 1)
                        }
                        notifyItemRangeRemoved(position + 1, bookmarkCount)
                    }
                    false -> {
                        folderToBookmarks[holder.nameTextView.text]?.mapIndexed{ i, bookmark ->
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
                holder.nameTextView.text = data.bookmark!!.name
                holder.bookmarkImageView.visibility = View.VISIBLE
                holder.folderImageView.visibility = View.GONE
            }
            false -> {
                holder.nameTextView.text = data.folder
                holder.bookmarkImageView.visibility = View.GONE
                holder.folderImageView.visibility = View.VISIBLE
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