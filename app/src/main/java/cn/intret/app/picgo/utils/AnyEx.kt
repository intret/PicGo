package cn.intret.app.picgo.utils

/**
 * Created by intret on 2018/3/21.
 */
infix fun Any?.ifNull(block: () -> Unit) {
    if (this == null) block()
}