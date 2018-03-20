package cn.intret.app.picgo.model.image


import java.io.File

/**
 * 比较项。在文件移动冲突比较中每个一个比较的项目，这个比较会有一个结果决定选择保留左边还是右边。
 */
class CompareItem {
    internal lateinit var result: ResolveResult
    internal lateinit var targetFile: File
    internal lateinit var sourceFile: File

    constructor(result: ResolveResult, targetFile: File, sourceFile: File) {
        this.result = result
        this.targetFile = targetFile
        this.sourceFile = sourceFile
    }


    override fun toString(): String {
        return "CompareItem{" +
                "result=" + result +
                ", targetFile=" + targetFile +
                ", sourceFile=" + sourceFile +
                '}'.toString()
    }
}
