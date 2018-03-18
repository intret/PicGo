package cn.intret.app.picgo.ui.event


class ImageFragmentSelectionChangeMessage {
    internal var currentCode: Int = 0

    internal lateinit var transitionName: String

    fun getTransitionName(): String {
        return transitionName
    }

    fun setTransitionName(transitionName: String): ImageFragmentSelectionChangeMessage {
        this.transitionName = transitionName
        return this
    }

    fun getCurrentCode(): Int {
        return currentCode
    }

    fun setCurrentCode(currentCode: Int): ImageFragmentSelectionChangeMessage {
        this.currentCode = currentCode
        return this
    }
}
