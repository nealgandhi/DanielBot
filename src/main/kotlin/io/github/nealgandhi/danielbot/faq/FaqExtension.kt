@file:OptIn(KordPreview::class)

package io.github.nealgandhi.danielbot.faq

import com.kotlindiscord.kord.extensions.commands.converters.impl.*
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommandContext
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.pagination.pages.Page
import dev.kord.common.annotation.KordPreview
import io.github.nealgandhi.danielbot.MessageLink
import io.github.nealgandhi.danielbot.isDm
import org.koin.core.component.inject
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class FaqExtension : Extension() {
    override val name: String get() = "faq"

    private val faqService: FaqService by inject()

    sealed class AddArgs : Arguments() {
        val question by string("question", "The frequently asked question")
        val answer by string("answer", "The answer to the question")
        val originalQuestionLink by optionalString("original-question-link", "Link to the message where the question was asked")
    }

    class AddChannelQuestionArgs : AddArgs() {
        val channel by optionalChannel("channel", "The channel the question is for. Uses the current channel by default.")
    }

    class AddGuildQuestionArgs : AddArgs() {
        val guild by optionalGuild("guild", "The guild the question is for. Uses the current guild by default.")
    }

    class ListChannelArgs : Arguments() {
        val channel by optionalChannel("channel", "The channel to list FAQs for. Uses the current channel by default.")
    }

    class ListGuildArgs : Arguments() {
        val guild by optionalGuild("guild", "The guild to list FAQs for. Uses the current guild by default.")
    }

    @OptIn(ExperimentalContracts::class)
    suspend inline fun <T: Arguments> SlashCommandContext<T>.isValid(location: QuestionLocation?, wantedLocationType: String): Boolean {
        contract {
            returns(true) implies(location != null)
        }
        return if (location == null) {
            ack(true)
            ephemeralFollowUp {
                content = "You must supply a $wantedLocationType when using this command in DMs."
            }
            false
        } else {
            true
        }
    }

    override suspend fun setup() {
        slashCommand {
            name = "faq"
            description = "FAQ-related commands"

            this.group("add") {
                description = "Add a question to the list of frequently-asked questions."

                suspend fun <T : AddArgs> SlashCommandContext<T>.addQuestion(location: QuestionLocation?, wantedLocationType: String) {
                    if (!isValid(location, wantedLocationType)) {
                        return
                    }

                    val link = arguments.originalQuestionLink?.let(MessageLink::fromText)
                    if (arguments.originalQuestionLink != null && link == null) {
                        ack(true)
                        ephemeralFollowUp {
                            content = "`${arguments.originalQuestionLink}` is not a valid message link."
                        }
                        return
                    }

                    val successful =
                        faqService.addQuestion(location, arguments.question, arguments.answer, link)
                    ack(true)
                    ephemeralFollowUp {
                        content = if (successful) {
                            "Added question successfully."
                        } else {
                            "Failed to add question."
                        }
                    }
                }

                subCommand(::AddChannelQuestionArgs) {
                    name = "channel-specific"
                    description = "Add a channel-specific question."
                    autoAck = AutoAckType.NONE

                    action {
                        val channel = arguments.channel ?: channel
                        addQuestion(ChannelSpecificQuestion(channel.id).takeUnless { channel.type.isDm }, "channel")
                    }
                }

                subCommand(::AddGuildQuestionArgs) {
                    name = "guild-wide"
                    description = "Add a guild-wide question."
                    autoAck = AutoAckType.NONE

                    action {
                        val guild = arguments.guild ?: guild
                        addQuestion(guild?.id?.let(::GuildWideQuestion), "guild")
                    }
                }
            }

            group("list") {
                description = "List frequently asked questions"

                suspend fun <T : Arguments> SlashCommandContext<T>.listFaqs(location: QuestionLocation?, wantedLocationType: String, locationMention: String?, footer: String?) {
                    if (isValid(location, wantedLocationType)) {
                        val entries = faqService.getAllEntries(location).iterator()

                        if (!entries.hasNext()) {
                            ack(true)
                            ephemeralFollowUp {
                                content = "There aren't any FAQs for $locationMention yet"
                            }
                        } else {
                            val owner = user.asUser()

                            ack(false)
                            paginator {
                                keepEmbed = false
                                this.owner = owner
                                entries.forEach { entry ->
                                    page(
                                        Page(
                                            title = entry.question,
                                            description = entry.answer,
                                            url = entry.originalQuestionLink?.asText,
                                            footer = footer,
                                        )
                                    )
                                }
                            }.send()
                        }
                    }
                }

                subCommand(::ListChannelArgs) {
                    name = "channel-specific"
                    description = "List channel-specific frequently asked questions"
                    autoAck = AutoAckType.NONE

                    action {
                        val channel = arguments.channel ?: channel
                        listFaqs(
                            ChannelSpecificQuestion(channel.id).takeUnless { channel.type.isDm },
                            "channel",
                            channel.mention,
                            "#${channel.data.name.value}",
                        )
                    }
                }

                subCommand(::ListGuildArgs) {
                    name = "guild-wide"
                    description = "List guild-wide frequently asked questions"
                    autoAck = AutoAckType.NONE

                    action {
                        val guild = arguments.guild ?: guild
                        listFaqs(
                            guild?.id?.let(::GuildWideQuestion),
                            "guild",
                            guild?.data?.name,
                            guild?.data?.name,
                        )
                    }
                }
            }
        }
    }
}
