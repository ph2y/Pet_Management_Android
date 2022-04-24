package com.sju18001.petmanagement.ui.community.post.createUpdatePost

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.databinding.ActivityCreateupdatepostBinding
import java.io.File

class CreateUpdatePostGeneralFileAdapter(
    private val createUpdatePostViewModel: CreateUpdatePostViewModel
    ) : RecyclerView.Adapter<CreateUpdatePostGeneralFileAdapter.ViewHolder>() {
    private var dataSet = mutableListOf<String>()

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.textview_name)
        val deleteButton: ImageView = view.findViewById(R.id.imageview_deletebutton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_createupdatepostgeneralfile, parent, false)

        val holder = ViewHolder(view)
        setListenerOnView(holder)

        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.name.text = dataSet[position]
    }

    override fun getItemCount() = dataSet.size

    private fun setListenerOnView(holder: ViewHolder) {
        holder.deleteButton.setOnClickListener {
            val position = holder.absoluteAdapterPosition

            File(createUpdatePostViewModel.generalFilePathList.value!![position]).delete()

            createUpdatePostViewModel.removeGeneralFileNameAt(position)
            createUpdatePostViewModel.removeGeneralFilePathAt(position)

            dataSet.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun addItem(item: String) {
        dataSet.add(item)
    }
}