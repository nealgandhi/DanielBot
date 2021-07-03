@file:OptIn(KordPreview::class)

package io.github.nealgandhi.danielbot.faq

import com.kotlindiscord.kord.extensions.commands.converters.impl.*
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.pagination.pages.Page
import dev.kord.common.annotation.KordPreview
import org.koin.core.component.inject

class FaqExtension : Extension() {
    override val name: String get() = "faq"

    private val faqService: FaqService by inject()

    class FaqCreateArgs : Arguments() {
        val question by string("question", "The frequently asked question")
        val answer by string("answer", "The answer to the question")
        val originalQuestionLink by optionalString("original-question-link", "Link to the message where the question was asked")
    }

    override suspend fun setup() {
        println("Setting up FAQ")

        slashCommand {
            name = "faq"
            description = "FAQ-related commands"

            subCommand(::FaqCreateArgs) {
                name = "create"
                description = "Add a question to the list of frequently-asked questions"

                action {
                    val successful = faqService.addQuestion(arguments.question, arguments.answer, arguments.originalQuestionLink)
                    ephemeralFollowUp {
                        content = if (successful) {
                            "Added question successfully!"
                        } else {
                            "Failed to add question to FAQ."
                        }
                    }
                }
            }

            subCommand {
                name = "list"
                description = "List all frequently-asked questions"
                autoAck = AutoAckType.PUBLIC

                action {
                    if (faqService.questionCount == 0) {
                        publicFollowUp {
                            content = "No questions yet!"
                        }
                    } else {
                        paginator {
                            keepEmbed = false
                            faqService.getAllEntries().forEach { entry ->
                                page(
                                    Page(
                                        title = entry.question,
                                        description = entry.answer,
                                        footer = entry.originalQuestionLink?.let { "[Original]($it)" },
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
