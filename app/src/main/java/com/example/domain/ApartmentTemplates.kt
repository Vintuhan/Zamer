package com.example.domain

object ApartmentTemplates {

    private val defaultFourWalls = listOf(
        "От входной двери направо. Мерь по низу, у пола",
        "Вторая стена (часто с окном). Мерь от угла до угла у пола",
        "Противоположная первой. Мерь по низу, у плинтуса",
        "Обратно к двери. Мерь от угла до угла по полу"
    )

    private fun room(
        name: String,
        walls: Int = 4,
        inner: Int = walls,
        outer: Int = 0
    ) = TemplateRoomSpec(
        name = name,
        wallCount = walls,
        defaultInnerCorners = inner,
        defaultOuterCorners = outer,
        wallHints = defaultFourWalls
    )

    val defaultEstimateCategories: List<EstimateCategoryConfig> = listOf(
        EstimateCategoryConfig("shpaklevka", "Шпаклёвка", "м²", "WALLS", isSelected = true),
        EstimateCategoryConfig("shtukaturka", "Штукатурка", "м²", "WALLS"),
        EstimateCategoryConfig("oboi", "Обои", "м²", "WALLS"),
        EstimateCategoryConfig("pokraska", "Покраска", "м²", "WALLS"),
        EstimateCategoryConfig("zachistka", "Зачистка", "м²", "WALLS"),
        EstimateCategoryConfig("gruntovka", "Грунтовка", "м²", "WALLS"),
        EstimateCategoryConfig("styazhka", "Стяжка пола", "м²", "FLOOR"),
        EstimateCategoryConfig("laminat", "Ламинат / пол", "м²", "FLOOR"),
        EstimateCategoryConfig("ugolki", "Уголки", "п.м.", "CORNERS"),
        EstimateCategoryConfig("nishi", "Ниши", "п.м.", "CORNERS")
    )

