# Mobile Banking — Fund Transfer & Transaction History

Kotlin · XML layouts / Android Views · MVVM · StateFlow · Coroutines · Navigation Component · Hilt · RecyclerView + ListAdapter.
**No Jetpack Compose anywhere.**

## Build and run
1. Open the project root in Android Studio (JDK 17+ — Android Studio's bundled JDK is fine).
2. Let Gradle sync. Versions are in `gradle/libs.versions.toml` (AGP 8.7.3, Kotlin 2.0.21, KSP, Hilt 2.52, Gradle 8.9). If Android Studio offers its AGP/Gradle upgrade assistant, accepting it is fine.
3. Run the `app` configuration on an emulator/device with API 26+ (`minSdk 26`, `compileSdk/targetSdk 35`).

## Run the unit tests
```
./gradlew :app:testDebugUnitTest      # or: right-click app/src/test → Run Tests
```
(If `gradlew` is missing, open the project once in Android Studio, or run `gradle wrapper`.)

## Architecture
MVVM with Clean Architecture.
Single activity, one nav graph. 
Bottom navigation: **Transfer** and **History**.

## Screenshots

<img width="700" height="350" alt="Screenshot_20261014_022318" src="https://github.com/user-attachments/assets/507028e2-1c2b-447c-aecf-952f7302cf4c" />
<img width="700" height="350" alt="Screenshot_20261014_022155" src="https://github.com/user-attachments/assets/c2233ac8-7e44-4e3f-babc-0239d780f137" />
<img width="700" height="350" alt="Screenshot_20261014_022112" src="https://github.com/user-attachments/assets/48b154ae-bed7-4d8d-b592-5f25baf62cd1" />
<img width="700" height="350" alt="Screenshot_20261014_021952" src="https://github.com/user-attachments/assets/40ba5756-70ba-40ab-bc29-4e51e7d6c783" />
<img width="700" height="350" alt="Screenshot_20261014_021927" src="https://github.com/user-attachments/assets/4aa6dabe-aea6-4a49-b0fe-e3682f88102a" />
<img width="700" height="350" alt="Screenshot_20261014_021856" src="https://github.com/user-attachments/assets/59cb6438-5360-439c-abbc-09c23d9f1061" />
<img width="700" height="350" alt="Screenshot_20261014_021837" src="https://github.com/user-attachments/assets/3c81ecd0-c1f2-47bc-b53c-d6062ead2d67" />
<img width="700" height="350" alt="Screenshot_20261014_021822" src="https://github.com/user-attachments/assets/4cea6aff-cb75-4613-9b34-7938449e048b" />

<img width="700" height="350" alt="Screenshot_20261014_021455" src="https://github.com/user-attachments/assets/ef6b4f7d-76ea-45d0-b814-72e7b435fd8b" />

### Major design decisions
- **StateFlow everywhere** (applied consistently, no LiveData). Fragments collect with `repeatOnLifecycle(STARTED)` inside `viewLifecycleOwner`; ViewModels use `viewModelScope`; no View/Fragment references leak into ViewModels.
- **Money is `Long` kobo** end to end. `MoneyParser` turns typed text into exact kobo using `BigDecimal`-free integer maths with `multiplyExact/addExact` overflow guards; `MoneyFormatter` uses `BigDecimal.valueOf(kobo, 2)`. `Double` is never used. Amounts above ₦1 trillion are rejected.
- **Validation and fee maths live in `TransferValidator`/`TransferAmounts`** (pure, unit-tested), not in fragments. Fee is a fixed ₦25.00 (`TransferPolicy.FEE_KOBO`); total debit = amount + fee.
- **One `TransferViewModel` scoped to the nav graph** shared by form → confirmation → result. That is why *Cancel* returns to the form with everything intact.
- **Account-resolution races.** Every change to bank/account number (1) cancels the in-flight job, (2) clears the old name immediately, (3) increments a request id and (4) re-checks the answer's `(bank, accountNumber)` key against what is currently entered before applying it. Two independent guards mean a stale answer cannot show even if a data source ignores cancellation (there is a test for exactly that). The validator additionally refuses to proceed unless the resolved key equals the *current* bank + number.
- **Duplicate submission.** The button is disabled/overlay shown, *and* the ViewModel moves `Idle → Submitting` atomically inside `StateFlow.update`, so repeated taps are rejected even if the UI guard failed.
- **Uncertain outcomes.** Timeout / exception ⇒ `Unknown` (never "failed"). The draft keeps its original `reference`; the result screen offers **Check status** (queries that same reference — it never submits again) and a banner on the form blocks a new transfer until the original is settled. Pending behaves the same way.
- **History = derived state.** `HistoryViewModel` combines `(repository.transactions, query, filter)`; the source list is never modified. Because the transfer repository writes to the same in-memory store, a completed (or pending) transfer appears in History immediately — no restart or reload.
- **Edge-to-edge** is handled with window insets in `MainActivity`; screens are scrollable and survive rotation (state lives in ViewModels; input fields use `saveEnabled=false` and are re-seeded from state).

## Demonstrating the states
Use the **⋮ menu in the toolbar**:
- **Simulate transfer outcome** → Successful / Failed / Pending / Timeout (unknown). Applies to the *next* transfer.
    - *Pending*: tap **Check status** once → still pending; again → successful.
    - *Timeout*: result says status could not be confirmed; **Check status** reveals the backend did process it (history updates at that point).
- **Simulate history data** → Normal / Empty / Service error. Pull down on History to reload (or **Retry** on the error state).

Account-resolution test numbers (any bank):
| Account number | Result |
|---|---|
| any 10 digits | resolves to a name (depends on bank + digits) |
| starts with `000` | account not found |
| ends with `99` | service error (shows **Retry**) |
| last digit `0` … `9` | latency 0.6 s … 2.85 s — type `0123456789`, then quickly change the last digit to `0` to see a slow request overtaken by a fast one |

Balances: Savings ₦150,000.00 · Current ₦2,350,500.75 · Salary ₦8,420.50 (use it to see the insufficient-balance message).

## Tests (`app/src/test`)
| Required scenario | Where |
|---|---|
| Form validation | `TransferValidatorTest` |
| Insufficient balance | `TransferValidatorTest` (exact balance, 1 kobo short, fee counted) |
| Fee and total debit | `TransferValidatorTest`, `MoneyTest` |
| Duplicate-submission prevention | `TransferViewModelTest` (`rapid repeated confirm taps…`) |
| Successful / failed / pending (+ unknown) | `TransferViewModelTest`, `TransferRepositoryImplTest` |
| Filtering by type + search | `FilterTransactionsTest`, `HistoryViewModelTest` |
| Extras | resolution race (cancellation honoured *and* ignored), bank/number change clears name, status check uses original reference, backend idempotent replay, overflow guards |

## Assumptions and limitations
- Everything is simulated and in memory; data resets on process restart. `Transaction.date` is a display string, as in the brief.
- **A UI/ViewModel guard does not give exactly-once processing.** Real exactly-once needs backend idempotency keys (the draft `reference` is designed to be that key) plus transaction-status reconciliation. The simulated backend demonstrates this by keying its ledger on `reference`.
- Client-side balance and fee checks are conveniences only. The simulated backend re-validates the balance independently, as a real one must (plus fees, authorization, limits).
- No sensitive account data is logged. There is no authentication.
- Pending transfers are treated as funds on hold (balance debited when accepted). A timed-out transfer is only recorded in history once its status is checked.
- If the process is killed, an unresolved transfer is forgotten (a production app would persist it).
- The UI uses the light Material 3 theme only.
