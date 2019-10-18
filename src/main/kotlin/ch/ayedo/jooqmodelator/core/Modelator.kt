package ch.ayedo.jooqmodelator.core

import ch.ayedo.jooqmodelator.core.configuration.Configuration
import ch.ayedo.jooqmodelator.core.configuration.DatabaseConfig
import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.DockerClient
import org.slf4j.LoggerFactory

class Modelator(configuration: Configuration) {

    private val log = LoggerFactory.getLogger(Modelator::class.java)

    private val dockerConfig = configuration.dockerConfig

    private val healthCheckConfig = configuration.healthCheckConfig

    private val jooqConfigPath = configuration.jooqConfigPath

    private val databaseConfig = DatabaseConfig.fromJooqConfig(jooqConfigPath)

    private val migrationConfig = configuration.migrationConfig

    private fun connectToDocker() = DefaultDockerClient.fromEnv().build()!!

    fun generate() {
        connectToDocker().use { docker ->
            val tag = dockerConfig.tag

            if (!docker.imageExists(tag)) {
                docker.pull(tag)
            }

            val existingContainers = docker.findLabeledContainers(key = dockerConfig.labelKey, value = dockerConfig.labelValue)
            existingContainers.forEach { c ->
                docker.removeContainer(
                    c.id(),
                    DockerClient.RemoveContainerParam.forceKill()
                )
            }

            val containerId = docker.createContainer(dockerConfig.toContainerConfig()).id()!!

            docker.useContainer(containerId) {
                waitForDatabase()
                migrateDatabase()
                runJooqGenerator()
            }

            docker.removeContainer(
                containerId,
                DockerClient.RemoveContainerParam.forceKill()
            )
        }
    }

    private fun waitForDatabase() {
        val healthChecker = HealthChecker.getDefault(databaseConfig, healthCheckConfig)

        healthChecker.waitForDatabase()
    }

    private fun migrateDatabase() {
        val migrator = Migrator.fromConfig(migrationConfig, databaseConfig)

        with(migrator) {
            clean()
            migrate()
        }
    }

    private fun runJooqGenerator() {
        val jooqConfig = jooqConfigPath.toFile().readText()

        val generationTool = Class.forName("org.jooq.codegen.GenerationTool", true, Thread.currentThread().contextClassLoader)

        val generateMethod = generationTool.getDeclaredMethod("generate", String::class.java)

        generateMethod.invoke(generationTool, jooqConfig)
    }

}