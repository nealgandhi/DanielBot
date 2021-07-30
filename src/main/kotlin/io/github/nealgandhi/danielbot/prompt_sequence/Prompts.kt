@file:OptIn(KordPreview::class)

package io.github.nealgandhi.danielbot.prompt_sequence

import com.kotlindiscord.kord.extensions.commands.slash.SlashCommandContext
import com.kotlindiscord.kord.extensions.utils.waitFor
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.entity.interaction.PublicFollowupMessage
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.live.channel.live
import io.github.nealgandhi.danielbot.util.implies
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class ConfirmationHandler<V>(val channel: ReceiveChannel<V>) {
    suspend inline fun <R> repeatUntilSelection(crossinline step: PromptStep<R>, crossinline onEach: suspend (R) -> Unit): V = coroutineScope {
        val repeatJob = launch {
            while (true) {
                onEach(step())
            }
        }
        val result = channel.receive()
        repeatJob.cancel()
        result
    }
}

data class ConfirmationButton(
    val label: String,
    val style: ButtonStyle,
    val body: (suspend () -> Unit)? = null,
)

typealias ConfirmationButtons<T> = Map<ConfirmationButton, T>

class Prompts(private val ownerId: Snowflake) {
    private val toDelete = mutableListOf<MessageBehavior>()

    suspend fun deleteMessages() = coroutineScope {
        toDelete.map {
            async { it.delete() }
        }.awaitAll()
    }

    suspend fun SlashCommandContext<*>.message(content: String): PublicFollowupMessage =
        publicFollowUp { this.content = content }.apply { toDelete.add(message) }

    suspend fun SlashCommandContext<*>.expectMessage(timeout: Long? = null, replyTo: Snowflake? = null): MessageCreateEvent? =
        channel.live()
            .waitFor<MessageCreateEvent>(timeout) { message.author?.id == ownerId && (replyTo != null) implies (message.referencedMessage?.id == replyTo) }
            ?.apply { toDelete.add(message) }

    suspend fun SlashCommandContext<*>.expectReaction(messageId: Snowflake, timeout: Long? = null): ReactionAddEvent? =
        channel.live()
            .waitFor(timeout) { userId == ownerId && this.messageId == messageId }

    suspend fun SlashCommandContext<*>.booleanConfirmation(
        content: String,
        trueLabel: String,
        falseLabel: String,
    ): Pair<ConfirmationHandler<Boolean>, PublicFollowupMessage> =
        confirmation(
            content,
            mapOf(
                ConfirmationButton(trueLabel, ButtonStyle.Success) to true,
                ConfirmationButton(falseLabel, ButtonStyle.Danger) to false,
            )
        )

    suspend fun <T> SlashCommandContext<*>.confirmation(
        content: String,
        buttons: ConfirmationButtons<T>,
    ): Pair<ConfirmationHandler<T>, PublicFollowupMessage> = run {
        val channel = Channel<T>()

        val message = publicFollowUp {
            this.content = content

            components {
                for ((button, result) in buttons) {
                    interactiveButton {
                        style = button.style
                        label = button.label
                        deferredAck = true

                        booleanCheck { it.interaction.user.id == ownerId }

                        action {
                            button.body?.invoke()
                            channel.send(result)
                        }
                    }
                }
            }
        }.apply { toDelete.add(message) }

        Pair(ConfirmationHandler(channel), message)
    }
}

suspend inline fun <T> prompts(ownerId: Snowflake, block: Prompts.() -> T): T {
    val prompts = Prompts(ownerId)

    val result = prompts.block()

    prompts.deleteMessages()

    return result
}
