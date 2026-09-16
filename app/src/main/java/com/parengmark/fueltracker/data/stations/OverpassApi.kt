package com.parengmark.fueltracker.data.stations

import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Nearby fuel stations via OpenStreetMap's Overpass API -- free, keyless, no
 * Google Cloud billing account needed (see the spec's §2's discussion of why
 * a keyless station locator is the realistic v1 approach vs. a paid places
 * API or a fully crowdsourced price backend). Station *locations* only; no
 * live prices are available from this source -- the user can attach the
 * price they see to their own log via the existing "log fill-up here" flow.
 */
object OverpassApi {
    private const val ENDPOINT = "https://overpass-api.de/api/interpreter"

    suspend fun findNearbyFuelStations(
        lat: Double,
        lon: Double,
        radiusMeters: Int = 5000
    ): List<FuelStation> = withContext(Dispatchers.IO) {
        val query = """
            [out:json][timeout:20];
            node["amenity"="fuel"](around:$radiusMeters,$lat,$lon);
            out body;
        """.trimIndent()

        var connection: HttpURLConnection? = null
        try {
            val url = URL("$ENDPOINT?data=${URLEncoder.encode(query, "UTF-8")}")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 15_000
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext emptyList()
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            parseResponse(text, lat, lon)
        } catch (e: Exception) {
            emptyList()
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseResponse(json: String, originLat: Double, originLon: Double): List<FuelStation> {
        val root = JSONObject(json)
        val elements = root.optJSONArray("elements") ?: return emptyList()
        val stations = mutableListOf<FuelStation>()

        for (i in 0 until elements.length()) {
            val el = elements.optJSONObject(i) ?: continue
            val stationLat = el.optDouble("lat", Double.NaN)
            val stationLon = el.optDouble("lon", Double.NaN)
            if (stationLat.isNaN() || stationLon.isNaN()) continue

            val tags = el.optJSONObject("tags")
            val name = tags?.optString("name")?.takeIf { it.isNotBlank() }
                ?: tags?.optString("brand")?.takeIf { it.isNotBlank() }
                ?: "Fuel station"

            val results = FloatArray(1)
            Location.distanceBetween(originLat, originLon, stationLat, stationLon, results)

            stations += FuelStation(
                name = name,
                lat = stationLat,
                lon = stationLon,
                distanceMeters = results[0]
            )
        }
        return stations.sortedBy { it.distanceMeters }
    }
}
