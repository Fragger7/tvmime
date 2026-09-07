with open('tvApp/src/main/java/com/tvmime/tv/ui/settings/SettingsScreen.kt', 'r') as f:
    content = f.read()

replacement = """            } else if (selectedCategory == 1) {
                var otaStatus by remember { mutableStateOf("Ready to check.") }
                val scope = rememberCoroutineScope()
                
                Column {
                    Text("System Updates", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(otaStatus)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        otaStatus = "Checking GitHub Releases..."
                        scope.launch {
                            otaStatus = com.tvmime.sync.OtaManager.checkForUpdates()
                        }
                    }) {
                        Text("Check for Updates Now")
                    }
                }
            }"""

old = """            } else if (selectedCategory == 1) {
                Column {
                    Text("System Updates", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Auto-checking for updates via GitHub Releases is enabled.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { /* Trigger OTA Check */ }) {
                        Text("Check for Updates Now")
                    }
                }
            }"""

content = content.replace("import androidx.compose.foundation.shape.RoundedCornerShape", "import androidx.compose.foundation.shape.RoundedCornerShape\nimport kotlinx.coroutines.launch")
content = content.replace(old, replacement)

with open('tvApp/src/main/java/com/tvmime/tv/ui/settings/SettingsScreen.kt', 'w') as f:
    f.write(content)
