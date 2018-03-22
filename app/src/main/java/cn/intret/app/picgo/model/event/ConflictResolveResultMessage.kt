package cn.intret.app.picgo.model.event


import cn.intret.app.picgo.model.image.data.CompareItemResolveResult

class ConflictResolveResultMessage {
    internal lateinit var compareItems: List<CompareItemResolveResult>

    fun getCompareItems(): List<CompareItemResolveResult> {
        return compareItems
    }

    fun setCompareItems(compareItems: List<CompareItemResolveResult>): ConflictResolveResultMessage {
        this.compareItems = compareItems
        return this
    }
}
