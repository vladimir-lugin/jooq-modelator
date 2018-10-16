package ch.ayedo.jooqmodelator.core

import ch.ayedo.jooqmodelator.core.configuration.DatabaseConfig
import ch.ayedo.jooqmodelator.core.configuration.MigrationConfig
import ch.ayedo.jooqmodelator.core.configuration.MigrationEngine.FLYWAY
import ch.ayedo.jooqmodelator.core.configuration.MigrationEngine.LIQUIBASE
import liquibase.CatalogAndSchema
import liquibase.Contexts
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.FileSystemResourceAccessor
import org.flywaydb.core.Flyway
import org.flywaydb.core.internal.util.jdbc.DriverDataSource
import org.flywaydb.core.internal.util.jdbc.JdbcUtils.openConnection
import java.io.File
import java.nio.file.Path

interface Migrator {

    /* deletes all objects in the database */
    fun clean()

    /* applies all migrations to the database */
    fun migrate()

    companion object {
        fun fromConfig(migrationConfig: MigrationConfig, databaseConfig: DatabaseConfig) =
            when (migrationConfig.engine) {
                FLYWAY -> FlywayMigrator(databaseConfig, migrationConfig.migrationsPaths, migrationConfig.schemas)
                LIQUIBASE -> LiquibaseMigrator(databaseConfig, migrationConfig.migrationsPaths, migrationConfig.schemas)
            }
    }
}

class FlywayMigrator(databaseConfig: DatabaseConfig, migrationsPaths: List<Path>, schemas: List<String>) : Migrator {

    private val flyway = Flyway().apply {
        with(databaseConfig) {
            setDataSource(url, user, password)
        }

        val fileSystemPaths = migrationsPaths.map({ "filesystem:$it" }).toTypedArray()

        setLocations(*fileSystemPaths)

        if (schemas.isNotEmpty()) {
            setSchemas(*schemas.toTypedArray())
        }
    }

    override fun clean() {
        flyway.clean()
    }

    override fun migrate() {
        flyway.migrate()
    }

}

class LiquibaseMigrator(databaseConfig: DatabaseConfig, migrationsPaths: List<Path>, private val schemas: List<String>) : Migrator {

    private val liquibase: Liquibase

    init {
        // TODO: not sure whether connection is closed correctly in this migrator
        val database = with(databaseConfig) {
            // Ugly workaround so that Liquibase uses the contextClassLoader
            val flywayDataSource = DriverDataSource(Thread.currentThread().contextClassLoader, driver, url, user, password, null)
            val connection = openConnection(flywayDataSource)
            DatabaseFactory.getInstance().findCorrectDatabaseImplementation(JdbcConnection(connection))
        }

        val changeLogFiles = migrationsPaths
            .map({ path -> path.toFile() })
            .flatMap({ file: File -> file.listFiles({ pathName -> pathName.nameWithoutExtension == "databaseChangeLog" }).toList() })

        if (changeLogFiles.isEmpty()) {
            throw IllegalStateException("Cannot find liquibase changelog file. It must be named 'databaseChangeLog'.")
        }

        if (changeLogFiles.size > 1) {
            throw IllegalStateException("More than one file named databaseChangeLog found in migrations folders:\nFiles: ${changeLogFiles.joinToString(prefix = "[", separator = ",", postfix = "]") { it.absolutePath }}")
        }

        liquibase = Liquibase(changeLogFiles.first().toString(), FileSystemResourceAccessor(), database)
    }

    override fun clean() {
        val catalogsAndSchemas = schemas.map { CatalogAndSchema(null, it) }
        liquibase.dropAll(*catalogsAndSchemas.toTypedArray())
    }

    override fun migrate() {
        val nullContext: Contexts? = null
        liquibase.update(nullContext)
    }

}