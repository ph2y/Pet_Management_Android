package com.sju18001.petmanagement.ui.map

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import com.sju18001.petmanagement.R
import net.daum.mf.map.api.CalloutBalloonAdapter
import net.daum.mf.map.api.MapPOIItem

class CustomCalloutBalloonAdapter(inflater: LayoutInflater) : CalloutBalloonAdapter {
    private val calloutBalloon: View = inflater.inflate(R.layout.custom_callout_balloon, null)

    override fun getCalloutBalloon(poiItem: MapPOIItem): View {
        val customCalloutBalloonImage = (calloutBalloon.findViewById<View>(R.id.custom_callout_balloon_image) as ImageView)
        customCalloutBalloonImage.setImageResource(R.drawable.ic_baseline_place_48)
        customCalloutBalloonImage.y = 8f

        return calloutBalloon
    }

    override fun getPressedCalloutBalloon(poiItem: MapPOIItem?): View? {
        return null
    }
}