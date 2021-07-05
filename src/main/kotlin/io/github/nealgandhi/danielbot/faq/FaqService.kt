package io.github.nealgandhi.danielbot.faq

import dev.kord.common.entity.Snowflake

data class FaqEntry(val question: String, val answer: String, val originalQuestionLink: String?)

interface FaqService {
    val questionCount: Int

    fun addQuestion(guildId: Snowflake, entry: FaqEntry): Boolean

    fun addQuestion(guildId: Snowflake, question: String, answer: String, originalQuestionLink: String?): Boolean =
        this.addQuestion(guildId, FaqEntry(question, answer, originalQuestionLink))

    fun getAllEntries(guildId: Snowflake): Iterable<FaqEntry>?
}
