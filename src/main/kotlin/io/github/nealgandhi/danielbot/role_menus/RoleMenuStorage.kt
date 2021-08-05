package io.github.nealgandhi.danielbot.role_menus

import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.flow.Flow

interface RoleMenuStorage {
    suspend fun getAll(): Flow<RoleMenuData>

    suspend fun get(id: Snowflake): RoleMenuData?
    suspend fun add(menu: RoleMenuData)
}
