package io.github.nealgandhi.danielbot.faq

class InMemoryFaqService : FaqService {
    private val faq = mutableMapOf<QuestionLocation, MutableList<FaqEntry>>()

    override fun addQuestion(location: QuestionLocation, entry: FaqEntry): Boolean {
        faq.computeIfAbsent(location) { mutableListOf() }.add(entry)
        return true
    }

    override fun getAllEntries(location: QuestionLocation): Iterable<FaqEntry>? = faq[location]
}
