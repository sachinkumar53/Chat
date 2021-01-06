package com.sachin.app.chat.ui

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sachin.app.chat.BuildConfig
import com.sachin.app.chat.R
import com.sachin.app.chat.util.showToast
import kotlinx.android.synthetic.main.activity_check_update.*
import java.io.File

class CheckUpdateActivity : AppCompatActivity() {

    private val updateDatabaseReference by lazy { FirebaseDatabase.getInstance().reference.child("updates") }
    private val currentAppVersion = BuildConfig.VERSION_NAME
    private var version: String? = ""
    private var url: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_update)

        app_version_text.text = "Version $currentAppVersion"
        back_button.setOnClickListener { onBackPressed() }

        update_button.visibility = View.GONE
        progress_bar.visibility = View.VISIBLE
        progress_bar.bringToFront()
        update_text.text = "Checking for updates"

        updateDatabaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                progress_bar.visibility = View.GONE
                if (p0.hasChildren()) {
                    version = p0.child("version_name").getValue(String::class.java)
                    url = p0.child("download_url").getValue(String::class.java)

                    val i = convertVersionToInt(version)
                    val j = convertVersionToInt(currentAppVersion)

                    Log.w("SEX", "i = $i  j =$j   i>j = ${i > j}")
                    val isUpdateAvailable = i > j
                    if (isUpdateAvailable) {
                        update_text.text = "New update version $version is available"
                        update_button.visibility = View.VISIBLE
                        update_button.bringToFront()
                        update_button.setOnClickListener {
                            checkStoragePermission()
                        }
                    } else update_text.text = "Your app is up to date"
                } else update_text.text = "Your app is up to date"
            }

            override fun onCancelled(p0: DatabaseError) {
                progress_bar.visibility = View.GONE
                update_text.text = "Error checking update"
            }
        })
    }

    private fun checkStoragePermission() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            downloadAndInstall(url)
        else requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 788)

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 788) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                downloadAndInstall(url)
            else showToast("storage permission denied")

        }
    }


    private fun downloadAndInstall(url: String?) {
        if (url == null || url.trim().isEmpty()) {
            showToast("Couldn't update. Download link is not available yet.")
            return
        }

        enqueueDownload(url)
    }

    private fun enqueueDownload(url: String?) {
        var destination = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/"
        destination += FILE_NAME
        val uri = Uri.parse("$FILE_BASE_PATH$destination")
        val file = File(destination)
        if (file.exists()) file.delete()

        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadUri = Uri.parse(url)
        val request = DownloadManager.Request(downloadUri)
        request.setMimeType(MIME_TYPE)
        request.setTitle("Chat app version $version")
        request.setDescription("Downloading update")
        // set destination
        request.setDestinationUri(uri)
        showInstallOption(destination, uri)
        // Enqueue a new download and same the referenceId
        downloadManager.enqueue(request)
        showToast("Downloading update")
    }

    private fun showInstallOption(destination: String, uri: Uri) {
        // set BroadcastReceiver to install app when .apk is downloaded
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val contentUri = FileProvider.getUriForFile(
                            context,
                            BuildConfig.APPLICATION_ID + PROVIDER_PATH,
                            File(destination)
                    )

                    val install = Intent(Intent.ACTION_VIEW)
                    install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    install.data = contentUri
                    context.startActivity(install)
                    context.unregisterReceiver(this)
                    // finish()
                } else {
                    val install = Intent(Intent.ACTION_VIEW)
                    install.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    install.setDataAndType(uri, APP_INSTALL_PATH)
                    context.startActivity(install)
                    context.unregisterReceiver(this)
                    // finish()
                }
            }
        }
        registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }


    private fun convertVersionToInt(version: String?): Int {
        if (version == null) return 0

        val digits = version.split(".")
        var v = ""
        digits.forEach { v += it }
        return v.toInt()
    }

    companion object {
        private const val FILE_NAME = "ChatApp.apk"
        private const val FILE_BASE_PATH = "file://"
        private const val MIME_TYPE = "application/vnd.android.package-archive"
        private const val PROVIDER_PATH = ".provider"
        private const val APP_INSTALL_PATH = "\"application/vnd.android.package-archive\""
    }
}
