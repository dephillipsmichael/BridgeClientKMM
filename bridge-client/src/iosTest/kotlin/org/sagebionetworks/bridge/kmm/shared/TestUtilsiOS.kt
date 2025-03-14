package org.sagebionetworks.bridge.kmm.shared

import co.touchlab.sqliter.DatabaseConfiguration
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.drivers.native.NativeSqliteDriver
import com.squareup.sqldelight.drivers.native.wrapConnection
import org.sagebionetworks.bridge.kmm.shared.cache.BridgeResourceDatabase
import platform.Foundation.NSUUID

internal actual fun testDatabaseDriver(): SqlDriver {
        val schema = BridgeResourceDatabase.Schema
        val tableName = NSUUID.UUID().UUIDString //So each test gets own db
        return NativeSqliteDriver(
            DatabaseConfiguration(
                name = tableName,
                version = schema.version,
                create = { connection ->
                    wrapConnection(connection) { schema.create(it) }
                },
                upgrade = { connection, oldVersion, newVersion ->
                    wrapConnection(connection) { schema.migrate(it, oldVersion, newVersion) }
                },
                inMemory = true
            )
        )
    }
