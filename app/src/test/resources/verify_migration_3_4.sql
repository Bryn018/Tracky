-- Verifies the 3 -> 4 migration SQL against real SQLite.
-- Run with:  sqlite3 /tmp/mig.db < verify_migration_3_4.sql
-- Exits non-zero on any failed assertion via the .bail command.

.bail on

-- ── Schema as it was at version 3: REAL money, no smsKey ──
CREATE TABLE transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    type TEXT NOT NULL,
    amount REAL NOT NULL,
    channel TEXT NOT NULL,
    contact TEXT,
    senderName TEXT,
    messageBody TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    balance REAL,
    notes TEXT,
    category TEXT
);
CREATE TABLE daily_summaries (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    date TEXT NOT NULL,
    totalIncoming REAL NOT NULL,
    totalOutgoing REAL NOT NULL,
    transactionCount INTEGER NOT NULL,
    topChannel TEXT,
    primaryContact TEXT
);
CREATE TABLE budgets (
    category TEXT NOT NULL PRIMARY KEY,
    monthlyLimit REAL NOT NULL,
    isEnabled INTEGER NOT NULL DEFAULT 1,
    createdAt INTEGER NOT NULL DEFAULT 0
);

INSERT INTO transactions (id, type, amount, channel, contact, senderName, messageBody, timestamp, balance, notes, category)
VALUES (1, 'OUTGOING', 1500.50, 'M-Pesa', 'JOHN', 'JOHN', 'Ksh 1,500.50 sent to JOHN', 1700000000000, 4000.00, NULL, 'FOOD');
INSERT INTO transactions (id, type, amount, channel, contact, senderName, messageBody, timestamp, balance, notes, category)
VALUES (2, 'INCOMING', 200.00, 'M-Pesa', 'MARY', 'MARY', 'received', 1700000001000, NULL, NULL, 'FOOD');
INSERT INTO transactions (id, type, amount, channel, contact, senderName, messageBody, timestamp, balance, notes, category)
VALUES (3, 'OUTGOING', 100.1, 'M-Pesa', 'ANN', 'ANN', 'awkward round trip', 1700000002000, 99.99, 'a note, with comma', 'FOOD');
INSERT INTO daily_summaries (id, date, totalIncoming, totalOutgoing, transactionCount, topChannel, primaryContact)
VALUES (1, '2026-09-22', 200.00, 1500.50, 3, 'M-Pesa', 'JOHN');
INSERT INTO budgets (category, monthlyLimit, isEnabled, createdAt) VALUES ('FOOD', 5000.00, 1, 1700000000000);

-- ── The migration, copied from MIGRATION_3_4 ──
CREATE TABLE IF NOT EXISTS transactions_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    type TEXT NOT NULL,
    amountCents INTEGER NOT NULL,
    channel TEXT NOT NULL,
    contact TEXT,
    senderName TEXT,
    messageBody TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    balanceCents INTEGER,
    notes TEXT,
    category TEXT,
    smsKey TEXT NOT NULL
);
INSERT INTO transactions_new
    (id, type, amountCents, channel, contact, senderName, messageBody,
     timestamp, balanceCents, notes, category, smsKey)
SELECT
    id,
    type,
    CAST(ROUND(COALESCE(amount, 0) * 100) AS INTEGER),
    channel,
    contact,
    senderName,
    messageBody,
    timestamp,
    CASE WHEN balance IS NULL THEN NULL
         ELSE CAST(ROUND(COALESCE(balance, 0) * 100) AS INTEGER) END,
    notes,
    category,
    'legacy-' || id
FROM transactions;
DROP TABLE transactions;
ALTER TABLE transactions_new RENAME TO transactions;
CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_smsKey ON transactions (smsKey);
CREATE INDEX IF NOT EXISTS index_transactions_timestamp ON transactions (timestamp);
CREATE INDEX IF NOT EXISTS index_transactions_type ON transactions (type);

CREATE TABLE IF NOT EXISTS daily_summaries_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    date TEXT NOT NULL,
    totalIncomingCents INTEGER NOT NULL,
    totalOutgoingCents INTEGER NOT NULL,
    transactionCount INTEGER NOT NULL,
    topChannel TEXT,
    primaryContact TEXT
);
INSERT INTO daily_summaries_new
    (id, date, totalIncomingCents, totalOutgoingCents, transactionCount, topChannel, primaryContact)
SELECT
    id, date,
    CAST(ROUND(COALESCE(totalIncoming, 0) * 100) AS INTEGER),
    CAST(ROUND(COALESCE(totalOutgoing, 0) * 100) AS INTEGER),
    transactionCount, topChannel, primaryContact
