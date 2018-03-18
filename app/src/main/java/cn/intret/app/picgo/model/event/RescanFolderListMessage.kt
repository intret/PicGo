package cn.intret.app.picgo.model.event

class RescanFolderListMessage : BaseMessage {

    constructor(desc: String) : super(desc)

    constructor()

    override fun toString(): String {
        return "RescanFolderListMessage{} " + super.toString()
    }
}
