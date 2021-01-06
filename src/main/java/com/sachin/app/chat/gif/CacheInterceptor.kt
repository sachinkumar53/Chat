package com.sachin.app.chat.gif

import android.content.Context
import android.net.ConnectivityManager
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.Response
import java.io.File
import java.io.IOException

internal class CacheInterceptor(private val context: Context,
                                cacheTimeMins: Int) : Interceptor {

    private val cacheTimeMills = cacheTimeMins * 60000

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalResponse = chain.proceed(chain.request())
        return if (isNetworkAvailable(context)) {
            originalResponse.newBuilder()
                    .header("Cache-Control", "public, max-age=$cacheTimeMills")
                    .build()
        } else {
            val maxStale = 2419200 // tolerate 4-weeks stale
            originalResponse.newBuilder()
                    .header("Cache-Control", "public, only-if-cached, max-stale=$maxStale")
                    .build()
        }
    }

    /**
     * Check if the internet is available?
     *
     * @param context instance.
     * @return True if the internet is available else false.
     */
    private fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = cm.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    companion object {
        private const val CACHE_SIZE = 5242880 //5 MB //Cache size.

        /**
         * Initialize the cache directory.
         *
         * @param context Instance of caller.
         * @return [Cache].
         */
        fun getCache(context: Context): Cache {
            //Define mCache
            val httpCacheDirectory = File(context.cacheDir, "responses")
            return Cache(httpCacheDirectory, CACHE_SIZE.toLong())
        }
    }

}