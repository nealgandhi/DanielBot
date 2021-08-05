package io.github.nealgandhi.danielbot.role_menus

import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

class InMemoryRoleMenuStorage private constructor(private val menus: MutableMap<Snowflake, RoleMenuData>) : RoleMenuStorage {
    constructor() : this(mutableMapOf())

    override suspend fun getAll(): Flow<RoleMenuData> = menus.values.asFlow()

    override suspend fun get(id: Snowflake): RoleMenuData? = menus[id]

    override suspend fun add(menu: RoleMenuData) {
        menus[menu.id] = menu
    }
}
