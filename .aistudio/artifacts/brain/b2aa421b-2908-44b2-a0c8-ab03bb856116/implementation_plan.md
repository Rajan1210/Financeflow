# Production-Ready Implementation Plan: Tasks 2 to 6

Following our completed Task 1 (Firebase Authentication & AuthGateScreen), we will proceed with the remaining production tasks in exact sequence, reporting after each task.

---

## 1. Task 2 — Security Fixes
- **AndroidManifest.xml**:
  - Set `android:allowBackup="false"`.
- **FinanceRepository.hashPin()**:
  - Generate a cryptographic random salt per installation.
  - Store salt securely using Android Keystore / EncryptedSharedPreferences (or Keystore-backed AES key storage with SharedPreferences fallback).
  - Use `PBKDF2WithHmacSHA256` with 10,000 iterations and a 256-bit key length.
  - Support backward-compatible migration for existing raw SHA-256 hashed PINs upon next successful unlock.
- **Biometric Integration**:
  - In `MainActivity.kt` and `AuthScreen.kt`, check biometric hardware status using `BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)`.
  - When available and enrolled, trigger `androidx.biometric.BiometricPrompt`.
  - When not available or not enrolled, automatically suppress the biometric button and fallback cleanly to PIN entry.

---

## 2. Task 3 — 3-Category SMS Parser & DueEntity
- **SMS Classification Flow**:
  1. **FAILED / BOUNCED (Priority 1)**:
     - Keywords: *failed, declined, insufficient balance, not honoured, not honored, unsuccessful, returned, will be re-presented, bounce charges*.
     - Executed *before* debit keyword checks to prevent false expense creation.
     - Action: Create `NotificationAlertEntity` ("Payment Failed ⚠️") and update any matching `DueEntity` or pending item.
  2. **UPCOMING / SCHEDULED DUE (Priority 2)**:
     - Keywords/Patterns: *will be deducted, will be debited, is scheduled, upcoming payment, mandate UMN, overdue amount, kindly clear your overdue amount, repay now*.
     - Action: Create `DueEntity` (id, title, amount, dueDate, lenderName, sourceSmsBody, status: PENDING/PAID/DISMISSED) and insert a reminder `NotificationAlertEntity`.
  3. **CONFIRMED TRANSACTION (Priority 3)**:
     - Past-tense confirmation (*debited, credited, spent, withdrawn, has been debited/credited*).
     - Action: Create standard `TransactionEntity` with auto-category detection.
- **UI Integration**:
  - Add "Upcoming Dues" section to Dashboard and/or dedicated view with pay/dismiss options.
- **Unit Tests**:
  - Add test suite in test folder covering confirmed debit, e-mandate upcoming debit, overdue reminder, and mixed failed EMI debit.

---

## 3. Task 4 — Account-to-SMS Matching
- **Repository Account Matching**:
  - Replace name matching with exact last-4-digit matching using `SmsTransactionParser.extractAccountSnippet(smsBody)` against `AccountEntity.accountNumber.takeLast(4)`.
  - If no account matches, keep `transaction.accountId = null`.
- **UI Unassigned Badge & Quick Assignment**:
  - Show "Unassigned — tap to assign to an account" badge on transactions with null `accountId`.
  - Tapping opens an account selection bottom sheet to assign the transaction and adjust that account's balance.

---

## 4. Task 5 — Manual Account / Transaction Polish & Reconciliation
- **Add Account Dialog**:
  - Ensure fields for: Account Name, Bank Type (dropdown of `BankType`), Last-4 account digits, Opening balance.
- **Add Transaction Dialog**:
  - Account picker populated with user's accounts, updating balance upon creation.
- **Reconcile Balance Action**:
  - Add "Reconcile Balance" action per account card.
  - Dialog lets user enter actual bank balance.
  - Automatically calculates `difference = realBalance - currentBalance`.
  - Inserts reconciliation transaction (Category `OTHERS`, Type `INCOME` if positive, `EXPENSE` if negative, note "Manual reconciliation") and updates account balance to match.

---

## 5. Task 6 — Room Database Migrations
- **Schema Migration**:
  - Bump database version from 1 to 2.
  - Add table `dues` for `DueEntity`.
  - Add column `firebaseUid TEXT` to `user_profiles`.
  - Replace `.fallbackToDestructiveMigration()` with `AppDatabase.MIGRATION_1_2` using exact SQLite schema declarations.
  - Verify existing user transaction, budget, and account data remain intact across upgrades.
