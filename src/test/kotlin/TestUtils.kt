import java.net.URL

private fun String.asResource(): URL = {}::class.java.classLoader.getResource(this)!!

fun resourceToString(filename: String) = filename.asResource().readText()