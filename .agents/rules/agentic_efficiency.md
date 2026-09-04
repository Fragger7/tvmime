# Agentic Efficiency & Token Economy Guidelines ⚡

1. **Fast Local Unit Tests & Mocking**:
   - Mock external LLMs, provider scrapers, and remote API calls in unit tests to ensure sub-second local test cycles.
2. **CLI & Context Hygiene**:
   - Limit verbose log dumps in tool outputs. Use targeted grep/slice patterns (`grep`, bounded `head`/`tail`).
3. **Bounded File Slicing**:
   - Read large files in targeted windows (`StartLine` and `EndLine`) rather than dumping 1,000+ lines into the conversation context.
4. **Prompt Cache Optimization**:
   - Keep system instructions, core token files, and persistent task goals consistent to maximize prompt prefix cache hits.
