package io.github.nealgandhi.danielbot.faq

import dev.kord.common.entity.Snowflake
import io.github.nealgandhi.danielbot.MessageLink

sealed interface QuestionLocation
data class ChannelSpecificQuestion(val id: Snowflake) : QuestionLocation
data class GuildWideQuestion(val id: Snowflake) : QuestionLocation

data class FaqEntry(val id: Snowflake, val question: String, val answer: String, val originalQuestionLink: MessageLink?)

interface FaqService {
    fun addQuestion(location: QuestionLocation, entry: FaqEntry): Boolean

    fun addQuestion(location: QuestionLocation, id: Snowflake, question: String, answer: String, originalQuestionLink: MessageLink?): Boolean =
        this.addQuestion(location, FaqEntry(id, question, answer, originalQuestionLink))

    fun getAllEntries(location: QuestionLocation): Iterable<FaqEntry>
}
