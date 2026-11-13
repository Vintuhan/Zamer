package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.domain.MeasurementCategory
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.ConstructorEditorScreen
import com.example.ui.screens.CreateProjectScreen
import com.example.ui.screens.EstimateScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LayoutSelectionScreen
import com.example.ui.screens.MaterialsScreen
import com.example.ui.screens.MeasurementCategoryScreen
import com.example.ui.screens.MeasurementWizardScreen
import com.example.ui.screens.MinorWorksScreen
import com.example.ui.screens.MoneyHistoryScreen
import com.example.ui.screens.MonthlyStatsScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.ProjectDashboardScreen
import com.example.ui.viewmodel.ZamerViewModel

object ZamerRoutes {
    const val ROUTE_HOME = "home"
    const val ROUTE_CREATE_PROJECT = "create_project"
    const val ROUTE_LAYOUT_SELECT = "layout_select"
    const val ROUTE_CALC_GLOBAL = "calculator_global"
    const val ROUTE_STATS = "monthly_stats"
    const val ROUTE_DASHBOARD = "dashboard/{projectId}"
    const val ROUTE_CATEGORY = "category/{projectId}/{category}"
    const val ROUTE_WIZARD = "wizard/{projectId}?roomId={roomId}"
    const val ROUTE_MINOR_WORKS = "minor_works/{projectId}"
    const val ROUTE_ESTIMATE = "estimate/{projectId}"
    const val ROUTE_CALCULATOR = "calculator/{projectId}"
    const val ROUTE_MATERIALS = "materials/{projectId}"
    const val ROUTE_NOTES = "notes/{projectId}"
    const val ROUTE_MONEY = "money/{projectId}"
    const val ROUTE_EDIT_DRAWING = "edit_drawing/{projectId}"

    fun dashboard(projectId: Long) = "dashboard/$projectId"
    fun category(projectId: Long, category: MeasurementCategory) = "category/$projectId/${category.name}"
    fun wizard(projectId: Long, roomId: Long? = null) =
        if (roomId != null) "wizard/$projectId?roomId=$roomId" else "wizard/$projectId"
    fun minorWorks(projectId: Long) = "minor_works/$projectId"
    fun estimate(projectId: Long) = "estimate/$projectId"
    fun calculator(projectId: Long) = "calculator/$projectId"
    fun materials(projectId: Long) = "materials/$projectId"
    fun notes(projectId: Long) = "notes/$projectId"
    fun money(projectId: Long) = "money/$projectId"
    fun editDrawing(projectId: Long) = "edit_drawing/$projectId"
}

