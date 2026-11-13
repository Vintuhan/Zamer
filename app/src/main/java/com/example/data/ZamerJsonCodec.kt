package com.example.data

import androidx.compose.ui.geometry.Offset
import com.example.domain.ApartmentTemplates
import com.example.domain.EstimateCategoryConfig
import com.example.domain.OpeningItem
import com.example.domain.OpeningType
import com.example.domain.SnappedSegment
import com.example.domain.WallMeasurement
import org.json.JSONArray
import org.json.JSONObject

object ZamerJsonCodec {

    fun encodeWalls(walls: List<WallMeasurement>): String {
        val arr = JSONArray()
        walls.forEach { w ->
            val obj = JSONObject()
            obj.put("index", w.index)
            obj.put("label", w.label)
            obj.put("hint", w.hint)
            obj.put("lengthMeters", w.lengthMeters)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun decodeWalls(json: String): List<WallMeasurement> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                WallMeasurement(
                    index = obj.optInt("index", i + 1),
                    label = obj.optString("label", "Стена ${i + 1}"),
                    hint = obj.optString("hint", "Мерь по низу, у пола"),
                    lengthMeters = obj.optDouble("lengthMeters", 0.0)
                )
            }
        }.getOrDefault(emptyList())
    }

    fun encodeOpenings(openings: List<OpeningItem>): String {
        val arr = JSONArray()
        openings.forEach { op ->
            val obj = JSONObject()
            obj.put("id", op.id)
            obj.put("type", op.type.name)
            obj.put("count", op.count)
            obj.put("widthMeters", op.widthMeters)
            obj.put("heightMeters", op.heightMeters)
            obj.put("label", op.label)
            if (op.directAreaSqm != null) {
                obj.put("directAreaSqm", op.directAreaSqm)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    fun decodeProjectOpenings(json: String): List<OpeningItem> {
        if (json.isBlank() || json == "[]") return emptyList()
        return decodeOpeningsList(json)
    }

    fun decodeOpenings(json: String): List<OpeningItem> {
        if (json.isBlank()) return emptyList()
        return decodeOpeningsList(json)
    }

    private fun decodeOpeningsList(json: String): List<OpeningItem> {
        return runCatching {
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                val typeStr = obj.optString("type", OpeningType.DOOR.name)
                val type = runCatching { OpeningType.valueOf(typeStr) }.getOrDefault(OpeningType.DOOR)
                val directArea = if (obj.has("directAreaSqm") && !obj.isNull("directAreaSqm")) {
                    obj.optDouble("directAreaSqm")
                } else {
                    null
                }
                OpeningItem(
                    id = obj.optString("id", "${type.name}_$i"),
                    type = type,
                    count = obj.optInt("count", 1),
                    widthMeters = obj.optDouble("widthMeters", if (type == OpeningType.DOOR) 0.8 else 1.4),
                    heightMeters = obj.optDouble("heightMeters", if (type == OpeningType.DOOR) 2.0 else 1.4),
                    label = obj.optString("label", ""),
                    directAreaSqm = directArea
                )
            }
        }.getOrDefault(emptyList())
    }

    fun encodeEstimateCategories(categories: List<EstimateCategoryConfig>): String {
        val arr = JSONArray()
        categories.forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("title", c.title)
            obj.put("unit", c.unit)
            obj.put("sourceKind", c.sourceKind)
            obj.put("isSelected", c.isSelected)
            obj.put("pricePerUnit", c.pricePerUnit)
            obj.put("customQuantity", c.customQuantity)
            obj.put("usePartial", c.usePartial)
            obj.put("partialQuantity", c.partialQuantity)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun decodeEstimateCategories(json: String): List<EstimateCategoryConfig> {
        if (json.isBlank()) return ApartmentTemplates.defaultEstimateCategories
        return runCatching {
            val arr = JSONArray(json)
            if (arr.length() == 0) return ApartmentTemplates.defaultEstimateCategories
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                EstimateCategoryConfig(
                    id = obj.optString("id", "cat_$i"),
                    title = obj.optString("title", "Работа"),
                    unit = obj.optString("unit", "м²"),
                    sourceKind = obj.optString("sourceKind", "WALLS"),
                    isSelected = obj.optBoolean("isSelected", false),
                    pricePerUnit = obj.optDouble("pricePerUnit", 0.0),
                    customQuantity = obj.optDouble("customQuantity", 0.0),
                    usePartial = obj.optBoolean("usePartial", false),
                    partialQuantity = obj.optDouble("partialQuantity", 0.0)
                )
            }
        }.getOrDefault(ApartmentTemplates.defaultEstimateCategories)
    }

    fun encodeDrawingSegments(segments: List<SnappedSegment>): String {
        val arr = JSONArray()
        segments.forEach { s ->
            val obj = JSONObject()
            obj.put("sx", s.start.x.toDouble())
            obj.put("sy", s.start.y.toDouble())
            obj.put("ex", s.end.x.toDouble())
            obj.put("ey", s.end.y.toDouble())
            arr.put(obj)
        }
        return arr.toString()
    }

    fun decodeDrawingSegments(json: String): List<SnappedSegment> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                SnappedSegment(
                    start = Offset(obj.optDouble("sx", 0.0).toFloat(), obj.optDouble("sy", 0.0).toFloat()),
                    end = Offset(obj.optDouble("ex", 0.0).toFloat(), obj.optDouble("ey", 0.0).toFloat())
                )
            }
        }.getOrDefault(emptyList())
    }
}
