package com.example.mypersonality.server.data

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.net.URI
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    fun createRepositoryFromEnvironment(): BackendRepository {
        val rawUrl = System.getenv("DATABASE_URL").orEmpty()
        if (rawUrl.isBlank()) {
            return InMemoryBackend()
        }
        val parsed = rawUrl.toJdbcConfig()

        val hikari = HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = parsed.jdbcUrl
                driverClassName = "org.postgresql.Driver"
                username = parsed.username
                password = parsed.password
                maximumPoolSize = 5
                isAutoCommit = false
                transactionIsolation = "TRANSACTION_REPEATABLE_READ"
                validate()
            }
        )
        Database.connect(hikari)
        hikari.connection.use(::migrateUsersPasswordHashesIfNeeded)
        transaction {
            SchemaUtils.createMissingTablesAndColumns(UsersTable, VacanciesTable, FavoritesTable, ApplicationsTable)
        }
        return PostgresBackendRepository()
    }
}

private data class JdbcConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String
)

private fun String.toJdbcConfig(): JdbcConfig {
    if (startsWith("jdbc:")) {
        return JdbcConfig(jdbcUrl = this, username = "", password = "")
    }

    val uri = URI(this)
    val userInfoParts = uri.userInfo?.split(":", limit = 2).orEmpty()
    val username = userInfoParts.getOrElse(0) { "" }
    val password = userInfoParts.getOrElse(1) { "" }
    val port = if (uri.port == -1) "" else ":${uri.port}"
    val query = uri.query?.let { "?$it" }.orEmpty()

    return JdbcConfig(
        jdbcUrl = "jdbc:postgresql://${uri.host}$port${uri.path}$query",
        username = username,
        password = password
    )
}

private fun migrateUsersPasswordHashesIfNeeded(connection: Connection) {
    connection.autoCommit = false
    val columnExists = connection.metaData.getColumns(null, null, "users", "password_hash").use { columns ->
        columns.next()
    }
    if (columnExists) {
        connection.rollback()
        return
    }

    val fallbackHash = PasswordHasher.hash("legacy-account-disabled").replace("'", "''")
    connection.createStatement().use { statement ->
        statement.executeUpdate("ALTER TABLE users ADD COLUMN password_hash VARCHAR(512)")
        statement.executeUpdate("UPDATE users SET password_hash = '$fallbackHash' WHERE password_hash IS NULL")
        statement.executeUpdate("ALTER TABLE users ALTER COLUMN password_hash SET NOT NULL")
    }
    connection.commit()
}
