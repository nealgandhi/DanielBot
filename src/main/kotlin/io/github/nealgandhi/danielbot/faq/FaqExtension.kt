@file:OptIn(KordPreview::class)

package io.github.nealgandhi.danielbot.faq

import com.kotlindiscord.kord.extensions.commands.converters.OptionalConverter
import com.kotlindiscord.kord.extensions.commands.converters.impl.*
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.ChannelType
import dev.kord.core.entity.Entity
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.CategorizableChannel
import dev.kord.core.entity.channel.Channel
import dev.kord.core.entity.channel.GuildChannel
import org.koin.core.component.inject

abstract class LocationSelector<L: Entity> : Arguments() {
    abstract val location: L?
}

class AddArgs<L: Entity>(type: String, selector: Arguments.(String, String) -> OptionalConverter<L?>) : LocationSelector<L>() {
    val question by string("question", "The frequently asked question")
    val answer by string("answer", "The answer to the question")
    val originalQuestionLink by optionalString("original-question-link", "Link to the message where the question was asked")
    override val location by selector(type, "The $type the question is for. Uses the current $type by default.")
}

class ListArgs<L: Entity>(type: String, selector: Arguments.(String, String) -> OptionalConverter<L?>) : LocationSelector<L>() {
    override val location by selector(type, "The $type to list FAQs for. Uses the current $type by default.")
}

class FaqExtension : Extension() {
    override val name: String get() = "faq"

    private val faqService: FaqService by inject()

    override suspend fun setup() {
        slashCommand {
            name = "faq"
            description = "FAQ-related commands"

            this.group("add") {
                description = "Add a question to the list of frequently asked questions."

                suspend fun <L: Entity> registerAddCommand(name: String, longName: String, selector: Arguments.(String, String) -> OptionalConverter<L?>, context: FaqContext<L>) {
                    subCommand({ AddArgs(name, selector) }) {
                        this.name = name
                        description = "Add a $longName question."
                        autoAck = AutoAckType.NONE

                        action { context.performAdd(this, faqService) }
                    }
                }

                registerAddCommand("channel", "channel-specific", Arguments::optionalChannel, FaqContext.Channel)
                registerAddCommand("category", "category-wide", Arguments::optionalChannel, FaqContext.Category)
                registerAddCommand("guild", "guild-wide", Arguments::optionalGuild, FaqContext.Guild)
            }

            group("list") {
                description = "List frequently asked questions."

                suspend fun <L: Entity> registerListCommand(name: String, longName: String, selector: Arguments.(String, String) -> OptionalConverter<L?>, context: FaqContext<L>) {
                    subCommand({ ListArgs(name, selector) }) {
                        this.name = name
                        description = "List $longName frequently asked questions."
                        autoAck = AutoAckType.NONE

                        action { context.performList(this, faqService) }
                    }
                }

                registerListCommand("channel", "channel-specific", Arguments::optionalChannel, FaqContext.Channel)
                registerListCommand("category", "category-wide", Arguments::optionalChannel, FaqContext.Category)
                registerListCommand("guild", "guild-wide", Arguments::optionalGuild, FaqContext.Guild)
            }
        }
    }
}
