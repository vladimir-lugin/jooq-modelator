package ch.ayedo.jooqmodelator

import ch.ayedo.jooqmodelator.IntegrationTest.Database.MARIADB
import ch.ayedo.jooqmodelator.IntegrationTest.Database.POSTGRES
import ch.ayedo.jooqmodelator.core.configuration.Configuration
import ch.ayedo.jooqmodelator.core.configuration.DockerConfig
import ch.ayedo.jooqmodelator.core.configuration.HealthCheckConfig
import ch.ayedo.jooqmodelator.core.configuration.MigrationConfig
import ch.ayedo.jooqmodelator.core.configuration.MigrationEngine
import ch.ayedo.jooqmodelator.core.configuration.PortMapping
import org.gradle.internal.impldep.org.junit.Ignore
import org.gradle.internal.impldep.org.junit.Rule
import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

@Ignore
class IntegrationTest {

    @Rule
    private val tempDir = TemporaryFolder().also { it.create() }

    private val jooqPackageName = "ch.ayedo.jooqmodelator.test"

    private val jooqPackagePath = "/" + jooqPackageName.replace(".", "/")

    @Test
    fun flywayPostgres() {

        val jooqConfig = createJooqConfig(POSTGRES)

        val config = Configuration(
            dockerConfig = DockerConfig(
                tag = "postgres:9.5",
                env = listOf("POSTGRES_DB=postgres", "POSTGRES_USER=postgres", "POSTGRES_PASSWORD=secret"),
                portMapping = PortMapping(5432, 5432)),
            healthCheckConfig = HealthCheckConfig(),
            migrationConfig = MigrationConfig(engine = MigrationEngine.FLYWAY,
                migrationsPaths = getMigrationPaths("/migrations", "/migrationsB"),
                schemaName = "",
                schemaHistoryTable = "flyway_schema_history"),
            jooqConfigPath = jooqConfig.toPath()
        )

        createBuildFile(config)

        assertBuildOutcome(SUCCESS)

        assertFileExists("${tempDir.root.absolutePath}$jooqPackagePath/tables/Tab.java")

        assertFileExists("${tempDir.root.absolutePath}$jooqPackagePath/tables/TabTwo.java")

    }

    @Test
    fun liquibasePostgres() {

        val jooqConfig = createJooqConfig(POSTGRES)

        val config = Configuration(
            dockerConfig = DockerConfig(
                tag = "postgres:9.5",
                env = listOf("POSTGRES_DB=postgres", "POSTGRES_USER=postgres", "POSTGRES_PASSWORD=secret"),
                portMapping = PortMapping(5432, 5432)),
            healthCheckConfig = HealthCheckConfig(),
            migrationConfig = MigrationConfig(engine = MigrationEngine.LIQUIBASE,
                migrationsPaths = getMigrationPaths("/migrations", "/migrationsB"),
                schemaName = "",
                schemaHistoryTable = "flyway_schema_history"),
            jooqConfigPath = jooqConfig.toPath()
        )

        createBuildFile(config)

        assertBuildOutcome(SUCCESS)

        assertFileExists("${tempDir.root.absolutePath}$jooqPackagePath/tables/Tab.java")

        assertFileExists("${tempDir.root.absolutePath}$jooqPackagePath/tables/TabTwo.java")

    }

    @Test
    fun flywayMariaDb() {

        val jooqConfig = createJooqConfig(MARIADB)

        val config = Configuration(
            dockerConfig = DockerConfig(
                tag = "mariadb:10.2",
                env = listOf("MYSQL_DATABASE=maria", "MYSQL_ROOT_PASSWORD=pass", "MYSQL_PASSWORD=pass"),
                portMapping = PortMapping(3306, 3306)),
            healthCheckConfig = HealthCheckConfig(),
            migrationConfig = MigrationConfig(engine = MigrationEngine.FLYWAY,
                migrationsPaths = getMigrationPaths("/migrations", "/migrationsB"),
                schemaName = "",
                schemaHistoryTable = "flyway_schema_history"),
            jooqConfigPath = jooqConfig.toPath()
        )

        createBuildFile(config)

        assertBuildOutcome(SUCCESS)

        assertFileExists("${tempDir.root.absolutePath}$jooqPackagePath/maria/tables/Tab.java")

        assertFileExists("${tempDir.root.absolutePath}$jooqPackagePath/maria/tables/TabTwo.java")

    }

    @Test
    fun liquibaseMariaDb() {

        val jooqConfig = createJooqConfig(MARIADB)

        val config = Configuration(
            dockerConfig = DockerConfig(
                tag = "mariadb:10.2",
                env = listOf("MYSQL_DATABASE=maria", "MYSQL_ROOT_PASSWORD=pass", "MYSQL_PASSWORD=pass"),
                portMapping = PortMapping(3306, 3306)),
            healthCheckConfig = HealthCheckConfig(),
            migrationConfig = MigrationConfig(engine = MigrationEngine.LIQUIBASE,
                migrationsPaths = getMigrationPaths("/migrations", "/migrationsB"),
                schemaName = "",
                schemaHistoryTable = "flyway_schema_history"),
            jooqConfigPath = jooqConfig.toPath()
        )

        createBuildFile(config)

        assertBuildOutcome(SUCCESS)

        assertFileExists("${tempDir.root.absolutePath}$jooqPackagePath/maria/tables/Tab.java")

        assertFileExists("${tempDir.root.absolutePath}$jooqPackagePath/maria/tables/TabTwo.java")

    }

