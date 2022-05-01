package com.sju18001.petmanagement.ui.myPet.petScheduleManager

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.sju18001.petmanagement.controller.Util
import java.time.LocalTime

object BindingAdapters {
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

    @JvmStatic
    @BindingAdapter("layout_marginLeft")
    fun setMarginLeft(view: View, dp: Int) {
        val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.leftMargin = dp
        view.layoutParams = layoutParams
    }
}