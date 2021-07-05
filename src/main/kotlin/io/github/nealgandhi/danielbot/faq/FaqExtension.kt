@file:OptIn(KordPreview::class)

package io.github.nealgandhi.danielbot.faq

import com.kotlindiscord.kord.extensions.commands.converters.impl.*
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommandContext
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.pagination.pages.Page
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Snowflake
import org.koin.core.component.inject

class FaqExtension : Extension() {
    override val name: String get() = "faq"

    private val faqService: FaqService by inject()

    interface HasGuildArg {
        val guildId: Long?
    }

    class CreateArgs : Arguments(), HasGuildArg {
        val question by string("question", "The frequently asked question")
        val answer by string("answer", "The answer to the question")
        val originalQuestionLink by optionalString("original-question-link", "Link to the message where the question was asked")
        override val guildId by optionalLong("guild-id", "ID of the guild this FAQ is for.")
    }

    class ListArgs : Arguments(), HasGuildArg {
        override val guildId by optionalLong("guild-id", "ID of the guild to list FAQs for.")
    }

    private inline val <T> SlashCommandContext<T>.guildId: Snowflake where T: Arguments, T: HasGuildArg get() {
        val id = arguments.guildId?.let(::Snowflake) ?: guild?.id
        check(id != null)
        return id
    }

    override suspend fun setup() {
        println("Setting up FAQ")

        slashCommand {
            name = "faq"
            description = "FAQ-related commands"

            subCommand(::CreateArgs) {
                name = "create"
                description = "Add a question to the list of frequently-asked questions"

                action {
                    val successful = faqService.addQuestion(guildId, arguments.question, arguments.answer, arguments.originalQuestionLink)
                    ephemeralFollowUp {
                        content = if (successful) {
                            "Added question successfully!"
                        } else {
                            "Failed to add question to FAQ."
                        }
                    }
                }
            }

            subCommand(::ListArgs) {
                name = "list"
                description = "List all frequently-asked questions"
                autoAck = AutoAckType.PUBLIC

                action {
                    if (faqService.questionCount == 0) {
                        publicFollowUp {
                            content = "No questions yet!"
                        }
                    } else {
                        val entries = faqService.getAllEntries(guildId)
                        check(entries != null)
                        val owner = user.asUser()

                        paginator {
                            keepEmbed = false
                            this.owner = owner
                            entries.forEach { entry ->
                                page(
                                    Page(
                                        title = entry.question,
                                        description = entry.answer,
                                        url = entry.originalQuestionLink,
                                    )
                                )
                            }
                        }.send()
                    }
                }
            }
        }
    }
}
