package com.sachin.app.chat.adapter

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.sachin.app.chat.R
import com.sachin.app.chat.model.AudioItem
import com.sachin.app.chat.ui.AudioPickerActivity
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.audio_list_item.view.*

class AudioListAdapter(private val audioList: List<AudioItem>) : RecyclerView.Adapter<AudioListAdapter.AudioViewHolder>() {

    private val selectedItems = arrayListOf<Int>()
    private var onPlayClickListener: (AudioItem) -> Unit = {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder =
            AudioViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.audio_list_item, parent, false))

    override fun getItemCount(): Int = audioList.size

    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) = holder.bind(audioList[position])

    inner class AudioViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        fun bind(item: AudioItem) {
            val position = audioList.indexOf(item)
            containerView.audio_title.text = item.title
            containerView.audio_artist.text = item.artist
            containerView.audio_details.text =
                    containerView.resources.getString(R.string.status_time_format,
                            formatMillis(item.duration),
                            Formatter.formatFileSize(containerView.context, item.size).toString())

            containerView.audio_radioButton.isChecked = selectedItems.contains(position)

            containerView.audio_radioButton.setOnClickListener {
                if (selectedItems.contains(position)) {
                    containerView.audio_radioButton.isChecked = false
                    selectedItems.remove(position)
                } else {
                    if (selectedItems.size == AudioPickerActivity.MAX_SELECTABLE) {
                        (it as RadioButton).isChecked = false
                        Toast.makeText(containerView.context, "Only 4 items can be selected at a time", Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }

                    containerView.audio_radioButton.isChecked = true
                    selectedItems.add(position)
                }
            }

            containerView.audio_play_pause_button.setImageDrawable(if (item.isPlaying) containerView.context.getDrawable(R.drawable.ic_baseline_pause_circle_filled_24)
            else containerView.context.getDrawable(R.drawable.ic_baseline_play_circle_filled_24))

            containerView.audio_play_pause_button.setOnClickListener { onPlayClickListener(item) }
        }
    }

    fun setOnPlayClickListener(onPlayClickListener: (AudioItem) -> Unit) {
        this.onPlayClickListener = onPlayClickListener
    }

    fun getSelectedItems() = selectedItems

    fun formatMillis(millis: Long): String {
        var finalTimeString = ""

        val hours = (millis / (1000 * 60 * 60)).toInt()
        val minutes = (millis % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = ((millis % (1000 * 60 * 60)) % (1000 * 60) / 1000)

        if (hours > 0)
            finalTimeString = "$hours:"

        val secondsString =  if (seconds < 10) "0$seconds" else seconds.toString()

        finalTimeString = "$finalTimeString$minutes:$secondsString"
        return finalTimeString
    }
}