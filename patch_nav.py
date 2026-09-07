with open('tvApp/src/main/java/com/tvmime/tv/ui/navigation/TvNavigation.kt', 'r') as f:
    content = f.read()

replacement = """import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState
import com.tvmime.tv.ui.common.StatelessAppShell

@Composable
fun TvMimeNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Destinations.ONBOARDING
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    
    val sharedViewModel: TvMainViewModel = viewModel(
        factory = TvMainViewModel.Factory(application)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: startDestination

    val isFullScreen = currentRoute == Destinations.ONBOARDING || currentRoute == Destinations.PLAYER

    if (isFullScreen) {
        NavHostComponent(navController, sharedViewModel, startDestination)
    } else {
        StatelessAppShell(
            currentRoute = currentRoute,
            onNavigate = { route ->
                val dest = when(route) {
                    "LIVETV" -> Destinations.LIVE_TV
                    else -> Destinations.LIVE_TV
                }
                navController.navigate(dest) {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }
        ) {
            NavHostComponent(navController, sharedViewModel, startDestination)
        }
    }
}

@Composable
fun NavHostComponent(
    navController: NavHostController,
    sharedViewModel: TvMainViewModel,
    startDestination: String
) {
    NavHost("""

content = content.replace("""@Composable
fun TvMimeNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Destinations.ONBOARDING
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    
    val sharedViewModel: TvMainViewModel = viewModel(
        factory = TvMainViewModel.Factory(application)
    )

    NavHost(""", replacement)

with open('tvApp/src/main/java/com/tvmime/tv/ui/navigation/TvNavigation.kt', 'w') as f:
    f.write(content)
