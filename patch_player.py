with open('tvApp/src/main/java/com/tvmime/tv/player/EngineController.kt', 'r') as f:
    content = f.read()

content = content.replace("private val livePreviewEngine: LivePreviewEngine,", "val livePreviewEngine: LivePreviewEngine,")
content = content.replace("private val mainPlayer: MainPlayer", "val mainPlayer: MainPlayer")

with open('tvApp/src/main/java/com/tvmime/tv/player/EngineController.kt', 'w') as f:
    f.write(content)

with open('tvApp/src/main/java/com/tvmime/tv/viewmodel/TvMainViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("val activeEngineState = engineController.activeEngine", "val engineControllerInstance = engineController\n    val activeEngineState = engineController.activeEngine")

with open('tvApp/src/main/java/com/tvmime/tv/viewmodel/TvMainViewModel.kt', 'w') as f:
    f.write(content)
