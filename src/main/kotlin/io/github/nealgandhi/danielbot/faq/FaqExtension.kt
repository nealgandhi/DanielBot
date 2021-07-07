@file:OptIn(KordPreview::class)

package io.github.nealgandhi.danielbot.faq

import com.kotlindiscord.kord.extensions.commands.converters.OptionalConverter
import com.kotlindiscord.kord.extensions.commands.converters.impl.*
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommandContext
import com.kotlindiscord.kord.extensions.commands.slash.SlashGroup
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.pagination.pages.Page
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Entity
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.CategorizableChannel
import dev.kord.core.entity.channel.Channel
import dev.kord.core.entity.channel.GuildChannel
import io.github.nealgandhi.danielbot.MessageLink
import org.koin.core.component.inject

class FaqExtension : Extension() {
    override val name: String get() = "faq"

    private val faqService: FaqService by inject()

    sealed class AddArgs<L: Entity>(type: String, selector: Arguments.(String, String) -> OptionalConverter<L?>) : Arguments() {
        val question by string("question", "The frequently asked question")
        val answer by string("answer", "The answer to the question")
        val originalQuestionLink by optionalString("original-question-link", "Link to the message where the question was asked")
        val location by selector(type, "The $type the question is for. Uses the current $type by default.")
    }

    class AddChannelQuestionArgs : AddArgs<Channel>("channel", Arguments::optionalChannel)
    class AddCategoryQuestionArgs : AddArgs<Channel>("category", Arguments::optionalChannel)
    class AddGuildQuestionArgs : AddArgs<Guild>("guild", Arguments::optionalGuild)

    sealed class ListArgs<L: Entity>(type: String, selector: Arguments.(String, String) -> OptionalConverter<L?>) : Arguments() {
        val location by selector(type, "The $type to list FAQs for. Uses the current $type by default.")
    }

    class ListChannelFaqsArgs : ListArgs<Channel>("channel", Arguments::optionalChannel)
    class ListCategoryFaqsArgs : ListArgs<Channel>("category", Arguments::optionalChannel)
    class ListGuildFaqsArgs : ListArgs<Guild>("guild", Arguments::optionalGuild)

    private suspend inline fun <L: Entity, A: AddArgs<L>> SlashGroup.registerAddCommand(
        name: String,
        wantedLocationType: String,
        noinline args: () -> A,
        crossinline defaultLocation: SlashCommandContext<out A>.() -> Snowflake?,
        crossinline locationWrapper: Snowflake.() -> QuestionLocation,
        crossinline validCondition: SlashCommandContext<out A>.(Snowflake?) -> Boolean = { true },
    ) {
        subCommand(args) {
            this.name = name
            description = "Add a $name question."
            autoAck = AutoAckType.EPHEMERAL

            action {
                val location = (arguments.location?.id ?: defaultLocation())?.takeIf { validCondition(it) }?.locationWrapper()

                if (location == null) {
                    ephemeralFollowUp {
                        content = "You must supply a $wantedLocationType if using this command in DMs."
                    }
                    return@action
                }

                val link = arguments.originalQuestionLink?.let(MessageLink::fromText)
                if (arguments.originalQuestionLink != null && link == null) {
                    ephemeralFollowUp {
                        content = "`${arguments.originalQuestionLink}` is not a valid message link."
                    }
                    return@action
                }

                val successful = faqService.addQuestion(location, interaction.id, arguments.question, arguments.answer, link)
                ephemeralFollowUp {
                    content = if (successful) {
                        "Added question successfully."
                    } else {
                        "Failed to add question. Please contact a moderator."
                    }
                }
            }
        }
    }

    private suspend inline fun <L: Entity, A: ListArgs<L>> SlashGroup.registerListCommand(
        name: String,
        wantedLocationType: String,
        noinline args: () -> A,
        crossinline defaultLocation: suspend SlashCommandContext<out A>.() -> L?,
        noinline locationWrapper: Snowflake.() -> QuestionLocation,
        crossinline getLocationMention: (L) -> String?,
        crossinline getFooter: (L) -> String?,
        crossinline validCondition: SlashCommandContext<out A>.(L?) -> Boolean = { true },
    ) {
        subCommand(args) {
            this.name = name
            description = "List $name frequently asked questions."
            autoAck = AutoAckType.NONE

            action {
                val rawLocation = (arguments.location ?: defaultLocation())?.takeIf { validCondition(it) }
                val location = rawLocation?.id?.locationWrapper()

                if (location == null) {
                    ack(true)
                    ephemeralFollowUp {
                        content = "You must supply a $wantedLocationType when using this command in DMs."
                    }
                    return@action
                }

                val entries = faqService.getAllEntries(location).iterator()

                if (!entries.hasNext()) {
                    ack(true)
                    ephemeralFollowUp {
                        content = "There aren't any FAQs for ${getLocationMention(rawLocation)} yet."
                    }
                    return@action
                }

                val owner = user.asUser()

                ack(false)
                paginator {
                    this.owner = owner
                    keepEmbed = false

                    entries.forEach { entry ->
                        page(
                            Page(
                                title = entry.question,
                                url = entry.originalQuestionLink?.asText,
                                description = entry.answer,
                                footer = getFooter(rawLocation),
                            ),
                        )
                    }
                }.send()
            }
        }
    }

    override suspend fun setup() {
        slashCommand {
            name = "faq"
            description = "FAQ-related commands"

            this.group("add") {
                description = "Add a question to the list of frequently asked questions."

                registerAddCommand(
                    name =               "channel-specific",
                    wantedLocationType = "channel",
                    args =               ::AddChannelQuestionArgs,
                    defaultLocation =    { channel.id },
                    locationWrapper =    QuestionLocation::channel,
                    validCondition =     { channel.type == ChannelType.GuildText },
                )
                registerAddCommand(
                    name =               "category-wide",
                    wantedLocationType = "category",
                    args =               ::AddCategoryQuestionArgs,
                    defaultLocation =    { channel.data.parentId?.value },
                    locationWrapper =    QuestionLocation::channel,
                )
                registerAddCommand(
                    name =               "guide-wide",
                    wantedLocationType = "guild",
                    args =               ::AddGuildQuestionArgs,
                    defaultLocation =    { guild?.id },
                    locationWrapper =    QuestionLocation::guild,
                )
            }

            group("list") {
                description = "List frequently asked questions."

                registerListCommand(
                    name =               "channel-specific",
                    wantedLocationType = "channel",
                    args =               ::ListChannelFaqsArgs,
                    defaultLocation =    { channel as? GuildChannel },
                    locationWrapper =    QuestionLocation.Companion::channel,
                    getLocationMention = { it.mention },
                    getFooter =          { channel -> channel.data.name.value?.let { "#$it" } },
                    validCondition =     { it?.type == ChannelType.GuildText },
                )
                registerListCommand(
                    name =               "category-wide",
                    wantedLocationType = "category",
                    args =               ::ListCategoryFaqsArgs,
                    defaultLocation =    { (channel as? CategorizableChannel)?.category?.asChannel() },
                    locationWrapper =    QuestionLocation::category,
                    getLocationMention = { it.data.name.value },
                    getFooter =          { it.data.name.value },
                )
                registerListCommand(
                    name =               "guild-wide",
                    wantedLocationType = "guild",
                    args =               ::ListGuildFaqsArgs,
                    defaultLocation =    { guild },
                    locationWrapper =    QuestionLocation::guild,
                    getLocationMention = { it.name },
                    getFooter =          { it.name },
                )
            }
        }
    }
}
