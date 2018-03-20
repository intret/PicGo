package cn.intret.app.picgo.widget

import android.content.Context
import android.graphics.*
import android.support.v7.widget.RecyclerView
import android.text.TextPaint
import android.util.Log
import android.view.View
import cn.intret.app.picgo.R

class SectionDecoration(context: Context, private val callback: DecorationCallback) : RecyclerView.ItemDecoration() {
    private val textPaint: TextPaint
    private val paint: Paint
    private val topGap: Int
    private var fontMetrics: Paint.FontMetrics = Paint.FontMetrics()

    private var textSize = 12 // in dp unit

    fun setTextSize(textSize: Int): SectionDecoration {
        this.textSize = textSize
        return this
    }


    init {
        val res = context.resources

        paint = Paint()
        paint.color = res.getColor(R.color.colorAccent)

        textPaint = TextPaint()
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.isAntiAlias = true
        textPaint.textSize = 60f
        textPaint.color = Color.BLACK
        textPaint.getFontMetrics(this.fontMetrics)
        textPaint.textAlign = Paint.Align.LEFT

        topGap = res.getDimensionPixelSize(R.dimen.section_top)//32dp
    }


    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State?) {
        super.getItemOffsets(outRect, view, parent, state)
        val pos = parent.getChildAdapterPosition(view)
        Log.i(TAG, "getItemOffsets：$pos")
        val groupId = callback.getGroupId(pos)
        if (groupId < 0) return
        if (pos == 0 || isFirstInGroup(pos)) {//同组的第一个才添加padding
            outRect.top = topGap
        } else {
            outRect.top = 0
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State?) {
        super.onDraw(c, parent, state)
        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight
        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val view = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(view)
            val groupId = callback.getGroupId(position)
            if (groupId < 0) {
                return
            }
            val textLine = callback.getGroupFirstLine(position).toUpperCase()
            if (position == 0 || isFirstInGroup(position)) {
                val top = (view.top - topGap).toFloat()
                val bottom = view.top.toFloat()
                c.drawRect(left.toFloat(), top, right.toFloat(), bottom, paint)//绘制红色矩形
                c.drawText(textLine, left.toFloat(), bottom, textPaint)//绘制文本
            }
        }
    }


    private fun isFirstInGroup(pos: Int): Boolean {
        if (pos == 0) {
            return true
        } else {
            val prevGroupId = callback.getGroupId(pos - 1)
            val groupId = callback.getGroupId(pos)
            return prevGroupId != groupId
        }
    }

    interface DecorationCallback {

        fun getGroupId(position: Int): Long

        fun getGroupFirstLine(position: Int): String
    }

    companion object {
        private val TAG = "SectionDecoration"
    }
}
