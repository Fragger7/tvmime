import os
import re

def remove_hilt(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
    content = re.sub(r'import javax\.inject\.Inject\n', '', content)
    content = re.sub(r'import javax\.inject\.Singleton\n', '', content)
    content = re.sub(r'@Singleton\n', '', content)
    content = re.sub(r'@Inject constructor', 'constructor', content)
    with open(filepath, 'w') as f:
        f.write(content)

remove_hilt('tvApp/src/main/java/com/tvmime/tv/player/EngineController.kt')
remove_hilt('tvApp/src/main/java/com/tvmime/tv/sync/SyncManagerM3uImporter.kt')
