package com.sachin.app.chat.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.provider.MediaStore
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.*


object ImageUtils {

    fun blurBitmap(context: Context, srcBitmap: Bitmap, scale: Float = 2.0F, radius: Float): Bitmap {
        try {
            val bitmap = Bitmap.createScaledBitmap(srcBitmap, (srcBitmap.width * scale).toInt(),
                    (srcBitmap.height * scale).toInt(), false)
            val rs: RenderScript = RenderScript.create(context)
            val input: Allocation = Allocation.createFromBitmap(rs, bitmap, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
            val output: Allocation = Allocation.createTyped(rs, input.type);
            val script: ScriptIntrinsicBlur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
            script.setRadius(radius /* e.g. 3.f */);
            script.setInput(input)
            script.forEach(output)
            output.copyTo(bitmap)
            return bitmap
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return srcBitmap
    }

    fun saveBitmapLocally(context: Context, bitmap: Bitmap?) {
        if (bitmap == null) return
        try {
            val myDir = File(context.getExternalFilesDir(null), "/media")
            if (!myDir.exists()) myDir.mkdirs()

            val outputFile = File(myDir, "profile_pic.png")
            if (outputFile.exists()) //delete previous data files
                outputFile.delete()

            try {
                val fos = FileOutputStream(outputFile)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.close()
            } catch (e: Exception) {
                logError(e)
            }
        } catch (e: Exception) {
            logError(e)
        }
    }

    fun rotateBitmap(srcBitmap: Bitmap, degrees: Float = 90F): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.width, srcBitmap.height, matrix, true)
    }

    fun compressBitmap(contentResolver: ContentResolver, imageUri: Uri?): Bitmap? {
        val filePath = getRealPathFromURI(contentResolver, imageUri)
        var scaledBitmap: Bitmap? = null
        val options = BitmapFactory.Options()

        /**
         *  By setting this field as true, the actual bitmap pixels are not loaded in the memory. Just the bounds are loaded. If
         *  you try the use the bitmap here, you will get null.  */

        options.inJustDecodeBounds = true
        var bmp = BitmapFactory.decodeFile(filePath, options)
        var actualHeight = options.outHeight
        var actualWidth = options.outWidth

        //max Height and width values of the compressed image is taken as 816x612
        val maxHeight = 816.0f
        val maxWidth = 612.0f
        var imgRatio = actualWidth / actualHeight.toFloat()
        val maxRatio = maxWidth / maxHeight

        //width and height values are set maintaining the aspect ratio of the image
        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            if (imgRatio < maxRatio) {
                imgRatio = maxHeight / actualHeight
                actualWidth = (imgRatio * actualWidth).toInt()
                actualHeight = maxHeight.toInt()
            } else if (imgRatio > maxRatio) {
                imgRatio = maxWidth / actualWidth
                actualHeight = (imgRatio * actualHeight).toInt()
                actualWidth = maxWidth.toInt()
            } else {
                actualHeight = maxHeight.toInt()
                actualWidth = maxWidth.toInt()
            }
        }

        options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight)
        options.inJustDecodeBounds = false
        options.inTempStorage = ByteArray(16 * 1024)

        try {
            //load the bitmap from its path
            bmp = BitmapFactory.decodeFile(filePath, options)
        } catch (exception: OutOfMemoryError) {
            exception.printStackTrace()
        }
        try {
            scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888)
        } catch (exception: OutOfMemoryError) {
            exception.printStackTrace()
        }

        val ratioX = actualWidth / options.outWidth.toFloat()
        val ratioY = actualHeight / options.outHeight.toFloat()
        val middleX = actualWidth / 2.0f
        val middleY = actualHeight / 2.0f
        val scaleMatrix = Matrix()
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY)
        val canvas = Canvas(scaledBitmap!!)
        canvas.setMatrix(scaleMatrix)
        canvas.drawBitmap(bmp, middleX - bmp.width / 2, middleY - bmp.height / 2, Paint(Paint.FILTER_BITMAP_FLAG))

//      check the rotation of the image and display it properly
        val exif: ExifInterface
        try {
            exif = ExifInterface(filePath!!)
            val orientation: Int = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, 0)
            Log.d("EXIF", "Exif: $orientation")
            val matrix = Matrix()
            if (orientation == 6) {
                matrix.postRotate(90f)
                Log.d("EXIF", "Exif: $orientation")
            } else if (orientation == 3) {
                matrix.postRotate(180f)
                Log.d("EXIF", "Exif: $orientation")
            } else if (orientation == 8) {
                matrix.postRotate(270f)
                Log.d("EXIF", "Exif: $orientation")
            }
            scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0,
                    scaledBitmap.width, scaledBitmap.height, matrix,
                    true)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val os = ByteArrayOutputStream()
        scaledBitmap!!.compress(Bitmap.CompressFormat.PNG, 80, os)
        val byteArray = os.toByteArray()
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)

    }

    fun getRealPathFromURI(contentResolver: ContentResolver, uri: Uri?): String? {
        if (uri == null)
            return null

        val cursor = contentResolver.query(uri, null, null, null, null)
        return if (cursor == null) {
            uri.path
        } else {
            cursor.moveToFirst()
            val index: Int = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            //val index: Int = cursor.getColumnIndex(MediaStore.Images.Media._ID)
            cursor.getString(index)
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val heightRatio = Math.round(height.toFloat() / reqHeight.toFloat())
            val widthRatio = Math.round(width.toFloat() / reqWidth.toFloat())
            inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
        }
        val totalPixels = width * height.toFloat()
        val totalReqPixelsCap = reqWidth * reqHeight * 2.toFloat()
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++
        }
        return inSampleSize
    }


    fun getResizedBitmap(image: Bitmap, maxSize: Int): Bitmap? {
        var width = image.width
        var height = image.height
        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = maxSize
            height = (width / bitmapRatio).toInt()
        } else {
            height = maxSize
            width = (height * bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(image, width, height, true)
    }

    fun uriToBitmap(contentResolver: ContentResolver, uri: Uri?): Bitmap? {
        if (uri == null) return null
        try {
            val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
            val fileDescriptor: FileDescriptor = parcelFileDescriptor!!.fileDescriptor
            val bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            parcelFileDescriptor.close()
            return bitmap
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private const val KB = 1024
    private const val MB = 1024 * 1024
    fun getFileSize(bitmap: Bitmap?): String {
        if (bitmap == null) return "Bitmap is null"
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val imageInByte = stream.toByteArray()
        val lengthbmp = imageInByte.size.toLong()

        return when {
            lengthbmp < KB -> "$lengthbmp bytes"
            lengthbmp in KB until MB -> "${lengthbmp.div(KB)} KB"
            lengthbmp >= MB -> "${lengthbmp.div(MB)} MB"
            else -> "$lengthbmp"
        }
    }

}