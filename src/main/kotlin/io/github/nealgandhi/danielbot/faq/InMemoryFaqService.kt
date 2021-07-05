package io.github.nealgandhi.danielbot.faq

import dev.kord.common.entity.Snowflake

class InMemoryFaqService : FaqService {
    private val faq = mutableMapOf<Snowflake, MutableList<FaqEntry>>()

    override val questionCount: Int get() = faq.size

    override fun addQuestion(guildId: Snowflake, entry: FaqEntry): Boolean {
        faq.computeIfAbsent(guildId) { mutableListOf() }.add(entry)
        return true
    }

    override fun getAllEntries(guildId: Snowflake): Iterable<FaqEntry>? = faq[guildId]
}
