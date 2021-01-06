package com.sachin.app.chat.widget

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.TextView
import com.sachin.app.chat.R
import com.sachin.app.chat.util.dpToPx
import kotlin.math.absoluteValue

class MessagePopupMenu(val context: Context) : PopupWindow(context, null, 0, R.style.Widget_AppCompat_PopupWindow) {
    private val items = arrayListOf(ITEM_COPY, ITEM_DOWNLOAD, ITEM_DETAILS, ITEM_DELETE)
    private val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, items)
    private val listView = ListView(context)

    init {

        val header = TextView(context).apply {
            text = "Message options"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5F)
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(context.getColor(R.color.colorAccent))
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
        }


        listView.run {
            addHeaderView(header, null, false)
            adapter = this@MessagePopupMenu.adapter
            divider = null
            val i = context.dpToPx(8)
            setPadding(i, i, i, i)
        }

        contentView = listView
        setBackgroundDrawable(context.getDrawable(R.drawable.popup_menu_background))

        width = context.dpToPx(164)
        elevation = context.dpToPx(10).toFloat()
        overlapAnchor = true
        isOutsideTouchable = true
    }

    fun addItem(item: String) {
        items.add(item)
        adapter.notifyDataSetChanged()
    }

    fun removeItem(item: String) {
        items.remove(item)
        adapter.notifyDataSetChanged()
    }

    fun setOnItemClickListener(onItemClickListener: (String) -> Unit) {
        listView.setOnItemClickListener { _, _, position, _ ->
            onItemClickListener(items[(position - 1)])
            dismiss()
        }
    }

    fun show(view: View) {
        val x = (view.layoutParams.width - width) / 2
        showAsDropDown(view, x.absoluteValue, 0, Gravity.CENTER)
    }

    companion object {
        const val ITEM_COPY = "Copy text"
        const val ITEM_DOWNLOAD = "Download"
        const val ITEM_DETAILS = "Details"
        const val ITEM_DELETE = "Delete"
    }
}