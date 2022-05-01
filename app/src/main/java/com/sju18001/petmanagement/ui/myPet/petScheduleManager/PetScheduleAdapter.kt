package com.sju18001.petmanagement.ui.myPet.petScheduleManager

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.ItemPetscheduleBinding
import com.sju18001.petmanagement.restapi.dao.PetSchedule
import java.time.LocalTime

interface PetScheduleAdapterInterface{
    fun startCreateUpdatePetScheduleFragmentForUpdate(data: PetSchedule)
    fun askForDeleteItem(position: Int, item: PetSchedule)
    fun deletePetSchedule(id: Long)
    fun updatePetSchedule(data: PetSchedule)
    fun getContext(): Context
}

class PetScheduleAdapter(
    private var dataSet: ArrayList<PetSchedule>,
    private val getPetNameForId: () -> HashMap<Long, String>,
    private var petScheduleAdapterInterface: PetScheduleAdapterInterface
    ) : RecyclerView.Adapter<PetScheduleAdapter.ViewHolder>(){
    class ViewHolder(
        private val adapter: PetScheduleAdapter,
        private val binding: ItemPetscheduleBinding,
        private val getPetNameForId: () -> HashMap<Long, String>
        ): RecyclerView.ViewHolder(binding.root){
        fun bind(petSchedule: PetSchedule) {
            binding.adapter = adapter
            binding.holder = this
            binding.petSchedule = petSchedule
            binding.petNameForId = getPetNameForId.invoke()

            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val binding = DataBindingUtil.inflate<ItemPetscheduleBinding>(LayoutInflater.from(parent.context),
            R.layout.item_petschedule, parent, false)
        return ViewHolder(this, binding, getPetNameForId)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataSet[position])
    }

    override fun getItemCount(): Int = dataSet.size

    fun removeItem(index: Int){
        dataSet.removeAt(index)
    }

    fun setDataSet(_dataSet: ArrayList<PetSchedule>){
        dataSet = _dataSet
    }


    /** databinding functions */
    fun onClickPetScheduleItem(holder: ViewHolder) {
        val position = holder.absoluteAdapterPosition
        petScheduleAdapterInterface.startCreateUpdatePetScheduleFragmentForUpdate(dataSet[position])
    }

    fun onLongClickPetScheduleItem(holder: ViewHolder): Boolean {
        val position = holder.absoluteAdapterPosition
        petScheduleAdapterInterface.askForDeleteItem(position, dataSet[position])
        return true
    }

    fun onClickEnabledSwitch(isChecked: Boolean, holder: ViewHolder) {
        val position = holder.absoluteAdapterPosition
        petScheduleAdapterInterface.updatePetSchedule(dataSet[position])

        if(isChecked){
            // Notification ON
            PetScheduleNotification.setAlarmManagerRepeating(
                petScheduleAdapterInterface.getContext(),
                dataSet[position].id,
                dataSet[position].time,
                Util.getPetNamesFromPetIdList(getPetNameForId.invoke(), dataSet[position].petIdList),
                dataSet[position].memo
            )
        }else{
            // Notification OFF
            PetScheduleNotification.cancelAlarmManagerRepeating(
                petScheduleAdapterInterface.getContext(),
                dataSet[position].id
            )
        }
    }
}