sed -i '' -e '417,421c\
                withFrameNanos { }\
                try {\
                    displayFocusRequester.requestFocus()\
                    restoreDisplayFocus = false\
                    return@LaunchedEffect\
                } catch(e: Exception) {}\
' tvApp/src/main/java/com/tvmime/tv/ui/common/TvUiComponents.kt
