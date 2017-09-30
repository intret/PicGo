package cn.intret.app.picgo.model;


import java.io.File;

/**
 * 比较项。在文件移动冲突比较中每个一个比较的项目，这个比较会有一个结果决定选择保留左边还是右边。
 */
public class CompareItem {
    ResolveResult mResult;
    File mTargetFile;
    File mSourceFile;

    public ResolveResult getResult() {
        return mResult;
    }

    public CompareItem setResult(ResolveResult result) {
        mResult = result;
        return this;
    }

    public File getTargetFile() {
        return mTargetFile;
    }

    public CompareItem setTargetFile(File targetFile) {
        mTargetFile = targetFile;
        return this;
    }

    public File getSourceFile() {
        return mSourceFile;
    }

    public CompareItem setSourceFile(File sourceFile) {
        mSourceFile = sourceFile;
        return this;
    }

    @Override
    public String toString() {
        return "CompareItem{" +
                "mResult=" + mResult +
                ", mTargetFile=" + mTargetFile +
                ", mSourceFile=" + mSourceFile +
                '}';
    }
}
