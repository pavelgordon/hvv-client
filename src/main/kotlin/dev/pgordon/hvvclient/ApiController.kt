package dev.pgordon.hvvclient

import mu.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}
@RestController
class ApiController(val geofoxClient: GeofoxClient) {
    @GetMapping("/api/departures")
    fun getSchedule(station: String, filter: String): List<Departure> {
        logger.info { "Request getSchedule station=$station, filter=$filter" }
        return geofoxClient.getDepartures(station, filter.toLowerCase())
    }

    /**
     * Filters by line name or line direction
     */
    @GetMapping("/api/departures/full")
    fun getScheduleFull(station: String, filter: String): List<FatDeparture> {

        logger.info { "Request getScheduleFull, station=$station, filter=$filter" }

        return geofoxClient.getDeparturesFull(station, filter.toLowerCase())
    }
}