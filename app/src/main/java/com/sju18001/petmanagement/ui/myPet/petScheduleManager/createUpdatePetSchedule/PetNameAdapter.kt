package com.sju18001.petmanagement.ui.myPet.petScheduleManager.createUpdatePetSchedule

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.sju18001.petmanagement.R

interface PetNameListAdapterInterface{
    fun onClickCheckBox(position: Int)
    fun updateCheckBoxByViewModel(checkBox: CheckBox, position: Int)
}

class PetNameAdapter(
    private val dataSet: ArrayList<PetNameItem>,
    private val petNameListAdapterInterface: PetNameListAdapterInterface
    ) : RecyclerView.Adapter<PetNameAdapter.ViewHolder>(){
    class ViewHolder(view: View): RecyclerView.ViewHolder(view){
        val petNameCheckBox: CheckBox = view.findViewById(R.id.pet_name_check_box)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.pet_name_item, parent, false)

        val holder = ViewHolder(view)
        setListenerOnView(holder)

        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        updateViewHolderByDataSet(holder, position)
    }

    override fun getItemCount(): Int = dataSet.size

    private fun setListenerOnView(holder: ViewHolder) {
        holder.petNameCheckBox.setOnClickListener {
            val position = holder.absoluteAdapterPosition
            petNameListAdapterInterface.onClickCheckBox(position)
        }
    }

    private fun updateViewHolderByDataSet(holder: ViewHolder, position: Int){
        holder.petNameCheckBox.text = dataSet[position].name
        petNameListAdapterInterface.updateCheckBoxByViewModel(holder.petNameCheckBox, position)
    }

    fun addItem(item: PetNameItem){
        dataSet.add(item)
    }

    fun getItem(position: Int): PetNameItem {
        return dataSet[position]
    }
}