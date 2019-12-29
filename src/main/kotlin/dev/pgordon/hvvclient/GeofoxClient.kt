package dev.pgordon.hvvclient

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import khttp.post
import org.intellij.lang.annotations.Language
import org.json.JSONObject
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.nio.charset.Charset
import java.time.LocalDateTime.now
import java.time.format.DateTimeFormatter.ofPattern
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.xml.bind.DatatypeConverter

@Component
class GeofoxClient : CommandLineRunner {
    val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    override fun run(vararg args: String?) {

        while (true) {
            println("""
                Following commands are supported
                - find station_name                     | aliases  = 1, f, find
                - schedule station_name service_name    | aliases  = 2, s, schedule
                - help
                Examples:
                - schedule altona 111
                - schedule iserbrook s1
                - schedule iserbrook flughafen
                - s iserbrook flughafen
            """.trimIndent())
            print("Enter Command: ")

            readLine()?.let {
                try {
                    val command = it.substringBefore(" ", "no_command")
                    val argument = it.substringAfter(" ", "no_argument")
                    when (command) {
                        "1", "f", "find" -> findStation(argument)
                        "2", "s", "schedule" -> getDepartures(argument)
                        else -> println("no such command")
                    }
                } catch (e: Exception) {
                    println("error during parsing: $it")
                }
            }

        }
    }

    fun findStation(name: String): JSONObject {
        val data = """{
            | "version":37, 
            | "theName":{ 
            |   "name":"$name", 
            |   "type":"UNKNOWN" 
            | }, 
            | "maxList":1, 
            | "coordinateType":"EPSG_4326" 
            |}""".trimMargin()
        val post = post(
            url = "http://api-test.geofox.de/gti/public/checkName",
            headers = headers(data),
            data = data
        )

//        println(post.jsonObject.toString(2))
        return post.jsonObject;

    }

    fun getDepartures(stationName: String, filter: String): List<Departure> {
        val stationJSON = findStation(stationName)
        if (stationJSON.getJSONArray("results").length() == 0) {
            println("no stations found by name $stationName")
            return emptyList()
        }
        val station = stationJSON.getJSONArray("results").getJSONObject(0)

        val (date, time) = now().format(
            ofPattern("dd.MM.YYY HH:mm")).toString().split(" ", limit = 2)

        ////            |"time": {"date": "$date", "time": "$time"},
        //"date": "heute", "time": "jetzt"
        @Language("JSON") val data = """
            |{
            |"station": $station,
            |"time": {"date": "heute", "time": "jetzt"},
            |"maxList": 30,
            |"maxTimeOffset": 1000,
            |"useRealtime": true
            |}""".trimMargin()
        val post = post(
            url = "http://api-test.geofox.de/gti/public/departureList",
            headers = headers(data),
            data = data
        )

        if(!post.jsonObject.has("departures")){
            println("Couldn't get departures:")
            println("Request " + data)
            println("Response " + post.statusCode + " " + post.text)
        }
        val departures = post.jsonObject.getJSONArray("departures")

        val list = departures
            .map { it as JSONObject }
            .filter {
                filter.toLowerCase() in it.getJSONObject("line").getString("name").toLowerCase()
                    ||
                    filter.toLowerCase() in it.getJSONObject("line").getString("direction").toLowerCase()
            }
            .map {
                (it.getJSONObject("line").getString("name") + " " +
                    it.getJSONObject("line").getString("direction")) to
                    it.getInt("timeOffset")
            }
            .map { Departure(it.first, it.second) }

        println(list)

        return list;
    }

    fun getDepartures(arguments: String): List<Departure> {
        val (name, serviceType) = arguments.split(" ", limit = 2)
        return getDepartures(name, serviceType)
    }

    fun headers(data: String) = mapOf(
        "geofox-auth-user" to "gordon",
        "geofox-auth-signature" to data.sign(),
        "content-type" to "application/json"
    )

    fun getDeparturesFull(stationName: String): String {
        val stationJSON = findStation(stationName)
        if (stationJSON.getJSONArray("results").length() == 0) {
            println("no stations found by name $stationName")
            return "no stations found by name $stationName"
        }
        val station = stationJSON.getJSONArray("results").getJSONObject(0)

        val (date, time) = now().format(
            ofPattern("dd.MM.YYY HH:mm")).toString().split(" ", limit = 2)

        //"date": "heute", "time": "jetzt"
        @Language("JSON") val data = """
            |{
            |"station": $station,
            |"time": {"date": "$date", "time": "$time"},
            |"maxList": 30,
            |"maxTimeOffset": 1000,
            |"useRealtime": true
            |}""".trimMargin()
        val post = post(
            url = "http://api-test.geofox.de/gti/public/departureList",
            headers = headers(data),
            data = data
        )
        return post.jsonObject.toString(2)
    }

}
data class Departure(val direction: String, val timeInMinutes: Int)

fun String.sign(): String {
    val passwordEncoding = Charset.forName("UTF-8")
    val algorithm = "HmacSHA1"
    val key = "Lq\$w@f0N5=1j".toByteArray(passwordEncoding)
    val keySpec = SecretKeySpec(key, algorithm)
    val mac = Mac.getInstance(algorithm);
    mac.init(keySpec)
    val signature = mac.doFinal(this.toByteArray())
    return DatatypeConverter.printBase64Binary(signature);
}