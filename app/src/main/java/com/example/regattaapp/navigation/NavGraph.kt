package com.example.regattaapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.regattaapp.ui.screen.home.HomeScreen
import com.example.regattaapp.ui.screen.create.CreateRoomScreen
import com.example.regattaapp.ui.screen.join.JoinRoomScreen
import com.example.regattaapp.ui.screen.map.MapScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("create") { CreateRoomScreen(navController) }
        composable("join") { JoinRoomScreen(navController) }
        composable("map/{roomId}") { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            MapScreen(roomId = roomId, navController = navController)
        }
    }
}
