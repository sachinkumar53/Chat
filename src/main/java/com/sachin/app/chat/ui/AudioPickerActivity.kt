package com.sachin.app.chat.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.sachin.app.chat.R
import com.sachin.app.chat.adapter.AudioListAdapter
import com.sachin.app.chat.model.AudioItem
import com.sachin.app.chat.util.showToast
import kotlinx.android.synthetic.main.activity_audio_picker.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


class AudioPickerActivity : AppCompatActivity() {
    private val audioList = arrayListOf<AudioItem>()
    val adapter by lazy { AudioListAdapter(audioList) }
    private var lastPlayAudioItem: AudioItem? = null
    private val mediaPlayer = MediaPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_picker)

        setSupportActionBar(toolbar_audio)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        lifecycleScope.launch { loadAudioFiles() }

        recyclerView_audio.layoutManager = LinearLayoutManager(this)
        recyclerView_audio.adapter = adapter

        adapter.setOnPlayClickListener {
            if (lastPlayAudioItem?.path == it.path) {
                if (it.isPlaying) mediaPlayer.pause()
                else mediaPlayer.start()
                it.isPlaying = !it.isPlaying
                adapter.notifyDataSetChanged()
            } else {
                mediaPlayer.reset()
                mediaPlayer.setDataSource(it.path)
                mediaPlayer.prepare()
                mediaPlayer.start()
                audioList.forEach { item -> item.isPlaying = false }
                it.isPlaying = true
                adapter.notifyDataSetChanged()
                lastPlayAudioItem = it
            }
        }

        cancel_button.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        done_button.setOnClickListener {
            val selectedItems = adapter.getSelectedItems()

            if (selectedItems.size == 0) {
                showToast("Please select items to send")
                return@setOnClickListener
            }

            val pathList = arrayListOf<String>()

            selectedItems.forEach { pathList.add(audioList[it].path) }

            val returnIntent = Intent()
            returnIntent.putStringArrayListExtra(SELECTED_AUDIO_FILES, pathList)
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
        }
    }

    override fun onPause() {
        super.onPause()

        if (mediaPlayer.isPlaying) mediaPlayer.pause()
        lastPlayAudioItem?.let {
            it.isPlaying = false
            adapter.notifyDataSetChanged()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.stop()
    }

    @SuppressLint("InlinedApi")
    private suspend fun loadAudioFiles() {
        withContext(Dispatchers.IO) {
            val client = contentResolver.acquireContentProviderClient(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)

            val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"

            val projection = arrayOf(
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.DATE_MODIFIED,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.SIZE,
                    MediaStore.Audio.Media.DATA
            )

            var cursor: Cursor? = null

            try {
                cursor = client?.query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        selection,
                        null,
                        "${MediaStore.Audio.Media.DATE_ADDED} DESC")

                cursor?.run {
                    while (moveToNext()) {
                        val title = cursor.getString(0)
                        val artist = cursor.getString(1)
                        val date = cursor.getString(2)
                        val duration = cursor.getString(3)
                        val size = cursor.getString(4)
                        val path = cursor.getString(5)

                        val item = AudioItem(title, artist, date.toLong(), duration.toLong(), size.toLong(), path)
                        audioList.add(item)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "loadAudioFiles: ", e)
                showToast("Error selecting audio")
            } finally {
                cursor?.close()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    client?.close()
                else client?.release()
            }
        }
    }

    companion object {
        private const val TAG = "AudioPickerActivity"
        const val MAX_SELECTABLE = 4
        const val SELECTED_AUDIO_FILES = "selected_audio_files"
    }
}


