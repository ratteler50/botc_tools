
import io.github.oshai.kotlinlogging.KotlinLogging
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URI
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request

private val logger = KotlinLogging.logger {}

fun main() {
  val urls = getRolesFromJson().mapNotNull { it.urls?.icon }
  val client = OkHttpClient()
  runBlocking {
    val jobs = urls.map { urlString ->
      launch(Dispatchers.IO) { // Launch each task in its coroutine on the IO dispatcher
        convertBmpToSvg(convertPngToBmp(downloadPng(client, urlString)))
      }
    }
    jobs.joinAll() // Wait for all tasks to complete
  }
}

fun downloadPng(client: OkHttpClient, urlString: String): File {
  val fileName = URI(urlString).toURL().file.substringAfterLast("/")
  val downloadedFile = File("./src/data/images/pngs/$fileName")

  if (downloadedFile.exists()) {
    logger.warn { "File already exists: $fileName" }
    return downloadedFile
  }

  logger.info { "Downloading file: $urlString" }
  // Ensure all ancestor directories exist
  downloadedFile.parentFile.mkdirs()
  val request = Request.Builder().url(urlString).build()
  client.newCall(request).execute().use { response ->
    if (!response.isSuccessful) throw IOException("Failed to download file: $urlString")

    val fos = FileOutputStream(downloadedFile)
    fos.use {
      logger.info { "Writing file: $fileName" }
      response.body?.bytes()?.let(it::write)
    }
  }
  return downloadedFile
}


fun convertPngToBmp(pngFile: File): File {
  // Convert PNG to BMP
  val bmpFile = File("./src/data/images/bmps/${pngFile.nameWithoutExtension}.bmp")
  val pngImage = ImageIO.read(pngFile)

  if (bmpFile.exists()) {
    logger.warn { "File already exists: ${bmpFile.name}" }
    return bmpFile
  }
  // Ensure all ancestor directories exist
  bmpFile.parentFile.mkdirs()

  // Create a new BufferedImage without alpha channel, fill it with white color, and draw the original image on it
  val rgbImage = BufferedImage(pngImage.width, pngImage.height, BufferedImage.TYPE_INT_RGB)
  val g: Graphics2D = rgbImage.createGraphics()
  g.color = Color.WHITE
  g.fillRect(0, 0, pngImage.width, pngImage.height)
  g.drawImage(pngImage, 0, 0, null)
  g.dispose()

  val writeSuccessful = ImageIO.write(rgbImage, "BMP", bmpFile)
  if (!writeSuccessful) {
    logger.warn { "Failed to write BMP image: ${bmpFile.absolutePath}" }
    return bmpFile
  }

  logger.info { "Converted PNG to BMP: ${bmpFile.name}" }
  return bmpFile
}

fun convertBmpToSvg(bmpFile: File): File {
  // Convert BMP to SVG using potrace
  val svgFile = File("./src/data/images/svgs/${bmpFile.nameWithoutExtension}.svg")
  // Ensure all ancestor directories exist
  svgFile.parentFile.mkdirs()
  val process =
    ProcessBuilder("potrace", "-s", bmpFile.absolutePath, "-o", svgFile.absolutePath).start()
  process.waitFor()
  val errorOutput = process.errorStream.bufferedReader().readText().trim()
  if (errorOutput.isNotEmpty()) {
      logger.error { errorOutput }
  }
  logger.info { "Converted BMP to SVG: ${svgFile.name}" }
  return svgFile
}