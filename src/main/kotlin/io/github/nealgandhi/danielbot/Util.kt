package io.github.nealgandhi.danielbot

import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Snowflake

inline val ChannelType.isDm: Boolean get() = this == ChannelType.DM || this == ChannelType.GroupDM

data class MessageLink(val guildId: Snowflake, val channelId: Snowflake, val messageId: Snowflake) {
    companion object {
        fun fromText(link: String): MessageLink? {
            if (!link.startsWith("https://discord.com/channels/")) {
                return null
            }
            val ids = link.substring("https://discord.com/channels/".length).split('/')
            if (ids.size != 3) {
                return null
            }
            return MessageLink(Snowflake(ids[0]), Snowflake(ids[1]), Snowflake(ids[2]))
        }
    }

    val asText: String get() = "https://discord.com/channels/${guildId.value}/${channelId.value}/${messageId.value}"
}
