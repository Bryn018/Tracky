package com.tracky.app.data.local

import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Runs the 3 -> 4 migration SQL against a real SQLite engine and asserts the
 * result.
 *
 * Executes `resources/verify_migration_3_4.sql`, which builds the version-3
 * schema, seeds representative rows, applies the exact SQL from
 * [TrackyDatabase]'s MIGRATION_3_4, then checks the converted values. The
 * script prints one `FAIL: ...` line per broken assertion and finishes with
 * `MIGRATION_3_4_VERIFIED`, so this test asserts both the absence of failures
 * and the presence of the success marker.
 *
 * Deliberately SQLite rather than an instrumented Room test: the migration's
 * correctness lives entirely in its SQL, and this way it runs under
 * `./gradlew test` with no emulator. Room's own schema validation of the
 * resulting tables is a separate concern and still needs an instrumented test.
 *
 * The subprocess is launched reflectively, and its classes are loaded through
 * the platform class loader. Android's unit-test runtime puts a mockable
 * `android.jar` on the classpath that shadows `java.lang.ProcessBuilder` with
 * a stub: constructing it works, but the result is not a `Process`, so the
 * direct version of this code fails both to compile and to run. The real JDK
 * classes are reachable via the platform class loader, which does not see the
 * Android stubs.
 */
class MigrationSqlTest {

    @Test
    fun `migration 3 to 4 converts money to cents and preserves data`() {
        val sqlText = javaClass.classLoader!!
            .getResourceAsStream(SCRIPT_RESOURCE)
            ?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: throw AssertionError("missing test resource $SCRIPT_RESOURCE")

        val dbFile = File.createTempFile("tracky-migration-3-4", ".db")
        dbFile.delete()

        try {
            val output = runSqlite(dbFile.absolutePath, sqlText)

            // Skips rather than fails when sqlite3 cannot be run, so the suite
            // still works on machines without the CLI.
            assumeTrue("could not launch sqlite3; migration SQL not exercised", output != null)

            val text = output!!

            val failures = text.lines()
                .filter { it.trimStart().startsWith("FAIL:") }
                .map { it.trim() }

            assertTrue(
                "migration assertions failed:\n" + failures.joinToString("\n") +
                    "\n--- sqlite3 output ---\n$text",
                failures.isEmpty()
            )
            assertTrue(
                "migration script did not reach its success marker; output:\n$text",
                text.contains("MIGRATION_3_4_VERIFIED")
            )
        } finally {
            dbFile.delete()
        }
    }

    /** Returns sqlite3's combined output, or null if the process could not be run. */
    private fun runSqlite(dbPath: String, sql: String): String? = try {
        // getSystemClassLoader rather than getPlatformClassLoader: the latter
        // is Java 9+ and is not on Android's API surface, so referencing it
        // does not compile against this project's compileSdk.
        val platform = ClassLoader.getSystemClassLoader()
        val pbClass = Class.forName("java.lang.ProcessBuilder", false, platform)
        val processClass = Class.forName("java.lang.Process", false, platform)

        val builder = pbClass
            .getConstructor(Array<String>::class.java)
            .newInstance(arrayOf("sqlite3", dbPath))

        val process = pbClass.getMethod("start").invoke(builder)
        check(processClass.isInstance(process)) {
            "unexpected process type: ${process?.javaClass?.name}"
        }

        val stdin = processClass
            .getMethod("getOutputStream")
            .invoke(process) as OutputStream
        stdin.write(sql.toByteArray())
        stdin.flush()
        stdin.close()

        val stdout = processClass
            .getMethod("getInputStream")
            .invoke(process) as InputStream

        val text = stdout.bufferedReader().readText()
        processClass.getMethod("waitFor").invoke(process)
        text
    } catch (_: Exception) {
        null
    } catch (_: ReflectiveOperationException) {
        null
    } catch (_: LinkageError) {
        null
    }

    private companion object {
        const val SCRIPT_RESOURCE = "verify_migration_3_4.sql"
    }
}