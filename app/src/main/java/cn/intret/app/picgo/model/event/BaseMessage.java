package cn.intret.app.picgo.model.event;


public class BaseMessage {
    String mDesc;

    public BaseMessage() {
    }

    public BaseMessage(String desc) {
        mDesc = desc;
    }

    public String getDesc() {
        return mDesc;
    }

    public BaseMessage setDesc(String desc) {
        mDesc = desc;
        return this;
    }

    @Override
    public String toString() {
        return "BaseMessage{" +
                "mDesc='" + mDesc + '\'' +
                '}';
    }
}
