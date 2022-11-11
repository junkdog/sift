package sift.core.jackson

import com.fasterxml.jackson.core.JsonParser

inline fun <reified T> JsonParser.readValueAs(): T {
    return readValueAs(T::class.java)
}