@file:OptIn(KordPreview::class)

package io.github.nealgandhi.danielbot.role_menus

import com.kotlindiscord.kord.extensions.CommandException
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.converters.impl.channel
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.channel.MessageChannel
import io.github.nealgandhi.danielbot.prompt_sequence.ConfirmationButton
import io.github.nealgandhi.danielbot.prompt_sequence.getFirst
import io.github.nealgandhi.danielbot.prompt_sequence.promptStep
import io.github.nealgandhi.danielbot.prompt_sequence.prompts
import io.github.nealgandhi.danielbot.util.editContent
import kotlinx.coroutines.flow.collect
import org.koin.core.component.inject

private val ACKNOWLEDGED_EMOJI = ReactionEmoji.Unicode("\u2705") // :white_check_mark:

private sealed interface Selection {
    object Cancelled : Selection
    data class Accepted(val allowPing: Boolean) : Selection
}

class RoleMenuExtension : Extension() {
    private val storage: RoleMenuStorage by inject()

    override val name: String
        get() = "Role Menus"

    private class CreateArgs : Arguments() {
        val title by string("title", "Title of the role menu.")
        val channel by channel("channel", "Channel to send the role menu in. Uses configured channel by default.") { _, channel ->
            if (channel !is MessageChannel) {
                throw CommandException("Cannot create a role menu in ${channel.mention} because it isn't a text channel.")
            }
        }
    }

    override suspend fun setup() {
        storage.getAll().collect { it.startListening(this) }

        slashCommand {
            name = "role-menu"
            description = "Role menu related commands."

            requirePermissions(
                Permission.ManageMessages,
                Permission.ManageRoles,
            )

            subCommand(::CreateArgs) {
                name = "create"
                description = "Create a role menu."
                autoAck = AutoAckType.NONE // info: NONE makes sure that validation errors in arguments follow up as ephemeral

                check(hasPermission(Permission.ManageRoles))
                check(anyGuild)

                action {
                    ack(false)

                    val guild = guild!! // ok: command has anyGuild check
                    val menuChannel = arguments.channel as MessageChannel // ok: ensured by validator

                    val menuBuilder = RoleMenuBuilder(interaction.id, arguments.title)

                    val menu = prompts(user.id) {
                        fun initialPromptContent() = """
                            React with an emoji to create a role, or reply to this message to create a role with no emoji.
                            ${menuBuilder.roleCount}/25
                        """.trimIndent()

                        val buttons = linkedMapOf(
                            ConfirmationButton("Cancel", ButtonStyle.Danger) to Selection.Cancelled,
                            ConfirmationButton("Finish (allow pinging)", ButtonStyle.Success) to Selection.Accepted(true),
                            ConfirmationButton("Finish (prevent pinging)", ButtonStyle.Success) to Selection.Accepted(false),
                        )
                        val (handler, initialPrompt) = confirmation(initialPromptContent(), buttons)

                        val timeout = 10_000L
                        val roleWithEmojiStep = promptStep {
                            val reaction = expectReaction(initialPrompt.id)
                            val namePrompt = message("Reply to this message with the name of the role for ${reaction!!.emoji.mention}.")
                            val nameMessage = expectMessage(timeout, replyTo = namePrompt.id)
                            Pair(reaction, nameMessage)
                        }

                        val roleWithoutEmojiStep = promptStep {
                            val nameMessage = expectMessage(replyTo = initialPrompt.id)
                            Pair(null, nameMessage)
                        }

                        val roleStep = promptStep { getFirst(roleWithEmojiStep, roleWithoutEmojiStep) }

                        while (true) {
                            val selection = handler.repeatUntilSelection(roleStep) { (reactionEvent, nameEvent) ->
                                if (nameEvent == null) {
                                    publicFollowUp { content = "Took too long! (must send within ${timeout / 1000} seconds.)" }
                                    return@repeatUntilSelection
                                }

                                reactionEvent?.let { it.message.deleteReaction(it.emoji) }

                                menuBuilder.addRole(reactionEvent?.emoji, nameEvent.message.content)

                                nameEvent.message.addReaction(ACKNOWLEDGED_EMOJI)
                                initialPrompt.editContent(initialPromptContent())
                            }

                            when (selection) {
                                is Selection.Cancelled -> {
                                    publicFollowUp { content = "Cancelled!" }
                                    deleteMessages()
                                    return@action
                                }
                                is Selection.Accepted -> {
                                    if (menuBuilder.roleCount <= 1) {
                                        message("You must create at least two roles.")
                                        continue
                                    }

                                    return@prompts menuBuilder.createRoles(guild, selection.allowPing)
                                }
                            }
                        }

                        @Suppress("UNREACHABLE_CODE", "ThrowableNotThrown") // linter is smarter than kotlinc
                        throw IllegalStateException("Unreachable.")
                    }

                    val (menuData, menuMessage) = menu.sendTo(menuChannel)
                    menuData.startListening(this@RoleMenuExtension)

                    publicFollowUp {
                        content = "Role menu created! Link: https://discord.com/channels/${guild.id.value}/${menuChannel.id.value}/${menuMessage.id.value}"
                    }

                    storage.add(menuData)
                }
            }
        }
    }
}
