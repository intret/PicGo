package cn.intret.app.picgo.ui.adapter;

import java.io.File;

public abstract class FileAble implements ItemSelectable {
    abstract File getFile();

    abstract void setFile(File file);
}
