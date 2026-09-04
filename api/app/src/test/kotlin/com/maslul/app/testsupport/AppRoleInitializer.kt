package com.maslul.app.testsupport

import java.sql.DriverManager

// Mirrors app/src/test/resources/db/postgres-init/01-create-app-role.sql (the script
// docker-compose mounts into docker-entrypoint-initdb.d for local dev). Testcontainers'
// own withInitScript is skipped here due to a shading bug in the pinned Testcontainers
// version (NoClassDefFoundError on its shaded commons-io) - plain JDBC works fine.
object AppRoleInitializer {

    fun ensureAppRole(jdbcUrl: String) {
        DriverManager.getConnection(jdbcUrl, "postgres", "postgres").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("CREATE ROLE maslul WITH LOGIN PASSWORD 'maslul'")
                statement.execute("ALTER SCHEMA public OWNER TO maslul")
            }
        }
    }
}
