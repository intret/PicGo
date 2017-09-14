package cn.intret.app.picgo.model;


import com.t9search.model.PinyinSearchUnit;
import com.t9search.util.PinyinUtil;

import java.io.File;
import java.util.LinkedList;
import java.util.List;


/**
 * 图片文件夹数据模型。
 */
public class ImageFolder implements Cloneable {
    /**
     * Folder file
     */
    File mFile;
    /*
     * File count in folder
     */
    int mCount;
    /**
     * Folder name
     */
    private String mName;
    List<File> mThumbList;
    File[] mMediaFiles;

    /*
     * T9 键盘过滤信息
     */
    PinyinSearchUnit mPinyinSearchUnit;
    private StringBuffer mMatchKeywords = new StringBuffer();        //Used to save the type of Match Keywords.(name or phoneNumber)
    private int mMatchStartIndex;                //the match start  position of mMatchKeywords in original string(name or phoneNumber).
    private int mMatchLength;                    //the match length of mMatchKeywords in original string(name or phoneNumber).

    public ImageFolder() {

    }


    public PinyinSearchUnit getPinyinSearchUnit() {
        return mPinyinSearchUnit;
    }

    public ImageFolder setPinyinSearchUnit(PinyinSearchUnit pinyinSearchUnit) {
        mPinyinSearchUnit = pinyinSearchUnit;
        return this;
    }

    public File[] getMediaFiles() {
        return mMediaFiles;
    }

    public ImageFolder setMediaFiles(File[] mediaFiles) {
        mMediaFiles = mediaFiles;
        return this;
    }

    public List<File> getThumbList() {
        return mThumbList;
    }

    public ImageFolder setThumbList(List<File> thumbList) {
        this.mThumbList = thumbList;
        return this;
    }

    public File getFile() {
        return mFile;
    }

    public ImageFolder setFile(File file) {
        mFile = file;
        return this;
    }

    public int getCount() {
        return mCount;
    }

    public ImageFolder setCount(int count) {
        mCount = count;
        return this;
    }

    public ImageFolder setName(String name) {
        mName = name;

        // Generate Pinyin search data
        setPinyinSearchUnit(new PinyinSearchUnit(name));
        PinyinUtil.parse(mPinyinSearchUnit);

        mMatchKeywords = new StringBuffer();
        mMatchKeywords.delete(0, mMatchKeywords.length());
        setMatchStartIndex(-1);
        setMatchLength(0);

        return this;
    }

    public String getName() {
        return mName;
    }

    public StringBuffer getMatchKeywords() {
        return mMatchKeywords;
    }

    public void setMatchKeywords(String matchKeywords) {
        if (mMatchKeywords != null) {
            mMatchKeywords.delete(0, mMatchKeywords.length());
            mMatchKeywords.append(matchKeywords);
        }
    }

    public void clearMatchKeywords() {
        mMatchKeywords.delete(0, mMatchKeywords.length());
    }

    public int getMatchStartIndex() {
        return mMatchStartIndex;
    }

    public ImageFolder setMatchStartIndex(int matchStartIndex) {
        mMatchStartIndex = matchStartIndex;
        return this;
    }

    public int getMatchLength() {
        return mMatchLength;
    }

    public ImageFolder setMatchLength(int matchLength) {
        mMatchLength = matchLength;
        return this;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        ImageFolder clone = (ImageFolder) super.clone();
        clone.mMatchKeywords = new StringBuffer(this.mMatchKeywords.toString());
        clone.setPinyinSearchUnit((PinyinSearchUnit) mPinyinSearchUnit.clone());
        clone.setFile(new File(mFile.getAbsolutePath()));
        clone.setName(mName);


        if (mThumbList != null) {
            clone.mThumbList = new LinkedList<>();
            for (int i = 0, mThumbListSize = mThumbList.size(); i < mThumbListSize; i++) {
                File file = mThumbList.get(i);
                clone.mThumbList.add(new File(file.getAbsolutePath()));
            }
        }
        return clone;
    }
}
