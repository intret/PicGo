package cn.intret.app.picgo.model

import android.annotation.TargetApi
import android.os.Build

/**
 * Created by intret on 2017/9/23.
 */

class NotEmptyException : Exception {

    constructor()

    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(cause: Throwable) : super(cause)

    @TargetApi(Build.VERSION_CODES.N)
    constructor(message: String, cause: Throwable, enableSuppression: Boolean, writableStackTrace: Boolean) : super(message, cause, enableSuppression, writableStackTrace)
}
