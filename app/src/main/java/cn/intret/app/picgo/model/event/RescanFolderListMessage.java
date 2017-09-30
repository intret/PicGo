package cn.intret.app.picgo.model.event;

public class RescanFolderListMessage extends BaseMessage {

    public RescanFolderListMessage(String desc) {
        super(desc);
    }

    public RescanFolderListMessage() {
    }

    @Override
    public String toString() {
        return "RescanFolderListMessage{} " + super.toString();
    }
}
