package com.sju18001.petmanagement.ui.map

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import com.sju18001.petmanagement.R
import net.daum.mf.map.api.CalloutBalloonAdapter
import net.daum.mf.map.api.MapPOIItem

class CalloutBalloonAdapter(inflater: LayoutInflater) :
    CalloutBalloonAdapter {
    private val view: View = inflater.inflate(R.layout.layout_calloutballoon, null)

    override fun getCalloutBalloon(poiItem: MapPOIItem): View {
        val iconImageView = (view.findViewById<View>(R.id.imageview_calloutballoon_icon) as ImageView)
        iconImageView.setImageResource(R.drawable.ic_baseline_place_48)
        iconImageView.y = 8f

        return view
    }

    override fun getPressedCalloutBalloon(poiItem: MapPOIItem?): View? {
        return null
    }
}