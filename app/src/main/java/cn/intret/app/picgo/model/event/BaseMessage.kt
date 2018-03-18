package cn.intret.app.picgo.model.event


open class BaseMessage {
    internal var mDesc: String? = null

    constructor()

    constructor(desc: String?) {
        mDesc = desc
    }

    fun getDesc(): String? {
        return mDesc
    }

    fun setDesc(desc: String?): BaseMessage {
        mDesc = desc
        return this
    }

    override fun toString(): String {
        return "BaseMessage{" +
                "mDesc='" + mDesc + '\''.toString() +
                '}'.toString()
    }
}
