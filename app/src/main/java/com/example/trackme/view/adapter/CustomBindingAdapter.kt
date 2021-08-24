package com.example.trackme.view.adapter

import android.graphics.BitmapFactory
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import java.text.SimpleDateFormat
import java.util.*

@BindingAdapter("imageBitmap")
fun ImageView.setImage(data: ByteArray?) {
    if (data == null)
        this.setImageResource(android.R.drawable.presence_online)
    else
        this.setImageBitmap(BitmapFactory.decodeByteArray(data, 0, data.size))
}


@BindingAdapter("timeFor")
fun TextView.setTimeText(seconds: Long) {
    val formatter = SimpleDateFormat("hh:mm:ss", Locale.getDefault())
    this.text = formatter.format(Date(seconds * 1000))
}