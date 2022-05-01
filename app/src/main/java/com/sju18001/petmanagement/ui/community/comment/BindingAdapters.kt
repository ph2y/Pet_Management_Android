package com.sju18001.petmanagement.ui.community.comment

import android.text.Spannable
import android.text.SpannableString
import android.text.style.LeadingMarginSpan
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.sju18001.petmanagement.controller.Util

object BindingAdapters {
    @JvmStatic
    @BindingAdapter("contents", "nicknameTextView")
    fun bindContents(textView: TextView, contents: String, nicknameTextView: TextView) {
        nicknameTextView.post{
            // contents의 첫줄에 닉네임 영역만큼 indent를 줍니다.
            val spannable = SpannableString(contents)
            val span = LeadingMarginSpan.Standard(nicknameTextView.width + 10, 0)
            spannable.setSpan(span, 0, spannable.count(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            textView.text = spannable
        }
    }

    @JvmStatic
    @BindingAdapter("timestamp")
    fun bindTimeStamp(textView: TextView, timestamp: String) {
        textView.text = Util.getTimestampForDisplay(timestamp)
    }
}