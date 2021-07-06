package io.github.nealgandhi.danielbot.faq

import dev.kord.common.entity.Snowflake
import io.github.nealgandhi.danielbot.MessageLink

sealed interface QuestionLocation
data class ChannelSpecificQuestion(val id: Snowflake) : QuestionLocation
data class GuildWideQuestion(val id: Snowflake) : QuestionLocation

data class FaqEntry(val question: String, val answer: String, val originalQuestionLink: MessageLink?)

interface FaqService {
    val questionCount: Int

    fun addQuestion(location: QuestionLocation, entry: FaqEntry): Boolean

    fun addQuestion(location: QuestionLocation, question: String, answer: String, originalQuestionLink: MessageLink?): Boolean =
        this.addQuestion(location, FaqEntry(question, answer, originalQuestionLink))

    fun getAllEntries(location: QuestionLocation): Iterable<FaqEntry>?
}
