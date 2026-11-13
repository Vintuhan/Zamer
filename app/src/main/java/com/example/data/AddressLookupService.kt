package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.example.domain.ApartmentTemplate
import com.example.domain.ApartmentTemplates
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.hypot

data class OsmBuildingInfo(
    val queryAddress: String,
    val foundOnMap: Boolean,
    val displayName: String,
    val lat: Double? = null,
    val lon: Double? = null,
    val levels: String? = null,
    val yearBuilt: String? = null,
    val material: String? = null,
    val seriesTag: String? = null,
    val footprintShape: String? = null,
    val hasSlantedOrComplexFootprint: Boolean = false,
    val wikiHint: String? = null,
    val networkNote: String? = null
)

data class MinZhkhBuildingInfo(
    val found: Boolean = false,
    val matchedAddress: String? = null,
    val yearBuilt: String? = null,
    val levels: String? = null,
    val wallMaterial: String? = null,
    val floorMaterial: String? = null,
    val apartmentsCount: String? = null,
    val series: String? = null,
    val searchUrl: String,
    val note: String? = null
)

data class PhotoBuildingsInfo(
    val found: Boolean = false,
    val matchedTitle: String? = null,
    val projectCode: String? = null,
    val series: String? = null,
    val yearBuilt: String? = null,
    val levels: String? = null,
    val architectureStyle: String? = null,
    val searchUrl: String,
    val note: String? = null
)

data class AiTemplateMatch(
    val template: ApartmentTemplate,
    val matchPercent: Int,
    val reason: String
)

data class AddressLayoutLookupResult(
    val address: String,
    val osmInfo: OsmBuildingInfo,
    val minZhkhInfo: MinZhkhBuildingInfo,
    val photoBuildingsInfo: PhotoBuildingsInfo,
    val houseSummary: String,
    val probableSeries: String,
    val yearBuiltDisplay: String,
    val wallAndFloorDisplay: String,
    val geometryNote: String,
    val recommendations: List<AiTemplateMatch>
)

sealed interface AddressLookupUiState {
    data object Idle : AddressLookupUiState
    data class Loading(val stage: String) : AddressLookupUiState
    data class Success(val result: AddressLayoutLookupResult) : AddressLookupUiState
    data class Error(val message: String) : AddressLookupUiState
}

internal data class ParsedStreetHouse(
    val city: String,
    val streetCore: String,
    val houseNumber: String
)

internal fun parseAddressParts(rawAddress: String): ParsedStreetHouse {
    val clean = rawAddress.trim()
    val city = when {
        "кимовск" in clean.lowercase() -> "Кимовск"
        else -> clean.substringBefore(",").trim()
    }
    val tokens = clean.split(Regex("[,;\\s]+"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    val houseNum = tokens.lastOrNull { tok ->
        tok.any { it.isDigit() } &&
            !tok.equals("1-228", ignoreCase = true) &&
            !tok.startsWith("д.", ignoreCase = true)
    }?.removePrefix("д.")?.removePrefix("Д.")
        ?: tokens.firstOrNull { it.startsWith("д.", ignoreCase = true) && it.any(Char::isDigit) }
            ?.drop(2).orEmpty()

    val ignored = setOf("г.", "г", "город", "ул.", "ул", "улица", "пр.", "пр", "проезд", "пер.", "переулок", "д.", "д", "дом", "тульская", "обл", "область", "россия", "рф")
    val streetTokens = tokens.filter { tok ->
        val low = tok.lowercase()
        low !in ignored &&
            low != houseNum.lowercase() &&
            !low.startsWith("кимовск") &&
            tok.any { it.isLetter() }
    }
    val streetCore = streetTokens.maxByOrNull { it.length }
        ?.lowercase()
        ?.take(7)
        .orEmpty()

    return ParsedStreetHouse(
        city = city,
        streetCore = streetCore,
        houseNumber = houseNum.lowercase()
    )
}

internal fun stripHtmlTags(html: String): String =
    html.replace(Regex("(?is)<script[^>]*>.*?</script>"), " ")
        .replace(Regex("(?is)<style[^>]*>.*?</style>"), " ")
        .replace(Regex("<[^>]+>"), " ")
        .replace("&nbsp;", " ")
        .replace("&laquo;", "«")
        .replace("&raquo;", "»")
        .replace("&quot;", "\"")
        .replace("&amp;", "&")
        .replace(Regex("\\s+"), " ")
        .trim()

private const val HTTP_BROWSER_UA =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

private fun fetchHttpText(urlStr: String, timeoutMs: Int = 8_000, accept: String = "text/html,application/json"): String {
    val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        instanceFollowRedirects = true
        connectTimeout = timeoutMs
        readTimeout = timeoutMs
        setRequestProperty("User-Agent", HTTP_BROWSER_UA)
        setRequestProperty("Accept", accept)
        setRequestProperty("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.5")
    }
    return try {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { r -> r.readText() } }.orEmpty()
        if (code !in 200..299) throw IllegalStateException("HTTP $code")
        text
    } finally {
        conn.disconnect()
    }
}

