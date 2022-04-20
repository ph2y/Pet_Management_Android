package com.sju18001.petmanagement.ui.community.post

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sju18001.petmanagement.R

class PostTagAdapter(private var dataSet: ArrayList<String>) : RecyclerView.Adapter<PostTagAdapter.ViewHolder>() {
    class ViewHolder(view: View): RecyclerView.ViewHolder(view){
        val tag: TextView = view.findViewById(R.id.textview_tag)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tag, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tag.text = dataSet[position]
    }

    override fun getItemCount(): Int = dataSet.size
}