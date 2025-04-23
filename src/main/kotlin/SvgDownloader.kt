
import io.github.oshai.kotlinlogging.KotlinLogging
import java.awt.Color
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
  // allRolesToStl()
  allPngsToStl()
}

private fun allRolesToStl() {
  val urls = getRolesFromJson().mapNotNull { it.urls?.icon }
  val client = OkHttpClient()
  runBlocking {
    val jobs = urls.map { urlString ->
      launch(Dispatchers.IO) { // Launch each task in its coroutine on the IO dispatcher
        convertSvgToStl(convertBmpToSvg(convertPngToBmp(downloadPng(client, urlString))))
      }
    }
    jobs.joinAll() // Wait for all tasks to complete
  }
}

private fun allPngsToStl() {
  // Read all files from the pngs directory as a list of Files
  val pngs = File("./data/images/pngs").listFiles()?.filter { it.extension == "png" }
  runBlocking {
    val jobs = pngs?.map { png ->
      launch(Dispatchers.IO) { // Launch each task in its coroutine on the IO dispatcher
        convertSvgToStl(convertBmpToSvg(convertPngToBmp(png)))
      }
    }
    jobs?.joinAll() // Wait for all tasks to complete
  }
}

fun downloadPng(client: OkHttpClient, urlString: String): File {
  val fileName = URI(urlString).toURL().file.substringAfterLast("/")
  val downloadedFile = File("./data/images/pngs/$fileName")

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
  val bmpFile = File("./data/images/bmps/${pngFile.nameWithoutExtension}.bmp")
  val pngImage = ImageIO.read(pngFile)

  // Scale up to double size to help preserve thin lines
  val scaledWidth = pngImage.width * 2
  val scaledHeight = pngImage.height * 2
  val scaledImage = pngImage.getScaledInstance(scaledWidth, scaledHeight, java.awt.Image.SCALE_SMOOTH)

  val rgbImage = BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB)
  val tempGraphics = rgbImage.createGraphics()
  tempGraphics.color = Color.WHITE
  tempGraphics.fillRect(0, 0, scaledWidth, scaledHeight)
  tempGraphics.drawImage(scaledImage, 0, 0, null)
  tempGraphics.dispose()

  // Apply thresholding to convert to high-contrast black and white
  for (y in 0 until rgbImage.height) {
      for (x in 0 until rgbImage.width) {
          val color = Color(rgbImage.getRGB(x, y))
          val brightness = (color.red + color.green + color.blue) / 3
          val isDark = brightness < 175

          rgbImage.setRGB(x, y, if (isDark) Color.BLACK.rgb else Color.WHITE.rgb)
      }
  }

  if (bmpFile.exists()) {
    logger.warn { "File already exists: ${bmpFile.name}" }
    return bmpFile
  }
  // Ensure all ancestor directories exist
  bmpFile.parentFile.mkdirs()

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
  val svgFile = File("./data/images/svgs/${bmpFile.nameWithoutExtension}.svg")

  if (svgFile.exists()) {
    logger.warn { "File already exists: ${svgFile.name}" }
    return svgFile
  }

  // Ensure all ancestor directories exist
  svgFile.parentFile.mkdirs()

  val process = ProcessBuilder(
    "potrace",
    "-s",
    "--turdsize",
    "20",
    "--alphamax",
    "1",
    bmpFile.absolutePath,
    "-o",
    svgFile.absolutePath
  ).start()
  val exitCode = process.waitFor()

  val errorOutput = process.errorStream.bufferedReader().readText().trim()

  if (exitCode == 0 && errorOutput.isNotEmpty()) {
    logger.warn { "Potrace completed with warnings: $errorOutput" }
  } else if (exitCode != 0) {
    logger.error { "Potrace failed with code $exitCode: $errorOutput" }
    throw IOException("Failed to convert BMP to SVG: ${bmpFile.name}")
  }

  logger.info { "Converted BMP to SVG: ${svgFile.name}" }
  return svgFile
}

fun convertSvgToStl(svgFile: File): File {
  val stlFile = File("./data/images/stls/${svgFile.nameWithoutExtension}.stl")
  stlFile.parentFile.mkdirs()

  if (stlFile.exists()) {
    logger.warn { "File already exists: ${stlFile.name}" }
    return stlFile
  }

  val scadScript = """
        linear_extrude(height = 2)
            import(file = "${svgFile.absolutePath}");
    """.trimIndent()

  val scadFile = File.createTempFile("temp_extrude", ".scad").apply {
    writeText(scadScript)
    deleteOnExit()
  }

  val process =
    ProcessBuilder("openscad", "-o", stlFile.absolutePath, scadFile.absolutePath).start()
  val exitCode = process.waitFor()

  val errorOutput = process.errorStream.bufferedReader().readText().trim()

  if (exitCode == 0 && errorOutput.isNotEmpty()) {
    logger.warn { "OpenSCAD completed for ${svgFile.name} with warnings: \n$errorOutput" }
  } else if (exitCode != 0) {
    logger.error { "OpenSCAD failed with code $exitCode: $errorOutput" }
    throw IOException("Failed to process file with OpenSCAD: ${svgFile.name}")
  }

  if (!stlFile.exists()) {
    logger.error { "STL file was not created: ${stlFile.absolutePath}" }
    throw IOException("OpenSCAD did not produce an STL file for: ${svgFile.name}")
  }

  logger.info { "Converted SVG to STL: ${stlFile.name}" }
  return stlFile
}