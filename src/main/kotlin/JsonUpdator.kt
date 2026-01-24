
import AppConfig.DATA_JSON
import AppConfig.SAO_JSON
import io.github.oshai.kotlinlogging.KotlinLogging
import models.DataJson
import java.io.File
import kotlin.system.measureTimeMillis

private val logger = KotlinLogging.logger {}

fun main() {
  measureTimeMillis { updateSaoFile() }.also { logger.info { "Updated SAO file in $it ms" } }
}


private fun updateSaoFile() {
  val dataJson = DataJson.fromJson(gson, File(DATA_JSON).readText())
  val rolesSortedBySao = dataJson.getAllRoles().map { it.id.normalize() }
  File(SAO_JSON).writeText(gson.toJson(rolesSortedBySao))
}
