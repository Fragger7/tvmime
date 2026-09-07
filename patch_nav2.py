with open('tvApp/src/main/java/com/tvmime/tv/ui/navigation/TvNavigation.kt', 'r') as f:
    content = f.read()

replacement = """@Composable
fun TvMimeNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Destinations.ONBOARDING,
    sharedViewModel: TvMainViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()"""

old = """@Composable
fun TvMimeNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Destinations.ONBOARDING
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    
    val sharedViewModel: TvMainViewModel = viewModel(
        factory = TvMainViewModel.Factory(application)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()"""

content = content.replace(old, replacement)

with open('tvApp/src/main/java/com/tvmime/tv/ui/navigation/TvNavigation.kt', 'w') as f:
    f.write(content)
