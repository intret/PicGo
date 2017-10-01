package cn.intret.app.picgo.model.image;

import android.util.Pair;

import java.io.File;
import java.util.List;


public class MoveFileDetectResult {

    private File mTargetDir;
    private List<Pair<File, File>> mCanMoveFiles;
    private List<Pair<File, File>> mConflictFiles;

    public File getTargetDir() {
        return mTargetDir;
    }

    public MoveFileDetectResult setTargetDir(File targetDir) {
        mTargetDir = targetDir;
        return this;
    }

    public MoveFileDetectResult setCanMoveFiles(List<Pair<File, File>> canMoveFiles) {
        mCanMoveFiles = canMoveFiles;
        return this;
    }

    public List<Pair<File, File>> getCanMoveFiles() {
        return mCanMoveFiles;
    }

    public MoveFileDetectResult setConflictFiles(List<Pair<File, File>> conflictFiles) {
        mConflictFiles = conflictFiles;
        return this;
    }

    public List<Pair<File, File>> getConflictFiles() {
        return mConflictFiles;
    }
}
