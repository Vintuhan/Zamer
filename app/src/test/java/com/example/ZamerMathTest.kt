package com.example

import androidx.compose.ui.geometry.Offset
import com.example.domain.ApartmentTemplates
import com.example.domain.BlueprintOpeningKind
import com.example.domain.ConstructorOpeningKind
import com.example.domain.ConstructorShapeKind
import com.example.domain.DrawnCustomRoom
import com.example.domain.EntryDoorSide
import com.example.domain.FloorPlanMinimapHelper
import com.example.domain.FloorPlanSnapper
import com.example.domain.FloorPlanVariants
import com.example.domain.GeometryAiAdvisor
import com.example.domain.LayoutConstructorEngine
import com.example.domain.MinimapRoomSpec
import com.example.domain.OpeningItem
import com.example.domain.OpeningType
import com.example.domain.RoomGeometryHelper
import com.example.domain.SimpleRoomShape
import com.example.domain.SnappedSegment
import com.example.domain.TemplateHouseGroup
import com.example.domain.WallMeasurement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    fun roomCalculation_whenAllOpeningsDeleted_subtractsZeroArea() {
        val walls = listOf(
            WallMeasurement(1, "Стена 1", "", 4.0),
            WallMeasurement(2, "Стена 2", "", 3.0),
            WallMeasurement(3, "Стена 3", "", 4.0),
            WallMeasurement(4, "Стена 4", "", 3.0)
        )
        val calc = RoomGeometryHelper.calculateRoom(
            walls = walls,
            heightMeters = 2.5,
            wallThicknessMeters = 0.15,
            openings = emptyList(),
            innerCorners = 4,
            outerCorners = 0,
            extraCornerMeters = 0.0,
            floorAreaOverrideMeters = null
        )

        assertEquals(0.0, calc.openingsAreaSqm, 0.001)
        assertEquals(35.0, calc.grossWallAreaSqm, 0.001)
        assertEquals(35.0, calc.netWallAreaSqm, 0.001)
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
    fun floorPlanAiInterpreter_convertsFreehandToCleanSquareBox() {
        val stroke = listOf(
            Offset(50f, 50f),
            Offset(250f, 55f),
            Offset(252f, 200f),
            Offset(52f, 198f)
        )
        val box = FloorPlanSnapper.interpretFreehandToSquareBox(listOf(stroke), "Комната", "box-1")
        assertNotNull(box)
        assertEquals(4, box!!.toSegments().size)
        assertTrue(box.width > box.height)
    }

    @Test
    fun wholeApartmentAi_splitsBigSquareAndInteriorWallsIntoAlignedRooms() {
        // Big outer square ~ (100,100)-(700,500) drawn slightly crookedly by finger
        val outerBox = listOf(
            Offset(102f, 105f),
            Offset(400f, 96f),
            Offset(695f, 104f),
            Offset(702f, 300f),
            Offset(696f, 496f),
            Offset(400f, 504f),
            Offset(98f, 498f),
            Offset(104f, 112f)
        )
        // Vertical partition down the middle-left (x ~ 350)
        val verticalWall = listOf(
            Offset(348f, 110f),
            Offset(354f, 300f),
            Offset(346f, 490f)
        )
        // Horizontal partition in the right half (y ~ 300, x ~ 350..695)
        val horizWall = listOf(
            Offset(356f, 296f),
            Offset(520f, 303f),
            Offset(690f, 298f)
        )

        val res = FloorPlanSnapper.interpretWholeApartmentFromStrokes(listOf(outerBox, verticalWall, horizWall))
        assertEquals(3, res.totalRooms)
        assertEquals(3, res.boxes.size)
        assertEquals(0, res.customRooms.size)
    }

    @Test
    fun wholeApartmentAi_extractsLShapedRoomWhenCornerBoxInsideOuterSquare() {
        val outerBox = listOf(
            Offset(100f, 100f),
            Offset(700f, 100f),
            Offset(700f, 500f),
            Offset(100f, 500f),
            Offset(102f, 105f)
        )
        // Small corner bathroom box at top-left (100..300, 100..260)
        val innerCornerWalls = listOf(
            Offset(298f, 102f),
            Offset(302f, 260f),
            Offset(104f, 262f)
        )

        val res = FloorPlanSnapper.interpretWholeApartmentFromStrokes(listOf(outerBox, innerCornerWalls))
        assertEquals(2, res.totalRooms)
        assertEquals(1, res.boxes.size)
        assertEquals(1, res.customRooms.size)
        assertEquals(6, res.customRooms.first().segments.size)
    }

    @Test
    fun floorPlanWallStick_snapsNearlyHorizontalTo90Degrees() {
        val snapped = FloorPlanSnapper.snapWallStick(
            start = Offset(100f, 100f),
            end = Offset(310f, 115f),
            existingCorners = emptyList()
        )
        assertEquals(snapped.start.y, snapped.end.y, 0.01f)
    }

    @Test
    fun floorPlanVariants_generatesMirroredAndAlternateEntryWhenPossible() {
        val khrushchevka = ApartmentTemplates.findById("khrushchyovka_1_464")!!
        val variants = FloorPlanVariants.buildVariants(khrushchevka)
        assertEquals(4, variants.size)
        assertEquals("Оригинал", variants[0].title)
        assertEquals("Зеркальный", variants[1].title)
        assertEquals("Другой вход", variants[2].title)
        assertEquals("Зеркало + вход", variants[3].title)

        // Mirrored flips horizontal bounds cleanly
        val origFirst = variants[0].rooms.first()
        val mirrorFirst = variants[1].rooms.first()
        assertEquals(1f - origFirst.right, mirrorFirst.left, 0.001f)
        assertEquals(1f - origFirst.left, mirrorFirst.right, 0.001f)

        // Alternate entry door moves entry door to a different valid hallway wall
        val origDoor = variants[0].rooms.first { it.entryDoorSide != EntryDoorSide.NONE }.entryDoorSide
        val altDoor = variants[2].rooms.first { it.entryDoorSide != EntryDoorSide.NONE }.entryDoorSide
        assertTrue(altDoor != origDoor)

        // 4th variant combines horizontal mirroring AND relocated entry door
        val combinedFirst = variants[3].rooms.first()
        assertEquals(1f - origFirst.right, combinedFirst.left, 0.001f)
        assertEquals(1f - origFirst.left, combinedFirst.right, 0.001f)
        val combinedDoor = variants[3].rooms.first { it.entryDoorSide != EntryDoorSide.NONE }.entryDoorSide
        val expectedCombinedDoor = when (altDoor) {
            EntryDoorSide.LEFT -> EntryDoorSide.RIGHT
            EntryDoorSide.RIGHT -> EntryDoorSide.LEFT
            else -> altDoor
        }
        assertEquals(expectedCombinedDoor, combinedDoor)
    }

    @Test
    fun minimapHelper_highlightsExactRoomAndWallForTemplatesAndVariants() {
        val studio = ApartmentTemplates.findById("novostroyka_studio")!!
        val specs = studio.rooms.mapIndexed { idx, spec ->
            MinimapRoomSpec(id = idx + 1L, name = spec.name, wallCount = spec.wallCount, sortOrder = idx)
        }
        // "Лоджия" is index 3 in studio.rooms, but index 0 in studio.schematicRooms
        val plan = FloorPlanMinimapHelper.buildMinimap(
            layoutType = studio.id,
            layoutTitle = "${studio.title} · Зеркальный",
            drawnSegments = emptyList(),
            rooms = specs,
            activeRoomId = 4L,
            activeWallIndex = 2
        )
        val activeRooms = plan.rooms.filter { it.isActive }
        assertEquals(1, activeRooms.size)
        assertEquals("Лоджия", activeRooms.first().label)
        assertEquals(1, activeRooms.first().activeWallEdgeIndex)
        assertTrue(plan.rooms.any { it.entryDoorSide != EntryDoorSide.NONE })

        val combinedPlan = FloorPlanMinimapHelper.buildMinimap(
            layoutType = studio.id,
            layoutTitle = "${studio.title} · Зеркало + вход",
            drawnSegments = emptyList(),
            rooms = specs,
            activeRoomId = 4L,
            activeWallIndex = 1
        )
        assertTrue(combinedPlan.rooms.any { it.entryDoorSide != EntryDoorSide.NONE })
        assertEquals(1f - studio.schematicRooms.first().right, combinedPlan.rooms.first().polygon[0].x, 0.001f)
    }

    @Test
    fun minimapHelper_reconstructsCustomDrawnApartmentsAndHighlightsActiveWall() {
        val ai = FloorPlanSnapper.interpretWholeApartmentFromStrokes(
            listOf(
                listOf(Offset(100f, 100f), Offset(700f, 100f), Offset(700f, 500f), Offset(100f, 500f), Offset(102f, 104f)),
                listOf(Offset(350f, 100f), Offset(350f, 500f))
            )
        )
        assertEquals(2, ai.boxes.size)
        val allSegs = ai.boxes.flatMap { it.toSegments() }
        val specs = ai.boxes.mapIndexed { idx, box ->
            MinimapRoomSpec(id = idx + 10L, name = box.name, wallCount = 4, sortOrder = idx)
        }
        val plan = FloorPlanMinimapHelper.buildMinimap(
            layoutType = "CUSTOM_DRAW",
            layoutTitle = "Свой чертёж",
            drawnSegments = allSegs,
            rooms = specs,
            activeRoomId = 11L,
            activeWallIndex = 3
        )
        assertEquals(2, plan.rooms.size)
        assertFalse(plan.rooms[0].isActive)
        assertTrue(plan.rooms[1].isActive)
        assertEquals(2, plan.rooms[1].activeWallEdgeIndex)
        plan.rooms.flatMap { it.polygon }.forEach { pt ->
            assertTrue(pt.x in 0f..1f && pt.y in 0f..1f)
        }
    }

    @Test
    fun apartmentTemplates_haveVariedEntryDoorsAndExpandedSeries() {
        assertTrue(ApartmentTemplates.allTemplates.size >= 25)
        assertTrue(ApartmentTemplates.allTemplates.any { it.houseGroup == TemplateHouseGroup.NEW_BUILD && it.roomCountCategory == 2 })
        assertTrue(ApartmentTemplates.allTemplates.any { it.houseGroup == TemplateHouseGroup.SECONDARY && it.roomCountCategory == 3 })
        assertTrue(ApartmentTemplates.allTemplates.any { it.houseGroup == TemplateHouseGroup.SPECIAL })

        val doorSides = ApartmentTemplates.allTemplates
            .flatMap { it.schematicRooms }
            .map { it.entryDoorSide }
            .filter { it != EntryDoorSide.NONE }
            .toSet()
        assertTrue("Expected varied entry door placements", doorSides.size >= 3)

        ApartmentTemplates.allTemplates.forEach { tpl ->
            assertTrue(
                "Template ${tpl.title} should have an entry door",
                tpl.schematicRooms.any { it.hasAnyEntryDoor() }
            )
        }
        assertFalse(
            "Estimate default categories must not include ugolki",
            ApartmentTemplates.defaultEstimateCategories.any { it.id == "ugolki" }
        )
    }

    @Test
    fun drawingApproxDimensions_computeMetersAndAreaCorrectly() {
        // 1 major cell = 96f px = 1.0 m; 1 minor step = 24f px = 0.25 m
        assertEquals(1.0, FloorPlanSnapper.pxToApproxMeters(96f), 0.001)
        assertEquals(3.25, FloorPlanSnapper.pxToApproxMeters(312f), 0.001)
        assertEquals("~3.5 м", FloorPlanSnapper.formatApproxMeters(336f))
        assertEquals("3.5 м", FloorPlanSnapper.formatApproxMeters(336f, withTilde = false))

        // 3.0 m x 4.0 m box = 288f x 384f -> 12.0 m2
        val boxArea = FloorPlanSnapper.approxBoxAreaSqm(288f, 384f)
        assertEquals(12.0, boxArea, 0.01)
        assertEquals("≈ 12 м²", FloorPlanSnapper.formatApproxArea(boxArea))

        // L-shaped room polygon: (0,0)->(3m,0)->(3m,2m)->(2m,2m)->(2m,4m)->(0,4m)
        // Area = 3*2 + 2*2 = 10.0 m2
        val m = FloorPlanSnapper.PX_PER_METER
        val lSegments = listOf(
            SnappedSegment(Offset(0f, 0f), Offset(3f * m, 0f)),
            SnappedSegment(Offset(3f * m, 0f), Offset(3f * m, 2f * m)),
            SnappedSegment(Offset(3f * m, 2f * m), Offset(2f * m, 2f * m)),
            SnappedSegment(Offset(2f * m, 2f * m), Offset(2f * m, 4f * m)),
            SnappedSegment(Offset(2f * m, 4f * m), Offset(0f, 4f * m)),
            SnappedSegment(Offset(0f, 4f * m), Offset(0f, 0f))
        )
        assertEquals(10.0, FloorPlanSnapper.approxPolygonAreaSqm(lSegments), 0.05)
    }

    @Test
    fun nonRectangularTemplates_haveSlantedOrLShapedGeometryAndValidVariants() {
        val slantedIds = listOf(
            "monolit_slanted_2",
            "chamfered_corner_2",
            "radial_fan_3",
            "trapezoid_studio",
            "seriya_p44",
            "stalinka_ryadovaya",
            "cheshka_2",
            "seriya_ii_57"
        )
        slantedIds.forEach { id ->
            val tpl = ApartmentTemplates.findById(id)
            assertNotNull("Template $id must exist", tpl)
            assertTrue("Template $id must be marked non-rectangular", tpl!!.hasNonRectangularShape)
            assertTrue(
                "Template $id must contain polygonal schematic room(s)",
                tpl.schematicRooms.any { it.polygon.size >= 3 }
            )
            tpl.schematicRooms.forEach { sr ->
                sr.resolvedPolygon().forEach { pt ->
                    assertTrue("Out-of-bounds vertex in $id: $pt", pt.x in 0f..1f && pt.y in 0f..1f)
                }
            }
            val variants = FloorPlanVariants.buildVariants(tpl)
            assertTrue("Variants must build cleanly for $id", variants.size >= 2)
        }

        // Check L-shaped 6-wall corridor templates actually have >= 6 vertices in their schematic polygon
        listOf("chamfered_corner_2", "seriya_1_515", "seriya_121", "cheshka_2").forEach { id ->
            val tpl = ApartmentTemplates.findById(id)!!
            assertTrue(
                "L-corridor in $id must have >= 6 polygon vertices",
                tpl.schematicRooms.any { it.polygon.size >= 6 }
            )
        }
    }

    @Test
    fun osmPolygonAnalyzer_detectsRectLShapeAndSlantedBuildingFootprints() {
        // Simple axis-aligned square building
        val rectLatLon = listOf(
            55.7500 to 37.6100,
            55.7500 to 37.6105,
            55.7503 to 37.6105,
            55.7503 to 37.6100,
            55.7500 to 37.6100
        )
        val (rectDesc, rectComplex) = com.example.data.OsmHouseLookupClient.analyzeLatLonPolygon(rectLatLon, 55.75)
        assertFalse(rectComplex)
        assertTrue("Expected 4 corners description, got: $rectDesc", "4" in rectDesc)

        // Slanted diagonal wall building (45 degree wing)
        val slantedLatLon = listOf(
            55.7500 to 37.6100,
            55.7500 to 37.6105,
            55.7502 to 37.6108,
            55.7504 to 37.6106,
            55.7503 to 37.6100
        )
        val (slantDesc, slantComplex) = com.example.data.OsmHouseLookupClient.analyzeLatLonPolygon(slantedLatLon, 55.75)
        assertTrue(slantComplex)
        assertTrue("Expected slanted/non-right angle note, got: $slantDesc", "косые" in slantDesc || "Непрямоугольный" in slantDesc)
    }

    @Test
    fun kimovskRegistriesParsers_extractMinZhkhAndPhotoBuildingsSpecsAccurately() {
        // 1-228 series template present and valid
        val ser1228 = ApartmentTemplates.findById("seriya_1_228")
        assertNotNull(ser1228)
        assertTrue(ser1228!!.hasNonRectangularShape)
        assertTrue(ser1228.schematicRooms.any { it.hasAnyEntryDoor() })

        // Address parsing for Kimovsk
        val parsed = com.example.data.parseAddressParts("Тульская область, Кимовск, ул. Октябрьская, д. 16")
        assertEquals("Кимовск", parsed.city)
        assertEquals("октябрь", parsed.streetCore)
        assertEquals("16", parsed.houseNumber)

        // MinZHKKh passport HTML parser
        val sampleMinZhkhHtml = """
            <html><body>
            <h1>Кимовск, ул. Бессолова, д. 42</h1>
            <table>
              <tr><td>Год постройки</td><td>1956</td></tr>
              <tr><td>Количество этажей</td><td>2</td></tr>
              <tr><td>Материал несущих стен</td><td>Шлакоблоки</td></tr>
              <tr><td>Тип перекрытий</td><td>Деревянные</td></tr>
              <tr><td>Количество жилых помещений</td><td>8</td></tr>
              <tr><td>Серия, тип постройки здания</td><td>1-228-1</td></tr>
            </table>
            </body></html>
        """.trimIndent()
        val mk = com.example.data.MinZhkhLookupClient.parseMinZhkhPassportHtml(
            sampleMinZhkhHtml,
            "https://dom.mingkh.ru/tulskaya-oblast/kimovsk/123"
        )
        assertTrue(mk.found)
        assertEquals("1956", mk.yearBuilt)
        assertEquals("2 эт.", mk.levels)
        assertEquals("Шлакоблоки", mk.wallMaterial)
        assertEquals("Деревянные", mk.floorMaterial)
        assertEquals("8", mk.apartmentsCount)
        assertEquals("1-228-1", mk.series)

        // PhotoBuildings search HTML parser
        val samplePbHtml = """
            <html><body>
            <a href="/object/551234/">Кимовск, Октябрьская улица, 16</a>
            <div>Проект: 1-447С-37</div>
            <div>Построено: 1968</div>
            <div>Этажность: 5</div>
            </body></html>
        """.trimIndent()
        val pb = com.example.data.PhotoBuildingsLookupClient.parsePhotoBuildingsHtml(
            samplePbHtml,
            parsed,
            "https://photobuildings.com/search.php"
        )
        assertTrue(pb.found)
        assertEquals("1-447С-37", pb.projectCode)
        assertEquals("1968", pb.yearBuilt)
        assertEquals("5 эт.", pb.levels)
        assertTrue(pb.searchUrl.contains("551234"))
    }

    @Test
    fun customRoomTranslation_andPolygonHitTest_workAccurately() {
        val m = FloorPlanSnapper.PX_PER_METER
        val lSegments = listOf(
            SnappedSegment(Offset(0f, 0f), Offset(3f * m, 0f)),
            SnappedSegment(Offset(3f * m, 0f), Offset(3f * m, 2f * m)),
            SnappedSegment(Offset(3f * m, 2f * m), Offset(2f * m, 2f * m)),
            SnappedSegment(Offset(2f * m, 2f * m), Offset(2f * m, 4f * m)),
            SnappedSegment(Offset(2f * m, 4f * m), Offset(0f, 4f * m)),
            SnappedSegment(Offset(0f, 4f * m), Offset(0f, 0f))
        )
        val room = DrawnCustomRoom(
            id = "custom_1",
            name = "Г-коридор",
            segments = lSegments,
            center = Offset(1f * m, 1.5f * m)
        )
        assertTrue(FloorPlanSnapper.isPointInsidePolygon(Offset(0.5f * m, 3.0f * m), room.segments))
        assertFalse(FloorPlanSnapper.isPointInsidePolygon(Offset(2.5f * m, 3.0f * m), room.segments))

        val moved = FloorPlanSnapper.translateCustomRoom(room, dx = 48f, dy = 24f)
        assertEquals(1f * m + 48f, moved.center.x, 0.01f)
        assertEquals(1.5f * m + 24f, moved.center.y, 0.01f)
        assertEquals(48f, moved.segments.first().start.x, 0.01f)
        assertEquals(24f, moved.segments.first().start.y, 0.01f)
        assertTrue(FloorPlanSnapper.isPointInsidePolygon(Offset(0.5f * m + 48f, 3.0f * m + 24f), moved.segments))

        val dist = FloorPlanSnapper.pointToSegmentDistance(
            p = Offset(150f, 20f),
            a = Offset(100f, 0f),
            b = Offset(200f, 0f)
        )
        assertEquals(20f, dist, 0.01f)
    }

    @Test
    fun allTemplates_haveBtiParityAreasAndDoors() {
        ApartmentTemplates.allTemplates.forEach { tpl ->
            assertEquals(
                "Template ${tpl.id} must have 1-to-1 rooms vs schematicRooms parity",
                tpl.rooms.size,
                tpl.schematicRooms.size
            )
            tpl.schematicRooms.forEach { sr ->
                assertTrue(
                    "Room '${sr.label}' in ${tpl.id} must have typicalAreaSqm > 0",
                    (sr.typicalAreaSqm ?: 0.0) > 0.0
                )
                sr.resolvedPolygon().forEach { pt ->
                    assertTrue(
                        "Vertex $pt out of 0..1 bounds in ${tpl.id}/${sr.label}",
                        pt.x in 0f..1f && pt.y in 0f..1f
                    )
                }
                if (!sr.isBalcony && !sr.hasAnyEntryDoor()) {
                    val hasRoomDoor = sr.hasDoorTop || sr.hasDoorRight || sr.hasDoorBottom || sr.hasDoorLeft ||
                        sr.doorEdgeIndices.isNotEmpty() ||
                        sr.openings.any {
                            it.kind == BlueprintOpeningKind.DOOR || it.kind == BlueprintOpeningKind.BALCONY_DOOR
                        }
                    assertTrue(
                        "Non-balcony room '${sr.label}' in ${tpl.id} must have at least one door",
                        hasRoomDoor
                    )
                }
            }
            val mirrored = FloorPlanVariants.mirrorHorizontally(tpl.schematicRooms)
            assertEquals(tpl.schematicRooms.size, mirrored.size)
            tpl.schematicRooms.zip(mirrored).forEach { (orig, mir) ->
                assertEquals(orig.openings.size, mir.openings.size)
                mir.openings.forEach { op ->
                    assertTrue(
                        "Mirrored opening centerFraction ${op.centerFraction} out of 0..1 in ${tpl.id}/${mir.label}",
                        op.centerFraction in 0f..1f
                    )
                }
            }
        }
    }

    @Test
    fun constructorEngine_supportsAll9ShapesSnappingRotateMirrorAndOpenings() {
        assertEquals(9, ConstructorShapeKind.entries.size)
        ConstructorShapeKind.entries.forEach { kind ->
            val verts = LayoutConstructorEngine.baseNormalizedVertices(kind)
            if (kind == ConstructorShapeKind.WALL_LINE) {
                assertEquals(2, verts.size)
            } else {
                assertTrue("Shape $kind must have >= 4 vertices", verts.size >= 4)
            }
            verts.forEach { pt ->
                assertTrue("Vertex $pt out of 0..1 for $kind", pt.x in 0f..1f && pt.y in 0f..1f)
            }
            if (kind != ConstructorShapeKind.WALL_LINE) {
                var signedArea = 0f
                for (i in verts.indices) {
                    val a = verts[i]
                    val b = verts[(i + 1) % verts.size]
                    signedArea += a.x * b.y - b.x * a.y
                }
                assertTrue("Shape $kind must be clockwise (signedArea > 0)", signedArea > 0f)
            }
        }

        // Wall-to-wall magnetic snapping
        val roomA = LayoutConstructorEngine.createRoom("a", ConstructorShapeKind.RECTANGLE, 200f, 200f, 0)
            .copy(left = 96f, top = 96f, width = 216f, height = 144f, name = "Кухня")
        val roomB = LayoutConstructorEngine.createRoom("b", ConstructorShapeKind.SQUARE, 400f, 200f, 1)
            .copy(left = 360f, top = 100f, width = 168f, height = 168f, name = "Комната")

        // Moving roomB left edge close to roomA's right edge (96 + 216 = 312) snaps flush to 312
        val snappedMove = LayoutConstructorEngine.moveWithSnap(
            room = roomB,
            rawLeft = 320f,
            rawTop = 102f,
            others = listOf(roomA)
        )
        assertEquals(312f, snappedMove.room.left, 0.01f)
        assertEquals(96f, snappedMove.room.top, 0.01f)

        // Corner resize snapping bottom edge to roomA's bottom (96 + 144 = 240)
        val resized = LayoutConstructorEngine.resizeCornerWithSnap(
            room = snappedMove.room,
            cornerIndex = 2,
            rawDragX = 480f,
            rawDragY = 235f,
            others = listOf(roomA)
        )
        assertEquals(144f, resized.room.height, 0.01f)

        // Place door on top edge, rotate 90°, and mirror while keeping clockwise winding
        val placedDoor = LayoutConstructorEngine.toggleOpeningAtTap(
            rooms = listOf(roomA),
            tapWorld = Offset(204f, 96f),
            kind = ConstructorOpeningKind.DOOR,
            newOpeningId = "op_1"
        )
        assertNotNull(placedDoor)
        assertEquals(1, placedDoor!!.second.openings.size)

        val rotated = LayoutConstructorEngine.rotate90Clockwise(placedDoor.second)
        assertEquals(roomA.height, rotated.width, 0.01f)
        assertEquals(roomA.width, rotated.height, 0.01f)
        assertEquals(1, rotated.openings.size)

        val mirrored = LayoutConstructorEngine.mirrorHorizontally(rotated)
        assertEquals(1, mirrored.openings.size)

        // Toggle same spot removes the door
        val toggledOff = LayoutConstructorEngine.toggleOpeningAtTap(
            rooms = listOf(placedDoor.second),
            tapWorld = Offset(204f, 96f),
            kind = ConstructorOpeningKind.DOOR,
            newOpeningId = "op_2"
        )
        assertNotNull(toggledOff)
        assertEquals(0, toggledOff!!.second.openings.size)

        // Export to project rooms
        val payload = LayoutConstructorEngine.toProjectDrawingPayload(listOf(roomA, resized.room))
        assertEquals(2, payload.size)
        assertEquals("Кухня", payload[0].first)
        assertEquals(4, payload[0].second.size)
        assertEquals("Комната", payload[1].first)
    }

    @Test
    fun simpleRoomShape_mapsRectLAndUShapesCorrectly() {
        assertEquals(4, SimpleRoomShape.RECT.wallCount)
        assertEquals(6, SimpleRoomShape.L_SHAPE.wallCount)
        assertEquals(8, SimpleRoomShape.U_SHAPE.wallCount)
        assertEquals(SimpleRoomShape.U_SHAPE, SimpleRoomShape.fromKey("AUTO", 8))
        assertEquals(SimpleRoomShape.L_SHAPE, SimpleRoomShape.fromKey("AUTO", 6))
        assertEquals(SimpleRoomShape.RECT, SimpleRoomShape.fromKey("AUTO", 4))
    }

    @Test
    fun constructorEngine_restoresRoomsFromSavedSegmentsRoundTrip() {
        val roomRect = LayoutConstructorEngine.createRoom("r1", ConstructorShapeKind.RECTANGLE, 240f, 200f, 0)
            .copy(name = "Кухня")
        val roomL = LayoutConstructorEngine.createRoom("r2", ConstructorShapeKind.L_SHAPE, 520f, 240f, 1)
            .copy(name = "Коридор")
        val roomU = LayoutConstructorEngine.createRoom("r3", ConstructorShapeKind.U_SHAPE, 280f, 520f, 2)
            .copy(name = "Комната")

        val payload = LayoutConstructorEngine.toProjectDrawingPayload(listOf(roomRect, roomL, roomU))
        val restored = LayoutConstructorEngine.roomsFromSegments(payload)

        assertEquals(3, restored.size)
        assertEquals("Кухня", restored[0].name)
        assertEquals("Коридор", restored[1].name)
        assertEquals("Комната", restored[2].name)
        assertEquals(ConstructorShapeKind.L_SHAPE, restored[1].shapeKind)
        assertEquals(ConstructorShapeKind.U_SHAPE, restored[2].shapeKind)
        assertEquals(roomL.left, restored[1].left, 0.5f)
        assertEquals(roomL.top, restored[1].top, 0.5f)
        assertEquals(roomL.width, restored[1].width, 0.5f)
        assertEquals(roomL.height, restored[1].height, 0.5f)
        assertEquals(roomU.worldSegments().size, restored[2].worldSegments().size)
    }

    @Test
    fun constructorEngine_zoomAndFitViewportMathWorkAccurately() {
        val anchorScreen = Offset(400f, 300f)
        val initialPan = Offset(40f, -20f)
        val initialZoom = 1f
        val worldBefore = LayoutConstructorEngine.screenToWorld(anchorScreen, initialZoom, initialPan)

        val (zoomedIn, panAfterIn) = LayoutConstructorEngine.zoomAroundAnchor(
            currentZoom = initialZoom,
            currentPan = initialPan,
            zoomChange = 2f,
            panChange = Offset.Zero,
            anchorScreen = anchorScreen
        )
        assertEquals(2f, zoomedIn, 0.001f)
        val worldAfter = LayoutConstructorEngine.screenToWorld(anchorScreen, zoomedIn, panAfterIn)
        assertEquals(worldBefore.x, worldAfter.x, 0.01f)
        assertEquals(worldBefore.y, worldAfter.y, 0.01f)

        // Clamping beyond MAX_ZOOM and MIN_ZOOM
        val (clampedMax, _) = LayoutConstructorEngine.zoomAroundAnchor(zoomedIn, panAfterIn, 10f, Offset.Zero, anchorScreen)
        assertEquals(LayoutConstructorEngine.MAX_ZOOM, clampedMax, 0.001f)
        val (clampedMin, _) = LayoutConstructorEngine.zoomAroundAnchor(zoomedIn, panAfterIn, 0.01f, Offset.Zero, anchorScreen)
        assertEquals(LayoutConstructorEngine.MIN_ZOOM, clampedMin, 0.001f)

        // Large spread of rooms fits cleanly into a phone canvas
        val wideRooms = listOf(
            LayoutConstructorEngine.createRoom("a", ConstructorShapeKind.RECTANGLE, 100f, 100f, 0),
            LayoutConstructorEngine.createRoom("b", ConstructorShapeKind.RECTANGLE, 1400f, 900f, 1)
        )
        val (fitZoom, fitPan) = LayoutConstructorEngine.fitRoomsToViewport(wideRooms, 800f, 900f)
        assertTrue("Expected zoom out < 1.0 for wide layout, was $fitZoom", fitZoom < 1f)
        val leftScreen = LayoutConstructorEngine.worldToScreen(Offset(wideRooms.minOf { it.left }, wideRooms.minOf { it.top }), fitZoom, fitPan)
        val rightScreen = LayoutConstructorEngine.worldToScreen(Offset(wideRooms.maxOf { it.left + it.width }, wideRooms.maxOf { it.top + it.height }), fitZoom, fitPan)
        assertTrue(leftScreen.x >= 0f && leftScreen.y >= 0f)
        assertTrue(rightScreen.x <= 800f && rightScreen.y <= 900f)
    }

    @Test
    fun calculatorEvaluator_handlesParenthesesAndPrecedence() {
        assertEquals(20.0, com.example.ui.viewmodel.CalculatorExpressionEvaluator.evaluate("(2 + 3) × 4"), 0.001)
        assertEquals(13.0, com.example.ui.viewmodel.CalculatorExpressionEvaluator.evaluate("10 + (6 ÷ 2)"), 0.001)
        assertEquals(20.0, com.example.ui.viewmodel.CalculatorExpressionEvaluator.evaluate("((2 + 1) × (3 + 4)) − 1"), 0.001)
        assertEquals(25.0, com.example.ui.viewmodel.CalculatorExpressionEvaluator.evaluate("5 × (2 + 3"), 0.001)
    }

    @Test
    fun geometryAiAdvisor_warnsOnOppositeRectWallMismatch5vs6() {
        val walls = listOf(
            WallMeasurement(1, "Стена 1", "", 5.0),
            WallMeasurement(2, "Стена 2", "", 3.0),
            WallMeasurement(3, "Стена 3", "", 6.0),
            WallMeasurement(4, "Стена 4", "", 3.0)
        )
        val check = GeometryAiAdvisor.validateRoomWalls(walls)
        assertFalse(check.isValid)
        assertTrue(check.warningMessage?.contains("Точно ли вы верно ввели?") == true)
    }

    @Test
    fun openingsDirectArea_andSingleWallBeam_computeAccurately() {
        val directOp = OpeningItem(
            id = "op_direct",
            type = OpeningType.WINDOW,
            count = 1,
            widthMeters = 0.0,
            heightMeters = 0.0,
            label = "Проёмы балкон",
            directAreaSqm = 4.5
        )
        assertEquals(4.5, directOp.totalAreaSqm, 0.001)

        // Single wall (ригель) has 0 floor area and clean 2-point minimap polyline
        assertEquals(0.0, RoomGeometryHelper.computeInnerFloorAreaFromWalls(listOf(3.5)), 0.001)
        val wallSeg = SnappedSegment(Offset(96f, 144f), Offset(480f, 144f))
        val plan = FloorPlanMinimapHelper.buildMinimap(
            layoutType = "CUSTOM_DRAW",
            layoutTitle = "Свой чертёж",
            drawnSegments = listOf(wallSeg),
            rooms = listOf(MinimapRoomSpec(id = 99L, name = "Ригель", wallCount = 1, sortOrder = 0)),
            activeRoomId = 99L,
            activeWallIndex = 1
        )
        assertEquals(1, plan.rooms.size)
        assertEquals(2, plan.rooms[0].polygon.size)
        assertEquals(0, plan.rooms[0].activeWallEdgeIndex)
    }

    @Test
    fun constructorEngine_7x7ZoomNonOverlappingLabelsAndSnapOffWork() {
        val zoom = LayoutConstructorEngine.default7x7Zoom(672f, 900f)
        assertEquals(1.0f, zoom, 0.001f)

        val resolved = LayoutConstructorEngine.resolveNonOverlappingLabelCenters(
            desiredCenters = listOf(Offset(200f, 200f), Offset(205f, 202f)),
            labelWidths = listOf(100f, 100f),
            labelHeights = listOf(30f, 30f)
        )
        assertEquals(2, resolved.size)
        assertTrue(resolved[0] != resolved[1])

        val roomA = LayoutConstructorEngine.createRoom("a", ConstructorShapeKind.RECTANGLE, 200f, 200f, 0, snapToGridEnabled = false)
        val freeMove = LayoutConstructorEngine.moveWithSnap(
            room = roomA,
            rawLeft = 113f,
            rawTop = 127f,
            others = emptyList(),
            snapToGridEnabled = false,
            snapToNeighborsEnabled = false
        )
        assertEquals(113f, freeMove.room.left, 0.001f)
        assertEquals(127f, freeMove.room.top, 0.001f)
    }
}
