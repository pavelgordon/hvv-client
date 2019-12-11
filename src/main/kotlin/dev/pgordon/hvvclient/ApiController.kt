package dev.pgordon.hvvclient

import org.json.JSONObject
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ApiController(
    val geofoxClient: GeofoxClient
) {
    @GetMapping("/api/departures")
    fun getSchedule(station: String, filter: String) = geofoxClient.getDepartures(station, filter)
    @GetMapping("/api/departures/full")
    fun getScheduleFull(station: String) = geofoxClient.getDeparturesFull(station)
}