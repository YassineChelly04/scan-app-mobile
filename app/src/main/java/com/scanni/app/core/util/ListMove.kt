package com.scanni.app.core.util

/** Returns a copy of the list with the element at [from] moved to [to]. */
fun <T> List<T>.move(from: Int, to: Int): List<T> {
    if (from == to || from !in indices || to !in indices) return this
    val mutable = toMutableList()
    val element = mutable.removeAt(from)
    mutable.add(to, element)
    return mutable
}