/**
 * Queries МинЖКХ (dom.mingkh.ru) registry by address (with dedicated Кимовск registry fallback)
 * to extract год постройки, этажность, тип перекрытий, материал стен, кол-во квартир и серию.
 */
object MinZhkhLookupClient {
    private const val TAG = "MinZhkhLookup"

    fun buildSearchUrl(address: String): String {
        val clean = address.trim()
        return if ("кимовск" in clean.lowercase() && clean.length < 10) {
            "https://dom.mingkh.ru/tulskaya-oblast/kimovsk/"
        } else {
            "https://dom.mingkh.ru/search?address=${URLEncoder.encode(clean, "UTF-8")}"
        }
    }

    suspend fun lookupBuilding(address: String): MinZhkhBuildingInfo = withContext(Dispatchers.IO) {
        val clean = address.trim()
        val searchUrl = buildSearchUrl(clean)
        if (clean.length < 4) {
            return@withContext MinZhkhBuildingInfo(searchUrl = searchUrl)
        }
        val parts = parseAddressParts(clean)
        try {
            val encoded = URLEncoder.encode(clean, "UTF-8")
            val candidateSearchUrls = buildList {
                add("https://dom.mingkh.ru/search?address=$encoded")
                if ("кимовск" in clean.lowercase()) {
                    add("https://dom.mingkh.ru/tulskaya-oblast/kimovsk/")
                }
            }

            for (url in candidateSearchUrls) {
                val html = runCatching { fetchHttpText(url, timeoutMs = 7_500) }.getOrNull() ?: continue
                val housePath = findMatchingHouseHref(html, parts)
                if (housePath != null) {
                    val fullHouseUrl = if (housePath.startsWith("http")) housePath else "https://dom.mingkh.ru$housePath"
                    val detailHtml = runCatching { fetchHttpText(fullHouseUrl, timeoutMs = 7_500) }.getOrNull()
                    if (!detailHtml.isNullOrBlank()) {
                        val parsed = parseMinZhkhPassportHtml(detailHtml, fullHouseUrl)
                        if (parsed.found) return@withContext parsed
                    }
                }
                // Sometimes search redirects directly to the passport page
                val direct = parseMinZhkhPassportHtml(html, url)
                if (direct.found) return@withContext direct
            }

            MinZhkhBuildingInfo(
                found = false,
                searchUrl = searchUrl,
                note = "Анкета дома не получена напрямую с МинЖКХ — можно открыть реестр кнопкой ниже."
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "MinZHKKh lookup skipped: ${e.message}")
            MinZhkhBuildingInfo(
                found = false,
                searchUrl = searchUrl,
                note = "МинЖКХ не ответил автоматически (${e.localizedMessage ?: "ограничение сети"}) — ссылка доступна ниже."
            )
        }
    }

