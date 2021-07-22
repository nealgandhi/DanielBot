@file:OptIn(KordPreview::class)

package io.github.nealgandhi.danielbot.faq

import com.kotlindiscord.kord.extensions.commands.slash.SlashCommandContext
import com.kotlindiscord.kord.extensions.pagination.pages.Page
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.ChannelType
import dev.kord.core.entity.Entity
import dev.kord.core.entity.channel.*
import io.github.nealgandhi.danielbot.MessageLink
import dev.kord.core.entity.Guild as KordGuild
import dev.kord.core.entity.channel.Channel as KordChannel

sealed class FaqContext<L : Entity> {
    protected abstract fun formatForFooter(rawLocation: L): String
    protected abstract fun formatForMention(rawLocation: L): String

    protected abstract suspend fun getRawLocation(context: SlashCommandContext<out LocationSelector<L>>): L?
    protected abstract fun wrapLocation(rawLocation: L): QuestionLocation

    suspend fun performAdd(context: SlashCommandContext<out AddArgs<L>>, faqService: FaqService): Unit = with(context) {
        val rawLocation = getRawLocation(context) ?: return
        ack(true)

        val link = arguments.originalQuestionLink?.let(MessageLink::fromText)
        if (arguments.originalQuestionLink != null && link == null) {
            ephemeralFollowUp {
                content = "`${arguments.originalQuestionLink}` is not a valid message link."
            }
            return
        }

        val successful = faqService.addQuestion(wrapLocation(rawLocation), interaction.id, arguments.question, arguments.answer, link)
        ephemeralFollowUp {
            content = if (successful) "Added question successfully." else "Failed to add question."
        }
    }

    suspend fun performList(context: SlashCommandContext<out ListArgs<L>>, faqService: FaqService) = with(context) {
        val rawLocation = getRawLocation(context) ?: return

        val entries = faqService.getAllEntries(wrapLocation(rawLocation)).iterator()
        if (!entries.hasNext()) {
            ack(true)
            ephemeralFollowUp {
                content = "There aren't any FAQs for ${formatForMention(rawLocation)} yet."
            }
            return
        }
        ack(false)

        val owner = user.asUser()

        paginator {
            this.owner = owner
            keepEmbed = false

            entries.forEach { entry ->
                page(
                    Page(
                        title = entry.question,
                        description = entry.answer,
                        url = entry.originalQuestionLink?.asText,
                        footer = formatForFooter(rawLocation)
                    )
                )
            }
        }.send()
    }

    object Channel : FaqContext<KordChannel>() {
        override fun formatForFooter(rawLocation: KordChannel): String = "#" + (rawLocation as GuildChannel).name
        override fun formatForMention(rawLocation: KordChannel): String = rawLocation.mention

        override suspend fun getRawLocation(context: SlashCommandContext<out LocationSelector<KordChannel>>): KordChannel? = with(context) {
            val givenChannel = arguments.location

            if (givenChannel != null && givenChannel.type != ChannelType.GuildText) {
                ack(true)
                ephemeralFollowUp {
                    content = "You can only specify text channels."
                }
                return null
            }

            val defaultChannel = channel

            if (givenChannel == null && channel is DmChannel) {
                ack(true)
                ephemeralFollowUp {
                    content = "You must specify a channel when using this command in DMs."
                }
                return null
            }

            return givenChannel ?: defaultChannel
        }

        override fun wrapLocation(rawLocation: KordChannel): QuestionLocation = QuestionLocation.channel(rawLocation.id)
    }

    object Category : FaqContext<KordChannel>() {
        override fun formatForFooter(rawLocation: KordChannel): String = (rawLocation as dev.kord.core.entity.channel.Category).name
        override fun formatForMention(rawLocation: KordChannel): String = formatForFooter(rawLocation)

        override suspend fun getRawLocation(context: SlashCommandContext<out LocationSelector<KordChannel>>): KordChannel? = with(context) {
            val givenCategory = arguments.location

            if (givenCategory !is dev.kord.core.entity.channel.Category?) {
                ack(true)
                ephemeralFollowUp {
                    content = "You can only specify categories."
                }
                return null
            }

            if (givenCategory == null && channel is DmChannel) {
                ack(true)
                ephemeralFollowUp {
                    content = "You must specify a category when using this command in DMs."
                }
                return null
            }

            if (givenCategory == null) {
                val defaultCategory = (channel as CategorizableChannel).category

                if (defaultCategory == null) {
                    ack(true)
                    ephemeralFollowUp {
                        content = "You must provide a category when using this command in DMs."
                    }
                    return null
                }

                return defaultCategory.asChannel()
            }
            return givenCategory
        }

        override fun wrapLocation(rawLocation: KordChannel): QuestionLocation = QuestionLocation.category(rawLocation.id)
    }

    object Guild : FaqContext<KordGuild>() {
        override fun formatForFooter(rawLocation: KordGuild): String = rawLocation.name
        override fun formatForMention(rawLocation: KordGuild): String = formatForFooter(rawLocation)

        override suspend fun getRawLocation(context: SlashCommandContext<out LocationSelector<KordGuild>>): KordGuild? = with(context) {
            val givenGuild = arguments.location
            val defaultGuild = guild

            if (givenGuild == null && channel is DmChannel) {
                ack(true)
                ephemeralFollowUp {
                    content = "You must specify a guild when using this command in DMs."
                }
                return null
            }

            return givenGuild ?: defaultGuild ?: throw Exception("Something is very wrong. Not in DMs but guild is null.")
        }

        override fun wrapLocation(rawLocation: KordGuild): QuestionLocation = QuestionLocation.guild(rawLocation.id)
    }
}