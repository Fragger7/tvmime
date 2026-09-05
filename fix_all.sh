#!/bin/bash
find tvApp/src/main/java/com/tvmime/tv/ui -name "*.kt" -type f -exec sed -i '' -E 's/stringResource\(R\.string\.[a-zA-Z0-9_]+\)/""/g' {} +
find tvApp/src/main/java/com/tvmime/tv/ui -name "*.kt" -type f -exec sed -i '' -E 's/painterResource\(R\.drawable\.[a-zA-Z0-9_]+\)/painterResource(android.R.drawable.ic_menu_help)/g' {} +
find tvApp/src/main/java/com/tvmime/tv/ui -name "*.kt" -type f -exec sed -i '' -E 's/R\.drawable\.[a-zA-Z0-9_]+/android.R.drawable.ic_menu_help/g' {} +
find tvApp/src/main/java/com/tvmime/tv/ui -name "*.kt" -type f -exec sed -i '' -E 's/R\.string\.[a-zA-Z0-9_]+/0/g' {} +
