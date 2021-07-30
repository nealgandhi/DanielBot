@file:OptIn(KordPreview::class)

package io.github.nealgandhi.danielbot.prompt_sequence

import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.addReaction
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.entity.ReactionEmoji
import dev.kord.rest.builder.message.create.embed

private data class SimpleData(
    val name: String,
    val favoriteEmoji: ReactionEmoji,
    val classes: List<String>,
    val otherEmoji: List<ReactionEmoji>,
    val emojiOpinion: String,
)

class PromptsTestExtension : Extension() {
    override val name: String
        get() = "Prompt Sequence Test"

    override suspend fun setup() {
        slashCommand {
            name = "test-simple"
            description = "Run a simple prompt sequence."
            autoAck = AutoAckType.PUBLIC

            action {
                val data = prompts(user.id) {
                    val namePrompt = message("What's your name?")

                    val nameEvent = expectMessage(timeout = 10_000, replyTo = namePrompt.id)
                    if (nameEvent == null) {
                        publicFollowUp { content = "Took too long." }
                        return@action
                    }
                    val name = nameEvent.message.content
                    nameEvent.message.addReaction("✅")

                    val favoriteEmojiPrompt = message("React with your favorite emoji")

                    val emojiEvent = expectReaction(favoriteEmojiPrompt.id, timeout = 10_000)
                    if (emojiEvent == null) {
                        publicFollowUp { content = "Took too long." }
                        return@action
                    }
                    val favoriteEmoji = emojiEvent.emoji

                    val classes = mutableListOf<String>()
                    val (classesHandler, _) = booleanConfirmation("What classes are you taking?", "Confirm", "Cancel")
                    val classesResult = classesHandler.repeatUntilSelection({ expectMessage() }) {
                        classes.add(it!!.message.content)
                    }

                    if (classesResult) {
                        message("You confirmed.")
                    } else {
                        publicFollowUp { content = "You cancelled." }
                        return@action
                    }

                    val emojiButtons: ConfirmationButtons<Boolean?> = linkedMapOf(
                        ConfirmationButton("I like these", ButtonStyle.Success) to true,
                        ConfirmationButton("I hate these", ButtonStyle.Success) to false,
                        ConfirmationButton("I don't want to", ButtonStyle.Danger) to null,
                    )
                    val otherEmoji = mutableListOf<ReactionEmoji>()
                    val (emojiHandler, emojiPrompt) = confirmation("React with a bunch of emoji", emojiButtons)
                    val emojiResult = emojiHandler.repeatUntilSelection({ expectReaction(emojiPrompt.id) }) {
                        otherEmoji.add(it!!.emoji)
                    }

                    if (emojiResult == null) {
                        publicFollowUp { content = "Ok :(" }
                        return@action
                    }
                    val emojiOpinion = if (emojiResult) "like" else "hate"

                    SimpleData(name, favoriteEmoji, classes, otherEmoji, emojiOpinion)
                }

                channel.createMessage("All done! Your name is ${data.name}, favorite emoji is ${data.favoriteEmoji}, classes are ${data.classes}, emoji you ${data.emojiOpinion} are ${data.otherEmoji}.")
            }
        }

        slashCommand {
            name = "test-combinators"
            description = "Run a prompt sequence using combinators."
            autoAck = AutoAckType.PUBLIC

            action {
                val pairs = prompts(user.id) {
                    val (handler, emojiPrompt) = booleanConfirmation("React with your favorite emojis.", "Confirm", "Cancel")

                    val step = promptStep {
                        val reaction = expectReaction(emojiPrompt.id)
                        val namePrompt = message("reply with your name")
                        val name = expectMessage(replyTo = namePrompt.id)
                        Pair(reaction, name)
                    }

                    val pairs = mutableListOf<Pair<String, ReactionEmoji>>()
                    val confirmed = handler.repeatUntilSelection(step) { (emojiEvent, nameEvent) ->
                        pairs.add(Pair(nameEvent!!.message.content, emojiEvent!!.emoji))
                    }

                    if (!confirmed) {
                        publicFollowUp { content = "Cancelled." }
                        return@action
                    }

                    val orPrompt = message("React or reply!")
                    val isReply = getFirst(
                        {
                            expectMessage(replyTo = orPrompt.message.id)
                            true
                        },
                        {
                            expectReaction(orPrompt.message.id)
                            false
                        }
                    )

                    if (isReply) {
                        publicFollowUp { content = "You replied." }
                    } else {
                        publicFollowUp { content = "You reacted." }
                    }

                    pairs
                }

                publicFollowUp {
                    embed {
                        pairs.forEach { (name, emoji) ->
                            field(name = name) { emoji.mention }
                        }
                    }
                }
            }
        }
    }
}
