package cn.intret.app.picgo.model;


public class CompareItemResolveResult {

    CompareItem mCompareItem;
    /**
     * true, 表示已经按照 {@link #getCompareItem() } 标识的方法解决了冲突。
     */
    boolean resolved;


    public CompareItem getCompareItem() {
        return mCompareItem;
    }

    public CompareItemResolveResult setCompareItem(CompareItem compareItem) {
        mCompareItem = compareItem;
        return this;
    }

    public boolean isResolved() {
        return resolved;
    }

    public CompareItemResolveResult setResolved(boolean resolved) {
        this.resolved = resolved;
        return this;
    }

    @Override
    public String toString() {
        return "CompareItemResolveResult{" +
                "mCompareItem=" + mCompareItem +
                ", resolved=" + resolved +
                '}';
    }
}
