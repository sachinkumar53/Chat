package com.sachin.app.chat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.sachin.app.chat.R
import com.sachin.app.chat.gif.GifItem
import com.sachin.app.chat.util.dpToPx
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.gif_item.view.*

class GIFAdapter(private val gifList: ArrayList<GifItem>) : RecyclerView.Adapter<GIFAdapter.GIFViewHolder>() {
    private var onGIFClickListener: (GifItem) -> Unit = {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GIFViewHolder =
            GIFViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.gif_item, parent, false))

    override fun onBindViewHolder(holder: GIFViewHolder, position: Int) = holder.bind(gifList[position])

    override fun getItemCount(): Int = gifList.size

    inner class GIFViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bind(gifItem: GifItem) {
            Glide.with(containerView).asGif()
                    .placeholder(R.drawable.gif_place_holder)
                    .load(gifItem.previewUrl)
                    .optionalCenterInside()
                    .into(containerView.gif_item)

            containerView.gif_item.setOnClickListener { onGIFClickListener(gifItem) }
        }
    }

    fun setOnGIFClickListener(onGIFClickListener: (GifItem) -> Unit) {
        this.onGIFClickListener = onGIFClickListener
    }
}