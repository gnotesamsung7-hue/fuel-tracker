package com.parengmark.fueltracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.parengmark.fueltracker.data.FuelRepository
import com.parengmark.fueltracker.ui.common.AppViewModelFactory
import com.parengmark.fueltracker.ui.compare.CompareVehiclesScreen
import com.parengmark.fueltracker.ui.compare.CompareVehiclesViewModel
import com.parengmark.fueltracker.ui.dashboard.DashboardScreen
import com.parengmark.fueltracker.ui.dashboard.DashboardViewModel
import com.parengmark.fueltracker.ui.fillup.AddFillUpScreen
import com.parengmark.fueltracker.ui.fillup.FillUpListScreen
import com.parengmark.fueltracker.ui.fillup.FillUpViewModel
import com.parengmark.fueltracker.ui.maintenance.AddMaintenanceReminderScreen
import com.parengmark.fueltracker.ui.maintenance.MaintenanceListScreen
import com.parengmark.fueltracker.ui.maintenance.MaintenanceViewModel
import com.parengmark.fueltracker.ui.scan.ScanReceiptScreen
import com.parengmark.fueltracker.ui.stations.NearbyStationsScreen
import com.parengmark.fueltracker.ui.vehicle.AddVehicleScreen
import com.parengmark.fueltracker.ui.vehicle.VehicleListScreen
import com.parengmark.fueltracker.ui.vehicle.VehicleViewModel
import java.net.URLDecoder
import java.net.URLEncoder

private object Routes {
    const val VEHICLES = "vehicles"
    const val ADD_VEHICLE = "addVehicle"
    const val DASHBOARD = "dashboard/{vehicleId}"
    const val FILL_UPS = "fillups/{vehicleId}"
    const val ADD_FILL_UP = "addFillUp/{vehicleId}?volume={volume}&cost={cost}&station={station}"
    const val SCAN_RECEIPT = "scanReceipt/{vehicleId}"
    const val MAINTENANCE = "maintenance/{vehicleId}"
    const val ADD_MAINTENANCE = "addMaintenance/{vehicleId}"
    const val NEARBY_STATIONS = "nearbyStations/{vehicleId}"
    const val COMPARE_VEHICLES = "compareVehicles"

    fun dashboard(vehicleId: Long) = "dashboard/$vehicleId"
    fun fillUps(vehicleId: Long) = "fillups/$vehicleId"
    fun addFillUp(vehicleId: Long, volume: Double? = null, cost: Double? = null, station: String? = null): String {
        val v = volume?.toString()?.let { enc(it) } ?: ""
        val c = cost?.toString()?.let { enc(it) } ?: ""
        val s = station?.let { enc(it) } ?: ""
        return "addFillUp/$vehicleId?volume=$v&cost=$c&station=$s"
    }
    fun scanReceipt(vehicleId: Long) = "scanReceipt/$vehicleId"
    fun maintenance(vehicleId: Long) = "maintenance/$vehicleId"
    fun addMaintenance(vehicleId: Long) = "addMaintenance/$vehicleId"
    fun nearbyStations(vehicleId: Long) = "nearbyStations/$vehicleId"

    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")
}

private fun dec(value: String?): String =
    if (value.isNullOrEmpty()) "" else URLDecoder.decode(value, "UTF-8")

