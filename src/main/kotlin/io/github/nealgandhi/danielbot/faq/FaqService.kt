package io.github.nealgandhi.danielbot.faq

import dev.kord.common.entity.Snowflake
import io.github.nealgandhi.danielbot.MessageLink

enum class QuestionLocationType {
    Guild, Category, Channel
}
data class QuestionLocation(val id: Snowflake, val type: QuestionLocationType) {
    companion object {
        fun guild(id: Snowflake) = QuestionLocation(id, QuestionLocationType.Guild)
        fun category(id: Snowflake) = QuestionLocation(id, QuestionLocationType.Category)
        fun channel(id: Snowflake) = QuestionLocation(id, QuestionLocationType.Channel)
    }
}

data class FaqEntry(val id: Snowflake, val question: String, val answer: String, val originalQuestionLink: MessageLink?)

interface FaqService {
    fun addQuestion(location: QuestionLocation, entry: FaqEntry): Boolean

    fun addQuestion(location: QuestionLocation, id: Snowflake, question: String, answer: String, originalQuestionLink: MessageLink?): Boolean =
        this.addQuestion(location, FaqEntry(id, question, answer, originalQuestionLink))

    fun getAllEntries(location: QuestionLocation): Iterable<FaqEntry>
}
