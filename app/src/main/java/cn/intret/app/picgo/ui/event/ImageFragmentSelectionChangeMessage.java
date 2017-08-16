package cn.intret.app.picgo.ui.event;


public class ImageFragmentSelectionChangeMessage {
    int currentCode;

    public int getCurrentCode() {
        return currentCode;
    }

    public ImageFragmentSelectionChangeMessage setCurrentCode(int currentCode) {
        this.currentCode = currentCode;
        return this;
    }
}
