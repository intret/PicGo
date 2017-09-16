package cn.intret.app.picgo.ui.adapter;

import java.io.File;

public interface BaseFileItem extends ItemSelectable {

    File getFile();

    void setFile(File file);
}