@Composable
fun ZamerNavGraph(
    viewModel: ZamerViewModel = viewModel()
) {
    val navController = rememberNavController()
    val projects by viewModel.projectSummaries.collectAsStateWithLifecycle()
    val monthlyStats by viewModel.monthlyStats.collectAsStateWithLifecycle()
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val calcUi by viewModel.calcUi.collectAsStateWithLifecycle()
    val globalCalcHistory by viewModel.globalCalcHistory.collectAsStateWithLifecycle()
    val addressLookupState by viewModel.addressLookupState.collectAsStateWithLifecycle()
    val sketchMatchState by viewModel.sketchMatchState.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = ZamerRoutes.ROUTE_HOME
    ) {
        composable(ZamerRoutes.ROUTE_HOME) {
            HomeScreen(
                projects = projects,
                onAddProjectClick = {
                    viewModel.resetDraft()
                    navController.navigate(ZamerRoutes.ROUTE_CREATE_PROJECT)
                },
                onOpenProjectClick = { projectId ->
                    viewModel.selectProject(projectId)
                    navController.navigate(ZamerRoutes.dashboard(projectId))
                },
                onDeleteProjectClick = { projectId ->
                    viewModel.deleteProject(projectId)
                },
                onOpenCalculatorClick = {
                    viewModel.loadGlobalCalc()
                    navController.navigate(ZamerRoutes.ROUTE_CALC_GLOBAL)
                },
                onOpenStatsClick = {
                    navController.navigate(ZamerRoutes.ROUTE_STATS)
                }
            )
        }

        composable(ZamerRoutes.ROUTE_CALC_GLOBAL) {
            LaunchedEffect(Unit) { viewModel.loadGlobalCalc() }
            CalculatorScreen(
                aggregate = null,
                globalHistory = globalCalcHistory,
                calcUi = calcUi,
                onBackClick = { navController.popBackStack() },
                onDigit = viewModel::onCalcDigit,
                onParen = viewModel::onCalcParen,
                onBackspace = viewModel::onCalcBackspace,
                onClear = viewModel::onCalcClear,
                onPercent = viewModel::onCalcPercent,
                onOperator = viewModel::onCalcOperator,
                onEquals = viewModel::onCalcEquals,
                onInsertMeasurement = viewModel::insertMeasurementIntoCalc,
                onRestoreRecent = viewModel::restoreHistoryValueToCalc,
                onClearHistory = viewModel::clearHistory,
                onFlushToHistory = viewModel::flushPendingCalcToHistory
            )
        }

        composable(ZamerRoutes.ROUTE_STATS) {
            MonthlyStatsScreen(
                stats = monthlyStats,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(ZamerRoutes.ROUTE_CREATE_PROJECT) {
            CreateProjectScreen(
                draft = draft,
                onDraftChange = { name, address, date, hasStretchCeiling ->
                    viewModel.updateDraft(name, address, date, hasStretchCeiling)
                },
                onNextClick = {
                    navController.navigate(ZamerRoutes.ROUTE_LAYOUT_SELECT)
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(ZamerRoutes.ROUTE_LAYOUT_SELECT) {
            LayoutSelectionScreen(
                initialAddress = draft.address,
                addressLookupState = addressLookupState,
                sketchMatchState = sketchMatchState,
                onSearchByAddress = { addr, rooms ->
                    viewModel.updateDraft(address = addr)
                    viewModel.searchLayoutByAddress(addr, rooms)
                },
                onClearAddressLookup = { viewModel.clearAddressLookup() },
                onAnalyzeSketch = viewModel::analyzeDrawnSketch,
                onClearSketchMatch = viewModel::clearSketchMatch,
                onSelectTemplate = { templateId, variantLabel ->
                    viewModel.createFromTemplate(templateId, variantLabel) { newId ->
                        navController.navigate(ZamerRoutes.dashboard(newId)) {
                            popUpTo(ZamerRoutes.ROUTE_HOME)
                        }
                    }
                },
                onCreateFromDrawing = { drawnRooms ->
                    viewModel.createFromDrawing(drawnRooms) { newId ->
                        navController.navigate(ZamerRoutes.dashboard(newId)) {
                            popUpTo(ZamerRoutes.ROUTE_HOME)
                        }
                    }
                },
                onCreateWithoutPlan = { rooms ->
                    viewModel.createWithoutPlan(rooms) { newId ->
                        navController.navigate(ZamerRoutes.dashboard(newId)) {
                            popUpTo(ZamerRoutes.ROUTE_HOME)
                        }
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = ZamerRoutes.ROUTE_DASHBOARD,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            LaunchedEffect(projectId) { viewModel.selectProject(projectId) }
            ProjectDashboardScreen(
                aggregate = activeProject,
                onBackClick = { navController.popBackStack() },
                onOpenCategory = { cat ->
                    navController.navigate(ZamerRoutes.category(projectId, cat))
                },
                onOpenMinorWorks = {
                    navController.navigate(ZamerRoutes.minorWorks(projectId))
                },
                onOpenEstimate = {
                    navController.navigate(ZamerRoutes.estimate(projectId))
                },
                onOpenCalculator = {
                    navController.navigate(ZamerRoutes.calculator(projectId))
                },
                onOpenMaterials = {
                    navController.navigate(ZamerRoutes.materials(projectId))
                },
                onOpenNotes = {
                    navController.navigate(ZamerRoutes.notes(projectId))
                },
                onOpenMoneyHistory = {
                    navController.navigate(ZamerRoutes.money(projectId))
                },
                onEditDrawing = {
                    navController.navigate(ZamerRoutes.editDrawing(projectId))
                }
            )
        }

        composable(
            route = ZamerRoutes.ROUTE_CATEGORY,
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType },
                navArgument("category") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            val catStr = backStackEntry.arguments?.getString("category") ?: MeasurementCategory.WALLS.name
            val category = runCatching { MeasurementCategory.valueOf(catStr) }
                .getOrDefault(MeasurementCategory.WALLS)
            LaunchedEffect(projectId) { viewModel.selectProject(projectId) }

            MeasurementCategoryScreen(
                category = category,
                aggregate = activeProject,
                onBackClick = { navController.popBackStack() },
                onStartWizard = { roomId ->
                    navController.navigate(ZamerRoutes.wizard(projectId, roomId))
                },
                onSaveWallThickness = { project, t ->
                    viewModel.saveProjectWallThickness(project, t)
                },
                onSaveFloorOverride = { room, area ->
                    viewModel.saveRoomFloorOverride(room, area)
                },
                onAddRoom = { pid, name, walls, shapeKey ->
                    viewModel.addRoom(pid, name, walls, shapeKey)
                },
                onRenameRoom = { room, newName ->
                    viewModel.renameRoom(room, newName)
                },
                onSaveProjectWallHeight = { pid, heightMeters ->
                    viewModel.saveProjectWallHeight(pid, heightMeters)
                },
                onSaveProjectOpenings = { pid, openings ->
                    viewModel.saveProjectOpenings(pid, openings)
                }
            )
        }

        composable(
            route = ZamerRoutes.ROUTE_WIZARD,
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType },
                navArgument("roomId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            val roomId = backStackEntry.arguments?.getString("roomId")?.toLongOrNull()
            LaunchedEffect(projectId) { viewModel.selectProject(projectId) }

            MeasurementWizardScreen(
                aggregate = activeProject,
                initialRoomId = roomId,
                onBackExit = { navController.popBackStack() },
                onSaveWallLength = { room, idx, len ->
                    viewModel.saveWallLength(room, idx, len)
                },
                onRenameRoom = { room, newName ->
                    viewModel.renameRoom(room, newName)
                }
            )
        }

        composable(
            route = ZamerRoutes.ROUTE_MINOR_WORKS,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            LaunchedEffect(projectId) { viewModel.selectProject(projectId) }
            MinorWorksScreen(
                aggregate = activeProject,
                onBackClick = { navController.popBackStack() },
                onSaveWork = { work -> viewModel.saveMinorWork(work) },
                onDeleteWork = { work -> viewModel.deleteMinorWork(work) }
            )
        }

        composable(
            route = ZamerRoutes.ROUTE_ESTIMATE,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            LaunchedEffect(projectId) { viewModel.selectProject(projectId) }
            EstimateScreen(
                aggregate = activeProject,
                onSaveCategories = { proj, cats, outer ->
                    viewModel.saveEstimateCategories(proj, cats, outer)
                },
                onSaveEstimateInsideApp = { agg ->
                    viewModel.saveEstimateSnapshot(agg)
                },
                onDeleteSavedEstimate = { id ->
                    viewModel.deleteSavedEstimate(id)
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = ZamerRoutes.ROUTE_CALCULATOR,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            LaunchedEffect(projectId) { viewModel.selectProject(projectId) }
            CalculatorScreen(
                aggregate = activeProject,
                calcUi = calcUi,
                onBackClick = { navController.popBackStack() },
                onDigit = viewModel::onCalcDigit,
                onParen = viewModel::onCalcParen,
                onBackspace = viewModel::onCalcBackspace,
                onClear = viewModel::onCalcClear,
                onPercent = viewModel::onCalcPercent,
                onOperator = viewModel::onCalcOperator,
                onEquals = viewModel::onCalcEquals,
                onInsertMeasurement = viewModel::insertMeasurementIntoCalc,
                onRestoreRecent = viewModel::restoreHistoryValueToCalc,
                onClearHistory = viewModel::clearHistory,
                onFlushToHistory = viewModel::flushPendingCalcToHistory
            )
        }

        composable(
            route = ZamerRoutes.ROUTE_MATERIALS,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            LaunchedEffect(projectId) { viewModel.selectProject(projectId) }
            MaterialsScreen(
                aggregate = activeProject,
                onBackClick = { navController.popBackStack() },
                onAddMaterial = viewModel::addMaterialExpense,
                onDeleteMaterial = viewModel::deleteMaterialExpense
            )
        }

        composable(
            route = ZamerRoutes.ROUTE_NOTES,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            LaunchedEffect(projectId) { viewModel.selectProject(projectId) }
            NotesScreen(
                aggregate = activeProject,
                onBackClick = { navController.popBackStack() },
                onSaveNotes = viewModel::saveProjectNotes
            )
        }

        composable(
            route = ZamerRoutes.ROUTE_MONEY,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            LaunchedEffect(projectId) { viewModel.selectProject(projectId) }
            MoneyHistoryScreen(
                aggregate = activeProject,
                onBackClick = { navController.popBackStack() },
                onAddMoney = viewModel::addMoneyEntry,
                onDeleteMoney = viewModel::deleteMoneyEntry
            )
        }

        composable(
            route = ZamerRoutes.ROUTE_EDIT_DRAWING,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            LaunchedEffect(projectId) { viewModel.selectProject(projectId) }
            val initialRooms = remember(activeProject) {
                viewModel.extractConstructorRooms(activeProject)
            }
            ConstructorEditorScreen(
                initialRooms = initialRooms,
                onBack = { navController.popBackStack() },
                onComplete = { drawnRooms ->
                    viewModel.updateProjectDrawing(projectId, drawnRooms) {
                        navController.popBackStack()
                    }
                }
            )
        }
    }
}