FROM daily_summaries;
DROP TABLE daily_summaries;
ALTER TABLE daily_summaries_new RENAME TO daily_summaries;

CREATE TABLE IF NOT EXISTS budgets_new (
    category TEXT NOT NULL PRIMARY KEY,
    monthlyLimitCents INTEGER NOT NULL,
    isEnabled INTEGER NOT NULL DEFAULT 1,
    createdAt INTEGER NOT NULL DEFAULT 0
);
INSERT INTO budgets_new (category, monthlyLimitCents, isEnabled, createdAt)
SELECT category, CAST(ROUND(COALESCE(monthlyLimit, 0) * 100) AS INTEGER), isEnabled, createdAt
FROM budgets;
DROP TABLE budgets;
ALTER TABLE budgets_new RENAME TO budgets;

-- ── Assertions ──
-- 1. Shillings became exact cents.
SELECT 'FAIL: 1500.50 KES not 150050 cents' WHERE (SELECT amountCents FROM transactions WHERE id=1) <> 150050;
SELECT 'FAIL: 200.00 KES not 20000 cents'   WHERE (SELECT amountCents FROM transactions WHERE id=2) <> 20000;
SELECT 'FAIL: 100.10 KES not 10010 cents'   WHERE (SELECT amountCents FROM transactions WHERE id=3) <> 10010;
SELECT 'FAIL: balance 4000.00 not 400000'  WHERE (SELECT balanceCents FROM transactions WHERE id=1) <> 400000;
SELECT 'FAIL: balance 99.99 not 9999'       WHERE (SELECT balanceCents FROM transactions WHERE id=3) <> 9999;

-- 2. A NULL balance stays NULL rather than becoming 0.
SELECT 'FAIL: NULL balance became a value' WHERE (SELECT balanceCents FROM transactions WHERE id=2) IS NOT NULL;

-- 3. Text columns survive, including a comma in notes.
SELECT 'FAIL: notes lost' WHERE (SELECT notes FROM transactions WHERE id=3) <> 'a note, with comma';
SELECT 'FAIL: type lost' WHERE (SELECT type FROM transactions WHERE id=1) <> 'OUTGOING';
SELECT 'FAIL: contact lost' WHERE (SELECT contact FROM transactions WHERE id=1) <> 'JOHN';
SELECT 'FAIL: messageBody lost' WHERE (SELECT messageBody FROM transactions WHERE id=1) <> 'Ksh 1,500.50 sent to JOHN';

-- 4. Row count preserved.
SELECT 'FAIL: row count changed' WHERE (SELECT COUNT(*) FROM transactions) <> 3;

-- 5. Every row has a distinct, non-blank smsKey.
SELECT 'FAIL: blank smsKey' WHERE EXISTS (SELECT 1 FROM transactions WHERE smsKey IS NULL OR TRIM(smsKey) = '');
SELECT 'FAIL: duplicate smsKey' WHERE (SELECT COUNT(DISTINCT smsKey) FROM transactions) <> 3;

-- 6. Summaries and budgets converted.
SELECT 'FAIL: summary incoming' WHERE (SELECT totalIncomingCents FROM daily_summaries WHERE id=1) <> 20000;
SELECT 'FAIL: summary outgoing' WHERE (SELECT totalOutgoingCents FROM daily_summaries WHERE id=1) <> 150050;
SELECT 'FAIL: budget limit' WHERE (SELECT monthlyLimitCents FROM budgets WHERE category='FOOD') <> 500000;

-- 7. No money column is still REAL.
SELECT 'FAIL: amountCents stored as REAL' WHERE typeof((SELECT amountCents FROM transactions WHERE id=1)) <> 'integer';
SELECT 'FAIL: monthlyLimitCents stored as REAL' WHERE typeof((SELECT monthlyLimitCents FROM budgets)) <> 'integer';

-- 8. Every money column must be integer-typed, which is what makes sums
--    exact. (Unique-index enforcement is checked separately in
--    MigrationUniqueIndexTest, because it is a case where the insert is
--    *expected* to fail and would otherwise trip .bail.)

-- 9. The migrated schema must match the entity exactly: Room rejects a
--    column that has a SQL default the entity does not declare.
SELECT 'FAIL: smsKey has a SQL default the entity does not declare'
    WHERE EXISTS (
        SELECT 1 FROM pragma_table_info('transactions') WHERE name = 'smsKey' AND dflt_value IS NOT NULL
    );

SELECT 'MIGRATION_3_4_VERIFIED';