import re

with open('tvApp/src/main/java/com/tvmime/tv/viewmodel/TvMainViewModel.kt', 'r') as f:
    content = f.read()

# Remove Hilt imports
content = re.sub(r'import dagger\.hilt\.android\.lifecycle\.HiltViewModel\n', '', content)
content = re.sub(r'import javax\.inject\.Inject\n', '', content)

# Remove annotations and inject
content = re.sub(r'@HiltViewModel\n', '', content)
content = re.sub(r'class TvMainViewModel @Inject constructor\(', 'class TvMainViewModel(', content)
content = re.sub(r'\) : ViewModel\(\)', ') : androidx.lifecycle.AndroidViewModel(database.portalDao() as? android.app.Application ?: error("Needs Context. Use Factory"))', content) # Wait, it uses AndroidViewModel. Let's rewrite it clean instead of regex.

with open('tvApp/src/main/java/com/tvmime/tv/viewmodel/TvMainViewModel.kt', 'w') as f:
    f.write(content)
