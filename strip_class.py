import re

with open('tvApp/src/main/java/com/streamvault/data/preferences/PreferencesRepository.kt', 'r') as f:
    content = f.read()

# Make it an interface
content = re.sub(r'class PreferencesRepository @Inject constructor\([^)]*\)\s*:\s*[^{]*{', 'interface PreferencesRepository {', content)

# Remove all properties assignments and method bodies
# This is tricky with regex. A simpler way is to just delete the file and recreate it with the 15-20 methods the UI actually calls.
