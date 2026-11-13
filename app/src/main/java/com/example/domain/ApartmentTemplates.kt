package com.example.domain

import androidx.compose.ui.geometry.Offset

object ApartmentTemplates {

    private val defaultFourWalls = listOf(
        "От входной двери направо. Мерь по низу, у пола",
        "Вторая стена (часто с окном). Мерь от угла до угла у пола",
        "Противоположная первой. Мерь по низу, у плинтуса",
        "Обратно к двери. Мерь от угла до угла по полу"
    )

    private fun wallHintsForCount(count: Int, hasSlanted: Boolean = false): List<String> =
        if (count == 4 && !hasSlanted) {
            defaultFourWalls
        } else {
            List(count) { idx ->
                when {
                    hasSlanted && idx in 1..(count - 2) ->
                        "Стена ${idx + 1} (включая косой угол / эркер). Мерь строго от угла до угла по полу"
                    idx == 0 -> "Первая стена (от входа по часовой стрелке). Мерь по низу у пола"
                    idx == count - 1 -> "Последняя стена (замыкает контур обратно). Мерь по полу"
                    else -> "Стена ${idx + 1} по часовой стрелке. Мерь от угла до угла у пола"
                }
            }
        }

    private fun room(
        name: String,
        walls: Int = 4,
        inner: Int = walls,
        outer: Int = 0,
        hasSlanted: Boolean = false
    ) = TemplateRoomSpec(
        name = name,
        wallCount = walls,
        defaultInnerCorners = inner,
        defaultOuterCorners = outer,
        wallHints = wallHintsForCount(walls, hasSlanted)
    )

    private fun winTop(cx: Float = 0.5f, w: Float = 0.52f) =
        BlueprintOpening(0, cx, w, BlueprintOpeningKind.WINDOW)

    private fun winRight(cy: Float = 0.5f, w: Float = 0.52f) =
        BlueprintOpening(1, cy, w, BlueprintOpeningKind.WINDOW)

    private fun winBottom(cx: Float = 0.5f, w: Float = 0.52f) =
        BlueprintOpening(2, 1f - cx, w, BlueprintOpeningKind.WINDOW)

    private fun winLeft(cy: Float = 0.5f, w: Float = 0.52f) =
        BlueprintOpening(3, 1f - cy, w, BlueprintOpeningKind.WINDOW)

    private fun doorTop(cx: Float = 0.5f, w: Float = 0.28f) =
        BlueprintOpening(0, cx, w, BlueprintOpeningKind.DOOR)

    private fun doorRight(cy: Float = 0.5f, w: Float = 0.28f) =
        BlueprintOpening(1, cy, w, BlueprintOpeningKind.DOOR)

    private fun doorBottom(cx: Float = 0.5f, w: Float = 0.28f) =
        BlueprintOpening(2, 1f - cx, w, BlueprintOpeningKind.DOOR)

    private fun doorLeft(cy: Float = 0.5f, w: Float = 0.28f) =
        BlueprintOpening(3, 1f - cy, w, BlueprintOpeningKind.DOOR)

    private fun balcTop(cx: Float = 0.5f) =
        BlueprintOpening(0, cx, 0.54f, BlueprintOpeningKind.BALCONY_DOOR)

    private fun balcRight(cy: Float = 0.5f) =
        BlueprintOpening(1, cy, 0.54f, BlueprintOpeningKind.BALCONY_DOOR)

    private fun balcBottom(cx: Float = 0.5f) =
        BlueprintOpening(2, 1f - cx, 0.54f, BlueprintOpeningKind.BALCONY_DOOR)

    private fun balcLeft(cy: Float = 0.5f) =
        BlueprintOpening(3, 1f - cy, 0.54f, BlueprintOpeningKind.BALCONY_DOOR)

    private fun polyOp(
        edgeIdx: Int,
        center: Float = 0.5f,
        w: Float = 0.30f,
        kind: BlueprintOpeningKind = BlueprintOpeningKind.DOOR
    ) = BlueprintOpening(edgeIdx, center, w, kind)

    private fun rect(
        l: Float,
        t: Float,
        r: Float,
        b: Float,
        label: String,
        hasDoorBottom: Boolean = false,
        hasDoorRight: Boolean = false,
        hasDoorLeft: Boolean = false,
        hasDoorTop: Boolean = false,
        hasWindowTop: Boolean = false,
        hasWindowBottom: Boolean = false,
        hasWindowLeft: Boolean = false,
        hasWindowRight: Boolean = false,
        entryDoorSide: EntryDoorSide = EntryDoorSide.NONE,
        isBalcony: Boolean = false,
        area: Double? = null,
        fx: BlueprintFixtureKind = BlueprintFixtureKind.NONE,
        openings: List<BlueprintOpening> = emptyList()
    ) = RoomGeometryRect(
        left = l,
        top = t,
        right = r,
        bottom = b,
        label = label,
        hasDoorBottom = hasDoorBottom,
        hasDoorRight = hasDoorRight,
        hasDoorLeft = hasDoorLeft,
        hasDoorTop = hasDoorTop,
        hasWindowTop = hasWindowTop,
        hasWindowBottom = hasWindowBottom,
        hasWindowLeft = hasWindowLeft,
        hasWindowRight = hasWindowRight,
        entryDoorSide = entryDoorSide,
        isBalcony = isBalcony,
        openings = openings,
        typicalAreaSqm = area,
        fixtureKind = fx
    )

    private fun poly(
        pts: List<Pair<Float, Float>>,
        label: String,
        windowEdges: List<Int> = emptyList(),
        doorEdges: List<Int> = emptyList(),
        entryDoorEdge: Int = -1,
        isBalcony: Boolean = false,
        labelPos: Pair<Float, Float>? = null,
        area: Double? = null,
        fx: BlueprintFixtureKind = BlueprintFixtureKind.NONE,
        openings: List<BlueprintOpening> = emptyList()
    ): RoomGeometryRect {
        val offsets = pts.map { Offset(it.first, it.second) }
        val minX = offsets.minOfOrNull { it.x } ?: 0f
        val maxX = offsets.maxOfOrNull { it.x } ?: 1f
        val minY = offsets.minOfOrNull { it.y } ?: 0f
        val maxY = offsets.maxOfOrNull { it.y } ?: 1f
        return RoomGeometryRect(
            left = minX,
            top = minY,
            right = maxX,
            bottom = maxY,
            label = label,
            isBalcony = isBalcony,
            polygon = offsets,
            windowEdgeIndices = windowEdges,
            doorEdgeIndices = doorEdges,
            entryDoorEdgeIndex = entryDoorEdge,
            labelOffset = labelPos?.let { Offset(it.first, it.second) },
            openings = openings,
            typicalAreaSqm = area,
            fixtureKind = fx
        )
    }

    // Removed "Уголки" per user request!
    val defaultEstimateCategories: List<EstimateCategoryConfig> = listOf(
        EstimateCategoryConfig("shpaklevka", "Шпаклёвка", "м²", "WALLS"),
        EstimateCategoryConfig("shtukaturka", "Штукатурка", "м²", "WALLS"),
        EstimateCategoryConfig("oboi", "Обои", "м²", "WALLS"),
        EstimateCategoryConfig("pokraska", "Покраска", "м²", "WALLS"),
        EstimateCategoryConfig("laminat", "Ламинат", "м²", "FLOOR"),
        EstimateCategoryConfig("plitka", "Плитка", "м²", "WALLS"),
        EstimateCategoryConfig("zachistka", "Зачистка", "м²", "WALLS"),
        EstimateCategoryConfig("gruntovka", "Грунтовка", "м²", "WALLS"),
        EstimateCategoryConfig("nishi", "Ниши", "м²", "WALLS")
    )

