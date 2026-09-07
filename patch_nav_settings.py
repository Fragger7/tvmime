with open('tvApp/src/main/java/com/tvmime/tv/ui/navigation/TvNavigation.kt', 'r') as f:
    content = f.read()

replacement = """        composable(
            route = Destinations.PLAYER,
            arguments = listOf(navArgument("streamUrl") { type = NavType.StringType })
        ) { backStackEntry ->
            val streamUrl = backStackEntry.arguments?.getString("streamUrl") ?: ""
            PlayerRoute(
                streamUrl = streamUrl,
                viewModel = sharedViewModel
            )
        }
        composable("SETTINGS") {
            com.tvmime.tv.ui.settings.SettingsScreen(
                viewModel = sharedViewModel,
                onLogoutSuccess = {
                    navController.navigate(Destinations.ONBOARDING) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }"""

content = content.replace("""        composable(
            route = Destinations.PLAYER,
            arguments = listOf(navArgument("streamUrl") { type = NavType.StringType })
        ) { backStackEntry ->
            val streamUrl = backStackEntry.arguments?.getString("streamUrl") ?: ""
            PlayerRoute(
                streamUrl = streamUrl,
                viewModel = sharedViewModel
            )
        }""", replacement)

with open('tvApp/src/main/java/com/tvmime/tv/ui/navigation/TvNavigation.kt', 'w') as f:
    f.write(content)
