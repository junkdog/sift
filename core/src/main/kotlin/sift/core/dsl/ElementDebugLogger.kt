package sift.core.dsl

import sift.core.api.Action
import sift.core.api.Iter
import sift.core.element.Element

interface ElementDebugLogger<ELEMENT : Element> {
    /**
     * When `--debug` is passed to the CLI, prints [tag] and all elements
     * currently in scope.
     *
     * Note that for most use-cases, `--profile` yields better results
     * without requiring modifying the template.
     **/
    fun log(tag: String)

    /**
     * When `--debug` is passed to the CLI, prints [tag] and the count
     * of elements currently in scope.
     *
     * Note that for most use-cases, `--profile` yields better results
     * without requiring modifying the template.
     **/
    fun logCount(tag: String)

    companion object {
        internal fun <ELEMENT : Element> scopedTo(
            action: Action.Chain<Iter<ELEMENT>>
        ): ElementDebugLogger<ELEMENT> = ElementDebugLoggerImpl(action)
    }
}

private class ElementDebugLoggerImpl<ELEMENT : Element>(
    val action: Action.Chain<Iter<ELEMENT>>
) : ElementDebugLogger<ELEMENT> {
    override fun log(tag: String) {
        action += Action.DebugLog(tag)
    }

    override fun logCount(tag: String) {
        action += Action.DebugLog(tag, format = Action.DebugLog.LogFormat.Count)
    }
}