    val allTemplates: List<ApartmentTemplate> = listOf(
        // === НЕ НОВОСТРОЙКА (ВТОРИЧКА) — 1 КОМНАТА ===
        ApartmentTemplate(
            id = "khrushchyovka_1",
            title = "Хрущёвка 1-комн.",
            subtitle = "Серия 1-464/1-335 · совм. с/у",
            roomCountCategory = 1,
            isNewBuild = false,
            rooms = listOf(room("Комната"), room("Кухня"), room("Прихожая"), room("Санузел")),
            schematicRooms = listOf(
                RoomGeometryRect(0.05f, 0.08f, 0.63f, 0.65f, "Комната", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.63f, 0.08f, 0.95f, 0.56f, "Кухня", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.05f, 0.65f, 0.63f, 0.92f, "Прихожая", hasEntryDoorBottom = true),
                RoomGeometryRect(0.63f, 0.56f, 0.95f, 0.92f, "С/У")
            )
        ),
        ApartmentTemplate(
            id = "kirpich_1_447_1",
            title = "Кирпичная 1-447 (1-к.)",
            subtitle = "Классика Новомосковска · 1 комн.",
            roomCountCategory = 1,
            isNewBuild = false,
            rooms = listOf(room("Комната"), room("Кухня"), room("Коридор"), room("Санузел")),
            schematicRooms = listOf(
                RoomGeometryRect(0.05f, 0.08f, 0.58f, 0.66f, "Комната", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.58f, 0.08f, 0.95f, 0.60f, "Кухня", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.05f, 0.66f, 0.68f, 0.92f, "Коридор", hasEntryDoorBottom = true),
                RoomGeometryRect(0.68f, 0.60f, 0.95f, 0.92f, "С/У")
            )
        ),
        ApartmentTemplate(
            id = "brezhnevka_1",
            title = "Брежневка 1-комн.",
            subtitle = "Улучшенная кухня, прихожая",
            roomCountCategory = 1,
            isNewBuild = false,
            rooms = listOf(room("Комната"), room("Кухня"), room("Прихожая"), room("Санузел")),
            schematicRooms = listOf(
                RoomGeometryRect(0.05f, 0.08f, 0.55f, 0.62f, "Комната", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.55f, 0.08f, 0.95f, 0.58f, "Кухня", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.05f, 0.62f, 0.62f, 0.92f, "Прихожая", hasEntryDoorBottom = true),
                RoomGeometryRect(0.62f, 0.58f, 0.95f, 0.92f, "С/У")
            )
        ),

        // === НЕ НОВОСТРОЙКА (ВТОРИЧКА) — 2 КОМНАТЫ ===
        ApartmentTemplate(
            id = "khrushchyovka_2_tram",
            title = "Хрущёвка 2-к. (смежная)",
            subtitle = "«Трамвайчик» · вход в спальню из зала",
            roomCountCategory = 2,
            isNewBuild = false,
            rooms = listOf(room("Зал"), room("Спальня"), room("Кухня"), room("Коридор"), room("Санузел")),
            schematicRooms = listOf(
                RoomGeometryRect(0.05f, 0.08f, 0.38f, 0.64f, "Спальня", hasDoorRight = true, hasWindowTop = true),
                RoomGeometryRect(0.38f, 0.08f, 0.70f, 0.64f, "Зал", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.70f, 0.08f, 0.95f, 0.52f, "Кухня", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.05f, 0.64f, 0.70f, 0.92f, "Коридор", hasEntryDoorBottom = true),
                RoomGeometryRect(0.70f, 0.52f, 0.95f, 0.92f, "С/У")
            )
        ),
        ApartmentTemplate(
            id = "khrushchyovka_2_book",
            title = "Хрущёвка 2-к. (книжка)",
            subtitle = "Угловая или распашонка 5-этажка",
            roomCountCategory = 2,
            isNewBuild = false,
            rooms = listOf(room("Зал"), room("Спальня"), room("Кухня"), room("Прихожая"), room("Санузел")),
            schematicRooms = listOf(
                RoomGeometryRect(0.05f, 0.08f, 0.48f, 0.60f, "Зал", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.48f, 0.08f, 0.95f, 0.56f, "Спальня", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.05f, 0.60f, 0.36f, 0.92f, "Кухня", hasDoorRight = true),
                RoomGeometryRect(0.36f, 0.60f, 0.72f, 0.92f, "Прихожая", hasEntryDoorBottom = true),
                RoomGeometryRect(0.72f, 0.56f, 0.95f, 0.92f, "С/У")
            )
        ),
        ApartmentTemplate(
            id = "brezhnevka_2",
            title = "Брежневка 2-комн.",
            subtitle = "Изолированные комнаты, разд. с/у",
            roomCountCategory = 2,
            isNewBuild = false,
            rooms = listOf(room("Зал"), room("Спальня"), room("Кухня"), room("Коридор"), room("Ванная"), room("Туалет")),
            schematicRooms = listOf(
                RoomGeometryRect(0.05f, 0.08f, 0.50f, 0.62f, "Зал", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.50f, 0.08f, 0.95f, 0.52f, "Спальня", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.05f, 0.62f, 0.55f, 0.92f, "Коридор", hasEntryDoorBottom = true),
                RoomGeometryRect(0.55f, 0.52f, 0.95f, 0.72f, "Кухня"),
                RoomGeometryRect(0.55f, 0.72f, 0.75f, 0.92f, "Ван."),
                RoomGeometryRect(0.75f, 0.72f, 0.95f, 0.92f, "С/У")
            )
        ),
        ApartmentTemplate(
            id = "stalinka_2",
            title = "Сталинка 2-комн.",
            subtitle = "Высокие потолки, большая кухня",
            roomCountCategory = 2,
            isNewBuild = false,
            rooms = listOf(room("Гостиная"), room("Спальня"), room("Кухня"), room("Прихожая"), room("Санузел")),
            schematicRooms = listOf(
                RoomGeometryRect(0.05f, 0.08f, 0.52f, 0.58f, "Гостиная", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.52f, 0.08f, 0.95f, 0.58f, "Спальня", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.05f, 0.58f, 0.38f, 0.92f, "Кухня", hasDoorRight = true),
                RoomGeometryRect(0.38f, 0.58f, 0.74f, 0.92f, "Прихожая", hasEntryDoorBottom = true),
                RoomGeometryRect(0.74f, 0.58f, 0.95f, 0.92f, "С/У")
            )
        ),
        ApartmentTemplate(
            id = "panel_111_90_2",
            title = "Панель 111-90 (2-к.)",
            subtitle = "9-этажка · кухня 9 м², разд. с/у",
            roomCountCategory = 2,
            isNewBuild = false,
            rooms = listOf(room("Зал"), room("Спальня"), room("Кухня"), room("Коридор"), room("Ванная"), room("Туалет")),
            schematicRooms = listOf(
                RoomGeometryRect(0.05f, 0.08f, 0.52f, 0.54f, "Зал", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.52f, 0.08f, 0.95f, 0.54f, "Спальня", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.05f, 0.54f, 0.38f, 0.92f, "Кухня", hasDoorRight = true),
                RoomGeometryRect(0.38f, 0.54f, 0.72f, 0.92f, "Коридор", hasEntryDoorBottom = true),
                RoomGeometryRect(0.72f, 0.54f, 0.95f, 0.73f, "Ван."),
                RoomGeometryRect(0.72f, 0.73f, 0.95f, 0.92f, "С/У")
            )
        ),

        // === НЕ НОВОСТРОЙКА (ВТОРИЧКА) — 3 КОМНАТЫ ===
        ApartmentTemplate(
            id = "khrushchyovka_3",
            title = "Хрущёвка 3-комн.",
            subtitle = "Зал проходной, 2 спальни, кухня",
            roomCountCategory = 3,
            isNewBuild = false,
            rooms = listOf(room("Зал"), room("Спальня 1"), room("Спальня 2"), room("Кухня"), room("Коридор"), room("Санузел")),
            schematicRooms = listOf(
                RoomGeometryRect(0.05f, 0.08f, 0.34f, 0.55f, "Сп.1", hasDoorRight = true, hasWindowTop = true),
                RoomGeometryRect(0.34f, 0.08f, 0.67f, 0.60f, "Зал", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.67f, 0.08f, 0.95f, 0.55f, "Сп.2", hasWindowTop = true),
                RoomGeometryRect(0.05f, 0.55f, 0.34f, 0.92f, "Кухня", hasDoorRight = true),
                RoomGeometryRect(0.34f, 0.60f, 0.70f, 0.92f, "Коридор", hasEntryDoorBottom = true),
                RoomGeometryRect(0.70f, 0.55f, 0.95f, 0.92f, "С/У")
            )
        ),
        ApartmentTemplate(
            id = "brezhnevka_3",
            title = "Брежневка 3-комн.",
            subtitle = "Все изолированные, разд. с/у",
            roomCountCategory = 3,
            isNewBuild = false,
            rooms = listOf(room("Зал"), room("Спальня 1"), room("Спальня 2"), room("Кухня"), room("Коридор"), room("Ванная"), room("Туалет")),
            schematicRooms = listOf(
                RoomGeometryRect(0.05f, 0.08f, 0.37f, 0.54f, "Зал", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.37f, 0.08f, 0.67f, 0.54f, "Сп.1", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.67f, 0.08f, 0.95f, 0.54f, "Сп.2", hasWindowTop = true),
                RoomGeometryRect(0.05f, 0.54f, 0.35f, 0.92f, "Кухня", hasDoorRight = true),
                RoomGeometryRect(0.35f, 0.54f, 0.72f, 0.92f, "Коридор", hasEntryDoorBottom = true),
                RoomGeometryRect(0.72f, 0.54f, 0.95f, 0.73f, "Ван."),
                RoomGeometryRect(0.72f, 0.73f, 0.95f, 0.92f, "С/У")
            )
        ),
        ApartmentTemplate(
            id = "panel_121_3",
            title = "Панель 121 (3-к.)",
            subtitle = "Распашонка, широкая прихожая",
            roomCountCategory = 3,
            isNewBuild = false,
            rooms = listOf(room("Комната 1"), room("Комната 2"), room("Комната 3"), room("Кухня"), room("Прихожая"), room("Санузел")),
            schematicRooms = listOf(
                RoomGeometryRect(0.05f, 0.08f, 0.36f, 0.50f, "Комн.1", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.36f, 0.08f, 0.67f, 0.50f, "Комн.2", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.67f, 0.08f, 0.95f, 0.50f, "Комн.3", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.05f, 0.50f, 0.36f, 0.92f, "Кухня", hasDoorRight = true),
                RoomGeometryRect(0.36f, 0.50f, 0.72f, 0.92f, "Прихожая", hasEntryDoorBottom = true),
                RoomGeometryRect(0.72f, 0.50f, 0.95f, 0.92f, "С/У")
            )
        ),
        ApartmentTemplate(
            id = "p44t_3",
            title = "Серия П-44Т (3-к.)",
            subtitle = "Большая кухня с эркером, распашонка",
            roomCountCategory = 3,
            isNewBuild = false,
            rooms = listOf(room("Гостиная"), room("Спальня 1"), room("Спальня 2"), room("Кухня"), room("Холл"), room("Ванная"), room("Туалет")),
            schematicRooms = listOf(
                RoomGeometryRect(0.05f, 0.08f, 0.38f, 0.52f, "Гост.", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.38f, 0.08f, 0.68f, 0.52f, "Сп.1", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.68f, 0.08f, 0.95f, 0.52f, "Сп.2", hasWindowTop = true),
                RoomGeometryRect(0.05f, 0.52f, 0.38f, 0.92f, "Кухня", hasDoorRight = true),
                RoomGeometryRect(0.38f, 0.52f, 0.73f, 0.92f, "Холл", hasEntryDoorBottom = true),
                RoomGeometryRect(0.73f, 0.52f, 0.95f, 0.72f, "Ван."),
                RoomGeometryRect(0.73f, 0.72f, 0.95f, 0.92f, "С/У")
            )
        ),

        // === НОВОСТРОЙКИ — 1 / 2 / 3 КОМНАТЫ ===
        ApartmentTemplate(
            id = "novostroyka_studio",
            title = "Новостройка Студия",
            subtitle = "Смарт-планировка 23–30 м²",
            roomCountCategory = 1,
            isNewBuild = true,
            rooms = listOf(room("Студия + кухня"), room("Прихожая"), room("Санузел")),
            schematicRooms = listOf(
                RoomGeometryRect(0.05f, 0.08f, 0.95f, 0.64f, "Студия / Кухня", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.05f, 0.64f, 0.60f, 0.92f, "Прихожая", hasEntryDoorBottom = true),
                RoomGeometryRect(0.60f, 0.64f, 0.95f, 0.92f, "С/У")
            )
        ),
        ApartmentTemplate(
            id = "novostroyka_1",
            title = "Новостройка 1-комн.",
            subtitle = "Просторная кухня, прихожая, с/у",
            roomCountCategory = 1,
            isNewBuild = true,
            rooms = listOf(room("Комната"), room("Кухня"), room("Прихожая"), room("Санузел")),
            schematicRooms = listOf(
                RoomGeometryRect(0.05f, 0.08f, 0.54f, 0.63f, "Комната", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.54f, 0.08f, 0.95f, 0.63f, "Кухня", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.05f, 0.63f, 0.65f, 0.92f, "Прихожая", hasEntryDoorBottom = true),
                RoomGeometryRect(0.65f, 0.63f, 0.95f, 0.92f, "С/У")
            )
        ),
        ApartmentTemplate(
            id = "novostroyka_euro2",
            title = "Евро-двушка (2E)",
            subtitle = "Новостройка · кухня-гостиная + спальня",
            roomCountCategory = 2,
            isNewBuild = true,
            rooms = listOf(room("Кухня-гостиная"), room("Спальня"), room("Прихожая"), room("Санузел")),
            schematicRooms = listOf(
                RoomGeometryRect(0.05f, 0.08f, 0.58f, 0.62f, "Кухня-гост.", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.58f, 0.08f, 0.95f, 0.62f, "Спальня", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.05f, 0.62f, 0.66f, 0.92f, "Прихожая", hasEntryDoorBottom = true),
                RoomGeometryRect(0.66f, 0.62f, 0.95f, 0.92f, "С/У")
            )
        ),
        ApartmentTemplate(
            id = "novostroyka_2",
            title = "Новостройка 2-комн.",
            subtitle = "Классическая распашонка 55–65 м²",
            roomCountCategory = 2,
            isNewBuild = true,
            rooms = listOf(room("Гостиная"), room("Спальня"), room("Кухня"), room("Прихожая"), room("Санузел")),
            schematicRooms = listOf(
                RoomGeometryRect(0.05f, 0.08f, 0.48f, 0.58f, "Гостиная", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.48f, 0.08f, 0.95f, 0.58f, "Спальня", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.05f, 0.58f, 0.40f, 0.92f, "Кухня", hasDoorRight = true),
                RoomGeometryRect(0.40f, 0.58f, 0.72f, 0.92f, "Прихожая", hasEntryDoorBottom = true),
                RoomGeometryRect(0.72f, 0.58f, 0.95f, 0.92f, "С/У")
            )
        ),
        ApartmentTemplate(
            id = "novostroyka_euro3",
            title = "Евро-трёшка (3E)",
            subtitle = "Новостройка · кухня-гостиная + 2 спальни",
            roomCountCategory = 3,
            isNewBuild = true,
            rooms = listOf(room("Кухня-гостиная"), room("Спальня 1"), room("Спальня 2"), room("Прихожая"), room("Санузел")),
            schematicRooms = listOf(
                RoomGeometryRect(0.05f, 0.08f, 0.44f, 0.58f, "Кухня-гост.", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.44f, 0.08f, 0.70f, 0.54f, "Сп.1", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.70f, 0.08f, 0.95f, 0.54f, "Сп.2", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.05f, 0.58f, 0.68f, 0.92f, "Прихожая", hasEntryDoorBottom = true),
                RoomGeometryRect(0.68f, 0.54f, 0.95f, 0.92f, "С/У")
            )
        ),
        ApartmentTemplate(
            id = "novostroyka_3",
            title = "Новостройка 3-комн.",
            subtitle = "3 изолированные комнаты, 2 с/у",
            roomCountCategory = 3,
            isNewBuild = true,
            rooms = listOf(room("Гостиная"), room("Спальня 1"), room("Спальня 2"), room("Кухня"), room("Прихожая"), room("Ванная"), room("Гост. С/У")),
            schematicRooms = listOf(
                RoomGeometryRect(0.05f, 0.08f, 0.37f, 0.52f, "Гост.", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.37f, 0.08f, 0.67f, 0.52f, "Сп.1", hasDoorBottom = true, hasWindowTop = true),
                RoomGeometryRect(0.67f, 0.08f, 0.95f, 0.52f, "Сп.2", hasWindowTop = true),
                RoomGeometryRect(0.05f, 0.52f, 0.37f, 0.92f, "Кухня", hasDoorRight = true),
                RoomGeometryRect(0.37f, 0.52f, 0.72f, 0.92f, "Прихожая", hasEntryDoorBottom = true),
                RoomGeometryRect(0.72f, 0.52f, 0.95f, 0.72f, "Ван."),
                RoomGeometryRect(0.72f, 0.72f, 0.95f, 0.92f, "С/У")
            )
        )
    )

    fun findById(id: String): ApartmentTemplate? = allTemplates.find { it.id == id }
}