    internal fun findMatchingHouseHref(html: String, parts: ParsedStreetHouse): String? {
        if (parts.streetCore.isBlank() && parts.houseNumber.isBlank()) return null
        val linkRegex = Regex("""<a\s+[^>]*href=["']([^"']*?/tulskaya-oblast/[^"']+|/[^"']+/\d+/?)["'][^>]*>(.*?)</a>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        for (m in linkRegex.findAll(html)) {
            val href = m.groupValues[1]
            val text = stripHtmlTags(m.groupValues[2]).lowercase()
            val streetOk = parts.streetCore.isBlank() || parts.streetCore in text || parts.streetCore in href.lowercase()
            val houseOk = parts.houseNumber.isBlank() || Regex("""\b${Regex.escape(parts.houseNumber)}\b""").containsMatchIn(text)
            if (streetOk && houseOk && href.count { it == '/' } >= 3) {
                return href
            }
        }
        return null
    }

    internal fun parseMinZhkhPassportHtml(html: String, sourceUrl: String): MinZhkhBuildingInfo {
        val yearBuilt = extractLabeledValue(html, listOf("Год постройки", "Год ввода в эксплуатацию"))
            ?.let { Regex("""\b(19\d\d|20[0-2]\d)\b""").find(it)?.value ?: it }
        val levels = extractLabeledValue(html, listOf("Количество этажей", "Этажность"))
            ?.let { raw -> Regex("""\d+""").find(raw)?.value?.let { "$it эт." } ?: raw }
        val wallMaterial = extractLabeledValue(html, listOf("Материал несущих стен", "Материал стен"))
        val floorMaterial = extractLabeledValue(html, listOf("Тип перекрытий", "Перекрытия"))
        val apartmentsCount = extractLabeledValue(html, listOf("Количество жилых помещений", "Количество квартир", "Квартир"))
            ?.let { raw -> Regex("""\d+""").find(raw)?.value ?: raw }
        val series = extractLabeledValue(html, listOf("Серия, тип постройки здания", "Серия, тип постройки", "Серия"))
        val addressTitle = Regex("""<h1[^>]*>(.*?)</h1>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(html)?.groupValues?.get(1)?.let(::stripHtmlTags)?.takeIf { it.isNotBlank() }

        val hasAny = listOf(yearBuilt, levels, wallMaterial, floorMaterial, series, apartmentsCount).any { !it.isNullOrBlank() }
        return MinZhkhBuildingInfo(
            found = hasAny,
            matchedAddress = addressTitle,
            yearBuilt = yearBuilt,
            levels = levels,
            wallMaterial = wallMaterial,
            floorMaterial = floorMaterial,
            apartmentsCount = apartmentsCount,
            series = series,
            searchUrl = sourceUrl
        )
    }

    private fun extractLabeledValue(html: String, labels: List<String>): String? {
        for (label in labels) {
            // Pattern 1: <td>Label</td><td>Value</td> or <dt>Label</dt><dd>Value</dd>
            val rowRegex = Regex(
                """<(?:td|th|dt|div|span)[^>]*>[^<]*${Regex.escape(label)}[^<]*</(?:td|th|dt|div|span)>\s*<(?:td|dd|div|span)[^>]*>(.*?)</(?:td|dd|div|span)>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            )
            val match = rowRegex.find(html)?.groupValues?.get(1)?.let(::stripHtmlTags)
            if (!match.isNullOrBlank() && match != "—" && match != "-" && match.length <= 90) {
                return match
            }
        }
        return null
    }
}

/**
 * Queries PhotoBuildings architectural catalog (photobuildings.com) for exact building series & project code
 * (e.g., 1-447, 1-228, 1-251, 86 in Kimovsk and other towns).
 */
object PhotoBuildingsLookupClient {
    private const val TAG = "PhotoBuildingsLookup"

    fun buildSearchUrl(address: String): String {
        val q = address.trim()
        return "https://photobuildings.com/search.php?name=${URLEncoder.encode(q, "UTF-8")}"
    }

    suspend fun lookupBuilding(address: String): PhotoBuildingsInfo = withContext(Dispatchers.IO) {
        val clean = address.trim()
        val searchUrl = buildSearchUrl(clean)
        if (clean.length < 4) {
            return@withContext PhotoBuildingsInfo(searchUrl = searchUrl)
        }
        val parts = parseAddressParts(clean)
        val queryText = listOfNotNull(
            parts.city.takeIf { it.isNotBlank() },
            parts.streetCore.takeIf { it.isNotBlank() },
            parts.houseNumber.takeIf { it.isNotBlank() }
        ).joinToString(" ").ifBlank { clean }

        val requestUrl = "https://photobuildings.com/search.php?name=${URLEncoder.encode(queryText, "UTF-8")}"
        try {
            val html = fetchHttpText(requestUrl, timeoutMs = 8_000)
            val parsed = parsePhotoBuildingsHtml(html, parts, requestUrl)
            if (parsed.found) return@withContext parsed

            PhotoBuildingsInfo(
                found = false,
                searchUrl = requestUrl,
                note = "Прямой проект в PhotoBuildings не найден по строке поиска — откройте каталог по кнопке ниже."
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "PhotoBuildings lookup skipped: ${e.message}")
            PhotoBuildingsInfo(
                found = false,
                searchUrl = requestUrl,
                note = "PhotoBuildings не ответил автоматически (${e.localizedMessage ?: "ограничение сети"}) — ссылка доступна ниже."
            )
        }
    }

    internal fun parsePhotoBuildingsHtml(
        html: String,
        parts: ParsedStreetHouse,
        sourceUrl: String
    ): PhotoBuildingsInfo {
        val plain = stripHtmlTags(html)
        // Extract project / series patterns like "1-447", "1-447С-37", "1-228-1", "1-251-13", "П-44"
        val projectCodeRegex = Regex("""(?:Проект|Серия|Тип)[:\s]*([А-ЯA-Z0-9][А-Яа-яA-Za-z0-9\-/()]{1,28})""")
        val matchedProject = projectCodeRegex.find(plain)?.groupValues?.get(1)?.trim()

        val seriesGuess = Regex("""\b(1-\d{3}(?:[А-Яа-яA-Za-z0-9\-/]+)?|II-\d{2}(?:-\d+)?|П-\d{2}[ТМ]?|Б-\d+-\d+)\b""")
            .find(plain)?.value

        val yearGuess = Regex("""(?:Построено|Построен|Год постройки)[:\s]*(\d{4})""")
            .find(plain)?.groupValues?.get(1)

        val levelsGuess = Regex("""(?:Этажность|Этажей)[:\s]*(\d+)""")
            .find(plain)?.groupValues?.get(1)?.let { "$it эт." }

        val objLinkMatch = Regex("""<a\s+[^>]*href=["']([^"']*object/\d+/?[^"']*)["'][^>]*>(.*?)</a>""", RegexOption.IGNORE_CASE)
            .findAll(html)
            .firstOrNull { m ->
                val txt = stripHtmlTags(m.groupValues[2]).lowercase()
                (parts.streetCore.isBlank() || parts.streetCore in txt) &&
                    (parts.houseNumber.isBlank() || parts.houseNumber in txt)
            }
        val title = objLinkMatch?.groupValues?.get(2)?.let(::stripHtmlTags)
        val objUrl = objLinkMatch?.groupValues?.get(1)?.let { href ->
            if (href.startsWith("http")) href else "https://photobuildings.com/${href.trimStart('/')}"
        } ?: sourceUrl

        val finalProject = matchedProject ?: seriesGuess
        val hasMatch = !finalProject.isNullOrBlank() || !title.isNullOrBlank()

        return PhotoBuildingsInfo(
            found = hasMatch,
            matchedTitle = title,
            projectCode = finalProject,
            series = seriesGuess ?: matchedProject,
            yearBuilt = yearGuess,
            levels = levelsGuess,
            searchUrl = objUrl
        )
    }
}

/**
 * Fetches real building metadata & top-down footprint geometry from OpenStreetMap
 * (Nominatim geocoding + Overpass building polygon geometry) and optional RuWiki series context.
 */
object OsmHouseLookupClient {
    private const val TAG = "OsmHouseLookup"
    private const val USER_AGENT = "ZamershchikApp/1.0 (Android floorplan lookup)"

    suspend fun lookupBuildingByAddress(address: String): OsmBuildingInfo = withContext(Dispatchers.IO) {
        val cleanAddress = address.trim()
        if (cleanAddress.length < 3) {
            return@withContext OsmBuildingInfo(
                queryAddress = cleanAddress,
                foundOnMap = false,
                displayName = cleanAddress,
                networkNote = "Адрес слишком короткий для поиска на карте"
            )
        }

        try {
            val encoded = URLEncoder.encode(cleanAddress, "UTF-8")
            val nominatimUrl =
                "https://nominatim.openstreetmap.org/search?q=$encoded&format=jsonv2&addressdetails=1&extratags=1&limit=1&accept-language=ru"
            val nomBody = httpGet(nominatimUrl, timeoutMs = 9_000)
            val nomArray = JSONArray(nomBody)
            if (nomArray.length() == 0) {
                return@withContext OsmBuildingInfo(
                    queryAddress = cleanAddress,
                    foundOnMap = false,
                    displayName = cleanAddress,
                    networkNote = "Точный дом не найден в OpenStreetMap"
                )
            }

            val hit = nomArray.getJSONObject(0)
            val displayName = hit.optString("display_name", cleanAddress)
            val lat = hit.optString("lat").toDoubleOrNull()
            val lon = hit.optString("lon").toDoubleOrNull()
            val extratags = hit.optJSONObject("extratags")

            var levels = extratags?.optString("building:levels")?.takeIf { it.isNotBlank() }
            var yearBuilt = extratags?.optString("start_date")?.takeIf { it.isNotBlank() }
                ?: extratags?.optString("building:year")?.takeIf { it.isNotBlank() }
            var material = translateMaterial(extratags?.optString("building:material"))
            var seriesTag = extractSeriesTag(extratags)
            var footprintShape: String? = null
            var hasComplexFootprint = false

            if (lat != null && lon != null) {
                runCatching {
                    val overpassQuery =
                        "[out:json][timeout:8];way(around:45,$lat,$lon)[\"building\"];out tags geom 1;"
                    val ovBody = httpPostForm(
                        "https://overpass-api.de/api/interpreter",
                        "data=${URLEncoder.encode(overpassQuery, "UTF-8")}",
                        timeoutMs = 9_000
                    )
                    val elements = JSONObject(ovBody).optJSONArray("elements")
                    if (elements != null && elements.length() > 0) {
                        val way = elements.getJSONObject(0)
                        val tags = way.optJSONObject("tags")
                        if (levels.isNullOrBlank()) {
                            levels = tags?.optString("building:levels")?.takeIf { it.isNotBlank() }
                        }
                        if (yearBuilt.isNullOrBlank()) {
                            yearBuilt = tags?.optString("start_date")?.takeIf { it.isNotBlank() }
                                ?: tags?.optString("building:year")?.takeIf { it.isNotBlank() }
                        }
                        if (material.isNullOrBlank()) {
                            material = translateMaterial(tags?.optString("building:material"))
                        }
                        if (seriesTag.isNullOrBlank()) {
                            seriesTag = extractSeriesTag(tags)
                        }
                        val geom = way.optJSONArray("geometry")
                        if (geom != null && geom.length() >= 4) {
                            val shapeInfo = analyzeBuildingPolygon(geom, lat)
                            footprintShape = shapeInfo.first
                            hasComplexFootprint = shapeInfo.second
                        }
                    }
                }.onFailure { e ->
                    Log.w(TAG, "Overpass geometry lookup skipped: ${e.message}")
                }
            }

            val wikiHint = if (!seriesTag.isNullOrBlank()) {
                runCatching { fetchRuWikiHint(seriesTag) }.getOrNull()
            } else null

            OsmBuildingInfo(
                queryAddress = cleanAddress,
                foundOnMap = true,
                displayName = displayName,
                lat = lat,
                lon = lon,
                levels = levels?.let { if (it.endsWith("эт.")) it else "$it эт." },
                yearBuilt = yearBuilt,
                material = material,
                seriesTag = seriesTag,
                footprintShape = footprintShape,
                hasSlantedOrComplexFootprint = hasComplexFootprint,
                wikiHint = wikiHint
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "OSM lookup unavailable: ${e.message}")
            OsmBuildingInfo(
                queryAddress = cleanAddress,
                foundOnMap = false,
                displayName = cleanAddress,
                networkNote = "OpenStreetMap временно недоступен (${e.localizedMessage ?: "ошибка сети"})"
            )
        }
    }

    private fun fetchRuWikiHint(series: String): String? {
        val q = URLEncoder.encode("$series типовая серия", "UTF-8")
        val url = "https://ru.wikipedia.org/w/api.php?action=query&list=search&srsearch=$q&format=json&utf8=1&srlimit=1"
        val body = httpGet(url, timeoutMs = 6_000)
        val hits = JSONObject(body).optJSONObject("query")?.optJSONArray("search") ?: return null
        if (hits.length() == 0) return null
        val snippet = stripHtmlTags(hits.getJSONObject(0).optString("snippet"))
        return snippet.takeIf { it.isNotBlank() }?.take(220)
    }

    private fun extractSeriesTag(tags: JSONObject?): String? {
        if (tags == null) return null
        return listOf("design:ref", "building:series", "ref:series", "architecture", "building:architecture")
            .asSequence()
            .map { tags.optString(it).trim() }
            .firstOrNull { it.isNotBlank() }
    }

    private fun translateMaterial(raw: String?): String? {
        val m = raw?.trim()?.lowercase() ?: return null
        return when {
            m.isBlank() -> null
            "brick" in m -> "кирпич"
            "panel" in m || "concrete_slab" in m -> "панель"
            "monolith" in m || "cast_in_place" in m || "reinforced" in m -> "монолит / ж/б"
            "block" in m -> "блочный / шлакоблок"
            "wood" in m -> "дерево"
            else -> raw
        }
    }

    internal fun analyzeBuildingPolygon(geom: JSONArray, refLat: Double): Pair<String, Boolean> {
        val latLon = buildList {
            for (i in 0 until geom.length()) {
                val pt = geom.getJSONObject(i)
                add(pt.getDouble("lat") to pt.getDouble("lon"))
            }
        }
        return analyzeLatLonPolygon(latLon, refLat)
    }

    internal fun analyzeLatLonPolygon(latLon: List<Pair<Double, Double>>, refLat: Double): Pair<String, Boolean> {
        val cosLat = cos(Math.toRadians(refLat)).coerceAtLeast(0.1)
        val rawPts = mutableListOf<Pair<Double, Double>>()
        for ((lat, lon) in latLon) {
            val x = lon * 111_320.0 * cosLat
            val y = lat * 110_540.0
            if (rawPts.isEmpty() || hypot(x - rawPts.last().first, y - rawPts.last().second) > 0.6) {
                rawPts.add(x to y)
            }
        }
        if (rawPts.size >= 2 && hypot(rawPts.first().first - rawPts.last().first, rawPts.first().second - rawPts.last().second) < 0.8) {
            rawPts.removeAt(rawPts.lastIndex)
        }
        if (rawPts.size < 3) return "Контур здания не определён" to false

        val n = rawPts.size
        var trueCorners = 0
        var nonRightAngles = 0
        for (i in 0 until n) {
            val prev = rawPts[(i - 1 + n) % n]
            val cur = rawPts[i]
            val next = rawPts[(i + 1) % n]
            val v1x = cur.first - prev.first
            val v1y = cur.second - prev.second
            val v2x = next.first - cur.first
            val v2y = next.second - cur.second
            val l1 = hypot(v1x, v1y)
            val l2 = hypot(v2x, v2y)
            if (l1 < 0.5 || l2 < 0.5) continue
            val dot = ((v1x * v2x + v1y * v2y) / (l1 * l2)).coerceIn(-1.0, 1.0)
            val turnDeg = Math.toDegrees(acos(dot))
            if (turnDeg < 8.0 || turnDeg > 172.0) continue
            trueCorners++
            if (abs(turnDeg - 90.0) > 12.0) {
                nonRightAngles++
            }
        }

        return when {
            nonRightAngles >= 1 ->
                "Непрямоугольный корпус сверху ($trueCorners углов, есть косые/эркерные углы ≠ 90°)" to true
            trueCorners >= 6 ->
                "Г- или П-образный ступенчатый корпус сверху ($trueCorners углов)" to true
            else ->
                "Прямоугольный корпус (4 прямых угла)" to false
        }
    }

    private fun httpGet(urlStr: String, timeoutMs: Int): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        return try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            if (code !in 200..299) throw IllegalStateException("HTTP $code")
            text
        } finally {
            conn.disconnect()
        }
    }

