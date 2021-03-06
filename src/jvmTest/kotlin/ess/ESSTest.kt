/*
 * Copyright 2017 Mark.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ess

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import resaver.Game
import resaver.ProgressModel
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.logging.Formatter
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger
import java.util.stream.Collectors
import java.util.stream.Stream


/**
 * Tests the read and write methods of the `ESS` class.
 *
 * @author Mark Fairchild
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ESSTest {
    private val PATHS: List<Path>

    fun pathProvider(): Stream<Path> {
        var paths: List<Path>
        try {
            paths = Files.walk(TESTSAVES_DIR)
                .filter { p: Path -> Game.FILTER_ALL.accept(p.toFile()) }
                .filter { path: Path? -> Files.isReadable(path!!) }
                .filter { path: Path? -> Files.isRegularFile(path!!) }
                .collect(Collectors.toList())
            println(paths)
        } catch (ex: IOException) {
            println("Error while reading test files.")
            System.err.println(ex.message)
            paths = emptyList()
        }
        return paths.stream()
    }

    fun testReadESS() {
        val COUNT = PATHS.size
        for ((index, path) in PATHS.withIndex()) {
            println(String.format("Test save %d / %d : %s", index, COUNT, path))
            testReadESS(path)
        }
    }

    /**
     * Test of readESS and writeESS methods, of class ESS.
     *
     * @param path
     */
    @ParameterizedTest
    @MethodSource("pathProvider")
    fun testReadESS(path: Path) {
        System.out.printf("readESS (%s)\n", WORK_DIR.relativize(path))
        try {
            val MODEL_ORIGINAL = ModelBuilder(ProgressModel(1))
            val IN_RESULT = ESS.readESS(path, MODEL_ORIGINAL)
            val ORIGINAL = IN_RESULT.ESS
            if (ORIGINAL != null) {
                if (ORIGINAL.isTruncated || ORIGINAL.papyrus?.stringTable!!.hasSTB()) {
                    return
                }
            }
            val EXT = "." + (ORIGINAL.header.GAME?.SAVE_EXT ?: "ess")
            val F2 = Files.createTempFile("ess_test", EXT)
            if (ORIGINAL != null) {
                ESS.writeESS(ORIGINAL, F2)
            }
            val MODEL_RESAVE = ModelBuilder(ProgressModel(1))
            val OUT_RESULT = ESS.readESS(F2, MODEL_RESAVE)
            val REWRITE = OUT_RESULT.ESS
            if (ORIGINAL != null) {
                if (REWRITE != null) {
                    Assertions.assertEquals(ORIGINAL.digest, REWRITE.digest, "Verify that digests match for $path")
                }
            }
            if (ORIGINAL != null) {
                if (REWRITE != null) {
                    ESS.verifyIdentical(ORIGINAL, REWRITE)
                }
            }
        } catch (ex: RuntimeException) {
            System.err.println(
                """
                    Problem with ${path.fileName}
                    ${ex.message}
                    """.trimIndent()
            )
            ex.printStackTrace(System.err)
            Assertions.fail<Any>(path.fileName.toString())
        } catch (ex: AssertionError) {
            System.err.println(
                """
                    Problem with ${path.fileName}
                    ${ex.message}
                    """.trimIndent()
            )
            ex.printStackTrace(System.err)
            Assertions.fail<Any>(path.fileName.toString())
        } catch (ex: IOException) {
            System.err.println(
                """
                    Problem with ${path.fileName}
                    ${ex.message}
                    """.trimIndent()
            )
            ex.printStackTrace(System.err)
            Assertions.fail<Any>(path.fileName.toString())
        }
    }

    companion object {
        val WORK_DIR: Path = Paths.get(System.getProperty("user.dir"))
        val TESTSAVES_DIR: Path = WORK_DIR.resolve("src/test/resources/TestSaves")
        private val LOG = Logger.getLogger(ESSTest::class.java.canonicalName)
    }

    init {
        // Set up logging stuff.
        LOG.parent.handlers[0].formatter = object : Formatter() {
            override fun format(record: LogRecord): String {
                val LEVEL = record.level
                val MSG = record.message
                val SRC = record.sourceClassName + "." + record.sourceMethodName
                return "$SRC: $LEVEL: $MSG\n"
            }
        }
        LOG.parent.handlers[0].level = Level.INFO
        val paths: List<Path> = try {
            Files.walk(TESTSAVES_DIR)
                .filter { p: Path -> Game.FILTER_ALL.accept(p.toFile()) }
                .filter { path: Path? -> Files.isReadable(path!!) }
                .filter { path: Path? -> Files.isRegularFile(path!!) }
                .collect(Collectors.toList())
        } catch (ex: IOException) {
            println("Error while reading test files.")
            System.err.println(ex.message)
            emptyList()
        }
        PATHS = paths
    }
}