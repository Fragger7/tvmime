import re

with open('TODO.md', 'r') as f:
    content = f.read()

# Remove the obsolete KSP stubs section since we are abandoning Dagger Hilt
content = re.sub(r'## 🏃 Next Up \(URGENT: Kotlin Compilation Fixes\).*', '', content, flags=re.DOTALL)

with open('TODO.md', 'w') as f:
    f.write(content)
