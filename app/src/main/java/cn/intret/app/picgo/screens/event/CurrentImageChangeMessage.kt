package cn.intret.app.picgo.screens.event

/**
 * 图片查看器当前显示图片发生变化
 */

class CurrentImageChangeMessage {
    internal var mPosition: Int = 0

    fun getPosition(): Int {
        return mPosition
    }

    fun setPosition(position: Int): CurrentImageChangeMessage {
        mPosition = position
        return this
    }

    override fun toString(): String {
        return "CurrentImageChangeMessage{" +
                "mPosition=" + mPosition +
                '}'.toString()
    }
}
