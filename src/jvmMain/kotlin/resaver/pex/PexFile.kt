/*
 * Copyright 2016 Mark Fairchild.
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
package resaver.pex

import resaver.Game
import resaver.IString
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

import java.util.regex.Pattern

/**
 * Describes a Skyrim PEX script and will read and write it from streams.
 *
 * @author Mark Fairchild
 */
class PexFile private constructor(input: ByteBuffer, game: Game) {
    /**
     * Write the object to a `ByteBuffer`.
     *
     * @param output The `ByteBuffer` to write.
     * @throws IOException IO errors aren't handled at all, they are simply
     * passed on.
     */
    @Throws(IOException::class)
    fun write(output: ByteBuffer) {
        HEADER!!.write(output)
        STRINGS!!.write(output)
        DEBUG!!.write(output)
        output.putShort(USERFLAGDEFS!!.size.toShort())
        for (flag in USERFLAGDEFS!!) {
            flag!!.write(output)
        }
        output.putShort(SCRIPTS!!.size.toShort())
        for (pex in SCRIPTS!!) {
            pex.write(output)
        }

        /*
        output.putShort((short) this.OBJECTS.size());
        for (Pex obj : this.OBJECTS) {
            obj.write(output);
        }*/
    }

    /**
     * @return The size of the `PexFile`, in bytes.
     */
    fun calculateSize(): Int {
        var sum = 0
        sum += HEADER!!.calculateSize()
        sum += STRINGS!!.calculateSize()
        sum += DEBUG!!.calculateSize()
        var sum1 = 0
        for (USERFLAGDEF in USERFLAGDEFS!!) {
            val size = USERFLAGDEF!!.calculateSize()
            sum1 += size
        }
        sum += 2 + sum1
        var result = 0
        for (SCRIPT in SCRIPTS!!) {
            val calculateSize = SCRIPT.calculateSize()
            result += calculateSize
        }
        sum += 2 + result
        return sum
    }

    /**
     * Rebuilds the string table. This is necessary if ANY strings in ANY of the
     * PexFile's members has changed at all. Otherwise, writing the PexFile will
     * produce an invalid file.
     *
     */
    fun rebuildStringTable() {
        val INUSE: MutableSet<TString?> = mutableSetOf()
        DEBUG!!.collectStrings(INUSE)
        for (flag in USERFLAGDEFS!!) {
            flag!!.collectStrings(INUSE)
        }
        for (obj in SCRIPTS!!) {
            obj.collectStrings(INUSE)
        }
        STRINGS!!.rebuildStringTable(INUSE)
    }

    /**
     * Tries to disassemble the script.
     *
     * @param level Partial disassembly flag.
     * @param code The code strings.
     */
    fun disassemble(code: MutableList<String?>?, level: AssemblyLevel?) {
        for (v in SCRIPTS!!) {
            if (code != null) {
                v.disassemble(code, level)
            }
        }
    }

    /**
     * Pretty-prints the PexFile.
     *
     * @return A string representation of the PexFile.
     */
    override fun toString(): String {
        val buf = StringBuilder()
        buf.append(HEADER)
        buf.append(DEBUG)
        buf.append("USER FLAGS\n")
        buf.append(USERFLAGDEFS)
        if (SCRIPTS != null) {
            for (obj in SCRIPTS!!) {
                buf.append("\n\nOBJECT\n").append(obj).append('\n')
            }
        }
        return buf.toString()
    }

    /**
     * @return The compilation date of the `PexFile`.
     */
    val date: Long
        get() = HEADER!!.compilationTime

    /**
     * @return The filename of the `PexFile`, determined from the
     * header.
     */
    val filename: IString
        get() {
            val SOURCE = HEADER!!.soureFilename
            val REGEX = "(psc)$"
            val REPLACEMENT = "pex"
            val PATTERN = Pattern.compile(REGEX, Pattern.CASE_INSENSITIVE)
            val MATCHER = PATTERN.matcher(SOURCE)
            val COMPILED = MATCHER.replaceAll(REPLACEMENT)
            return IString[COMPILED]
        }
    var GAME: Game? = null
    var HEADER: Header? = null
    var STRINGS: StringTable? = null
    var DEBUG: DebugInfo? = null
    var USERFLAGDEFS: MutableList<UserFlag?>? = null
    var SCRIPTS: MutableList<Pex>? = null

    companion object {
        /**
         * Reads a script file and creates a PexFile object to represent it.
         *
         * Exceptions are not handled. At all. Not even a little bit.
         *
         * @param data An array of bytes containing the script data.
         * @return The PexFile object.
         *
         * @throws IOException
         */
        @Throws(IOException::class)
        fun readScript(data: ByteBuffer): PexFile {
            return when (data.getInt(0)) {
                -0x213fa806 -> PexFile(data, Game.FALLOUT4)
                -0x5a83f22 -> PexFile(data, Game.SKYRIM_LE)
                else -> throw IOException("Invalid magic number.")
            }
        }

        /**
         * Reads a script file and creates a PexFile object to represent it.
         *
         * Exceptions are not handled. At all. Not even a little bit.
         *
         * @param scriptFile The script file to read, which must exist and be
         * readable.
         * @return The PexFile object.
         *
         * @throws FileNotFoundException
         * @throws IOException
         */

        @Throws(FileNotFoundException::class, IOException::class)
        fun readScript(scriptFile: Path?): PexFile {
            FileChannel.open(scriptFile, StandardOpenOption.READ).use { CHANNEL ->
                val input = ByteBuffer.allocate(
                    Files.size(scriptFile!!).toInt()
                )
                CHANNEL.read(input)
                (input as Buffer).flip()
                return readScript(input)
            }
        }

        /**
         * Writes a PexFile object to a script file.
         *
         * Exceptions are not handled. At all. Not even a little bit.
         *
         * @param script The PexFile object to write.
         * @param scriptFile The script file to write. If it exists, it must be a
         * file and it must be writable.
         *
         * @throws FileNotFoundException
         * @throws IOException
         */
        @Throws(FileNotFoundException::class, IOException::class)
        fun writeScript(script: PexFile, scriptFile: Path?) {
            assert(!Files.exists(scriptFile!!) || Files.isRegularFile(scriptFile))
            assert(!Files.exists(scriptFile) || Files.isWritable(scriptFile))
            FileChannel.open(
                scriptFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
            ).use { CHANNEL ->
                val output = ByteBuffer.allocate(2 * script.calculateSize())
                script.write(output)
                output.flip()
                CHANNEL.write(output)
            }
        }
    }

    /**
     * Creates a Pex by reading from a DataInput.
     *
     * @param input A datainput for a Skyrim PEX file.
     * @param game The game for which the script was compiled.
     * @throws IOException Exceptions aren't handled.
     */
    init {
        try {
            GAME = game
            HEADER = Header(input)
            STRINGS = StringTable(input)
            DEBUG = DebugInfo(this, input, STRINGS)
            var flagCount = UtilityFunctions.toUnsignedInt(input.short)
            USERFLAGDEFS = mutableListOf()
            while (0 < flagCount) {
                USERFLAGDEFS!!.add(UserFlag(input, STRINGS!!))
                flagCount--
            }
            var scriptCount = UtilityFunctions.toUnsignedInt(input.short)
            check(scriptCount >= 1) { "Pex files must contain at least one script." }
            SCRIPTS = mutableListOf()
            while (0 < scriptCount) {
                val pex = Pex(input, game, USERFLAGDEFS, STRINGS!!)
                SCRIPTS!!.add(pex)
                scriptCount--
            }
        } catch (ex: IOException) {
            ex.printStackTrace(System.err)
            throw ex
        }
    }
}