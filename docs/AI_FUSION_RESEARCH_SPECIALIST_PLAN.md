# AI-FUSION Research Specialist — Engineering Plan

## Status
Design and integration contract only. No Research Specialist model has been trained, exported, packaged, or connected by this document. Existing ResearchCore is a web-search prototype, not the trained model.

## Product contract
- Primary task: research questions, compare evidence, detect conflicting claims, summarize sources, and provide traceable citations.
- Languages: Bahasa Malaysia and English first; add other Malaysian languages only when training/evaluation data supports them.
- Online: search the web, fetch readable pages, extract dated evidence, compare independent sources, and cite the exact source URL and retrieval time.
- Offline: reason over the user's local documents and a locally stored evidence cache. Clearly disclose that live web checks are unavailable.
- Never fabricate a citation, claim live verification without retrieved evidence, or treat a model's confidence score as proof.

## Runtime architecture
1. Chat Core normalizes the request and conversation context.
2. Local AI Router classifies intent as research, ordinary chat, document QA, or another specialist task.
3. Research Orchestrator selects:
   - Online path when network is available: Search Adapter -> Page Fetcher -> Readability/HTML parser -> Evidence Store -> claim extraction -> cross-source verifier -> synthesis.
   - Offline path: local document retrieval + cached evidence + local Research Specialist inference.
4. Research Specialist is a model artifact plus tokenizer, prompt template, and runtime adapter. Model formats are not interchangeable:
   - GGUF: llama.cpp-compatible generation path.
   - ONNX: only with a compatible exported graph, tokenizer, and generation loop; ONNX Runtime alone does not make an arbitrary graph a chat model.
   - LiteRT/TFLite: only when graph ops, tokenizer and generation are supported by the chosen LiteRT runtime.
5. Output renderer returns summary, individual claims, evidence snippets, source URLs, publication/retrieval dates, contradictions, and unknowns.

## Model trained from scratch
Train a compact decoder-only language model from random initialization. Build tokenizer and vocabulary from licensed, deduplicated Bahasa Malaysia + English corpora. Do not initialize from third-party model weights. Keep the architecture and training recipe reproducible and versioned. A small model trained from scratch will initially be much weaker than a strong pretrained model; research quality must be measured, not assumed.

### Training stages
1. Data audit: provenance, license, language, deduplication, PII removal, contamination checks, source/date metadata.
2. Tokenizer training: representative BM/English text; test spelling, code-switching, names, URLs and citations.
3. Pretraining: general language + factual prose + document structure using a reproducible training run; validation split by document/source, not random paragraphs.
4. Research instruction tuning: question decomposition, evidence extraction, source comparison, uncertainty statements, BM/English answer synthesis. Use only properly licensed or authored examples.
5. Preference/calibration stage: reward abstaining when evidence is insufficient, faithful quoting, correct citations, and explicit disagreement handling.
6. Export: canonical checkpoint -> ONNX and/or GGUF and/or LiteRT only after numerical parity and task quality tests. Quantization is a deployment conversion, not training evidence.
7. Regression: compare full precision vs exported/quantized output, citations, multilingual quality, latency, memory, and crash rate.

## Data plan
Create a dataset manifest with fields: record_id, source_url_or_origin, license, language, created_at, retrieved_at, document_hash, split, pii_review, quality_review, and transformation_version.
- General language: openly licensed BM/English text with explicit provenance.
- Research tasks: human-authored or appropriately licensed question/evidence/answer triples.
- Evidence verification: claims paired with primary sources, contradictory sources, dates, and labels such as supported, contradicted, mixed, or insufficient evidence.
- Citation tasks: answers where each factual claim maps to an exact supporting passage and URL.
- Negative examples: broken URLs, outdated pages, copied/duplicate sources, unsupported claims, spoofed titles, and irrelevant search results.
- Holdout sets: never used in training; split by source and time to reduce leakage.
Do not scrape or train on content unless the collection and training rights permit it. Avoid private conversations and sensitive personal data.

## Verification policy
The verifier must not decide truth from domain count or a heuristic score alone. For each claim, record:
- normalized claim and its scope/time period;
- supporting, contradicting, and inconclusive evidence;
- exact quoted passage and source URL;
- source type (primary/secondary), publication date and retrieval date;
- independence/duplicate cluster;
- verdict: supported, contradicted, mixed, or insufficient evidence.
Prioritize primary sources where applicable. Treat government/university domains as context, not automatic proof. The verifier may return "insufficient evidence" and should do so when evidence is weak.

## Android integration contract
- Keep the chat UI independent of a specific model format.
- Add specialist metadata to model registry: specialistId, format, tokenizer/config locations, runtime adapter, model version, license, checksum, supported languages, and estimated resource class.
- Router selects a model only if its specialistId and runtime are compatible and current available memory passes a safety margin.
- Use app-private storage for model artifacts, verify checksums, and load/unload models off the main thread.
- CPU path is the reliable baseline. Enable GPU/NPU only after runtime capability detection and a real inference smoke test; API presence alone is not proof of acceleration.
- Online fetching runs on IO dispatchers with timeouts, size limits, redirect validation, and safe HTML extraction. Do not execute page scripts or trust page text as instructions.
- Cache evidence with timestamps and expiry. Mark stale cache as stale.
- No model or research result is considered connected until integration tests and on-device tests pass.

## Acceptance tests
1. Offline question with no local evidence reports the limitation rather than inventing citations.
2. Online question returns clickable URLs and quoted evidence from retrieved pages.
3. Two conflicting sources are both surfaced.
4. Old evidence is identified as old for a current-events question.
5. Search failure and HTTP timeout degrade gracefully.
6. BM, English, and code-switched prompts are tested.
7. Prompt injection embedded in a page is treated as untrusted content.
8. Runtime mismatch or insufficient memory selects a safe fallback without crashing.
9. Exported runtime output passes quality/parity checks.
10. Android tests verify cancellation, no main-thread network, and stable memory under repeated queries.

## Existing code gaps to address before claiming integration
- ResearchCore currently uses a lightweight DuckDuckGo HTML search and returns titles/URLs; it does not fetch and parse full evidence passages.
- FactVerificationEngine currently computes a heuristic score from URL counts/domains. This is not claim-level fact verification and must be replaced or clearly labelled as a discovery heuristic.
- ModelChatRouter currently selects generic GGUF chat models by format/size and treats ONNX/LiteRT as task-specific imports; it has no specialist identity or trained Research Specialist artifact.
- Mini Server handoff acknowledgement is not research inference.
