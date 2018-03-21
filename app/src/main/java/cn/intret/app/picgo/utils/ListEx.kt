// [idioms - Idiomatic way of handling nullable or empty List in Kotlin -
// Stack Overflow]
// (https://stackoverflow.com/questions/26341225/idiomatic-way-of-handling-nullable-or-empty-list-in-kotlin)

package cn.intret.app.picgo.utils

inline fun <E: Any, T: Collection<E>> T?.withNotNullNorEmpty(func: T.() -> Unit): T? {
    if (this != null && this.isNotEmpty()) {
        with (this) { func() }
    }
    return this
}

inline fun  <E: Any, T: Collection<E>, R: Any> T?.whenNotNullNorEmpty(func: (T) -> R?): R? {
    if (this != null && this.isNotEmpty()) {
        return func(this)
    }
    return null
}

inline fun <E: Any, T: Collection<E>> T?.withNullOrEmpty(func: () -> Unit): T? {
    if (this == null || this.isEmpty()) {
        func()
    }
    return this
}

inline fun <E: Any, T: Collection<E>, R: Any> T?.whenNullOrEmpty(func: () -> R?): R?  {
    if (this == null || this.isEmpty()) {
        return func()
    }
    return null
}