    @Test
    fun incrementalBuildTest() {

        val jooqConfig = createJooqConfig(POSTGRES)

        val config = Configuration(
            dockerConfig = DockerConfig(
                tag = "postgres:9.5",
                env = listOf("POSTGRES_DB=postgres", "POSTGRES_USER=postgres", "POSTGRES_PASSWORD=secret"),
                portMapping = PortMapping(5432, 5432)),
            healthCheckConfig = HealthCheckConfig(),
            migrationConfig = MigrationConfig(engine = MigrationEngine.FLYWAY,
                migrationsPaths = getMigrationPaths("/migrations", "/migrationsB"),
                schemaName = "",
                schemaHistoryTable = "flyway_schema_history"),
            jooqConfigPath = jooqConfig.toPath()
        )

        createBuildFile(config)

        assertBuildOutcome(SUCCESS)

        assertBuildOutcome(UP_TO_DATE)

        assertFileExists("${tempDir.root.absolutePath}$jooqPackagePath/tables/Tab.java")

        assertFileExists("${tempDir.root.absolutePath}$jooqPackagePath/tables/TabTwo.java")

    }

    @Test
    fun incrementalBuildChangeFilesTest() {

        val jooqConfig = createJooqConfig(POSTGRES)

        val additionalMigrationsDir = tempDir.newFolder("migrationsC").toPath()

        val config = Configuration(
            dockerConfig = DockerConfig(
                tag = "postgres:9.5",
                env = listOf("POSTGRES_DB=postgres", "POSTGRES_USER=postgres", "POSTGRES_PASSWORD=secret"),
                portMapping = PortMapping(5432, 5432)),
            healthCheckConfig = HealthCheckConfig(),
            migrationConfig = MigrationConfig(engine = MigrationEngine.FLYWAY,
                migrationsPaths = getMigrationPaths("/migrations") + listOf(additionalMigrationsDir),
                schemaName = "",
                schemaHistoryTable = "flyway_schema_history"),
            jooqConfigPath = jooqConfig.toPath()
        )

        createBuildFile(config)

        assertBuildOutcome(SUCCESS)

        assertBuildOutcome(UP_TO_DATE)

        assertFileExists("${tempDir.root.absolutePath}$jooqPackagePath/tables/Tab.java")
        assertFileNotExists("${tempDir.root.absolutePath}$jooqPackagePath/tables/TabTwo.java")

        Files.copy(getMigrationPaths("/migrationsB/V2__flyway_test.sql").first(), additionalMigrationsDir.resolve("V2__flyway_test.sql"), REPLACE_EXISTING)

        assertBuildOutcome(SUCCESS)

        assertFileExists("${tempDir.root.absolutePath}$jooqPackagePath/tables/Tab.java")
        assertFileExists("${tempDir.root.absolutePath}$jooqPackagePath/tables/TabTwo.java")

    }

    @Test
    fun changePortsTest() {

        val firstPort = 2346
        val secondPort = 5432

        val jooqConfig = createJooqConfig(POSTGRES, port = firstPort)

        val config = Configuration(
            dockerConfig = DockerConfig(
                tag = "postgres:9.5",
                env = listOf("POSTGRES_DB=postgres", "POSTGRES_USER=postgres", "POSTGRES_PASSWORD=secret"),
                portMapping = PortMapping(firstPort, 5432)),
            healthCheckConfig = HealthCheckConfig(),
            migrationConfig = MigrationConfig(engine = MigrationEngine.FLYWAY,
                migrationsPaths = getMigrationPaths("/migrations", "/migrationsB"),
                schemaName = "",
                schemaHistoryTable = "flyway_schema_history"),
            jooqConfigPath = jooqConfig.toPath()
        )

        createBuildFile(config)

        assertBuildOutcome(SUCCESS)

        val newJooqConfig = createJooqConfig(POSTGRES, port = secondPort)

        val newConfig = Configuration(
            dockerConfig = DockerConfig(
                tag = "postgres:9.5",
                env = listOf("POSTGRES_DB=postgres", "POSTGRES_USER=postgres", "POSTGRES_PASSWORD=secret"),
                portMapping = PortMapping(secondPort, 5432)),
            healthCheckConfig = HealthCheckConfig(),
            migrationConfig = MigrationConfig(engine = MigrationEngine.FLYWAY,
                migrationsPaths = getMigrationPaths("/migrations", "/migrationsB"),
                schemaName = "",
                schemaHistoryTable = "flyway_schema_history"),
            jooqConfigPath = newJooqConfig.toPath()
        )

        createBuildFile(newConfig)

        assertBuildOutcome(SUCCESS)

    }

