package cn.intret.app.picgo.model.image

/**
 * Created by intret on 2018/3/21.
 */
interface T9Filterable {
    fun match(keywords: String): Boolean
}