sed -i '' -e '417,420c\
                displayFocusRequester.requestFocus()\
                restoreDisplayFocus = false\
                return@LaunchedEffect\
' tvApp/src/main/java/com/tvmime/tv/ui/common/TvUiComponents.kt
