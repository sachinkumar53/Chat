package com.sachin.app.chat.gif

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.LayoutTransition
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sachin.app.chat.R
import com.sachin.app.chat.adapter.GIFAdapter
import com.sachin.app.chat.constants.Constant
import com.sachin.app.chat.emoji.AnimEmojiAdapter
import com.sachin.app.chat.util.EndlessRecyclerViewScrollListener
import com.sachin.app.chat.util.hide
import com.sachin.app.chat.util.show
import com.sachin.app.chat.util.showToast
import kotlinx.android.synthetic.main.fragment_gif.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class GifFragment : Fragment(), View.OnClickListener {
    private val gifList = arrayListOf<GifItem>()
    private val gifAdapter by lazy { GIFAdapter(gifList) }
    private val gifClient by lazy { GiphyApiClient.getGIFClient(requireContext()).create(GiphyApiService::class.java) }
    private val emojiAdapter by lazy { AnimEmojiAdapter() }
    private val gifLayoutManager by lazy { GridLayoutManager(requireContext(), 2) }
    private val emojiLayoutManager by lazy { GridLayoutManager(requireContext(), 6) }
    private val handler = Handler(Looper.getMainLooper())
    private var searchQuery :String? = null
    private var selectedTab = 0

    private val scrollListener by lazy {   object : EndlessRecyclerViewScrollListener(gifLayoutManager){
        override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
            lifecycleScope.launch {
                val offSet = page* LIMIT
                if (selectedTab == 0)
                    if (isSearchMode)
                        searchGIF(searchQuery!!,offSet)
                    else loadTrendingGIF(offSet)
                else if (selectedTab == 1)
                    if (isSearchMode)
                        searchSticker(searchQuery!!,offSet)
                    else loadTrendingSticker(offSet)
            }
        }
    }
    }

    private val searchRunnable = Runnable{
        if (!searchQuery.isNullOrEmpty()) {
            gifList.clear()
            scrollListener.resetState()
            showError(false)
            showProgress()
            lifecycleScope.launch {
                if (selectedTab == 0)
                searchGIF(searchQuery!!)
                else searchSticker(searchQuery!!)
            }
        } else showToast("Enter a keyword to search")
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_gif, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showProgress()

        setSelectedTab(0)

        val searchBar = gif_search.findViewById<LinearLayout>(androidx.appcompat.R.id.search_bar)
        searchBar.layoutTransition = LayoutTransition()

        gif_search.setOnSearchClickListener {
            search_layout.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            search_layout.requestLayout()
            tab_layout.animate().alpha(0F).setInterpolator(AccelerateDecelerateInterpolator())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator?) {
                            super.onAnimationEnd(animation)
                            tab_layout.hide(true)
                        }
                    })
                    .start()
        }

        for (i in 0 until tab_layout.childCount) {
            tab_layout.getChildAt(i).setOnClickListener(this)
        }

        gif_search.setOnCloseListener {
            isSearchMode = false
            showError(false)
            showProgress()

            lifecycleScope.launch { if (selectedTab == 0) loadTrendingGIF() else loadTrendingSticker()}

            search_layout.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
            search_layout.requestLayout()
            tab_layout.animate().alpha(1F).setInterpolator(AccelerateDecelerateInterpolator())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator?) {
                            super.onAnimationEnd(animation)
                            tab_layout.show()
                        }
                    })
                    .start()
            false
        }


        gif_search.setOnQueryTextListener(object :SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchQuery = query
                isSearchMode = true

                handler.removeCallbacks(searchRunnable)
                handler.post(searchRunnable)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery = newText
                isSearchMode = !newText.isNullOrEmpty()
                handler.removeCallbacks(searchRunnable)
                handler.postDelayed(searchRunnable,500)
                return false
            }
        })
    }

    override fun onClick(v: View) {
        val index = tab_layout.indexOfChild(v)
        if (selectedTab == index)
            return

        selectedTab = index
        setSelectedTab(index)
    }

    private fun setSelectedTab(position: Int) {
        selectedTab = position
        for (i in 0 until tab_layout.childCount) {
            tab_layout.getChildAt(i).isSelected = i == position
        }

        when(selectedTab){
            0 ->{
                gif_grid_view.apply {
                    adapter = gifAdapter
                    layoutManager = gifLayoutManager
                    addOnScrollListener(scrollListener)
                }

                scrollListener.resetState()
                gifList.clear()
                lifecycleScope.launch { loadTrendingGIF() }
            }

            1 ->{
                gif_grid_view.apply {
                    adapter = gifAdapter
                    layoutManager = gifLayoutManager
                    addOnScrollListener(scrollListener)
                }

                scrollListener.resetState()
                gifList.clear()
                lifecycleScope.launch { loadTrendingSticker() }
            }

            2 ->{
                gif_grid_view.apply {
                    adapter = emojiAdapter
                    layoutManager = emojiLayoutManager
                    clearOnScrollListeners()
                    //removeOnScrollListener(scrollListener)
                }
            }
        }
    }

    private fun getSelectedTab(): Int {
        for (i in 0 until tab_layout.childCount) {
            if (tab_layout.getChildAt(i).isSelected)
                selectedTab = i
        }

        return selectedTab
    }

    fun setOnGIFClickListener(onGIFClickListener: (GifItem) -> Unit) {
        gifAdapter.setOnGIFClickListener { onGIFClickListener(it) }
    }

    private fun showProgress(show: Boolean = true) {
        if (show) {
            gif_progress.show()
            gif_grid_view.hide()
            gif_error.hide(true)
        } else {
            gif_error.hide(true)
            gif_progress.hide()
            gif_grid_view.show()
        }
    }

    private fun showError(show: Boolean = true) {
        if (show) {
            gif_error.show()
            gif_progress.hide(true)
            gif_grid_view.hide()
        } else {
            gif_error.hide(true)
            gif_progress.hide(true)
            gif_grid_view.show()
        }
    }

    private suspend fun searchGIF(query: String,offSet: Int = 0) {
        withContext(Dispatchers.IO) {
            gifClient.searchGIFs(Constant.GIPHY_API_KEY, query, LIMIT,offSet).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {

                    val body = response.body() ?: return
                    val json = body.string()
                    parse(json)

                    gifAdapter.notifyDataSetChanged()

                    if (isAdded)
                        showProgress(false)
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("TAG", "onFailure: ", t)
                    showError(true)
                }
            })
        }
    }

    private suspend fun searchSticker(query: String,offSet: Int = 0) {
        withContext(Dispatchers.IO) {
            gifClient.searchStickers(Constant.GIPHY_API_KEY, query, LIMIT,offSet).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {

                    val body = response.body() ?: return
                    val json = body.string()
                    parse(json)

                    gifAdapter.notifyDataSetChanged()

                    if (isAdded)
                        showProgress(false)
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("TAG", "onFailure: ", t)
                    showError(true)
                }
            })
        }
    }


    private suspend fun loadTrendingGIF(offSet: Int = 0) {
        withContext(Dispatchers.IO) {
            gifClient.getTrendingGIFs(Constant.GIPHY_API_KEY, LIMIT, offSet).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    val body = response.body() ?: return
                    val json = body.string()

                    parse(json)

                    gifAdapter.notifyDataSetChanged()

                    if (isAdded)
                        showProgress(false)
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("TAG", "onFailure: ", t)
                    showError(true)
                }
            })
        }
    }

    private suspend fun loadTrendingSticker(offSet: Int = 0) {
        withContext(Dispatchers.IO) {
            gifClient.getTrendingStickers(Constant.GIPHY_API_KEY, LIMIT, offSet).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    val body = response.body() ?: return
                    val json = body.string()
                    parse(json)
                    gifAdapter.notifyDataSetChanged()
                    if (isAdded)
                        showProgress(false)
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("TAG", "onFailure: ", t)
                    showError(true)
                }
            })
        }
    }

    private fun parse(response: String) {
        val data = JSONObject(response).getJSONArray("data")

        for (i in 0 until data.length()) {
            val images = data.getJSONObject(i).getJSONObject("images")
            val fhs = images.getJSONObject("fixed_width")
            val url = fhs.getString("url")
            val previewUrl = if (images.has("preview_gif"))
                images.getJSONObject("preview_gif").getString("url")
            else null
            gifList.add(GifItem(url, previewUrl))
        }
    }

    companion object {
        private const val LIMIT = 24
        private var isSearchMode = false
    }
}