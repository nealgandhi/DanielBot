package io.github.nealgandhi.danielbot.role_menus

import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq

class MongoRoleMenuStorage : RoleMenuStorage, KoinComponent {
    companion object {
        const val COLLECTION_NAME: String = "role-menus"
    }

    private val col: CoroutineCollection<RoleMenuData> = get<CoroutineDatabase>().getCollection(COLLECTION_NAME)

    override suspend fun getAll(): Flow<RoleMenuData> = col.find().toFlow()
    override suspend fun get(id: Snowflake): RoleMenuData? = col.findOne(RoleMenuData::id eq id)

    override suspend fun add(menu: RoleMenuData) {
        col.insertOne(menu)
    }
}
