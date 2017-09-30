package cn.intret.app.picgo.model.event;


import java.io.File;

/**
 * A folder has been delete.
 */
public class DeleteFolderMessage extends BaseMessage {
    File mDir;


    public DeleteFolderMessage(String desc, File dir) {
        super(desc);
        mDir = dir;
    }

    public DeleteFolderMessage(File dir) {
        mDir = dir;
    }

    public File getDir() {
        return mDir;
    }

    public DeleteFolderMessage setDir(File dir) {
        mDir = dir;
        return this;
    }

    @Override
    public String toString() {
        return "DeleteFolderMessage{" +
                "mDir=" + mDir +
                "} " + super.toString();
    }
}
