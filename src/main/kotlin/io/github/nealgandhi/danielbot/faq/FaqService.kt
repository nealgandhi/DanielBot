package io.github.nealgandhi.danielbot.faq

data class FaqEntry(val question: String, val answer: String, val originalQuestionLink: String?)

interface FaqService {
    val questionCount: Int

    fun addQuestion(entry: FaqEntry): Boolean

    fun addQuestion(question: String, answer: String, originalQuestionLink: String?): Boolean =
        this.addQuestion(FaqEntry(question, answer, originalQuestionLink))

    fun getAllEntries(): Iterable<FaqEntry>
}
