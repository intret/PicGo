package cn.intret.app.picgo.ui.event;

/**
 * 图片查看器当前显示图片发生变化
 */

public class CurrentImageChangeMessage {
    int mPosition;

    public int getPosition() {
        return mPosition;
    }

    public CurrentImageChangeMessage setPosition(int position) {
        mPosition = position;
        return this;
    }

    @Override
    public String toString() {
        return "CurrentImageChangeMessage{" +
                "mPosition=" + mPosition +
                '}';
    }
}
