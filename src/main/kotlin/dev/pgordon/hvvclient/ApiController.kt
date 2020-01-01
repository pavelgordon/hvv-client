package dev.pgordon.hvvclient

import mu.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}
@RestController
class ApiController(
    val geofoxClient: GeofoxClient
) {
    @GetMapping("/api/departures")
    fun getSchedule(station: String, filter: String): List<Departure> {
        logger.info { "Request station=$station, filter=$filter" }
        return geofoxClient.getDepartures(station, filter)
    }

    @GetMapping("/api/departures/full")
    fun getScheduleFull(station: String): String {

        logger.info { "Request full, station=$station" }

        return geofoxClient.getDeparturesFull(station)
    }
}