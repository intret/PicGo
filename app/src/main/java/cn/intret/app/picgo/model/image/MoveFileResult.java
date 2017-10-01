package cn.intret.app.picgo.model.image;

import android.util.Pair;

import java.io.File;
import java.util.LinkedList;
import java.util.List;


public class MoveFileResult {
    List<Pair<File, File>> mConflictFiles = new LinkedList<>();

    List<Pair<File, File>> mSuccessFiles = new LinkedList<>();

    List<Pair<File, File>> mFailedFiles = new LinkedList<>();

    public List<Pair<File, File>> getFailedFiles() {
        return mFailedFiles;
    }

    public MoveFileResult setFailedFiles(List<Pair<File, File>> failedFiles) {
        mFailedFiles = failedFiles;
        return this;
    }

    public List<Pair<File, File>> getSuccessFiles() {
        return mSuccessFiles;
    }

    public MoveFileResult setSuccessFiles(List<Pair<File, File>> successFiles) {
        mSuccessFiles = successFiles;
        return this;
    }

    public List<Pair<File, File>> getConflictFiles() {
        return mConflictFiles;
    }

    public MoveFileResult setConflictFiles(List<Pair<File, File>> conflictFiles) {
        mConflictFiles = conflictFiles;
        return this;
    }
}
