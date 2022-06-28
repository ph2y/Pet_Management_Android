package com.sju18001.petmanagement.ui.community.post.createUpdatePost

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.databinding.FragmentCreateUpdatePostBinding
import java.io.File

class CreateUpdatePostGeneralFileAdapter(private val createUpdatePostViewModel: CreateUpdatePostViewModel,
                                         private val context: Context, private val binding: FragmentCreateUpdatePostBinding,
                                         private val confirmButtonAndUsageInterface: ConfirmButtonAndUsageInterface) :
    RecyclerView.Adapter<CreateUpdatePostGeneralFileAdapter.HistoryListViewHolder>() {

    private var resultList = mutableListOf<String>()

    class HistoryListViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.textview_createupdatepostgeneralfile_name)
        val deleteButton: ImageView = view.findViewById(R.id.imageview_createupdatepostgeneralfile_deletebutton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_createupdatepostgeneralfile, parent, false)

        val holder = HistoryListViewHolder(view)
        setListenerOnView(holder)

        return holder
    }

    override fun onBindViewHolder(holder: HistoryListViewHolder, position: Int) {
        holder.name.text = resultList[position]
    }

    override fun getItemCount() = resultList.size

    private fun setListenerOnView(holder: HistoryListViewHolder) {
        holder.deleteButton.setOnClickListener {
            val position = holder.absoluteAdapterPosition

            deleteItem(position)

            // update general upload layout
            val uploadedCount = createUpdatePostViewModel.generalFileNameList.size
            if (uploadedCount == 0) {
                binding.generalRecyclerView.visibility = View.GONE
            }
            else {
                binding.generalRecyclerView.visibility = View.VISIBLE
            }
            val generalUsageText = "$uploadedCount/10"
            binding.generalUsage.text = generalUsageText
        }
    }

    private fun deleteItem(position: Int) {
        // delete file
        File(createUpdatePostViewModel.generalFilePathList[position]).delete()

        // delete ViewModel values + RecyclerView list
        createUpdatePostViewModel.generalFilePathList.removeAt(position)
        createUpdatePostViewModel.generalFileNameList.removeAt(position)

        // update recyclerview
        notifyDataSetChanged()

        confirmButtonAndUsageInterface.verifyAndEnableConfirmButton()
    }

    public fun setResult(result: MutableList<String>){
        this.resultList = result
        notifyDataSetChanged()
    }
}