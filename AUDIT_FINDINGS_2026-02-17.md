# 0xRupex Project Audit Findings (2026-02-17)

## Scope
- Reviewed full repository structure (`android`, `backend`, docs, deploy/config files).
- Performed static code audit for architecture, API contract consistency, data integrity, security posture, and testing health.
- Executed available tests:
  - `backend`: `npm test -- --runInBand` (no tests found, exit 1)
  - `android`: `./gradlew testDebugUnitTest --no-daemon` (passed)

## Executive Summary
- The largest risk is a backend-Android contract mismatch (payload shapes, field names, and ID types) across auth/accounts/categories/transactions.
- Registration currently has hard breakpoints on both backend and Android.
- Transaction/account consistency is not transactionally protected in backend service logic.
- Security posture is acceptable for local dev but not production-safe in several defaults.

## Findings

### Critical

1. Registration is broken by missing constant in backend default-category bootstrap.
- `backend/src/services/category.service.js:3`
- `backend/src/services/category.service.js:125`
- `backend/src/config/constants.js:1`
- `CategoryService` imports `DEFAULT_CATEGORIES`, but `constants.js` only exports `DEFAULT_EXPENSE_CATEGORIES` and `DEFAULT_INCOME_CATEGORIES`.

2. Registration request contract mismatch (Android sends no `name`, backend requires it).
- `android/app/src/main/java/com/rupex/app/ui/LoginActivity.java:113`
- `android/app/src/main/java/com/rupex/app/data/remote/model/LoginRequest.java:16`
- `backend/src/api/middleware/validate.middleware.js:33`
- Register endpoint validation requires `name` (2-100 chars), but Android reuses login payload (email/password only).

3. API response shape mismatch: backend returns wrapped objects, Android expects flat lists/objects.
- Backend responses:
  - `backend/src/api/controllers/account.controller.js:11` returns `{ accounts, total_balance }`
  - `backend/src/api/controllers/category.controller.js:11` returns `{ categories }`
  - `backend/src/api/controllers/transaction.controller.js:37` returns `{ transaction }`
- Android expects:
  - `ApiResponse<List<AccountDto>>` / `ApiResponse<List<CategoryDto>>` / `ApiResponse<TransactionDto>`
  - `android/app/src/main/java/com/rupex/app/data/repository/FinanceRepository.java:77`
  - `android/app/src/main/java/com/rupex/app/data/repository/FinanceRepository.java:105`
  - `android/app/src/main/java/com/rupex/app/data/repository/FinanceRepository.java:220`
  - `android/app/src/main/java/com/rupex/app/ui/MainViewModel.java:555`
  - `android/app/src/main/java/com/rupex/app/sync/SyncWorker.java:99`

4. DTO field naming/type mismatch (snake_case backend vs camelCase Android, UUID backend vs int Android).
- Android uses `int` IDs:
  - `android/app/src/main/java/com/rupex/app/data/remote/model/AccountDto.java:11`
  - `android/app/src/main/java/com/rupex/app/data/remote/model/CategoryDto.java:11`
- Backend uses UUID IDs in schema.
- Android transaction DTO expects camelCase:
  - `android/app/src/main/java/com/rupex/app/data/remote/model/TransactionDto.java:14`
  - `android/app/src/main/java/com/rupex/app/data/remote/model/TransactionDto.java:38`
- Backend returns snake_case fields for transaction columns.

### High

5. Transaction creation not atomic; account/category ownership checks are incomplete.
- `backend/src/services/transaction.service.js:141`
- `backend/src/services/transaction.service.js:163`
- `backend/src/services/account.service.js:20`
- Insert occurs before account ownership validation (happens indirectly in balance update).
- No explicit category ownership validation before insert.
- If post-insert account balance update fails, transaction may remain inserted (partial write).

6. Transfer logic in balances is incorrect.
- `backend/src/services/account.service.js:108`
- `backend/src/services/transaction.service.js:243`
- `backend/src/services/transaction.service.js:286`
- `updateBalance` handles only income/expense; transfer is ignored, while update/delete reversal logic treats non-expense by fallback logic.

7. Account type validation conflicts with DB enum.
- `backend/src/api/middleware/validate.middleware.js:52`
- `backend/src/database/migrations/003_create_accounts.js:9`
- API validator allows `savings/current/investment`; DB allows `bank/wallet/cash/credit_card`.

8. Time handling risks (timezone + parsing + monthly boundary).
- Android sends `"Z"` timestamp without setting timezone in formatter:
  - `android/app/src/main/java/com/rupex/app/data/remote/model/CreateTransactionRequest.java:95`
