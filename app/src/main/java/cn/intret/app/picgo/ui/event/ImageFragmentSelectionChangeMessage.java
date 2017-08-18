package cn.intret.app.picgo.ui.event;


public class ImageFragmentSelectionChangeMessage {
    int currentCode;

    String transitionName;

    public String getTransitionName() {
        return transitionName;
    }

    public ImageFragmentSelectionChangeMessage setTransitionName(String transitionName) {
        this.transitionName = transitionName;
        return this;
    }

    public int getCurrentCode() {
        return currentCode;
    }

    public ImageFragmentSelectionChangeMessage setCurrentCode(int currentCode) {
        this.currentCode = currentCode;
        return this;
    }
}
