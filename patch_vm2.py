import re

with open('tvApp/src/main/java/com/tvmime/tv/viewmodel/TvMainViewModel.kt', 'r') as f:
    content = f.read()

# Add activeEngine flow exposure
exposure = """
    val activeEngineState = engineController.activeEngine
        .flatMapLatest { it.currentState }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.tvmime.tv.player.EngineState.IDLE)
"""

content = content.replace("    val selectedCategoryId = _selectedCategoryId.asStateFlow()", "    val selectedCategoryId = _selectedCategoryId.asStateFlow()\n" + exposure)

with open('tvApp/src/main/java/com/tvmime/tv/viewmodel/TvMainViewModel.kt', 'w') as f:
    f.write(content)
