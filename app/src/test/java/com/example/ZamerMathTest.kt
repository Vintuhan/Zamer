package com.example

import androidx.compose.ui.geometry.Offset
import com.example.domain.ApartmentTemplates
import com.example.domain.FloorPlanSnapper
import com.example.domain.GeometryAiAdvisor
import com.example.domain.OpeningItem
import com.example.domain.OpeningType
import com.example.domain.RoomGeometryHelper
import com.example.domain.WallMeasurement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZamerMathTest {

    @Test
    fun roomCalculation_computesWallsFloorAndCornersAccurately() {
        val walls = listOf(
            WallMeasurement(1, "Стена 1", "", 4.0),
            WallMeasurement(2, "Стена 2", "", 3.0),
            WallMeasurement(3, "Стена 3", "", 4.0),
            WallMeasurement(4, "Стена 4", "", 3.0)
        )
        val openings = listOf(
            OpeningItem("d1", OpeningType.DOOR, count = 1, widthMeters = 0.8, heightMeters = 2.0),
            OpeningItem("w1", OpeningType.WINDOW, count = 1, widthMeters = 1.4, heightMeters = 1.5)
        )
        val calc = RoomGeometryHelper.calculateRoom(
            walls = walls,
            heightMeters = 2.5,
            wallThicknessMeters = 0.15,
            openings = openings,
            innerCorners = 4,
            outerCorners = 1,
            extraCornerMeters = 1.2,
            floorAreaOverrideMeters = null
        )

        assertEquals(14.0, calc.perimeterMeters, 0.001)
        assertEquals(3.7, calc.openingsAreaSqm, 0.001)
        assertEquals(31.3, calc.netWallAreaSqm, 0.001)
        assertEquals(12.0, calc.innerFloorAreaSqm, 0.001)
        assertEquals(14.19, calc.outerFloorAreaSqm, 0.001)
        assertEquals(13.7, calc.totalCornersMeters, 0.001)
    }

    @Test
    fun geometryAiAdvisor_detectsUnrealisticWallMismatch() {
        val badWalls = listOf(
            WallMeasurement(1, "Стена 1", "", 5.0),
            WallMeasurement(2, "Стена 2", "", 4.0),
            WallMeasurement(3, "Стена 3", "", 50.0),
            WallMeasurement(4, "Стена 4", "", 4.0)
        )
        val check = GeometryAiAdvisor.validateRoomWalls(badWalls)
        assertFalse(check.isValid)
        assertTrue(!check.warningMessage.isNullOrBlank())
    }

    @Test
    fun floorPlanAiInterpreter_closesFreehandRoom() {
        val stroke = listOf(
            Offset(50f, 50f),
            Offset(250f, 55f),
            Offset(252f, 200f),
            Offset(52f, 198f)
        )
        val result = FloorPlanSnapper.interpretFreehandStrokesToRoom(listOf(stroke))
        assertTrue(result.size >= 3)
    }

    @Test
    fun apartmentTemplates_haveEntryDoorsAndFilters() {
        assertTrue(ApartmentTemplates.allTemplates.size >= 16)
        assertTrue(ApartmentTemplates.allTemplates.any { it.isNewBuild && it.roomCountCategory == 2 })
        assertTrue(ApartmentTemplates.allTemplates.any { !it.isNewBuild && it.roomCountCategory == 3 })
        ApartmentTemplates.allTemplates.forEach { tpl ->
            assertTrue(
                "Template ${tpl.title} should have bottom entry door",
                tpl.schematicRooms.any { it.hasEntryDoorBottom }
            )
        }
    }
}
