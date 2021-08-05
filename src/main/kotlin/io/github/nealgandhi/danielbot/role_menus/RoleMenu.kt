@file:OptIn(KordPreview::class)

package io.github.nealgandhi.danielbot.role_menus

import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.components.Components
import com.kotlindiscord.kord.extensions.components.builders.MenuBuilder
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.createRole
import dev.kord.core.entity.Message
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.Role
import dev.kord.core.entity.channel.GuildChannel
import dev.kord.rest.builder.message.create.actionRow
import io.github.nealgandhi.danielbot.util.asPartial
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class RoleMenuBuilder(private val id: Snowflake, private val title: String) {
    private val names = mutableMapOf<ReactionEmoji?, MutableSet<String>>()

    val roleCount: Int get() = names.values.sumOf { it.size }

    fun addRole(emoji: ReactionEmoji?, name: String) = names.computeIfAbsent(emoji) { mutableSetOf() }.add(name)

    suspend fun createRoles(guild: GuildBehavior, mentionable: Boolean): RoleMenu = coroutineScope {
        val roles = names.entries.map { (emoji, names) ->
            async {
                emoji to names.mapTo(HashSet(names.size)) { name ->
                    guild.createRole {
                        this.name = name
                        this.mentionable = mentionable
                    }
                }
            }
        }
            .awaitAll()
            .toMap(LinkedHashMap())

        RoleMenu(guild.id, id, title, roles)
    }
}

class RoleMenu(
    private val guildId: Snowflake,
    private val id: Snowflake,
    private val title: String,
    private val roles: Map<ReactionEmoji?, HashSet<Role>>
) {
    suspend fun sendTo(channel: MessageChannelBehavior): Pair<RoleMenuData, Message> {
        if (channel !is GuildChannel) {
            throw IllegalArgumentException("Can only send role menu to guild channel.")
        } else if (channel.guildId != this.guildId) {
            throw IllegalArgumentException("Cannot send role menu $title to guild with id ${channel.id} ($title is for guild with id ${this.guildId}")
        }

        val message = channel.createMessage {
            content = "Roles for $title"

            actionRow {
                selectMenu(id.asString) {
                    allowedValues = 0..roles.values.sumOf { it.size }

                    roles.forEach { (emoji, roles) ->
                        roles.forEach { role ->
                            option(role.name, role.id.asString) {
                                this.emoji = emoji?.asPartial
                            }
                        }
                    }
                }
            }
        }

        return Pair(
            RoleMenuData(id, title, roles.values.flatMap { it.map { role -> role.id } }),
            message,
        )
    }
}

@Serializable
data class RoleMenuData(
    @SerialName("_id")
    val id: Snowflake,
    val title: String,
    val roleIds: List<Snowflake>,
) {
    suspend fun startListening(extension: Extension) {
        val id = id.asString

        Components(extension).apply {
            actionableComponents[id] = MenuBuilder().apply {
                this.id = id

                check(anyGuild)

                action {
                    val member = user.asMember(interaction.data.guildId.value!!) // ok: anyGuild check

                    val selectedIds = selected.mapTo(HashSet(), ::Snowflake)

                    roleIds.map { roleId ->
                        val shouldAdd = roleId in selectedIds
                        val alreadyHas = roleId in member.roleIds

                        kord.async {
                            if (shouldAdd && !alreadyHas) {
                                member.addRole(roleId, "Selected role from menu $title")
                            } else if (!shouldAdd && alreadyHas) {
                                member.removeRole(roleId, "Deselected role from menu $title")
                            }
                        }
                    }.awaitAll()

                    respond("Updated your roles!")
                }
            }

            startListening()
        }
    }
}
