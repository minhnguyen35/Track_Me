package com.example.trackme.view.adapter

import android.graphics.BitmapFactory
import android.text.format.DateUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter

@BindingAdapter("imageBitmap")
fun ImageView.setImage(data: ByteArray?) {
    if (data == null)
        this.setImageResource(android.R.drawable.presence_online)
    else
        this.setImageBitmap(BitmapFactory.decodeByteArray(data, 0, data.size))
}


@BindingAdapter("timeFor")
fun TextView.setTimeText(seconds: Long) {
    this.text = if (seconds < 60 * 60) {
        //seconds.toString()
        "00:" + DateUtils.formatElapsedTime(seconds)
    } else {
        DateUtils.formatElapsedTime(seconds)
    }
}