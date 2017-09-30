package cn.intret.app.picgo.model.event;


import java.util.List;

import cn.intret.app.picgo.model.CompareItemResolveResult;

public class ConflictResolveResultMessage {
    List<CompareItemResolveResult> compareItems;

    public List<CompareItemResolveResult> getCompareItems() {
        return compareItems;
    }

    public ConflictResolveResultMessage setCompareItems(List<CompareItemResolveResult> compareItems) {
        this.compareItems = compareItems;
        return this;
    }
}
