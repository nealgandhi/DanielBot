package io.github.nealgandhi.danielbot.faq

class InMemoryFaqService : FaqService {
    private val faq = mutableListOf<FaqEntry>()

    override val questionCount: Int get() = faq.size

    override fun addQuestion(entry: FaqEntry): Boolean {
        faq.add(entry)
        return true
    }

    override fun getAllEntries(): Iterable<FaqEntry> = faq
}