    val allTemplates: List<ApartmentTemplate> = listOf(
        // ==================== 0. КВАРТИРЫ С КОСЫМИ СТЕНАМИ, ЭРКЕРАМИ И СКОШЕННЫМИ УГЛАМИ ====================
        ApartmentTemplate(
            id = "monolit_slanted_2",
            title = "Монолит с косой стеной (2-к.)",
            subtitle = "Скошенный фасад, косая лоджия и Г-коридор (~61 м²)",
            roomCountCategory = 2,
            houseGroup = TemplateHouseGroup.NEW_BUILD,
            planAspectRatio = 1.45f,
            hasNonRectangularShape = true,
            rooms = listOf(
                room("Гостиная (косая стена)", walls = 4, hasSlanted = true),
                room("Кухня (5 углов)", walls = 5, hasSlanted = true),
                room("Косая лоджия", walls = 4, hasSlanted = true),
                room("Спальня", walls = 4, hasSlanted = true),
                room("Прихожая (Г-форма)", walls = 6, inner = 5, outer = 1),
                room("Санузел")
            ),
            schematicRooms = listOf(
                poly(
                    pts = listOf(0.16f to 0.12f, 0.54f to 0.12f, 0.54f to 0.56f, 0.06f to 0.56f),
                    label = "Гостиная",
                    windowEdges = listOf(0, 3),
                    openings = listOf(polyOp(2, center = 0.22f, w = 0.26f)),
                    labelPos = 0.32f to 0.34f,
                    area = 19.8
                ),
                poly(
                    pts = listOf(0.06f to 0.56f, 0.36f to 0.56f, 0.36f to 0.92f, 0.14f to 0.92f, 0.04f to 0.72f),
                    label = "Кухня",
                    windowEdges = listOf(3, 4),
                    doorEdges = listOf(1),
                    labelPos = 0.21f to 0.74f,
                    area = 11.2,
                    fx = BlueprintFixtureKind.KITCHEN
                ),
                poly(
                    pts = listOf(0.54f to 0.04f, 0.94f to 0.14f, 0.94f to 0.25f, 0.54f to 0.18f),
                    label = "Лоджия",
                    isBalcony = true,
                    area = 3.8
                ),
                poly(
                    pts = listOf(0.54f to 0.18f, 0.94f to 0.25f, 0.94f to 0.62f, 0.54f to 0.62f),
                    label = "Спальня",
                    openings = listOf(
                        polyOp(0, center = 0.45f, w = 0.52f, kind = BlueprintOpeningKind.BALCONY_DOOR),
                        polyOp(2, center = 0.76f, w = 0.26f, kind = BlueprintOpeningKind.DOOR)
                    ),
                    area = 14.5
                ),
                poly(
                    pts = listOf(
                        0.36f to 0.56f, 0.54f to 0.56f, 0.54f to 0.62f,
                        0.72f to 0.62f, 0.72f to 0.92f, 0.36f to 0.92f
                    ),
                    label = "Прихожая",
                    entryDoorEdge = 4,
                    labelPos = 0.52f to 0.78f,
                    area = 7.8
                ),
                rect(0.72f, 0.62f, 0.94f, 0.88f, "С/У", hasDoorLeft = true, area = 4.2, fx = BlueprintFixtureKind.COMBINED_BATH)
            )
        ),
        ApartmentTemplate(
            id = "chamfered_corner_2",
            title = "Со скошенным углом 45° (2-к.)",
            subtitle = "5-стенная гостиная с панорамным углом + лоджия (~63 м²)",
            roomCountCategory = 2,
            houseGroup = TemplateHouseGroup.NEW_BUILD,
            planAspectRatio = 1.48f,
            hasNonRectangularShape = true,
            rooms = listOf(
                room("Гостиная (скош. угол)", walls = 5, hasSlanted = true),
                room("Спальня"),
                room("Лоджия (трапеция)", walls = 4, hasSlanted = true),
                room("Кухня"),
                room("Коридор (Г-форма)", walls = 6, inner = 5, outer = 1),
                room("Санузел")
            ),
            schematicRooms = listOf(
                poly(
                    pts = listOf(0.05f to 0.28f, 0.25f to 0.06f, 0.56f to 0.06f, 0.56f to 0.58f, 0.05f to 0.58f),
                    label = "Гостиная",
                    windowEdges = listOf(0, 1),
                    openings = listOf(polyOp(3, center = 0.24f, w = 0.24f)),
                    labelPos = 0.33f to 0.35f,
                    area = 20.4
                ),
                rect(
                    0.56f, 0.14f, 0.95f, 0.58f, "Спальня",
                    area = 14.8,
                    openings = listOf(balcTop(0.45f), winRight(0.50f), doorBottom(0.22f, 0.26f))
                ),
                poly(
                    pts = listOf(0.56f to 0.04f, 0.88f to 0.04f, 0.95f to 0.14f, 0.56f to 0.14f),
                    label = "Лоджия",
                    isBalcony = true,
                    area = 3.5
                ),
                rect(0.05f, 0.58f, 0.34f, 0.94f, "Кухня", hasDoorRight = true, hasWindowLeft = true, area = 10.2, fx = BlueprintFixtureKind.KITCHEN),
                poly(
                    pts = listOf(
                        0.34f to 0.58f, 0.72f to 0.58f, 0.72f to 0.84f,
                        0.95f to 0.84f, 0.95f to 0.95f, 0.34f to 0.95f
                    ),
                    label = "Коридор",
                    entryDoorEdge = 4,
                    labelPos = 0.54f to 0.78f,
                    area = 8.1
                ),
                rect(0.72f, 0.58f, 0.95f, 0.84f, "С/У", hasDoorLeft = true, area = 4.3, fx = BlueprintFixtureKind.COMBINED_BATH)
            )
        ),
        ApartmentTemplate(
            id = "radial_fan_3",
            title = "Веерная / лучевая (монолит 3-к.)",
            subtitle = "Трапециевидные комнаты расширяются к фасаду (~76 м²)",
            roomCountCategory = 3,
            houseGroup = TemplateHouseGroup.NEW_BUILD,
            planAspectRatio = 1.60f,
            hasNonRectangularShape = true,
            rooms = listOf(
                room("Зал (трапеция)", walls = 4, hasSlanted = true),
                room("Спальня 1 (трапеция)", walls = 4, hasSlanted = true),
                room("Спальня 2 (косая)", walls = 4, hasSlanted = true),
                room("Кухня"),
                room("Холл"),
                room("Санузел")
            ),
            schematicRooms = listOf(
                poly(
                    pts = listOf(0.04f to 0.16f, 0.34f to 0.06f, 0.38f to 0.58f, 0.12f to 0.58f),
                    label = "Зал",
                    windowEdges = listOf(0),
                    openings = listOf(polyOp(2, center = 0.22f, w = 0.28f)),
                    area = 21.0
                ),
                poly(
                    pts = listOf(0.34f to 0.06f, 0.66f to 0.06f, 0.62f to 0.58f, 0.38f to 0.58f),
                    label = "Сп.1",
                    windowEdges = listOf(0),
                    doorEdges = listOf(2),
                    area = 15.2
                ),
                poly(
                    pts = listOf(0.66f to 0.06f, 0.96f to 0.16f, 0.88f to 0.58f, 0.62f to 0.58f),
                    label = "Сп.2",
                    windowEdges = listOf(0),
                    openings = listOf(polyOp(2, center = 0.78f, w = 0.28f)),
                    area = 14.6
                ),
                poly(
                    pts = listOf(0.12f to 0.58f, 0.34f to 0.58f, 0.34f to 0.92f, 0.18f to 0.92f),
                    label = "Кухня",
                    windowEdges = listOf(3),
                    doorEdges = listOf(1),
                    area = 10.8,
                    fx = BlueprintFixtureKind.KITCHEN
                ),
                rect(0.34f, 0.58f, 0.70f, 0.92f, "Холл", entryDoorSide = EntryDoorSide.BOTTOM, area = 9.4),
                poly(
                    pts = listOf(0.70f to 0.58f, 0.88f to 0.58f, 0.84f to 0.92f, 0.70f to 0.92f),
                    label = "С/У",
                    doorEdges = listOf(3),
                    area = 4.6,
                    fx = BlueprintFixtureKind.COMBINED_BATH
                )
            )
        ),
        ApartmentTemplate(
            id = "trapezoid_studio",
            title = "Студия со скошенной стеной",
            subtitle = "Трапеция + треугольный балкон на углу (~32 м²)",
            roomCountCategory = 0,
            houseGroup = TemplateHouseGroup.NEW_BUILD,
            planAspectRatio = 1.32f,
            hasNonRectangularShape = true,
            rooms = listOf(
                room("Студия + кухня (косая)", walls = 4, hasSlanted = true),
                room("Треуг. балкон", walls = 3, hasSlanted = true),
                room("Прихожая"),
                room("Санузел")
            ),
            schematicRooms = listOf(
                poly(
                    pts = listOf(0.06f to 0.22f, 0.74f to 0.06f, 0.94f to 0.64f, 0.06f to 0.64f),
                    label = "Студия / Кухня",
                    windowEdges = listOf(0),
                    openings = listOf(
                        polyOp(1, center = 0.45f, w = 0.44f, kind = BlueprintOpeningKind.BALCONY_DOOR),
                        polyOp(2, center = 0.72f, w = 0.20f, kind = BlueprintOpeningKind.DOOR)
                    ),
                    labelPos = 0.45f to 0.42f,
                    area = 19.6,
                    fx = BlueprintFixtureKind.KITCHEN
                ),
                poly(
                    pts = listOf(0.74f to 0.06f, 0.96f to 0.12f, 0.94f to 0.54f),
                    label = "Балкон",
                    isBalcony = true,
                    labelPos = 0.88f to 0.24f,
                    area = 3.2
                ),
                rect(0.06f, 0.64f, 0.56f, 0.94f, "Прихожая", entryDoorSide = EntryDoorSide.BOTTOM, area = 5.0),
                rect(0.56f, 0.64f, 0.88f, 0.94f, "С/У", hasDoorLeft = true, area = 4.1, fx = BlueprintFixtureKind.COMBINED_BATH)
            )
        ),

        // ==================== 1. СТУДИИ / МАЛОСЕМЕЙКИ / ГОСТИНКИ ====================
        ApartmentTemplate(
            id = "novostroyka_studio",
            title = "Квартира-студия (смарт)",
            subtitle = "Г-форма · ниша под кухню, лоджия 3.4 м² (~27 м²)",
            roomCountCategory = 0,
            houseGroup = TemplateHouseGroup.NEW_BUILD,
            planAspectRatio = 1.24f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Студия + кухня", walls = 6, inner = 5, outer = 1), room("Прихожая"), room("Санузел"), room("Лоджия")),
            schematicRooms = listOf(
                rect(0.06f, 0.04f, 0.66f, 0.16f, "Лоджия", isBalcony = true, area = 3.4),
                poly(
                    pts = listOf(
                        0.06f to 0.16f, 0.92f to 0.16f, 0.92f to 0.56f,
                        0.66f to 0.56f, 0.66f to 0.68f, 0.06f to 0.68f
                    ),
                    label = "Студия / Кухня",
                    windowEdges = listOf(1),
                    openings = listOf(
                        polyOp(0, center = 0.36f, w = 0.42f, kind = BlueprintOpeningKind.BALCONY_DOOR),
                        polyOp(4, center = 0.58f, w = 0.24f, kind = BlueprintOpeningKind.DOOR)
                    ),
                    labelPos = 0.44f to 0.40f,
                    area = 16.5,
                    fx = BlueprintFixtureKind.KITCHEN
                ),
                rect(0.06f, 0.68f, 0.54f, 0.95f, "Прихожая", entryDoorSide = EntryDoorSide.BOTTOM, area = 3.6),
                rect(0.54f, 0.56f, 0.88f, 0.90f, "С/У", hasDoorLeft = true, area = 3.5, fx = BlueprintFixtureKind.COMBINED_BATH)
            )
        ),
        ApartmentTemplate(
            id = "gostinka",
            title = "Гостинка (угловая с выступом)",
            subtitle = "Выступающая кухня-ниша · вход слева (~23 м²)",
            roomCountCategory = 0,
            houseGroup = TemplateHouseGroup.SECONDARY,
            planAspectRatio = 1.38f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Жилая комната"), room("Кухня-ниша"), room("Прихожая"), room("Санузел")),
            schematicRooms = listOf(
                rect(
                    0.05f, 0.08f, 0.64f, 0.66f, "Комната",
                    area = 12.8,
                    openings = listOf(winTop(0.5f), doorBottom(0.45f, 0.24f))
                ),
                rect(
                    0.64f, 0.18f, 0.95f, 0.66f, "Кухня",
                    area = 4.5,
                    fx = BlueprintFixtureKind.KITCHEN,
                    openings = listOf(winTop(0.5f), doorLeft(0.52f, 0.34f))
                ),
                rect(0.05f, 0.66f, 0.56f, 0.94f, "Прихожая", entryDoorSide = EntryDoorSide.LEFT, area = 2.9),
                rect(0.56f, 0.66f, 0.88f, 0.90f, "С/У", hasDoorLeft = true, area = 2.6, fx = BlueprintFixtureKind.COMBINED_BATH)
            )
        ),
        ApartmentTemplate(
            id = "malosemeyka",
            title = "Малосемейка",
            subtitle = "Ступенчатый контур · балкон сверху, вход справа (~29 м²)",
            roomCountCategory = 1,
            houseGroup = TemplateHouseGroup.SECONDARY,
            planAspectRatio = 1.45f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Балкон"), room("Комната"), room("Кухня"), room("Коридор"), room("Санузел")),
            schematicRooms = listOf(
                rect(0.04f, 0.03f, 0.56f, 0.14f, "Балкон", isBalcony = true, area = 2.2),
                rect(
                    0.04f, 0.14f, 0.58f, 0.64f, "Комната",
                    area = 14.5,
                    openings = listOf(balcTop(0.48f), doorBottom(0.86f, 0.22f))
                ),
                rect(
                    0.58f, 0.16f, 0.94f, 0.58f, "Кухня",
                    area = 5.6,
                    fx = BlueprintFixtureKind.KITCHEN,
                    openings = listOf(winTop(0.5f), doorBottom(0.45f, 0.32f))
                ),
                rect(0.14f, 0.64f, 0.46f, 0.94f, "С/У", hasDoorRight = true, area = 2.8, fx = BlueprintFixtureKind.COMBINED_BATH),
                rect(0.46f, 0.58f, 0.96f, 0.94f, "Коридор", entryDoorSide = EntryDoorSide.RIGHT, area = 3.9)
            )
        ),

        // ==================== 2. ХРУЩЁВКИ И РАННИЕ СЕРИИ ====================
        ApartmentTemplate(
            id = "khrushchyovka_1_464",
            title = "Хрущёвка панельная (1-464)",
            subtitle = "1-комн. с выступом балкона и входным карманом (~31 м²)",
            roomCountCategory = 1,
            houseGroup = TemplateHouseGroup.SECONDARY,
            planAspectRatio = 1.40f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Балкон"), room("Комната"), room("Кухня"), room("Прихожая"), room("Санузел")),
            schematicRooms = listOf(
                rect(0.05f, 0.04f, 0.56f, 0.15f, "Балкон", isBalcony = true, area = 2.4),
                rect(
                    0.05f, 0.15f, 0.58f, 0.56f, "Комната",
                    area = 17.2,
                    openings = listOf(balcTop(0.48f), doorBottom(0.75f, 0.24f))
                ),
                rect(
                    0.58f, 0.16f, 0.94f, 0.56f, "Кухня",
                    area = 5.8,
                    fx = BlueprintFixtureKind.KITCHEN,
                    openings = listOf(winTop(0.5f), doorBottom(0.24f, 0.28f))
                ),
                rect(0.30f, 0.56f, 0.74f, 0.94f, "Прихожая", entryDoorSide = EntryDoorSide.BOTTOM, area = 4.6),
                rect(0.74f, 0.56f, 0.94f, 0.90f, "С/У", hasDoorLeft = true, area = 3.0, fx = BlueprintFixtureKind.COMBINED_BATH)
            )
        ),
        ApartmentTemplate(
            id = "khrushchyovka_1_335",
            title = "Хрущёвка блочная (1-335)",
            subtitle = "2-комн. Г-образная с выступом спальни (~43 м²)",
            roomCountCategory = 2,
            houseGroup = TemplateHouseGroup.SECONDARY,
            planAspectRatio = 1.60f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Спальня"), room("Зал"), room("Кухня"), room("Коридор"), room("Санузел")),
            schematicRooms = listOf(
                rect(
                    0.04f, 0.06f, 0.36f, 0.70f, "Спальня",
                    area = 13.8,
                    openings = listOf(winTop(0.5f), winLeft(0.45f), doorRight(0.52f, 0.22f))
                ),
                rect(
                    0.36f, 0.12f, 0.68f, 0.62f, "Зал",
                    area = 17.4,
                    openings = listOf(winTop(0.5f), doorBottom(0.50f, 0.26f))
                ),
                rect(
                    0.68f, 0.12f, 0.96f, 0.58f, "Кухня",
                    area = 6.0,
                    fx = BlueprintFixtureKind.KITCHEN,
                    openings = listOf(winTop(0.5f), doorBottom(0.18f, 0.24f))
                ),
                rect(0.36f, 0.62f, 0.76f, 0.94f, "Коридор", entryDoorSide = EntryDoorSide.BOTTOM, area = 4.2),
                rect(0.76f, 0.58f, 0.96f, 0.90f, "С/У", hasDoorLeft = true, area = 3.0, fx = BlueprintFixtureKind.COMBINED_BATH)
            )
        ),
        ApartmentTemplate(
            id = "kirpich_1_447",
            title = "Кирпичная хрущёвка (1-447)",
            subtitle = "2-к. угловая «книжка» с выступом зала и балкона (~44 м²)",
            roomCountCategory = 2,
            houseGroup = TemplateHouseGroup.SECONDARY,
            planAspectRatio = 1.48f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Балкон"), room("Зал"), room("Спальня"), room("Кухня"), room("Прихожая"), room("Санузел")),
            schematicRooms = listOf(
                rect(0.05f, 0.03f, 0.50f, 0.14f, "Балкон", isBalcony = true, area = 2.6),
                rect(
                    0.05f, 0.14f, 0.54f, 0.58f, "Зал",
                    area = 17.6,
                    openings = listOf(balcTop(0.46f), winLeft(0.5f), doorBottom(0.76f, 0.24f))
                ),
                rect(
                    0.54f, 0.16f, 0.95f, 0.60f, "Спальня",
                    area = 12.4,
                    openings = listOf(winTop(0.5f), winRight(0.5f), doorBottom(0.18f, 0.24f))
                ),
                rect(0.05f, 0.58f, 0.36f, 0.92f, "Кухня", hasDoorRight = true, hasWindowLeft = true, area = 5.9, fx = BlueprintFixtureKind.KITCHEN),
                rect(0.36f, 0.58f, 0.68f, 0.95f, "Прихожая", entryDoorSide = EntryDoorSide.BOTTOM, area = 4.8),
                rect(0.68f, 0.60f, 0.92f, 0.90f, "С/У", hasDoorLeft = true, area = 3.1, fx = BlueprintFixtureKind.COMBINED_BATH)
            )
        ),
        ApartmentTemplate(
            id = "seriya_1_510_511",
            title = "Серия 1-510 / 1-511 (блочно-кирп.)",
            subtitle = "2-к. распашонка · Г-коридор, балкон, выступ спальни (~45 м²)",
            roomCountCategory = 2,
            houseGroup = TemplateHouseGroup.SECONDARY,
            planAspectRatio = 1.62f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Балкон"), room("Зал"), room("Кухня"), room("Спальня"), room("Коридор (Г-форма)", walls = 6, inner = 5, outer = 1), room("Санузел")),
            schematicRooms = listOf(
                rect(0.04f, 0.04f, 0.40f, 0.15f, "Балкон", isBalcony = true, area = 2.5),
                rect(
                    0.04f, 0.15f, 0.44f, 0.60f, "Зал",
                    area = 17.5,
                    openings = listOf(balcTop(0.46f), doorBottom(0.55f, 0.26f))
                ),
                rect(0.44f, 0.14f, 0.70f, 0.54f, "Кухня", hasDoorBottom = true, hasWindowTop = true, area = 6.2, fx = BlueprintFixtureKind.KITCHEN),
                rect(
                    0.70f, 0.06f, 0.96f, 0.94f, "Спальня",
                    area = 13.2,
                    openings = listOf(winTop(0.5f), winBottom(0.5f), doorLeft(0.62f, 0.16f))
                ),
                poly(
                    pts = listOf(
                        0.04f to 0.60f, 0.44f to 0.60f, 0.44f to 0.54f,
                        0.70f to 0.54f, 0.70f to 0.68f, 0.48f to 0.68f,
                        0.48f to 0.92f, 0.04f to 0.92f
                    ),
                    label = "Коридор",
                    entryDoorEdge = 6,
                    labelPos = 0.26f to 0.76f,
                    area = 5.0
                ),
                rect(0.48f, 0.68f, 0.70f, 0.94f, "С/У", hasDoorLeft = true, area = 3.1, fx = BlueprintFixtureKind.COMBINED_BATH)
            )
        ),
        ApartmentTemplate(
            id = "seriya_1_467",
            title = "Серия 1-467 (линейная + балкон)",
            subtitle = "Выступающий балкон слева, раздельный с/у (~46 м²)",
            roomCountCategory = 2,
            houseGroup = TemplateHouseGroup.SECONDARY,
            planAspectRatio = 1.68f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Балкон"), room("Спальня"), room("Зал"), room("Кухня"), room("Ванная"), room("Туалет"), room("Коридор")),
            schematicRooms = listOf(
                rect(0.04f, 0.03f, 0.36f, 0.14f, "Балкон", isBalcony = true, area = 2.6),
                rect(
                    0.04f, 0.14f, 0.40f, 0.64f, "Спальня",
                    area = 13.6,
                    openings = listOf(balcTop(0.46f), doorBottom(0.84f, 0.22f))
                ),
                rect(0.40f, 0.14f, 0.72f, 0.64f, "Зал", hasDoorBottom = true, hasWindowTop = true, area = 17.8),
                rect(0.72f, 0.14f, 0.96f, 0.64f, "Кухня", hasDoorBottom = true, hasWindowTop = true, area = 6.3, fx = BlueprintFixtureKind.KITCHEN),
                rect(0.04f, 0.64f, 0.28f, 0.79f, "Ван.", hasDoorRight = true, area = 2.2, fx = BlueprintFixtureKind.BATH),
                rect(0.04f, 0.79f, 0.28f, 0.92f, "С/У", hasDoorRight = true, area = 1.1, fx = BlueprintFixtureKind.TOILET),
                rect(0.28f, 0.64f, 0.96f, 0.94f, "Коридор", entryDoorSide = EntryDoorSide.RIGHT, area = 5.4)
            )
        ),
        ApartmentTemplate(
            id = "seriya_1_515",
            title = "Серия 1-515 (9-эт. панель)",
            subtitle = "3-комн. ступенчатая с Г-коридором и балконом (~62 м²)",
            roomCountCategory = 3,
            houseGroup = TemplateHouseGroup.SECONDARY,
            planAspectRatio = 1.72f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Балкон"), room("Спальня 1"), room("Зал"), room("Спальня 2"), room("Кухня"), room("Коридор (Г-форма)", walls = 6, inner = 5, outer = 1), room("Санузел")),
            schematicRooms = listOf(
                rect(0.34f, 0.03f, 0.68f, 0.14f, "Балкон", isBalcony = true, area = 2.8),
                rect(
                    0.04f, 0.08f, 0.34f, 0.56f, "Сп.1",
                    area = 11.8,
                    openings = listOf(winTop(0.5f), doorRight(0.50f, 0.26f))
                ),
                rect(
                    0.34f, 0.14f, 0.68f, 0.56f, "Зал",
                    area = 18.4,
                    openings = listOf(balcTop(0.50f), doorBottom(0.50f, 0.26f))
                ),
                rect(
                    0.68f, 0.14f, 0.96f, 0.56f, "Сп.2",
                    area = 13.0,
                    openings = listOf(winTop(0.5f), doorBottom(0.20f, 0.24f))
                ),
                rect(0.04f, 0.56f, 0.34f, 0.92f, "Кухня", hasDoorRight = true, hasWindowLeft = true, area = 7.4, fx = BlueprintFixtureKind.KITCHEN),
                poly(
                    pts = listOf(
                        0.34f to 0.56f, 0.78f to 0.56f, 0.78f to 0.82f,
                        0.96f to 0.82f, 0.96f to 0.94f, 0.34f to 0.94f
                    ),
                    label = "Коридор",
                    entryDoorEdge = 4,
                    labelPos = 0.55f to 0.76f,
                    area = 7.6
                ),
                rect(0.78f, 0.56f, 0.96f, 0.82f, "С/У", hasDoorLeft = true, area = 3.8, fx = BlueprintFixtureKind.COMBINED_BATH)
            )
        ),

        // ==================== 3. СТАЛИНКИ И СТАРЫЙ ФОНД ====================
        ApartmentTemplate(
            id = "stalinka_ryadovaya",
            title = "Сталинка с эркером (2-к.)",
            subtitle = "5-стенный эркер в гостиной + утопленная входная (~58 м²)",
            roomCountCategory = 2,
            houseGroup = TemplateHouseGroup.SECONDARY,
            planAspectRatio = 1.52f,
            hasNonRectangularShape = true,
            rooms = listOf(
                room("Гостиная с эркером", walls = 6, hasSlanted = true),
                room("Спальня"),
                room("Кухня"),
                room("Прихожая"),
                room("Ванная"),
                room("Туалет")
            ),
            schematicRooms = listOf(
                poly(
                    pts = listOf(0.05f to 0.16f, 0.15f to 0.04f, 0.38f to 0.04f, 0.48f to 0.16f, 0.48f to 0.60f, 0.05f to 0.60f),
                    label = "Гостиная",
                    windowEdges = listOf(0, 1, 2),
                    openings = listOf(polyOp(4, center = 0.16f, w = 0.24f)),
                    labelPos = 0.26f to 0.36f,
                    area = 21.4
                ),
                rect(
                    0.48f, 0.16f, 0.94f, 0.60f, "Спальня",
                    area = 15.6,
                    openings = listOf(winTop(0.5f), doorBottom(0.28f, 0.24f))
                ),
                rect(0.05f, 0.60f, 0.36f, 0.94f, "Кухня", hasDoorRight = true, hasWindowLeft = true, area = 8.8, fx = BlueprintFixtureKind.KITCHEN),
                rect(0.36f, 0.60f, 0.72f, 0.90f, "Прихожая", entryDoorSide = EntryDoorSide.BOTTOM, area = 7.4),
                rect(0.72f, 0.60f, 0.94f, 0.77f, "Ван.", hasDoorLeft = true, area = 3.2, fx = BlueprintFixtureKind.BATH),
                rect(0.72f, 0.77f, 0.94f, 0.94f, "С/У", hasDoorLeft = true, area = 1.5, fx = BlueprintFixtureKind.TOILET)
            )
        ),
        ApartmentTemplate(
            id = "stalinka_nomenklaturnaya",
            title = "Сталинка номенклатурная (угловая 3-к.)",
            subtitle = "Г-образный план со срезанным углом на площади (~79 м²)",
            roomCountCategory = 3,
            houseGroup = TemplateHouseGroup.SECONDARY,
            planAspectRatio = 1.50f,
            hasNonRectangularShape = true,
            rooms = listOf(
                room("Гостиная (скош. угол)", walls = 5, hasSlanted = true),
                room("Спальня"),
                room("Кабинет"),
                room("Кухня"),
                room("Холл (Г-форма)", walls = 6, inner = 5, outer = 1),
                room("Ванная"),
                room("Туалет")
            ),
            schematicRooms = listOf(
                poly(
                    pts = listOf(0.04f to 0.24f, 0.22f to 0.05f, 0.44f to 0.05f, 0.44f to 0.56f, 0.04f to 0.56f),
                    label = "Гостиная",
                    windowEdges = listOf(0, 1),
                    openings = listOf(polyOp(3, center = 0.14f, w = 0.22f)),
                    labelPos = 0.26f to 0.34f,
                    area = 22.5
                ),
                rect(0.44f, 0.05f, 0.70f, 0.56f, "Спальня", hasDoorBottom = true, hasWindowTop = true, area = 15.8),
                rect(
                    0.70f, 0.14f, 0.96f, 0.56f, "Кабинет",
                    area = 13.4,
                    openings = listOf(winTop(0.5f), doorBottom(0.20f, 0.24f))
                ),
                rect(0.04f, 0.56f, 0.34f, 0.94f, "Кухня", hasDoorRight = true, hasWindowLeft = true, area = 10.6, fx = BlueprintFixtureKind.KITCHEN),
                poly(
                    pts = listOf(
                        0.34f to 0.56f, 0.80f to 0.56f, 0.80f to 0.82f,
                        0.96f to 0.82f, 0.96f to 0.94f, 0.34f to 0.94f
                    ),
                    label = "Холл",
                    entryDoorEdge = 4,
                    labelPos = 0.55f to 0.76f,
                    area = 11.2
                ),
                rect(0.80f, 0.56f, 0.96f, 0.69f, "Ван.", hasDoorLeft = true, area = 3.8, fx = BlueprintFixtureKind.BATH),
                rect(0.80f, 0.69f, 0.96f, 0.82f, "С/У", hasDoorLeft = true, area = 1.6, fx = BlueprintFixtureKind.TOILET)
            )
        ),
        ApartmentTemplate(
            id = "seriya_1_228",
            title = "Серия 1-228 (2–3 эт. малоэтажка)",
            subtitle = "Кимовск и др. · шлакоблок/кирпич, дер. перекрытия, выс. 2.9 м (~47 м²)",
            roomCountCategory = 2,
            houseGroup = TemplateHouseGroup.SECONDARY,
            planAspectRatio = 1.48f,
            hasNonRectangularShape = true,
            rooms = listOf(
                room("Зал (с эркером/выступом)", walls = 5, hasSlanted = true),
                room("Спальня"),
                room("Кухня"),
                room("Прихожая (Г-форма)", walls = 6, inner = 5, outer = 1),
                room("Санузел")
            ),
            schematicRooms = listOf(
                poly(
                    pts = listOf(
                        0.04f to 0.14f, 0.14f to 0.04f, 0.48f to 0.04f,
                        0.52f to 0.58f, 0.04f to 0.58f
                    ),
                    label = "Зал",
                    windowEdges = listOf(0, 1, 4),
                    openings = listOf(polyOp(3, center = 0.18f, w = 0.24f)),
                    labelPos = 0.28f to 0.34f,
                    area = 18.2
                ),
                rect(
                    0.52f, 0.12f, 0.96f, 0.58f, "Спальня",
                    area = 13.4,
                    openings = listOf(winTop(0.5f), winRight(0.5f), doorBottom(0.22f, 0.24f))
                ),
                rect(0.04f, 0.58f, 0.36f, 0.94f, "Кухня", hasDoorRight = true, hasWindowLeft = true, area = 6.8, fx = BlueprintFixtureKind.KITCHEN),
                poly(
                    pts = listOf(
                        0.36f to 0.58f, 0.72f to 0.58f, 0.72f to 0.82f,
                        0.95f to 0.82f, 0.95f to 0.94f, 0.36f to 0.94f
                    ),
                    label = "Прихожая",
                    entryDoorEdge = 4,
                    labelPos = 0.54f to 0.76f,
                    area = 5.6
                ),
                rect(0.72f, 0.58f, 0.95f, 0.82f, "С/У", hasDoorLeft = true, area = 3.4, fx = BlueprintFixtureKind.COMBINED_BATH)
            )
        ),
        ApartmentTemplate(
            id = "barachniy_tip",
            title = "Барачный тип (малоэтажный)",
            subtitle = "Г-образная пристройка · вход с торца (~41 м²)",
            roomCountCategory = 2,
            houseGroup = TemplateHouseGroup.SPECIAL,
            planAspectRatio = 1.58f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Комната 1"), room("Комната 2"), room("Кухня"), room("Коридор"), room("Санузел")),
            schematicRooms = listOf(
                rect(
                    0.04f, 0.06f, 0.48f, 0.62f, "Комн.1",
                    area = 14.0,
                    openings = listOf(winTop(0.5f), doorBottom(0.86f, 0.18f))
                ),
                rect(
                    0.48f, 0.06f, 0.96f, 0.62f, "Комн.2",
                    area = 12.6,
                    openings = listOf(winTop(0.5f), doorBottom(0.32f, 0.24f))
                ),
                rect(0.10f, 0.62f, 0.38f, 0.94f, "Кухня", hasDoorRight = true, hasWindowBottom = true, area = 6.2, fx = BlueprintFixtureKind.KITCHEN),
                rect(0.38f, 0.62f, 0.80f, 0.94f, "Коридор", entryDoorSide = EntryDoorSide.BOTTOM, area = 5.4),
                rect(0.80f, 0.62f, 0.96f, 0.88f, "С/У", hasDoorLeft = true, area = 2.8, fx = BlueprintFixtureKind.COMBINED_BATH)
            )
        ),
        ApartmentTemplate(
            id = "kommunalnaya",
            title = "Коммунальная квартира",
            subtitle = "Длинный коридор + выступ общей кухни (~72 м²)",
            roomCountCategory = 3,
            houseGroup = TemplateHouseGroup.SPECIAL,
            planAspectRatio = 1.85f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Комната 1"), room("Комната 2"), room("Комната 3"), room("Общая кухня"), room("Коридор"), room("Санузел")),
            schematicRooms = listOf(
                rect(0.04f, 0.08f, 0.26f, 0.62f, "Комн.1", hasDoorBottom = true, hasWindowTop = true, area = 15.2),
                rect(0.26f, 0.08f, 0.48f, 0.62f, "Комн.2", hasDoorBottom = true, hasWindowTop = true, area = 14.6),
                rect(0.48f, 0.08f, 0.70f, 0.62f, "Комн.3", hasDoorBottom = true, hasWindowTop = true, area = 16.0),
                rect(
                    0.70f, 0.04f, 0.96f, 0.62f, "Кухня",
                    area = 11.4,
                    fx = BlueprintFixtureKind.KITCHEN,
                    openings = listOf(winTop(0.5f), winRight(0.5f), doorBottom(0.18f, 0.24f))
                ),
                rect(0.04f, 0.62f, 0.78f, 0.92f, "Длинный коридор", entryDoorSide = EntryDoorSide.LEFT, area = 10.5),
                rect(0.78f, 0.62f, 0.96f, 0.94f, "С/У", hasDoorLeft = true, area = 4.2, fx = BlueprintFixtureKind.COMBINED_BATH)
            )
        ),

        // ==================== 4. БРЕЖНЕВКИ И СОВЕТСКИЕ СЕРИИ ====================
        ApartmentTemplate(
            id = "brezhnevka_2_raspa",
            title = "Брежневка 2-к. (распашонка)",
            subtitle = "Утопленная входная группа · длинный зал (~46 м²)",
            roomCountCategory = 2,
            houseGroup = TemplateHouseGroup.SECONDARY,
            planAspectRatio = 1.58f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Зал"), room("Кухня"), room("Спальня"), room("Коридор (Г-форма)", walls = 6, inner = 5, outer = 1), room("Ванная"), room("Туалет")),
            schematicRooms = listOf(
                rect(
                    0.04f, 0.06f, 0.42f, 0.94f, "Зал",
                    area = 18.0,
                    openings = listOf(winTop(0.5f), winBottom(0.5f), doorRight(0.62f, 0.16f))
                ),
                rect(0.42f, 0.14f, 0.72f, 0.52f, "Кухня", hasDoorBottom = true, hasWindowTop = true, area = 6.5, fx = BlueprintFixtureKind.KITCHEN),
                rect(
                    0.72f, 0.06f, 0.96f, 0.88f, "Спальня",
                    area = 13.8,
                    openings = listOf(winTop(0.5f), winBottom(0.5f), doorLeft(0.62f, 0.16f))
                ),
                poly(
                    pts = listOf(
                        0.42f to 0.52f, 0.72f to 0.52f, 0.72f to 0.72f,
                        0.57f to 0.72f, 0.57f to 0.92f, 0.42f to 0.92f
                    ),
                    label = "Коридор",
                    entryDoorEdge = 4,
                    labelPos = 0.54f to 0.64f,
                    area = 4.8
                ),
                rect(0.57f, 0.72f, 0.72f, 0.82f, "Ван.", hasDoorLeft = true, area = 2.1, fx = BlueprintFixtureKind.BATH),
                rect(0.57f, 0.82f, 0.72f, 0.92f, "С/У", hasDoorLeft = true, area = 1.1, fx = BlueprintFixtureKind.TOILET)
            )
        ),
        ApartmentTemplate(
            id = "seriya_ii_18",
            title = "Серия II-18 (башня 1-к.)",
            subtitle = "Г-контур с выступающим балконом · вход справа (~35 м²)",
            roomCountCategory = 1,
            houseGroup = TemplateHouseGroup.SECONDARY,
            planAspectRatio = 1.38f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Балкон"), room("Комната"), room("Кухня"), room("Прихожая"), room("Санузел")),
            schematicRooms = listOf(
                rect(0.04f, 0.03f, 0.56f, 0.14f, "Балкон", isBalcony = true, area = 2.6),
                rect(
                    0.04f, 0.14f, 0.58f, 0.68f, "Комната",
                    area = 18.8,
                    openings = listOf(balcTop(0.48f), doorBottom(0.84f, 0.20f))
                ),
                rect(0.58f, 0.15f, 0.94f, 0.58f, "Кухня", hasDoorBottom = true, hasWindowTop = true, area = 7.2, fx = BlueprintFixtureKind.KITCHEN),
                rect(0.12f, 0.68f, 0.44f, 0.94f, "С/У", hasDoorRight = true, area = 3.1, fx = BlueprintFixtureKind.COMBINED_BATH),
                rect(0.44f, 0.58f, 0.94f, 0.94f, "Прихожая", entryDoorSide = EntryDoorSide.RIGHT, area = 4.5)
            )
        ),
        ApartmentTemplate(
            id = "seriya_ii_29",
            title = "Серия II-29 (кирпичная 2-к.)",
            subtitle = "Г-образная угловая · два окна в зале (~48 м²)",
            roomCountCategory = 2,
            houseGroup = TemplateHouseGroup.SECONDARY,
            planAspectRatio = 1.48f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Зал"), room("Спальня"), room("Кухня"), room("Коридор"), room("Санузел")),
            schematicRooms = listOf(
                rect(
                    0.04f, 0.06f, 0.54f, 0.58f, "Зал",
                    area = 18.6,
                    openings = listOf(winTop(0.5f), winLeft(0.5f), doorBottom(0.80f, 0.22f))
                ),
                rect(
                    0.54f, 0.14f, 0.96f, 0.58f, "Спальня",
                    area = 13.4,
                    openings = listOf(winTop(0.5f), doorBottom(0.22f, 0.24f))
                ),
                rect(0.04f, 0.58f, 0.36f, 0.94f, "Кухня", hasDoorRight = true, hasWindowLeft = true, area = 7.4, fx = BlueprintFixtureKind.KITCHEN),
                rect(0.36f, 0.58f, 0.72f, 0.92f, "Коридор", entryDoorSide = EntryDoorSide.BOTTOM, area = 5.4),
                rect(0.72f, 0.58f, 0.94f, 0.90f, "С/У", hasDoorLeft = true, area = 3.2, fx = BlueprintFixtureKind.COMBINED_BATH)
            )
        ),
        ApartmentTemplate(
            id = "seriya_ii_49",
            title = "Серия II-49 (3-комн.)",
            subtitle = "Распашонка + выступающий балкон + ниша с/у (~59 м²)",
            roomCountCategory = 3,
            houseGroup = TemplateHouseGroup.SECONDARY,
            planAspectRatio = 1.68f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Балкон"), room("Спальня 1"), room("Зал"), room("Кухня"), room("Спальня 2"), room("Коридор"), room("Ванная"), room("Туалет")),
            schematicRooms = listOf(
                rect(0.36f, 0.03f, 0.68f, 0.14f, "Балкон", isBalcony = true, area = 2.6),
                rect(
                    0.04f, 0.12f, 0.36f, 0.60f, "Сп.1",
                    area = 11.4,
                    openings = listOf(winTop(0.5f), doorBottom(0.86f, 0.18f))
                ),
                rect(
                    0.36f, 0.14f, 0.68f, 0.60f, "Зал",
                    area = 17.8,
                    openings = listOf(balcTop(0.50f), doorBottom(0.50f, 0.26f))
                ),
                rect(
                    0.68f, 0.14f, 0.96f, 0.60f, "Кухня",
                    area = 6.4,
                    fx = BlueprintFixtureKind.KITCHEN,
                    openings = listOf(winTop(0.5f), doorBottom(0.18f, 0.24f))
                ),
                rect(0.04f, 0.60f, 0.30f, 0.95f, "Сп.2", hasDoorRight = true, hasWindowBottom = true, area = 11.2),
                rect(0.30f, 0.60f, 0.76f, 0.92f, "Коридор", entryDoorSide = EntryDoorSide.BOTTOM, area = 7.4),
                rect(0.76f, 0.60f, 0.96f, 0.76f, "Ван.", hasDoorLeft = true, area = 2.4, fx = BlueprintFixtureKind.BATH),
                rect(0.76f, 0.76f, 0.96f, 0.92f, "С/У", hasDoorLeft = true, area = 1.2, fx = BlueprintFixtureKind.TOILET)
            )
        ),
        ApartmentTemplate(
            id = "seriya_ii_57",
            title = "Серия II-57 (с косой лоджией)",
            subtitle = "Линейная + выступающий спаренный балкон (~48 м²)",
            roomCountCategory = 2,
            houseGroup = TemplateHouseGroup.SECONDARY,
            planAspectRatio = 1.60f,
            hasNonRectangularShape = true,
            rooms = listOf(
                room("Лоджия (трапеция)", walls = 4, hasSlanted = true),
                room("Спальня"),
                room("Зал"),
                room("Кухня"),
                room("Коридор"),
                room("Ванная"),
                room("Туалет")
            ),
            schematicRooms = listOf(
                poly(
                    pts = listOf(0.06f to 0.04f, 0.64f to 0.04f, 0.68f to 0.15f, 0.04f to 0.15f),
                    label = "Лоджия",
                    isBalcony = true,
                    area = 4.2
                ),
                rect(
                    0.04f, 0.15f, 0.36f, 0.62f, "Спальня",
                    area = 12.6,
                    openings = listOf(balcTop(0.50f), doorBottom(0.50f, 0.26f))
                ),
                rect(
                    0.36f, 0.15f, 0.70f, 0.62f, "Зал",
                    area = 18.2,
                    openings = listOf(balcTop(0.48f), doorBottom(0.50f, 0.26f))
                ),
                rect(
                    0.70f, 0.18f, 0.96f, 0.62f, "Кухня",
                    area = 6.8,
                    fx = BlueprintFixtureKind.KITCHEN,
                    openings = listOf(winTop(0.5f), doorBottom(0.18f, 0.24f))
                ),
                rect(0.04f, 0.62f, 0.78f, 0.94f, "Коридор", entryDoorSide = EntryDoorSide.BOTTOM, area = 6.2),
                rect(0.78f, 0.62f, 0.96f, 0.78f, "Ван.", hasDoorLeft = true, area = 2.4, fx = BlueprintFixtureKind.BATH),
                rect(0.78f, 0.78f, 0.96f, 0.94f, "С/У", hasDoorLeft = true, area = 1.2, fx = BlueprintFixtureKind.TOILET)
            )
        ),
        ApartmentTemplate(
            id = "seriya_ii_68",
            title = "Серия II-68 (башня 2-к.)",
            subtitle = "Выступающая лоджия на 2 комнаты + кухня-выступ (~51 м²)",
            roomCountCategory = 2,
            houseGroup = TemplateHouseGroup.SECONDARY,
            planAspectRatio = 1.50f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Лоджия"), room("Спальня"), room("Зал"), room("Кухня"), room("Прихожая"), room("Ванная"), room("Туалет")),
            schematicRooms = listOf(
                rect(0.04f, 0.04f, 0.70f, 0.16f, "Лоджия", isBalcony = true, area = 4.6),
                rect(
                    0.04f, 0.16f, 0.40f, 0.64f, "Спальня",
                    area = 13.4,
                    openings = listOf(balcTop(0.50f), doorBottom(0.52f, 0.26f))
                ),
                rect(
                    0.40f, 0.16f, 0.70f, 0.64f, "Зал",
                    area = 18.8,
                    openings = listOf(balcTop(0.50f), doorBottom(0.50f, 0.26f))
                ),
                rect(
                    0.70f, 0.22f, 0.96f, 0.64f, "Кухня",
                    area = 8.4,
                    fx = BlueprintFixtureKind.KITCHEN,
                    openings = listOf(winTop(0.5f), doorBottom(0.18f, 0.22f))
                ),
                rect(0.08f, 0.64f, 0.78f, 0.95f, "Прихожая", entryDoorSide = EntryDoorSide.LEFT, area = 6.5),
                rect(0.78f, 0.64f, 0.96f, 0.78f, "Ван.", hasDoorLeft = true, area = 2.6, fx = BlueprintFixtureKind.BATH),
                rect(0.78f, 0.78f, 0.96f, 0.92f, "С/У", hasDoorLeft = true, area = 1.2, fx = BlueprintFixtureKind.TOILET)
            )
        ),
        ApartmentTemplate(
            id = "seriya_114_85",
            title = "Серия 114-85 (3-комн.)",
            subtitle = "Улучшенная кирпичная · Г-силуэт, выступ зала (~65 м²)",
            roomCountCategory = 3,
            houseGroup = TemplateHouseGroup.SECONDARY,
            planAspectRatio = 1.62f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Зал"), room("Спальня 1"), room("Спальня 2"), room("Кухня"), room("Холл"), room("Ванная"), room("Туалет")),
            schematicRooms = listOf(
                rect(
                    0.04f, 0.05f, 0.40f, 0.56f, "Зал",
                    area = 18.6,
                    openings = listOf(winTop(0.5f), doorBottom(0.86f, 0.18f))
                ),
                rect(0.40f, 0.12f, 0.68f, 0.56f, "Сп.1", hasDoorBottom = true, hasWindowTop = true, area = 13.8),
                rect(
                    0.68f, 0.12f, 0.96f, 0.56f, "Сп.2",
                    area = 12.2,
                    openings = listOf(winTop(0.5f), doorBottom(0.22f, 0.24f))
                ),
                rect(0.04f, 0.56f, 0.32f, 0.94f, "Кухня", hasDoorRight = true, hasWindowLeft = true, area = 8.4, fx = BlueprintFixtureKind.KITCHEN),
                rect(0.32f, 0.56f, 0.78f, 0.94f, "Холл", entryDoorSide = EntryDoorSide.BOTTOM, area = 8.2),
                rect(0.78f, 0.56f, 0.96f, 0.73f, "Ван.", hasDoorLeft = true, area = 2.8, fx = BlueprintFixtureKind.BATH),
                rect(0.78f, 0.73f, 0.96f, 0.90f, "С/У", hasDoorLeft = true, area = 1.4, fx = BlueprintFixtureKind.TOILET)
            )
        ),
        ApartmentTemplate(
            id = "seriya_90",
            title = "Серия 90 / 111-90 (2-к.)",
            subtitle = "Новомосковск · Г-контур, выступ лоджии, 9.5 м² кухня (~55 м²)",
            roomCountCategory = 2,
            houseGroup = TemplateHouseGroup.SECONDARY,
            planAspectRatio = 1.54f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Лоджия"), room("Зал"), room("Спальня"), room("Кухня"), room("Холл"), room("Ванная"), room("Туалет")),
            schematicRooms = listOf(
                rect(0.50f, 0.03f, 0.94f, 0.14f, "Лоджия", isBalcony = true, area = 3.6),
                rect(0.04f, 0.06f, 0.50f, 0.56f, "Зал", hasDoorBottom = true, hasWindowTop = true, area = 17.8),
                rect(
                    0.50f, 0.14f, 0.96f, 0.56f, "Спальня",
                    area = 13.4,
                    openings = listOf(balcTop(0.48f), doorBottom(0.24f, 0.24f))
                ),
                rect(0.04f, 0.56f, 0.38f, 0.95f, "Кухня", hasDoorRight = true, hasWindowBottom = true, area = 9.5, fx = BlueprintFixtureKind.KITCHEN),
                rect(0.38f, 0.56f, 0.72f, 0.90f, "Холл", entryDoorSide = EntryDoorSide.BOTTOM, area = 6.8),
                rect(0.72f, 0.56f, 0.94f, 0.74f, "Ван.", hasDoorLeft = true, area = 2.6, fx = BlueprintFixtureKind.BATH),
                rect(0.72f, 0.74f, 0.94f, 0.92f, "С/У", hasDoorLeft = true, area = 1.3, fx = BlueprintFixtureKind.TOILET)
            )
        ),
        ApartmentTemplate(
            id = "seriya_121",
            title = "Серия 121 (3-к. распашонка)",
            subtitle = "Новомосковск · Г-прихожая, утопленный вход (~66 м²)",
            roomCountCategory = 3,
            houseGroup = TemplateHouseGroup.SECONDARY,
            planAspectRatio = 1.66f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Зал"), room("Спальня 1"), room("Спальня 2"), room("Кухня"), room("Прихожая (Г-форма)", walls = 6, inner = 5, outer = 1), room("Санузел")),
            schematicRooms = listOf(
                rect(
                    0.04f, 0.06f, 0.38f, 0.54f, "Зал",
                    area = 17.6,
                    openings = listOf(winTop(0.5f), doorBottom(0.88f, 0.18f))
                ),
                rect(0.38f, 0.12f, 0.67f, 0.54f, "Сп.1", hasDoorBottom = true, hasWindowTop = true, area = 13.8),
                rect(
                    0.67f, 0.06f, 0.96f, 0.54f, "Сп.2",
                    area = 12.0,
                    openings = listOf(winTop(0.5f), doorBottom(0.18f, 0.24f))
                ),
                rect(0.04f, 0.54f, 0.32f, 0.94f, "Кухня", hasDoorRight = true, hasWindowBottom = true, area = 9.0, fx = BlueprintFixtureKind.KITCHEN),
                poly(
                    pts = listOf(
                        0.32f to 0.54f, 0.76f to 0.54f, 0.76f to 0.82f,
                        0.96f to 0.82f, 0.96f to 0.94f, 0.32f to 0.94f
                    ),
                    label = "Прихожая",
                    entryDoorEdge = 4,
                    labelPos = 0.54f to 0.74f,
                    area = 9.2
                ),
                rect(0.76f, 0.54f, 0.96f, 0.82f, "С/У", hasDoorLeft = true, area = 4.2, fx = BlueprintFixtureKind.COMBINED_BATH)
            )
        ),
        ApartmentTemplate(
            id = "seriya_p44",
            title = "Серия П-44 / П-44Т (с эркером 2-к.)",
            subtitle = "Трапециевидный эркер на кухне (6 стен) + косая лоджия (~60 м²)",
            roomCountCategory = 2,
            houseGroup = TemplateHouseGroup.SECONDARY,
            planAspectRatio = 1.56f,
            hasNonRectangularShape = true,
            rooms = listOf(
                room("Зал"),
                room("Спальня"),
                room("Кухня с эркером", walls = 6, hasSlanted = true),
                room("Холл (Г-форма)", walls = 6, inner = 5, outer = 1),
                room("Ванная"),
                room("Туалет"),
                room("Лоджия-сапожок", walls = 4, hasSlanted = true)
            ),
            schematicRooms = listOf(
                rect(
                    0.04f, 0.14f, 0.40f, 0.92f, "Зал",
                    area = 19.1,
                    openings = listOf(winTop(0.5f), winBottom(0.5f), doorRight(0.62f, 0.16f))
                ),
                rect(
                    0.70f, 0.14f, 0.95f, 0.72f, "Спальня",
                    area = 14.2,
                    openings = listOf(winTop(0.5f), balcBottom(0.50f), doorLeft(0.80f, 0.20f))
                ),
                poly(
                    pts = listOf(
                        0.40f to 0.14f, 0.46f to 0.04f, 0.64f to 0.04f,
                        0.70f to 0.14f, 0.70f to 0.52f, 0.40f to 0.52f
                    ),
                    label = "Кухня",
                    windowEdges = listOf(0, 1, 2),
                    doorEdges = listOf(4),
                    labelPos = 0.55f to 0.30f,
                    area = 12.8,
                    fx = BlueprintFixtureKind.KITCHEN
                ),
                poly(
                    pts = listOf(
                        0.40f to 0.52f, 0.70f to 0.52f, 0.70f to 0.74f,
                        0.56f to 0.74f, 0.56f to 0.94f, 0.40f to 0.94f
                    ),
                    label = "Холл",
                    entryDoorEdge = 4,
                    labelPos = 0.52f to 0.65f,
                    area = 8.6
                ),
                rect(0.56f, 0.74f, 0.70f, 0.84f, "Ван.", hasDoorLeft = true, area = 3.1, fx = BlueprintFixtureKind.BATH),
                rect(0.56f, 0.84f, 0.70f, 0.94f, "С/У", hasDoorLeft = true, area = 1.4, fx = BlueprintFixtureKind.TOILET),
                poly(
                    pts = listOf(0.70f to 0.72f, 0.95f to 0.72f, 0.89f to 0.94f, 0.70f to 0.94f),
                    label = "Лоджия",
                    isBalcony = true,
                    area = 3.6
                )
            )
        ),

        // ==================== 5. РЕГИОНАЛЬНЫЕ ПРОЕКТЫ ====================
        ApartmentTemplate(
            id = "uluchshenka_2",
            title = "Улучшенка (2-комн.)",
            subtitle = "Г-форма · квадратный холл, лоджия сверху слева (~53 м²)",
            roomCountCategory = 2,
            houseGroup = TemplateHouseGroup.SECONDARY,
            planAspectRatio = 1.50f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Лоджия"), room("Гостиная"), room("Спальня"), room("Кухня"), room("Холл"), room("Ванная"), room("Туалет")),
            schematicRooms = listOf(
                rect(0.04f, 0.03f, 0.50f, 0.13f, "Лоджия", isBalcony = true, area = 3.2),
                rect(
                    0.04f, 0.13f, 0.50f, 0.58f, "Гостиная",
                    area = 18.2,
                    openings = listOf(balcTop(0.48f), doorBottom(0.86f, 0.18f))
                ),
                rect(
                    0.50f, 0.14f, 0.96f, 0.58f, "Спальня",
                    area = 13.6,
                    openings = listOf(winTop(0.5f), doorBottom(0.26f, 0.24f))
                ),
                rect(0.04f, 0.58f, 0.36f, 0.94f, "Кухня", hasDoorRight = true, hasWindowLeft = true, area = 8.8, fx = BlueprintFixtureKind.KITCHEN),
                rect(0.36f, 0.58f, 0.76f, 0.90f, "Холл", entryDoorSide = EntryDoorSide.BOTTOM, area = 6.8),
                rect(0.76f, 0.58f, 0.96f, 0.74f, "Ван.", hasDoorLeft = true, area = 2.6, fx = BlueprintFixtureKind.BATH),
                rect(0.76f, 0.74f, 0.96f, 0.90f, "С/У", hasDoorLeft = true, area = 1.3, fx = BlueprintFixtureKind.TOILET)
            )
        ),
        ApartmentTemplate(
            id = "cheshka_2",
            title = "Чешка (чешский проект 2-к.)",
            subtitle = "Г-образная квартира + 6-стенный коридор + косая лоджия (~52 м²)",
            roomCountCategory = 2,
            houseGroup = TemplateHouseGroup.SECONDARY,
            planAspectRatio = 1.52f,
            hasNonRectangularShape = true,
            rooms = listOf(
                room("Лоджия"),
                room("Кухня"),
                room("Зал"),
                room("Спальня"),
                room("Коридор (Г-форма)", walls = 6, inner = 5, outer = 1),
                room("Санузел")
            ),
            schematicRooms = listOf(
                poly(
                    pts = listOf(0.04f to 0.04f, 0.58f to 0.04f, 0.64f to 0.16f, 0.04f to 0.16f),
                    label = "Лоджия",
                    isBalcony = true,
                    area = 4.0
                ),
                rect(
                    0.04f, 0.16f, 0.36f, 0.58f, "Кухня",
                    area = 8.4,
                    fx = BlueprintFixtureKind.KITCHEN,
                    openings = listOf(balcTop(0.50f), doorBottom(0.50f, 0.26f))
                ),
                rect(
                    0.36f, 0.16f, 0.68f, 0.58f, "Зал",
                    area = 17.8,
                    openings = listOf(balcTop(0.42f), doorBottom(0.50f, 0.26f))
                ),
                rect(
                    0.68f, 0.14f, 0.96f, 0.94f, "Спальня",
                    area = 12.6,
                    openings = listOf(winTop(0.5f), doorLeft(0.62f, 0.14f))
                ),
                poly(
                    pts = listOf(0.04f to 0.58f, 0.68f to 0.58f, 0.68f to 0.68f, 0.44f to 0.68f, 0.44f to 0.90f, 0.04f to 0.90f),
                    label = "Коридор",
                    entryDoorEdge = 5,
                    labelPos = 0.24f to 0.74f,
                    area = 6.2
                ),
                rect(0.44f, 0.68f, 0.68f, 0.94f, "С/У", hasDoorLeft = true, area = 3.8, fx = BlueprintFixtureKind.COMBINED_BATH)
            )
        ),
        ApartmentTemplate(
            id = "leningradka_3",
            title = "Ленинградка (3-комн.)",
            subtitle = "Выступающая лоджия на 3 окна + утопленный коридор (~67 м²)",
            roomCountCategory = 3,
            houseGroup = TemplateHouseGroup.SECONDARY,
            planAspectRatio = 1.82f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Лоджия"), room("Кухня"), room("Гостиная"), room("Спальня 1"), room("Спальня 2"), room("Коридор"), room("Санузел")),
            schematicRooms = listOf(
                rect(0.04f, 0.04f, 0.75f, 0.15f, "Лоджия", isBalcony = true, area = 5.4),
                rect(
                    0.04f, 0.15f, 0.28f, 0.62f, "Кухня",
                    area = 9.0,
                    fx = BlueprintFixtureKind.KITCHEN,
                    openings = listOf(balcTop(0.5f), doorBottom(0.62f, 0.26f))
                ),
                rect(
                    0.28f, 0.15f, 0.54f, 0.62f, "Гост.",
                    area = 18.0,
                    openings = listOf(balcTop(0.5f), doorBottom(0.5f, 0.26f))
                ),
                rect(
                    0.54f, 0.15f, 0.75f, 0.62f, "Сп.1",
                    area = 13.4,
                    openings = listOf(balcTop(0.5f), doorBottom(0.5f, 0.26f))
                ),
                rect(
                    0.75f, 0.20f, 0.96f, 0.62f, "Сп.2",
                    area = 11.8,
                    openings = listOf(winTop(0.5f), doorBottom(0.16f, 0.20f))
                ),
                rect(0.10f, 0.62f, 0.80f, 0.94f, "Коридор", entryDoorSide = EntryDoorSide.BOTTOM, area = 8.4),
                rect(0.80f, 0.62f, 0.96f, 0.94f, "С/У", hasDoorLeft = true, area = 4.2, fx = BlueprintFixtureKind.COMBINED_BATH)
            )
        ),
        ApartmentTemplate(
            id = "moskovskiy_2",
            title = "Московский проект (2-к.)",
            subtitle = "Г-контур · изолированные комнаты + кладовая (~54 м²)",
            roomCountCategory = 2,
            houseGroup = TemplateHouseGroup.SECONDARY,
            planAspectRatio = 1.54f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Зал"), room("Спальня"), room("Кухня"), room("Прихожая"), room("Кладовая"), room("Санузел")),
            schematicRooms = listOf(
                rect(
                    0.04f, 0.06f, 0.50f, 0.60f, "Зал",
                    area = 18.2,
                    openings = listOf(winTop(0.5f), doorBottom(0.84f, 0.20f))
                ),
                rect(
                    0.50f, 0.14f, 0.96f, 0.60f, "Спальня",
                    area = 13.8,
                    openings = listOf(winTop(0.5f), doorBottom(0.28f, 0.24f))
                ),
                rect(0.04f, 0.60f, 0.36f, 0.94f, "Кухня", hasDoorRight = true, hasWindowLeft = true, area = 8.6, fx = BlueprintFixtureKind.KITCHEN),
                rect(0.36f, 0.60f, 0.76f, 0.92f, "Прихожая", entryDoorSide = EntryDoorSide.BOTTOM, area = 7.4),
                rect(0.76f, 0.60f, 0.96f, 0.74f, "Клад.", hasDoorLeft = true, area = 2.0, fx = BlueprintFixtureKind.WARDROBE),
                rect(0.76f, 0.74f, 0.96f, 0.94f, "С/У", hasDoorLeft = true, area = 4.0, fx = BlueprintFixtureKind.COMBINED_BATH)
            )
        ),
        ApartmentTemplate(
            id = "bolgarka_polskiy_yugo",
            title = "Болгарка / Польский / Югославский",
            subtitle = "3-к. центрическая с выступающим холлом (~64 м²)",
            roomCountCategory = 3,
            houseGroup = TemplateHouseGroup.SECONDARY,
            planAspectRatio = 1.58f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Зал"), room("Спальня 1"), room("Спальня 2"), room("Кухня"), room("Холл"), room("Санузел")),
            schematicRooms = listOf(
                rect(
                    0.04f, 0.06f, 0.38f, 0.56f, "Зал",
                    area = 18.4,
                    openings = listOf(winTop(0.5f), doorBottom(0.86f, 0.18f))
                ),
                rect(0.38f, 0.06f, 0.68f, 0.56f, "Сп.1", hasDoorBottom = true, hasWindowTop = true, area = 13.4),
                rect(
                    0.68f, 0.14f, 0.96f, 0.56f, "Сп.2",
                    area = 11.8,
                    openings = listOf(winTop(0.5f), doorBottom(0.20f, 0.24f))
                ),
                rect(0.08f, 0.56f, 0.32f, 0.92f, "Кухня", hasDoorRight = true, hasWindowLeft = true, area = 8.4, fx = BlueprintFixtureKind.KITCHEN),
                rect(0.32f, 0.56f, 0.76f, 0.95f, "Холл", entryDoorSide = EntryDoorSide.BOTTOM, area = 8.0),
                rect(0.76f, 0.56f, 0.94f, 0.88f, "С/У", hasDoorLeft = true, area = 4.2, fx = BlueprintFixtureKind.COMBINED_BATH)
            )
        ),

        // ==================== 6. НОВОСТРОЙКИ И ЕВРОФОРМАТЫ ====================
        ApartmentTemplate(
            id = "euro_1",
            title = "Еврооднушка (1E)",
            subtitle = "Выступ кухни-гостиной + лоджия (~36 м²)",
            roomCountCategory = 1,
            houseGroup = TemplateHouseGroup.NEW_BUILD,
            planAspectRatio = 1.38f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Лоджия"), room("Кухня-гостиная"), room("Спальня"), room("Прихожая"), room("Санузел")),
            schematicRooms = listOf(
                rect(0.04f, 0.03f, 0.56f, 0.13f, "Лоджия", isBalcony = true, area = 3.2),
                rect(
                    0.04f, 0.13f, 0.58f, 0.64f, "Кухня-гост.",
                    area = 16.4,
                    fx = BlueprintFixtureKind.KITCHEN,
                    openings = listOf(balcTop(0.48f), doorBottom(0.56f, 0.24f))
                ),
                rect(
                    0.58f, 0.16f, 0.96f, 0.64f, "Спальня",
                    area = 11.2,
                    openings = listOf(winTop(0.5f), doorBottom(0.18f, 0.22f))
                ),
                rect(0.12f, 0.64f, 0.68f, 0.94f, "Прихожая", entryDoorSide = EntryDoorSide.BOTTOM, area = 4.6),
                rect(0.68f, 0.64f, 0.94f, 0.90f, "С/У", hasDoorLeft = true, area = 3.6, fx = BlueprintFixtureKind.COMBINED_BATH)
            )
        ),
        ApartmentTemplate(
            id = "novostroyka_1_classic",
            title = "Классическая 1-комн. (новостр.)",
            subtitle = "Ступенчатый контур с широкой кухней и лоджией (~41 м²)",
            roomCountCategory = 1,
            houseGroup = TemplateHouseGroup.NEW_BUILD,
            planAspectRatio = 1.40f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Лоджия"), room("Комната"), room("Кухня"), room("Прихожая"), room("Санузел")),
            schematicRooms = listOf(
                rect(0.54f, 0.03f, 0.94f, 0.14f, "Лоджия", isBalcony = true, area = 3.4),
                rect(0.04f, 0.06f, 0.54f, 0.62f, "Комната", hasDoorBottom = true, hasWindowTop = true, area = 17.8),
                rect(
                    0.54f, 0.14f, 0.96f, 0.62f, "Кухня",
                    area = 11.4,
                    fx = BlueprintFixtureKind.KITCHEN,
                    openings = listOf(balcTop(0.48f), doorBottom(0.22f, 0.24f))
                ),
                rect(0.04f, 0.62f, 0.68f, 0.94f, "Прихожая", entryDoorSide = EntryDoorSide.LEFT, area = 5.6),
                rect(0.68f, 0.62f, 0.94f, 0.90f, "С/У", hasDoorLeft = true, area = 4.0, fx = BlueprintFixtureKind.COMBINED_BATH)
            )
        ),
        ApartmentTemplate(
            id = "euro_2",
            title = "Евродвушка (2E)",
            subtitle = "Г-контур · кухня-гостиная + гардеробная ниша (~46 м²)",
            roomCountCategory = 2,
            houseGroup = TemplateHouseGroup.NEW_BUILD,
            planAspectRatio = 1.54f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Кухня-гостиная"), room("Спальня"), room("Прихожая"), room("Гардеробная"), room("Санузел")),
            schematicRooms = listOf(
                rect(0.04f, 0.06f, 0.58f, 0.58f, "Кухня-гост.", hasDoorBottom = true, hasWindowTop = true, area = 19.4, fx = BlueprintFixtureKind.KITCHEN),
                rect(
                    0.58f, 0.14f, 0.96f, 0.58f, "Спальня",
                    area = 13.6,
                    openings = listOf(winTop(0.5f), doorBottom(0.24f, 0.24f))
                ),
                rect(0.10f, 0.58f, 0.74f, 0.94f, "Прихожая", entryDoorSide = EntryDoorSide.BOTTOM, area = 6.2),
                rect(0.74f, 0.58f, 0.96f, 0.73f, "Гард.", hasDoorLeft = true, area = 2.8, fx = BlueprintFixtureKind.WARDROBE),
                rect(0.74f, 0.73f, 0.96f, 0.92f, "С/У", hasDoorLeft = true, area = 4.2, fx = BlueprintFixtureKind.COMBINED_BATH)
            )
        ),
        ApartmentTemplate(
            id = "novostroyka_2_grebenka",
            title = "Гребёнка 2-комн.",
            subtitle = "Выступ кухни и гостиной, утопленный вход (~56 м²)",
            roomCountCategory = 2,
            houseGroup = TemplateHouseGroup.NEW_BUILD,
            planAspectRatio = 1.68f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Кухня"), room("Гостиная"), room("Спальня"), room("Коридор"), room("Санузел")),
            schematicRooms = listOf(
                rect(
                    0.04f, 0.12f, 0.36f, 0.62f, "Кухня",
                    area = 11.6,
                    fx = BlueprintFixtureKind.KITCHEN,
                    openings = listOf(winTop(0.5f), doorBottom(0.58f, 0.26f))
                ),
                rect(0.36f, 0.05f, 0.68f, 0.62f, "Гостиная", hasDoorBottom = true, hasWindowTop = true, area = 18.8),
                rect(
                    0.68f, 0.12f, 0.96f, 0.62f, "Спальня",
                    area = 14.2,
                    openings = listOf(winTop(0.5f), doorBottom(0.18f, 0.22f))
                ),
                rect(0.12f, 0.62f, 0.76f, 0.94f, "Коридор", entryDoorSide = EntryDoorSide.BOTTOM, area = 7.4),
                rect(0.76f, 0.62f, 0.96f, 0.90f, "С/У", hasDoorLeft = true, area = 4.2, fx = BlueprintFixtureKind.COMBINED_BATH)
            )
        ),
        ApartmentTemplate(
            id = "euro_3",
            title = "Евротрёшка (3E)",
            subtitle = "Г-контур · мастер-спальня + 2-й с/у (~65 м²)",
            roomCountCategory = 3,
            houseGroup = TemplateHouseGroup.NEW_BUILD,
            planAspectRatio = 1.62f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Кухня-гостиная"), room("Детская"), room("Мастер-спальня"), room("Прихожая"), room("Ванная"), room("Гост. С/У")),
            schematicRooms = listOf(
                rect(0.04f, 0.06f, 0.42f, 0.58f, "Кухня-гост.", hasDoorBottom = true, hasWindowTop = true, area = 22.4, fx = BlueprintFixtureKind.KITCHEN),
                rect(0.42f, 0.12f, 0.68f, 0.58f, "Детская", hasDoorBottom = true, hasWindowTop = true, area = 13.2),
                rect(
                    0.68f, 0.06f, 0.96f, 0.58f, "Спальня",
                    area = 15.6,
                    openings = listOf(winTop(0.5f), doorBottom(0.20f, 0.24f))
                ),
                rect(0.10f, 0.58f, 0.76f, 0.94f, "Прихожая", entryDoorSide = EntryDoorSide.BOTTOM, area = 8.4),
                rect(0.76f, 0.58f, 0.96f, 0.76f, "Ван.", hasDoorLeft = true, area = 3.8, fx = BlueprintFixtureKind.BATH),
                rect(0.76f, 0.76f, 0.96f, 0.94f, "С/У", hasDoorLeft = true, area = 2.0, fx = BlueprintFixtureKind.TOILET)
            )
        ),
        ApartmentTemplate(
            id = "euro_4",
            title = "Еврочетырёшка (4E) / 4-к.",
            subtitle = "Ступенчатый бизнес-контур с постирочной (~86 м²)",
            roomCountCategory = 4,
            houseGroup = TemplateHouseGroup.NEW_BUILD,
            planAspectRatio = 1.78f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Кухня-гостиная"), room("Спальня 1"), room("Спальня 2"), room("Спальня 3"), room("Холл"), room("Постирочная"), room("Санузел")),
            schematicRooms = listOf(
                rect(0.04f, 0.06f, 0.34f, 0.56f, "Кухня-г.", hasDoorBottom = true, hasWindowTop = true, area = 24.5, fx = BlueprintFixtureKind.KITCHEN),
                rect(0.34f, 0.12f, 0.54f, 0.56f, "Сп.1", hasDoorBottom = true, hasWindowTop = true, area = 13.4),
                rect(0.54f, 0.12f, 0.72f, 0.56f, "Сп.2", hasDoorBottom = true, hasWindowTop = true, area = 12.8),
                rect(
                    0.72f, 0.06f, 0.96f, 0.56f, "Сп.3",
                    area = 16.2,
                    openings = listOf(winTop(0.5f), doorBottom(0.18f, 0.22f))
                ),
                rect(0.10f, 0.56f, 0.78f, 0.94f, "Холл", entryDoorSide = EntryDoorSide.BOTTOM, area = 11.4),
                rect(0.78f, 0.56f, 0.96f, 0.74f, "Постир.", hasDoorLeft = true, area = 3.2, fx = BlueprintFixtureKind.WARDROBE),
                rect(0.78f, 0.74f, 0.96f, 0.92f, "С/У", hasDoorLeft = true, area = 4.5, fx = BlueprintFixtureKind.COMBINED_BATH)
            )
        ),

        // ==================== 7. ОСОБЫЕ ФОРМАТЫ ====================
        ApartmentTemplate(
            id = "svobodnaya_planirovka",
            title = "Свободная планировка / Лофт",
            subtitle = "Г-образный открытый зал вокруг санузла (~52 м²)",
            roomCountCategory = 0,
            houseGroup = TemplateHouseGroup.SPECIAL,
            planAspectRatio = 1.40f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Общий зал (Г-форма)", walls = 6, inner = 5, outer = 1), room("Входная зона"), room("Санузел")),
            schematicRooms = listOf(
                poly(
                    pts = listOf(
                        0.04f to 0.06f, 0.96f to 0.06f, 0.96f to 0.64f,
                        0.66f to 0.64f, 0.66f to 0.72f, 0.04f to 0.72f
                    ),
                    label = "Открытое пространство",
                    windowEdges = listOf(0, 1, 5),
                    openings = listOf(polyOp(4, center = 0.45f, w = 0.24f)),
                    labelPos = 0.45f to 0.36f,
                    area = 42.0,
                    fx = BlueprintFixtureKind.KITCHEN
                ),
                rect(0.10f, 0.72f, 0.66f, 0.94f, "Входная зона", entryDoorSide = EntryDoorSide.BOTTOM, area = 5.2),
                rect(0.66f, 0.64f, 0.94f, 0.94f, "С/У", hasDoorLeft = true, area = 4.4, fx = BlueprintFixtureKind.COMBINED_BATH)
            )
        ),
        ApartmentTemplate(
            id = "panorama_uglovoe",
            title = "С панорамным / угловым остеклением",
            subtitle = "Скошенный панорамный угол + окна по 2 фасадам (~61 м²)",
            roomCountCategory = 2,
            houseGroup = TemplateHouseGroup.NEW_BUILD,
            planAspectRatio = 1.48f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Панорамная гостиная", walls = 5, hasSlanted = true), room("Спальня"), room("Кухня"), room("Прихожая"), room("Санузел")),
            schematicRooms = listOf(
                poly(
                    pts = listOf(0.04f to 0.24f, 0.22f to 0.05f, 0.56f to 0.05f, 0.56f to 0.62f, 0.04f to 0.62f),
                    label = "Гостиная",
                    windowEdges = listOf(0, 1, 4),
                    openings = listOf(polyOp(3, center = 0.20f, w = 0.24f)),
                    labelPos = 0.32f to 0.36f,
                    area = 21.6
                ),
                rect(
                    0.56f, 0.08f, 0.96f, 0.62f, "Спальня",
                    area = 15.2,
                    openings = listOf(winTop(0.5f), winRight(0.5f), doorBottom(0.24f, 0.24f))
                ),
                rect(0.04f, 0.62f, 0.36f, 0.92f, "Кухня", hasDoorRight = true, hasWindowLeft = true, area = 11.4, fx = BlueprintFixtureKind.KITCHEN),
                rect(0.36f, 0.62f, 0.72f, 0.95f, "Прихожая", entryDoorSide = EntryDoorSide.BOTTOM, area = 8.2),
                rect(0.72f, 0.62f, 0.94f, 0.90f, "С/У", hasDoorLeft = true, area = 4.6, fx = BlueprintFixtureKind.COMBINED_BATH)
            )
        ),
        ApartmentTemplate(
            id = "terrasa_penthouse",
            title = "С Г-террасой / Пентхаус",
            subtitle = "Угловая терраса вокруг квартиры + окно в ванной (~88 м²)",
            roomCountCategory = 3,
            houseGroup = TemplateHouseGroup.SPECIAL,
            planAspectRatio = 1.54f,
            hasNonRectangularShape = true,
            rooms = listOf(
                room("Терраса (Г-форма)", walls = 6, inner = 5, outer = 1),
                room("Гостиная"),
                room("Спальня"),
                room("Ванная с окном"),
                room("Кухня"),
                room("Холл")
            ),
            schematicRooms = listOf(
                poly(
                    pts = listOf(
                        0.04f to 0.04f, 0.96f to 0.04f, 0.96f to 0.66f,
                        0.82f to 0.66f, 0.82f to 0.22f, 0.04f to 0.22f
                    ),
                    label = "Терраса",
                    isBalcony = true,
                    labelPos = 0.45f to 0.13f,
                    area = 18.0
                ),
                rect(
                    0.04f, 0.22f, 0.42f, 0.66f, "Гостиная",
                    area = 24.0,
                    openings = listOf(balcTop(0.48f), doorBottom(0.86f, 0.16f))
                ),
                rect(
                    0.42f, 0.22f, 0.66f, 0.66f, "Спальня",
                    area = 16.5,
                    openings = listOf(balcTop(0.50f), doorBottom(0.50f, 0.26f))
                ),
                rect(
                    0.66f, 0.22f, 0.82f, 0.66f, "Ван.",
                    area = 7.2,
                    fx = BlueprintFixtureKind.BATH,
                    openings = listOf(winTop(0.5f), winRight(0.45f), doorBottom(0.5f, 0.36f))
                ),
                rect(0.04f, 0.66f, 0.34f, 0.94f, "Кухня", hasDoorRight = true, area = 12.8, fx = BlueprintFixtureKind.KITCHEN),
                rect(0.34f, 0.66f, 0.82f, 0.94f, "Холл", entryDoorSide = EntryDoorSide.BOTTOM, area = 9.5)
            )
        ),
        ApartmentTemplate(
            id = "anfilada",
            title = "Анфиладная планировка",
            subtitle = "Проходные комнаты + Г-образный силуэт кухни (~68 м²)",
            roomCountCategory = 3,
            houseGroup = TemplateHouseGroup.SPECIAL,
            planAspectRatio = 1.72f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Комната 1"), room("Комната 2"), room("Комната 3"), room("Кухня"), room("Прихожая"), room("Санузел")),
            schematicRooms = listOf(
                rect(0.04f, 0.06f, 0.36f, 0.62f, "Комн.1", hasDoorRight = true, hasWindowTop = true, area = 16.5),
                rect(
                    0.36f, 0.06f, 0.68f, 0.62f, "Комн.2",
                    area = 17.8,
                    openings = listOf(winTop(0.5f), doorRight(0.5f, 0.26f), doorBottom(0.5f, 0.26f))
                ),
                rect(
                    0.68f, 0.14f, 0.96f, 0.62f, "Комн.3",
                    area = 14.2,
                    openings = listOf(winTop(0.5f), doorBottom(0.18f, 0.22f))
                ),
                rect(0.08f, 0.62f, 0.38f, 0.95f, "Кухня", hasDoorRight = true, hasWindowLeft = true, area = 9.2, fx = BlueprintFixtureKind.KITCHEN),
                rect(0.38f, 0.62f, 0.76f, 0.92f, "Прихожая", entryDoorSide = EntryDoorSide.BOTTOM, area = 6.8),
                rect(0.76f, 0.62f, 0.94f, 0.90f, "С/У", hasDoorLeft = true, area = 3.8, fx = BlueprintFixtureKind.COMBINED_BATH)
            )
        ),
        ApartmentTemplate(
            id = "dvukhurovnevaya_antresol",
            title = "Двухуровневая / С антресолью",
            subtitle = "Г-контур · второй свет + кухня в эркерном крыле (~58 м²)",
            roomCountCategory = 4,
            houseGroup = TemplateHouseGroup.SPECIAL,
            planAspectRatio = 1.56f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Гостиная (1 ур.)"), room("Антресоль (2 ур.)"), room("Кухня"), room("Прихожая"), room("Санузел 1+2")),
            schematicRooms = listOf(
                rect(
                    0.04f, 0.06f, 0.50f, 0.62f, "Гостиная 1ур.",
                    area = 21.0,
                    openings = listOf(winTop(0.5f), doorBottom(0.84f, 0.20f))
                ),
                rect(
                    0.50f, 0.14f, 0.96f, 0.62f, "Антресоль 2ур.",
                    area = 15.8,
                    openings = listOf(winTop(0.5f), doorBottom(0.24f, 0.24f))
                ),
                rect(0.04f, 0.62f, 0.36f, 0.94f, "Кухня", hasDoorRight = true, hasWindowLeft = true, area = 10.2, fx = BlueprintFixtureKind.KITCHEN),
                rect(0.36f, 0.62f, 0.74f, 0.90f, "Прихожая", entryDoorSide = EntryDoorSide.BOTTOM, area = 6.4),
                rect(0.74f, 0.62f, 0.94f, 0.90f, "С/У 1+2", hasDoorLeft = true, area = 4.8, fx = BlueprintFixtureKind.COMBINED_BATH)
            )
        ),
        ApartmentTemplate(
            id = "apartments_loft",
            title = "Апартаменты / Индивид. проект",
            subtitle = "Непрямоугольный контур + гардеробная + 2 фасада (~57 м²)",
            roomCountCategory = 2,
            houseGroup = TemplateHouseGroup.NEW_BUILD,
            planAspectRatio = 1.48f,
            hasNonRectangularShape = true,
            rooms = listOf(room("Гостиная"), room("Спальня"), room("Кухня"), room("Прихожая"), room("Гардеробная"), room("Санузел")),
            schematicRooms = listOf(
                rect(
                    0.04f, 0.06f, 0.54f, 0.60f, "Гостиная",
                    area = 19.4,
                    openings = listOf(winTop(0.5f), winLeft(0.5f), doorBottom(0.82f, 0.20f))
                ),
                rect(
                    0.54f, 0.14f, 0.96f, 0.56f, "Спальня",
                    area = 14.0,
                    openings = listOf(winTop(0.5f), doorBottom(0.24f, 0.24f))
                ),
                rect(0.08f, 0.60f, 0.34f, 0.94f, "Кухня", hasDoorRight = true, area = 9.4, fx = BlueprintFixtureKind.KITCHEN),
                rect(0.34f, 0.60f, 0.76f, 0.92f, "Прихожая", entryDoorSide = EntryDoorSide.BOTTOM, area = 7.2),
                rect(0.76f, 0.56f, 0.96f, 0.73f, "Гард.", hasDoorLeft = true, area = 2.8, fx = BlueprintFixtureKind.WARDROBE),
                rect(0.76f, 0.73f, 0.96f, 0.90f, "С/У", hasDoorLeft = true, area = 4.2, fx = BlueprintFixtureKind.COMBINED_BATH)
            )
        )
    )

    fun findById(id: String): ApartmentTemplate? = allTemplates.find { it.id == id }
}