    private fun httpPostForm(urlStr: String, body: String, timeoutMs: Int): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        }
        return try {
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            if (code !in 200..299) throw IllegalStateException("HTTP $code")
            text
        } finally {
            conn.disconnect()
        }
    }
}

data class CombinedHouseEvidence(
    val osm: OsmBuildingInfo,
    val minZhkh: MinZhkhBuildingInfo,
    val photoBuildings: PhotoBuildingsInfo
)

/**
 * Combines OpenStreetMap + МинЖКХ + PhotoBuildings lookup with Firebase AI Logic (gemini-3.5-flash-lite)
 * to deduce year built, wall/floor materials, series/project (e.g., 1-447, 1-228 in Kimovsk),
 * top-down corner geometry (angled walls, bay windows, L-shapes), and best matching catalog layouts.
 */
class FirebaseAddressLayoutAiRepository {

    private val responseSchema = Schema.obj(
        properties = mapOf(
            "houseSummary" to Schema.string(
                description = "Подробное описание дома по адресу: этажность, количество квартир, конструктивные особенности района/города"
            ),
            "probableSeries" to Schema.string(
                description = "Серия и точный шифр проекта дома (например: 1-228 (проект 1-228-1), 1-447, П-44, сталинка, монолит)"
            ),
            "yearBuilt" to Schema.string(
                description = "Год постройки или ориентировочный период строительства (например: 1956 г. или конец 1950-х)"
            ),
            "wallAndFloor" to Schema.string(
                description = "Материал стен и тип перекрытий (например: Шлакоблок, деревянные перекрытия или Кирпич, ж/б перекрытия)"
            ),
            "geometryNote" to Schema.string(
                description = "Геометрия квартир: косые стены, углы 45°, эркеры, Г-коридоры, угловые или распашные планировки"
            ),
            "recommendations" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "templateId" to Schema.string(description = "Точный id планировки из каталога"),
                        "matchPercent" to Schema.integer(description = "Вероятность совпадения от 10 до 99"),
                        "reason" to Schema.string(description = "Короткое объяснение на русском, почему планировка подходит под серию/дом")
                    )
                ),
                description = "От 3 до 5 лучших планировок из каталога по убыванию вероятности"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = this@FirebaseAddressLayoutAiRepository.responseSchema
            }
        )
    }

    suspend fun collectHouseEvidence(address: String): CombinedHouseEvidence = coroutineScope {
        val osmDeferred = async { OsmHouseLookupClient.lookupBuildingByAddress(address) }
        val minZhkhDeferred = async { MinZhkhLookupClient.lookupBuilding(address) }
        val photoDeferred = async { PhotoBuildingsLookupClient.lookupBuilding(address) }
        CombinedHouseEvidence(
            osm = osmDeferred.await(),
            minZhkh = minZhkhDeferred.await(),
            photoBuildings = photoDeferred.await()
        )
    }

    suspend fun analyzeAddressAndMatchLayouts(
        address: String,
        roomCountHint: Int?,
        evidence: CombinedHouseEvidence
    ): Result<AddressLayoutLookupResult> {
        val osmInfo = evidence.osm
        val minZhkh = evidence.minZhkh
        val pb = evidence.photoBuildings

        val catalogLines = ApartmentTemplates.allTemplates.joinToString("\n") { tpl ->
            val shapeTag = if (tpl.hasNonRectangularShape) "непрямоугольная/косые/эркер/Г-форма" else "обычная"
            val roomsLabel = when (tpl.roomCountCategory) {
                0 -> "студия"
                4 -> "4+ комн."
                else -> "${tpl.roomCountCategory}-комн."
            }
            "- id=\"${tpl.id}\" | ${tpl.title} ($roomsLabel, $shapeTag) | ${tpl.subtitle}"
        }

        val combinedContext = buildString {
            appendLine("=== 1. ДАННЫЕ OPENSTREETMAP / OVERPASS ===")
            appendLine("Найден в OSM: ${if (osmInfo.foundOnMap) "да (${osmInfo.displayName})" else "нет"}")
            osmInfo.levels?.let { appendLine("Этажность OSM: $it") }
            osmInfo.yearBuilt?.let { appendLine("Год постройки OSM: $it") }
            osmInfo.material?.let { appendLine("Материал стен OSM: $it") }
            osmInfo.seriesTag?.let { appendLine("Серия OSM: $it") }
            osmInfo.footprintShape?.let { appendLine("Форма дома сверху (анализ полигона OSM): $it") }
            osmInfo.wikiHint?.let { appendLine("Справка Википедии по серии: $it") }
            osmInfo.networkNote?.let { appendLine("Примечание OSM: $it") }

            appendLine()
            appendLine("=== 2. РЕЕСТР МИНЖКХ (dom.mingkh.ru) ===")
            appendLine("Найдена анкета: ${if (minZhkh.found) "ДА" else "нет прямой анкеты"}")
            minZhkh.matchedAddress?.let { appendLine("Адрес в МинЖКХ: $it") }
            minZhkh.yearBuilt?.let { appendLine("Год постройки (МинЖКХ): $it") }
            minZhkh.levels?.let { appendLine("Этажность (МинЖКХ): $it") }
            minZhkh.wallMaterial?.let { appendLine("Материал несущих стен (МинЖКХ): $it") }
            minZhkh.floorMaterial?.let { appendLine("Тип перекрытий (МинЖКХ): $it") }
            minZhkh.apartmentsCount?.let { appendLine("Количество квартир (МинЖКХ): $it") }
            minZhkh.series?.let { appendLine("Шифр серии (МинЖКХ): $it") }
            minZhkh.note?.let { appendLine("Примечание МинЖКХ: $it") }

            appendLine()
            appendLine("=== 3. АРХИТЕКТУРНАЯ БАЗА PHOTOBUILDINGS (photobuildings.com) ===")
            appendLine("Найдена запись: ${if (pb.found) "ДА" else "нет точного совпадения"}")
            pb.matchedTitle?.let { appendLine("Объект: $it") }
            pb.projectCode?.let { appendLine("Проект/шифр (PhotoBuildings): $it") }
            pb.series?.let { appendLine("Серия (PhotoBuildings): $it") }
            pb.yearBuilt?.let { appendLine("Год (PhotoBuildings): $it") }
            pb.levels?.let { appendLine("Этажей (PhotoBuildings): $it") }
            pb.note?.let { appendLine("Примечание PhotoBuildings: $it") }
        }.trim()

        val filterHint = when (roomCountHint) {
            0 -> "Пользователь ищет преимущественно студию."
            1, 2, 3 -> "Пользователь указал комнатность: $roomCountHint-комн."
            4 -> "Пользователь ищет 4+ комнатную квартиру."
            else -> "Комнатность любая (покажи самые вероятные варианты для этого дома)."
        }

        val prompt = """
Ты опытный архитектор-сметчик и эксперт по типовым проектам СССР, России и конкретно жилого фонда Тульской области (включая г. Кимовск).
Алгоритм анализа конкретного здания по адресу:
1) Сначала определи год постройки, этажность, материал стен, тип перекрытий, количество квартир и точный шифр серии/проекта (опираясь на полученные данные из МинЖКХ, PhotoBuildings и OSM).
   • Для Кимовска особенно учитывай характерную застройку: кирпичные хрущёвки серии 1-447 (и 1-447С), 2–3-этажные сталинские/«немецкие» шлакоблочные или кирпичные дома серии 1-228 (проекты 1-228-1, 1-228-2, 1-228-3 и т.д. с деревянными перекрытиями и потолками ~2.9 м), серию 1-251 (напр., 1-251-13), серию 86, панельные 1-464 / 90 / 121.
2) Проанализируй форму корпуса сверху и углы (по данным OSM и серии): бывают ли в доме косые углы 45°, эркеры, Г-образные коридоры или выступающие торцевые комнаты.
3) Выбери от 3 до 5 лучших совпадений `templateId` СТРОГО из списка каталога ниже.

Адрес: "$address"
$filterHint

Собранные данные из интернет-баз:
$combinedContext

Доступный каталог планировок (используй ТОЛЬКО эти templateId):
$catalogLines
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("ИИ вернул пустой ответ. Попробуйте ещё раз."))

            val json = JSONObject(rawJson)
            val houseSummary = json.optString("houseSummary").ifBlank { "Анализ дома по адресу: $address" }
            val probableSeries = json.optString("probableSeries").ifBlank {
                pb.projectCode ?: minZhkh.series ?: osmInfo.seriesTag ?: "Серия уточняется"
            }
            val yearBuilt = json.optString("yearBuilt").ifBlank {
                minZhkh.yearBuilt ?: pb.yearBuilt ?: osmInfo.yearBuilt ?: "Уточняется"
            }
            val wallAndFloor = json.optString("wallAndFloor").ifBlank {
                listOfNotNull(
                    minZhkh.wallMaterial ?: osmInfo.material,
                    minZhkh.floorMaterial?.let { "перекрытия: $it" }
                ).joinToString(", ").ifBlank { "Уточняется" }
            }
            val geometryNote = json.optString("geometryNote").ifBlank {
                osmInfo.footprintShape ?: "Учитывайте возможные выступы, ниши и непрямые углы при замере."
            }

            val recArray = json.optJSONArray("recommendations") ?: JSONArray()
            val matches = mutableListOf<AiTemplateMatch>()
            val seenIds = mutableSetOf<String>()
            for (i in 0 until recArray.length()) {
                val obj = recArray.getJSONObject(i)
                val tplId = obj.optString("templateId").trim()
                val tpl = ApartmentTemplates.findById(tplId) ?: continue
                if (!seenIds.add(tplId)) continue
                val pct = obj.optInt("matchPercent", 75).coerceIn(10, 99)
                val reason = obj.optString("reason").ifBlank { tpl.subtitle }
                matches.add(AiTemplateMatch(template = tpl, matchPercent = pct, reason = reason))
            }

            if (matches.isEmpty()) {
                return Result.failure(IllegalStateException("ИИ не смог сопоставить планировки из каталога. Уточните адрес (город, улица, номер дома)."))
            }

            Result.success(
                AddressLayoutLookupResult(
                    address = address,
                    osmInfo = osmInfo,
                    minZhkhInfo = minZhkh,
                    photoBuildingsInfo = pb,
                    houseSummary = houseSummary,
                    probableSeries = probableSeries,
                    yearBuiltDisplay = yearBuilt,
                    wallAndFloorDisplay = wallAndFloor,
                    geometryNote = geometryNote,
                    recommendations = matches.sortedByDescending { it.matchPercent }
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Address lookup AI timed out", e)
            Result.failure(IllegalStateException("Превышено время ожидания ИИ (60 сек). Проверьте интернет и повторите."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Address lookup AI failed", e)
            Result.failure(IllegalStateException(e.localizedMessage ?: "Не удалось выполнить ИИ-поиск по адресу."))
        }
    }

    companion object {
        private const val TAG = "AddressAiRepo"
    }
}

data class SketchMatchResult(
    val sketchSummary: String,
    val recommendations: List<AiTemplateMatch>
)

sealed interface SketchMatchUiState {
    data object Idle : SketchMatchUiState
    data object Loading : SketchMatchUiState
    data class Success(val result: SketchMatchResult) : SketchMatchUiState
    data class Error(val message: String) : SketchMatchUiState
}

/**
 * Multimodal Gemini AI repository that analyzes a Bitmap of the user's hand-drawn sketch
 * and suggests the most likely apartment layouts from the template catalog.
 */
class SketchLayoutAiRepository {

    private val schema = Schema.obj(
        properties = mapOf(
            "sketchSummary" to Schema.string(
                description = "Что ИИ распознал на эскизе: форма квартиры, число комнат, углы/эркеры/лоджия"
            ),
            "recommendations" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "templateId" to Schema.string(description = "Точный id планировки из каталога"),
                        "matchPercent" to Schema.integer(description = "Совпадение с рисунком от 10 до 99"),
                        "reason" to Schema.string(description = "Почему эта типовая планировка похожа на рисунок")
                    )
                ),
                description = "От 3 до 5 наиболее похожих планировок из каталога по убыванию совпадения"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = this@SketchLayoutAiRepository.schema
            }
        )
    }

    suspend fun suggestLayoutsForSketch(
        sketchBitmap: Bitmap,
        roomSummaryHint: String
    ): Result<SketchMatchResult> {
        val catalogLines = ApartmentTemplates.allTemplates.joinToString("\n") { tpl ->
            val roomsLabel = when (tpl.roomCountCategory) {
                0 -> "студия"
                4 -> "4+ комн."
                else -> "${tpl.roomCountCategory}-комн."
            }
            val shape = if (tpl.hasNonRectangularShape) "непрямоугольная/эркер/Г-форма/косые" else "прямая"
            "- id=\"${tpl.id}\" | ${tpl.title} ($roomsLabel, $shape) | ${tpl.subtitle}"
        }

        val prompt = """
Посмотри на эскиз планировки квартиры, который нарисовал замерщик на сетке (1 крупная клетка = 1 м).
Учитывай геометрию внешнего контура (прямоугольник, Г-образный выступ, скошенные стены, эркер), количество комнат, их взаимное расположение и пропорции.
Выбери от 3 до 5 наиболее похожих вариантов `templateId` СТРОГО из списка каталога ниже.

Сводка по эскизу от редактора: $roomSummaryHint

Каталог планировок (используй только эти templateId):
$catalogLines
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                model.generateContent(
                    content {
                        image(sketchBitmap)
                        text(prompt)
                    }
                )
            }
            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("ИИ вернул пустой ответ по эскизу."))

            val json = JSONObject(rawJson)
            val summary = json.optString("sketchSummary").ifBlank {
                "По вашему рисунку подобраны ближайшие типовые планировки."
            }
            val recArray = json.optJSONArray("recommendations") ?: JSONArray()
            val matches = mutableListOf<AiTemplateMatch>()
            val seen = mutableSetOf<String>()
            for (i in 0 until recArray.length()) {
                val obj = recArray.getJSONObject(i)
                val tplId = obj.optString("templateId").trim()
                val tpl = ApartmentTemplates.findById(tplId) ?: continue
                if (!seen.add(tplId)) continue
                matches.add(
                    AiTemplateMatch(
                        template = tpl,
                        matchPercent = obj.optInt("matchPercent", 75).coerceIn(10, 99),
                        reason = obj.optString("reason").ifBlank { tpl.subtitle }
                    )
                )
            }
            if (matches.isEmpty()) {
                return Result.failure(IllegalStateException("Не удалось подобрать похожие шаблоны из каталога. Попробуйте дорисовать стены комнат."))
            }
            Result.success(
                SketchMatchResult(
                    sketchSummary = summary,
                    recommendations = matches.sortedByDescending { it.matchPercent }
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Sketch matching AI timed out", e)
            Result.failure(IllegalStateException("Превышено время ожидания ИИ (60 сек). Попробуйте ещё раз."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Sketch matching AI failed", e)
            Result.failure(IllegalStateException(e.localizedMessage ?: "Не удалось подобрать варианты по рисунку."))
        }
    }

    companion object {
        private const val TAG = "SketchAiRepo"
    }
}
