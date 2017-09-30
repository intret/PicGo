package cn.intret.app.picgo.model.event;


public class ExcludeFolderListChangeMessage extends BaseMessage {

    public ExcludeFolderListChangeMessage(String desc) {
        super(desc);
    }

    public ExcludeFolderListChangeMessage() {
    }

    @Override
    public String toString() {
        return "ExcludeFolderListChangeMessage{} " + super.toString();
    }
}
