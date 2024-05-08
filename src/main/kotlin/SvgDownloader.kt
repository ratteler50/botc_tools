
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import javax.imageio.ImageIO
import okhttp3.OkHttpClient
import okhttp3.Request


fun main() {
  val urls = getRolesFromJson().mapNotNull { it.urls?.icon }
  val client = OkHttpClient()
  urls.forEach { urlString ->
    convertBmpToSvg(convertPngToBmp(downloadPng(client, urlString)))

  }
}

fun downloadPng(client: OkHttpClient, urlString: String): File {
  val fileName = URL(urlString).file.substringAfterLast("/")
  val downloadedFile = File("./src/data/images/pngs/$fileName")

  if (downloadedFile.exists()) {
    println("File already exists: $fileName")
    return downloadedFile
  }

  println("Downloading file: $urlString")
  val request = Request.Builder().url(urlString).build()
  client.newCall(request).execute().use { response ->
    if (!response.isSuccessful) throw IOException("Failed to download file: $urlString")

    val fos = FileOutputStream(downloadedFile)
    fos.use {
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
    println("File already exists: ${bmpFile.name}")
    return bmpFile
  }

  // Create a new BufferedImage without alpha channel, fill it with white color, and draw the original image on it
  val rgbImage = BufferedImage(pngImage.width, pngImage.height, BufferedImage.TYPE_INT_RGB)
  val g: Graphics2D = rgbImage.createGraphics()
  g.color = Color.WHITE
  g.fillRect(0, 0, pngImage.width, pngImage.height)
  g.drawImage(pngImage, 0, 0, null)
  g.dispose()

  val writeSuccessful = ImageIO.write(rgbImage, "BMP", bmpFile)
  if (!writeSuccessful) {
    println("Failed to write BMP image: ${bmpFile.absolutePath}")
    return bmpFile
  }

  println("Converted PNG to BMP: ${bmpFile.name}")
  return bmpFile
}

fun convertBmpToSvg(bmpFile: File): File {
  // Convert BMP to SVG using potrace
  val svgFile = File("./src/data/images/svgs/${bmpFile.nameWithoutExtension}.svg")
  val process =
    ProcessBuilder("potrace", "-s", bmpFile.absolutePath, "-o", svgFile.absolutePath).start()
  process.waitFor()
  println(process.errorStream.bufferedReader().readText())
  println("Converted BMP to SVG: ${svgFile.name}")
  return svgFile
}