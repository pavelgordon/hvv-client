package dev.pgordon.hvvclient

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import khttp.post
import org.intellij.lang.annotations.Language
import org.json.JSONObject
import org.springframework.stereotype.Component
import java.nio.charset.Charset
import java.time.LocalDateTime.now
import java.time.format.DateTimeFormatter.ofPattern
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.xml.bind.DatatypeConverter

@Component
class GeofoxClient {
    val gson: Gson = GsonBuilder().setPrettyPrinting().create()


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

        val departures = getDeparturesFromHVV(stationName).getJSONArray("departures")

        val list = departures
            .map { it as JSONObject }
            .filter {
                filter.toLowerCase() in it.getJSONObject("line").getString("name").toLowerCase()
                    || filter.toLowerCase() in it.getJSONObject("line").getString("direction").toLowerCase()
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

    fun getDeparturesFull(stationName: String, filter: String): List<FatDeparture> {

        val departures = getDeparturesFromHVV(stationName).getJSONArray("departures")

        val list = departures
            .map { gson.fromJson((it as JSONObject).toString(), FatDeparture::class.java) }
            .filter {
                filter in it.line.name.toLowerCase()
                    || filter in it.line.direction.toLowerCase()
                    || filter in it.line.type.longInfo.toLowerCase()
                    || filter in it.line.type.shortInfo.toLowerCase()
                    || filter in it.line.type.simpleType.toLowerCase()
            }

        println(list)

        return list;
    }

    fun headers(data: String) = mapOf(
        "geofox-auth-user" to "gordon",
        "geofox-auth-signature" to data.sign(),
        "content-type" to "application/json"
    )

    fun getDeparturesFromHVV(stationName: String): JSONObject {

        val stationJSON = findStation(stationName)
        if (stationJSON.getJSONArray("results").length() == 0) {
            println("no stations found by name $stationName")
            return JSONObject()
        }
        val station = stationJSON.getJSONArray("results").getJSONObject(0)

        val (date, time) = now().format(ofPattern("dd.MM.YYY HH:mm")).toString().split(" ", limit = 2)

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


        return post.jsonObject;
    }

}

data class Departure(val direction: String, val timeInMinutes: Int)

data class FatDeparture(val line: Line, val timeOffset: Int)
data class Line(val name: String, val type: TrainType, val direction: String)
data class TrainType(val simpleType: String, val shortInfo: String, val longInfo: String)

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