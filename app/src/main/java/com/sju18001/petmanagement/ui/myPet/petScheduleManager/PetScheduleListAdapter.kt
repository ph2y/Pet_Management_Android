package com.sju18001.petmanagement.ui.myPet.petScheduleManager

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.sju18001.petmanagement.R
import com.sju18001.petmanagement.controller.Util
import com.sju18001.petmanagement.databinding.PetScheduleListItemBinding
import com.sju18001.petmanagement.restapi.dao.PetSchedule
import com.sju18001.petmanagement.ui.myPet.MyPetViewModel
import java.time.LocalTime

interface PetScheduleListAdapterInterface{
    fun startCreateUpdatePetScheduleFragmentForUpdate(data: PetSchedule)
    fun askForDeleteItem(position: Int, item: PetSchedule)
    fun deletePetSchedule(id: Long)
    fun updatePetSchedule(data: PetSchedule)
    fun getContext(): Context
}

class PetScheduleListAdapter(
    private var dataSet: ArrayList<PetSchedule>,
    private val getPetNameForId: () -> HashMap<Long, String>,
    private var petScheduleListAdapterInterface: PetScheduleListAdapterInterface
    ) : RecyclerView.Adapter<PetScheduleListAdapter.ViewHolder>(){
    class ViewHolder(
        private val adapter: PetScheduleListAdapter,
        private val binding: PetScheduleListItemBinding,
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
        val binding = DataBindingUtil.inflate<PetScheduleListItemBinding>(LayoutInflater.from(parent.context),
            R.layout.pet_schedule_list_item, parent, false)
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
    companion object {
        @JvmStatic
        @BindingAdapter("setNoonTextView")
        fun setNoonTextView(textView: TextView, time: String) {
            val localTime = LocalTime.parse(time)
            textView.text = if(localTime.hour <= 12) "오전" else "오후"
        }

        @JvmStatic
        @BindingAdapter("setTimeTextView")
        fun setTimeTextView(textView: TextView, time: String) {
            val localTime = LocalTime.parse(time)
            textView.text = localTime.hour.toString().padStart(2, '0') +
                    ":" + localTime.minute.toString().padStart(2, '0')
        }

        @JvmStatic
        @BindingAdapter("petNameForId", "petIdList")
        fun setPetListView(textView: TextView, petNameForId: HashMap<Long, String>, petIdList: String) {
            textView.text = Util.getPetNamesFromPetIdList(petNameForId, petIdList)
        }
    }

    fun onClickPetScheduleItem(holder: ViewHolder) {
        val position = holder.absoluteAdapterPosition
        petScheduleListAdapterInterface.startCreateUpdatePetScheduleFragmentForUpdate(dataSet[position])
    }

    fun onLongClickPetScheduleItem(holder: ViewHolder): Boolean {
        val position = holder.absoluteAdapterPosition
        petScheduleListAdapterInterface.askForDeleteItem(position, dataSet[position])
        return true
    }

    fun onClickEnabledSwitch(isChecked: Boolean, holder: ViewHolder) {
        val position = holder.absoluteAdapterPosition
        petScheduleListAdapterInterface.updatePetSchedule(dataSet[position])

        if(isChecked){
            // Notification ON
            PetScheduleNotification.setAlarmManagerRepeating(
                petScheduleListAdapterInterface.getContext(),
                dataSet[position].id,
                dataSet[position].time,
                Util.getPetNamesFromPetIdList(getPetNameForId.invoke(), dataSet[position].petIdList),
                dataSet[position].memo
            )
        }else{
            // Notification OFF
            PetScheduleNotification.cancelAlarmManagerRepeating(
                petScheduleListAdapterInterface.getContext(),
                dataSet[position].id
            )
        }
    }
}