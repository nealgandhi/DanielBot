@file:OptIn(KordPreview::class)

package io.github.nealgandhi.danielbot.util

import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.common.entity.optional.optional
import dev.kord.core.behavior.interaction.PublicFollowupMessageBehavior
import dev.kord.core.behavior.interaction.edit
import dev.kord.core.entity.ReactionEmoji

val ReactionEmoji.asPartial: DiscordPartialEmoji
    get() = this.let {
        when (it) {
            is ReactionEmoji.Custom -> DiscordPartialEmoji(it.id, it.name, it.isAnimated.optional())
            is ReactionEmoji.Unicode -> DiscordPartialEmoji(null, it.name, false.optional())
        }
    }

suspend inline fun PublicFollowupMessageBehavior.editContent(newContent: String) =
    edit {
        this.content = newContent
    }
