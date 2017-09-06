package cn.intret.app.picgo.ui.main;

import java.io.File;

import cn.intret.app.picgo.model.MoveFileResult;

class MoveFileResultMessage {
    private MoveFileResult mResult;
    private File mDestDir;

    public MoveFileResultMessage setResult(MoveFileResult result) {
        mResult = result;
        return this;
    }

    public MoveFileResult getResult() {
        return mResult;
    }

    public MoveFileResultMessage setDestDir(File destDir) {
        mDestDir = destDir;
        return this;
    }

    public File getDestDir() {
        return mDestDir;
    }
}
