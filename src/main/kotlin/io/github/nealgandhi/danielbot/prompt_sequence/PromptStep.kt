package io.github.nealgandhi.danielbot.prompt_sequence

import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select

typealias PromptStep<T> = suspend () -> T

inline fun <T> promptStep(crossinline step: PromptStep<T>): PromptStep<T> = { step() }

suspend inline fun <A> getFirst(crossinline a: PromptStep<A>, crossinline b: PromptStep<A>): A = coroutineScope {
    select<A> {
        async { a() }.onAwait { it }
        async { b() }.onAwait { it }
    }.also { coroutineContext.cancelChildren() }
}
