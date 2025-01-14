package ch.ayedo.jooqmodelator.gradle


open class JooqModelatorExtension {

    var jooqVersion: String? = null

    var jooqEdition: String? = null

    var jooqConfigPath: String? = null

    var jooqOutputPath: String? = null

    var migrationsPaths: List<String>? = null

    var dockerTag: String? = null

    var dockerEnv: List<String> = emptyList()

    var dockerHostPort: Int? = null

    var dockerContainerPort: Int? = null

    var migrationEngine: String? = null

    var delayMs: Long = 500

    var maxDurationMs: Long = 20000

    var sql: String = "SELECT 1"

    var labelKey: String = "ch.ayedo.jooqmodelator"

    var cleanDatabase: Boolean = true

    var schemaName: String = ""

    var schemaHistoryTable: String = "flyway_schema_history"
}