- Android server date parsing likely fails for backend ISO values with millis/timezone and falls back to "now":
  - `android/app/src/main/java/com/rupex/app/ui/MainViewModel.java:300`
  - `android/app/src/main/java/com/rupex/app/ui/MainViewModel.java:305`
- Monthly summary end boundary may exclude last day after midnight:
  - `backend/src/services/transaction.service.js:303`

9. Retrofit API contract contains incorrect endpoint/query definitions.
- `transactions/bulk` endpoint is declared in Android but backend exposes `transactions/sync`:
  - `android/app/src/main/java/com/rupex/app/data/remote/RupexApi.java:60`
  - `backend/src/api/routes/transaction.routes.js:14`
- Android uses `startDate`/`endDate` query names; backend expects `start_date`/`end_date`:
  - `android/app/src/main/java/com/rupex/app/data/remote/RupexApi.java:53`
  - `backend/src/api/middleware/validate.middleware.js:103`

10. Pagination field mismatch.
- Backend returns `total_pages`:
  - `backend/src/services/transaction.service.js:86`
- Android expects `totalPages`:
  - `android/app/src/main/java/com/rupex/app/data/remote/model/PaginatedApiResponse.java:43`

11. Update payload field mismatch (`categoryName`/`transactionAt` vs backend expected snake_case branches).
- Android update map keys:
  - `android/app/src/main/java/com/rupex/app/data/remote/model/UpdateTransactionRequest.java:31`
  - `android/app/src/main/java/com/rupex/app/data/remote/model/UpdateTransactionRequest.java:84`
- Backend update service primarily reads snake_case for many fields except partial special handling:
  - `backend/src/services/transaction.service.js:224`
  - `backend/src/services/transaction.service.js:261`
  - `backend/src/services/transaction.service.js:267`

### Medium

12. Sensitive request body is logged in global error logger.
- `backend/src/api/middleware/error.middleware.js:16`
- May log passwords/tokens on auth failures.

13. Rate limit config exists but no limiter middleware mounted.
- Config:
  - `backend/src/config/index.js:27`
- App middleware stack:
  - `backend/src/app.js:14`
- No `express-rate-limit` application in routes.

14. Android production safety concerns.
- Hardcoded local HTTP server in build config:
  - `android/app/build.gradle:20`
- Cleartext traffic explicitly allowed for specific domains:
  - `android/app/src/main/res/xml/network_security_config.xml:4`
- TokenManager falls back to plain `SharedPreferences` if encrypted prefs setup fails:
  - `android/app/src/main/java/com/rupex/app/util/TokenManager.java:42`

15. Backend `logout` route is authenticated but does not validate refresh token body.
- `backend/src/api/routes/auth.routes.js:13`
- `backend/src/api/controllers/auth.controller.js:53`
- This is functional but weak for strict session revocation semantics.

### Low

16. Several Android data models are duplicated or legacy and increase maintenance risk.
- `android/app/src/main/java/com/rupex/app/data/model/*`
- `android/app/src/main/java/com/rupex/app/data/remote/model/*`
- Mixed model families with inconsistent field naming conventions.

17. No backend automated tests currently present.
- Test run result: no Jest tests discovered.

## Testing Evidence

### Backend
- Command: `cd backend && npm test -- --runInBand`
- Result: failed with "No tests found".

### Android
- Command: `cd android && ./gradlew testDebugUnitTest --no-daemon`
- Result: `BUILD SUCCESSFUL`.

## Recommended Fix Order

1. Fix registration path end-to-end:
- Add/derive default category list in backend service.
- Add `name` in Android register payload and UI.

2. Unify API contracts:
- Normalize response wrappers (or adjust Android deserialization wrappers).
- Convert Android DTO IDs to `String` UUID.
- Align snake_case/camelCase fields with `@SerializedName`.

3. Fix transaction integrity:
- Wrap create/update/delete + balance adjustments in DB transactions.
- Enforce account/category ownership before insert/update.
- Implement explicit transfer accounting logic.

4. Fix query/pagination contracts:
- Align `start_date/end_date`.
- Align `total_pages/totalPages`.
- Remove or replace dead `transactions/bulk` API method.

5. Harden security:
- Stop logging `req.body` for sensitive paths or redact.
- Apply rate limiting middleware.
- Remove hardcoded HTTP base URL for release and tighten cleartext policy.

6. Add regression tests:
- Auth register/login/refresh.
- Transaction create/update/delete with balance invariants.
- DTO contract tests for accounts/categories/transactions.