@Composable
fun AppNavigation(repository: FuelRepository) {
    val navController = rememberNavController()
    val factory = AppViewModelFactory(repository)

    NavHost(navController = navController, startDestination = Routes.VEHICLES) {
        composable(Routes.VEHICLES) {
            val vm: VehicleViewModel = viewModel(factory = factory)
            VehicleListScreen(
                viewModel = vm,
                onVehicleClick = { id -> navController.navigate(Routes.dashboard(id)) },
                onAddVehicle = { navController.navigate(Routes.ADD_VEHICLE) }
            )
        }

        composable(Routes.ADD_VEHICLE) {
            val vm: VehicleViewModel = viewModel(factory = factory)
            AddVehicleScreen(
                viewModel = vm,
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.DASHBOARD,
            arguments = listOf(navArgument("vehicleId") { type = NavType.LongType })
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: return@composable
            val vm: DashboardViewModel = viewModel(factory = factory)
            DashboardScreen(
                viewModel = vm,
                vehicleId = vehicleId,
                onAddFillUp = { navController.navigate(Routes.addFillUp(vehicleId)) },
                onScanReceipt = { navController.navigate(Routes.scanReceipt(vehicleId)) },
                onOpenMaintenance = { navController.navigate(Routes.maintenance(vehicleId)) },
                onNearbyStations = { navController.navigate(Routes.nearbyStations(vehicleId)) },
                onCompareVehicles = { navController.navigate(Routes.COMPARE_VEHICLES) },
                onViewAllFillUps = { navController.navigate(Routes.fillUps(vehicleId)) },
                onSwitchVehicle = { navController.popBackStack(Routes.VEHICLES, inclusive = false) }
            )
        }

        composable(
            Routes.FILL_UPS,
            arguments = listOf(navArgument("vehicleId") { type = NavType.LongType })
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: return@composable
            val vm: FillUpViewModel = viewModel(factory = factory)
            FillUpListScreen(
                viewModel = vm,
                vehicleId = vehicleId,
                onAddFillUp = { navController.navigate(Routes.addFillUp(vehicleId)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.ADD_FILL_UP,
            arguments = listOf(
                navArgument("vehicleId") { type = NavType.LongType },
                navArgument("volume") { type = NavType.StringType; defaultValue = "" },
                navArgument("cost") { type = NavType.StringType; defaultValue = "" },
                navArgument("station") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: return@composable
            val vm: FillUpViewModel = viewModel(factory = factory)
            AddFillUpScreen(
                viewModel = vm,
                vehicleId = vehicleId,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
                initialVolumeText = dec(backStackEntry.arguments?.getString("volume")),
                initialCostText = dec(backStackEntry.arguments?.getString("cost")),
                initialStationText = dec(backStackEntry.arguments?.getString("station"))
            )
        }

        composable(
            Routes.SCAN_RECEIPT,
            arguments = listOf(navArgument("vehicleId") { type = NavType.LongType })
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: return@composable
            ScanReceiptScreen(
                onUseValues = { parsed ->
                    navController.navigate(
                        Routes.addFillUp(
                            vehicleId = vehicleId,
                            volume = parsed.volume,
                            cost = parsed.totalCost,
                            station = parsed.stationGuess
                        )
                    ) {
                        popUpTo(Routes.dashboard(vehicleId))
                    }
                },
                onSkip = {
                    navController.navigate(Routes.addFillUp(vehicleId)) {
                        popUpTo(Routes.dashboard(vehicleId))
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.MAINTENANCE,
            arguments = listOf(navArgument("vehicleId") { type = NavType.LongType })
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: return@composable
            val vm: MaintenanceViewModel = viewModel(factory = factory)
            MaintenanceListScreen(
                viewModel = vm,
                vehicleId = vehicleId,
                onAddReminder = { navController.navigate(Routes.addMaintenance(vehicleId)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.ADD_MAINTENANCE,
            arguments = listOf(navArgument("vehicleId") { type = NavType.LongType })
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: return@composable
            val vm: MaintenanceViewModel = viewModel(factory = factory)
            AddMaintenanceReminderScreen(
                viewModel = vm,
                vehicleId = vehicleId,
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Routes.NEARBY_STATIONS,
            arguments = listOf(navArgument("vehicleId") { type = NavType.LongType })
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: return@composable
            NearbyStationsScreen(
                onBack = { navController.popBackStack() },
                onLogFillUpAt = { stationName ->
                    navController.navigate(
                        Routes.addFillUp(vehicleId = vehicleId, station = stationName)
                    ) {
                        popUpTo(Routes.dashboard(vehicleId))
                    }
                }
            )
        }

        composable(Routes.COMPARE_VEHICLES) {
            val vm: CompareVehiclesViewModel = viewModel(factory = factory)
            CompareVehiclesScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
