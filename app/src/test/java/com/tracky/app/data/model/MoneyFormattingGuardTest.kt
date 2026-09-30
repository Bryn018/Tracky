package com.tracky.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Guards against the cents migration reintroducing float formatting of money.
 *
 * The bug this pins: `String.format("%.2f", someMoney)` compiles cleanly,
 * because String.format accepts Any. It then throws
 * IllegalFormatConversionException at runtime, which in a Compose
 * recomposition takes the whole screen down. The Home screen shipped that
 * way and crashed on every launch while all 54 unit tests still passed,
 * because no test rendered the screen.
 *
 * So this is a source-level check rather than a behavioural one: it fails the
 * build if any float format specifier is applied to a variable that is not
 * obviously numeric. Money formatting must go through [Money.toDisplayString].
 */
class MoneyFormattingGuardTest {

    @Test
    fun `no float format specifier is applied to a money value`() {
        val offenders = mutableListOf<String>()

        sourceFiles().forEach { file ->
            val lines = file.readLines()
            lines.forEachIndexed { index, rawLine ->
                val line = rawLine.substringBefore("//")
                if (!FLOAT_FORMAT.containsMatchIn(line)) return@forEachIndexed

                // A literal numeric argument is fine; so is a known float/int.
                val argument = afterFormatCall(line) ?: return@forEachIndexed
                if (!isSuspicious(argument)) return@forEachIndexed

                offenders += "${file.name}:${index + 1}: $line"
            }
        }

        assertTrue(
            "Money is formatted with String.format, which throws at runtime " +
                "and crashes the screen. Use Money.toDisplayString() instead:\n" +
                offenders.joinToString("\n"),
            offenders.isEmpty()
        )
    }

    @Test
    fun `the guard itself would catch a bad call`() {
        // Proves the check is not vacuously passing. Built at runtime so the
        // Kotlin compiler does not try to interpolate the sample expression.
        val sample = "value = \"-Ksh " + '$' + "{String.format(\"%.2f\", todaySpending)}\","
        val caught = listOf(sample)
            .filter { FLOAT_FORMAT.containsMatchIn(it) }
            .filter { isSuspicious(afterFormatCall(it) ?: "") }
        assertEquals("guard failed to flag a known-bad line", 1, caught.size)
    }

    @Test
    fun `legitimate numeric formats are not flagged`() {
        val good = listOf(
            """String.format("%.0f", item.percentageOfTotal * 100)""",
            """String.format("%02d:%02d", reportHour, reportMinute)"""
        )
        val flagged = good.filter { FLOAT_FORMAT.containsMatchIn(it) }
            .filter { isSuspicious(afterFormatCall(it) ?: "") }
        assertTrue("guard flagged legitimate numeric formatting: $flagged", flagged.isEmpty())
    }

    // ── helpers ────────────────────────────────────────────────────────

    private fun sourceFiles(): List<File> {
        val mainDir = File("src/main/java")
        if (!mainDir.isDirectory) {
            fail("expected sources at ${mainDir.absolutePath}; run from the app module")
        }
        return mainDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()
    }

    private fun afterFormatCall(line: String): String? {
        val open = line.indexOfFirst { it == '(' }
        if (open < 0) return null
        // Take the first argument, ignoring the format string literal itself.
        val rest = line.substring(open + 1)
        val parts = splitTopLevelArgs(rest)
        return parts.getOrNull(1)?.trim()?.removeSuffix(")")?.trim()
    }

    private fun splitTopLevelArgs(text: String): List<String> {
        val args = mutableListOf<String>()
        var depth = 0
        var inString = false
        var current = StringBuilder()
        for (ch in text) {
            when {
                ch == '"' -> { inString = !inString; current.append(ch) }
                !inString && ch == '(' -> { depth++; current.append(ch) }
                !inString && ch == ')' -> {
                    depth--
                    if (depth < 0) { args.add(current.toString()); return args }
                    current.append(ch)
                }
                !inString && ch == ',' && depth == 0 -> { args.add(current.toString()); current = StringBuilder() }
                else -> current.append(ch)
            }
        }
        args.add(current.toString())
        return args
    }

    /**
     * True when the argument looks like a non-numeric value. Numeric literals,
     * arithmetic on numbers, and known float/int locals are all allowed.
     */
    private fun isSuspicious(argument: String): Boolean {
        val a = argument.trim().removeSuffix(")").trim()
        if (a.isEmpty()) return false
        if (NUMERIC_LITERAL.matches(a)) return false
        if (KNOWN_NUMERIC_NAMES.any { a == it || a.endsWith(".$it") }) return false
        // Anything ending in a numeric op is arithmetic -> numeric.
        if (a.endsWith(" * 100") || a.endsWith("* 100.0") || a.endsWith("f") ||
            a.endsWith("L") || a.endsWith(".0f")
        ) return false
        if (a.contains("format(")) return false
        return true
    }

    private companion object {
        val FLOAT_FORMAT = Regex("""%\.\d+f""")
        val NUMERIC_LITERAL = Regex("""-?\d+(\.\d+)?[fL]?""")
        val KNOWN_NUMERIC_NAMES = setOf(
            "percentageOfTotal", "percentage", "ratio", "percent", "value",
            "progress", "fraction"
        )
    }
}
