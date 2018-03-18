package cn.intret.app.picgo.model.event


class ExcludeFolderListChangeMessage : BaseMessage {

    constructor(desc: String) : super(desc)

    constructor()

    override fun toString(): String {
        return "ExcludeFolderListChangeMessage{} " + super.toString()
    }
}
