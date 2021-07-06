package io.github.nealgandhi.danielbot.faq

import dev.kord.common.entity.Snowflake

sealed interface QuestionLocation
data class ChannelSpecificQuestion(val id: Snowflake) : QuestionLocation
data class GuildWideQuestion(val id: Snowflake) : QuestionLocation

data class FaqEntry(val question: String, val answer: String, val originalQuestionLink: String?)

interface FaqService {
    val questionCount: Int

    fun addQuestion(location: QuestionLocation, entry: FaqEntry): Boolean

    fun addQuestion(location: QuestionLocation, question: String, answer: String, originalQuestionLink: String?): Boolean =
        this.addQuestion(location, FaqEntry(question, answer, originalQuestionLink))

    fun getAllEntries(location: QuestionLocation): Iterable<FaqEntry>?
}