    private fun createJooqConfig(
        database: Database,
        databaseName: String? = null,
        port: Int? = null,
        user: String? = null,
        password: String? = null): File {

        File("${tempDir.root.absolutePath}/jooqConfig.xml").delete()

        return tempDir.newFile("jooqConfig.xml").also {
            val configFilePath = tempDir.root.absolutePath

            val content = when (database) {
                POSTGRES -> jooqPostgresConfig(configFilePath,
                    port ?: 5432,
                    databaseName ?: "postgres",
                    user ?: "postgres",
                    password ?: "secret")
                MARIADB -> jooqMariaDbConfig(configFilePath,
                    port ?: 3306,
                    databaseName ?: "maria",
                    user ?: "root",
                    password ?: "pass")
            }

            it.writeText(content)
        }
    }

    private enum class Database {
        POSTGRES,
        MARIADB
    }

    private fun jooqPostgresConfig(target: String, port: Int = 5432, database: String, user: String, password: String) = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <configuration>
            <jdbc>
                <driver>org.postgresql.Driver</driver>
                <url>jdbc:postgresql://localhost:$port/$database?loggerLevel=DEBUG</url>
                <user>$user</user>
                <password>$password</password>
            </jdbc>
            <generator>
                <database>
                    <name>org.jooq.meta.postgres.PostgresDatabase</name>
                    <inputSchema>public</inputSchema>
                </database>
                <target>
                    <packageName>$jooqPackageName</packageName>
                    <directory>$target</directory>
                </target>
            </generator>
        </configuration>
    """.trimIndent()

    private fun jooqMariaDbConfig(target: String, port: Int = 3306, database: String, user: String, password: String) = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <configuration>
            <jdbc>
                <driver>org.mariadb.jdbc.Driver</driver>
                <url>jdbc:mariadb://localhost:$port/$database</url>
                <user>$user</user>
                <password>$password</password>
            </jdbc>
            <generator>
                <database>
                    <name>org.jooq.meta.mariadb.MariaDBDatabase</name>
                </database>
                <target>
                    <packageName>$jooqPackageName</packageName>
                    <directory>$target</directory>
                </target>
            </generator>
        </configuration>
    """.trimIndent()


    private fun getMigrationPaths(vararg paths: String): List<Path> = paths.map({ path -> getResourcePath(path) })

    private fun assertFileNotExists(fileName: String) {
        Assertions.assertFalse(fileExists(fileName), "File was expected to not exist.")
    }

    private fun assertFileExists(fileName: String) {
        Assertions.assertTrue(fileExists(fileName), "Expected file does not exist.")
    }

    private fun fileExists(fileName: String) = File(fileName).exists()

    private fun getResourcePath(path: String): Path = Paths.get(this.javaClass.getResource(path).toURI())

    private fun createBuildFile(config: Configuration) {

        File("${tempDir.root.absolutePath}/build.gradle").delete()

        tempDir.newFile("build.gradle").also {
            val buildFileText = buildFileFromConfiguration(config, tempDir.root.absolutePath + jooqPackagePath)

            it.writeText(buildFileText)
        }
    }

    private fun assertBuildOutcome(targetOutcome: TaskOutcome) {

        val result = GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(tempDir.root)
            .withArguments("generateJooqMetamodel", "--stacktrace")
            .withDebug(true)
            .build()

        Assertions.assertTrue(result.task(":generateJooqMetamodel")?.outcome == targetOutcome)

        println(result.output)
    }

    private fun buildFileFromConfiguration(config: Configuration, jooqOutputPath: String, jooqVersion: String = "3.11.4", jooqEdition: String = "OSS") =
        """
            plugins {
                id 'ch.ayedo.jooqmodelator'
            }

            jooqModelator {

                jooqVersion = '$jooqVersion'

                jooqEdition = '$jooqEdition'

                jooqConfigPath = '${config.jooqConfigPath}'

                jooqOutputPath = '$jooqOutputPath'

                migrationsPaths = ${config.migrationConfig.migrationsPaths.joinToString(prefix = "[", postfix = "]") { "'$it'" }}

                dockerTag = '${config.dockerConfig.tag}'

                dockerEnv = ${config.dockerConfig.env.joinToString(prefix = "[", postfix = "]") { "'$it'" }}

                dockerHostPort = ${config.dockerConfig.portMapping.host}

                dockerContainerPort = ${config.dockerConfig.portMapping.container}

                labelKey = '${config.dockerConfig.labelKey}'

                migrationEngine = '${config.migrationConfig.engine}'

                delayMs = ${config.healthCheckConfig.delayMs}

                maxDurationMs = ${config.healthCheckConfig.maxDurationMs}

                sql = '${config.healthCheckConfig.sql}'

            }

            repositories {
                jcenter()
            }

            dependencies {
                jooqModelatorRuntime('org.postgresql:postgresql:42.2.4')
                jooqModelatorRuntime('org.mariadb.jdbc:mariadb-java-client:2.2.6')
            }

        """.trimIndent()
}
