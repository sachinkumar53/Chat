package com.sachin.app.chat.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.sachin.app.chat.R
import com.sachin.app.chat.util.ImageUtils
import com.sachin.app.chat.util.ImageUtils.uriToBitmap
import kotlinx.android.synthetic.main.activity_image_crop.*


class ImageCropActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {
    private var bitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_crop)

        crop_view.applyAspectRatio(1, 1)
        LoadImageTask().execute()
        bottomNavigationView.setOnNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.image_crop_save -> save()
            R.id.image_crop_rotate -> {
                bitmap = ImageUtils.rotateBitmap(bitmap!!)
                crop_view.setBitmap(bitmap!!)
            }

            R.id.image_crop_discard -> {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }

        return true
    }

    private fun save() {
        bitmap = crop_view.getCroppedBitmap()
        bitmap = ImageUtils.getResizedBitmap(bitmap!!, 256)
        //ImageUtils.saveBitmapLocally(this@ImageCropActivity, bitmap)
        val returnIntent = Intent()
        returnIntent.putExtra("photo", bitmap)
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }

    inner class LoadImageTask : AsyncTask<Void?, Void?, Void?>() {
        override fun onPreExecute() {
            super.onPreExecute()
            runOnUiThread { progress_view.visibility = View.VISIBLE }
        }

        override fun doInBackground(vararg params: Void?): Void? {
            bitmap = uriToBitmap(contentResolver, intent.data)
            //Log.w("SEX", "Original size ${ImageUtils.getFileSize(bitmap)}")
            bitmap = ImageUtils.compressBitmap(contentResolver, intent.data)
            //Log.w("SEX", "After compression size ${ImageUtils.getFileSize(bitmap)}")
            return null
        }

        override fun onPostExecute(result: Void?) {
            super.onPostExecute(result)
            runOnUiThread {
                progress_view.visibility = View.GONE

                if (bitmap == null) {
                    finish()
                } else {
                    crop_view.setBitmap(bitmap!!)
                }
            }
        }

    }
}
