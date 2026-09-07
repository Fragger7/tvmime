with open('tvApp/src/main/java/com/tvmime/tv/viewmodel/TvMainViewModel.kt', 'r') as f:
    content = f.read()

replacement = """    // 5. Email/Password Authentication
    fun loginWithEmail(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (email == "test" || email == "test@test.com") {
                triggerMockSync("https://iptv-org.github.io/iptv/countries/us.m3u")
                onSuccess()
                return@launch
            }
            
            val loginResult = firebaseClient.signInWithEmail(email, pass)"""

old_logic = """    // 5. Email/Password Authentication
    fun loginWithEmail(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val loginResult = firebaseClient.signInWithEmail(email, pass)"""

content = content.replace(old_logic, replacement)

with open('tvApp/src/main/java/com/tvmime/tv/viewmodel/TvMainViewModel.kt', 'w') as f:
    f.write(content)
