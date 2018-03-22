package cn.intret.app.picgo.model.image.data


class CompareItemResolveResult {

    internal lateinit var compareItem: CompareItem
    /**
     * true, 表示已经按照 [.getCompareItem] 标识的方法解决了冲突。
     */
    internal var resolved: Boolean = false

    constructor(compareItem: CompareItem, resolved: Boolean) {
        this.compareItem = compareItem
        this.resolved = resolved
    }


    override fun toString(): String {
        return "CompareItemResolveResult{" +
                "compareItem=" + compareItem +
                ", resolved=" + resolved +
                '}'.toString()
    }

}
