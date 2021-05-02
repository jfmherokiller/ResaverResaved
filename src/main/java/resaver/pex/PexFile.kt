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

import mf.BufferUtil
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
import java.util.*
import java.util.function.Consumer
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
            flag.write(output)
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
        sum += 2 + USERFLAGDEFS!!.stream().mapToInt { obj: UserFlag -> obj.calculateSize() }.sum()
        sum += 2 + SCRIPTS!!.stream().mapToInt { obj: Pex -> obj.calculateSize() }.sum()
        return sum
    }

    /**
     * Rebuilds the string table. This is necessary if ANY strings in ANY of the
     * PexFile's members has changed at all. Otherwise, writing the PexFile will
     * produce an invalid file.
     *
     */
    fun rebuildStringTable() {
        val INUSE: MutableSet<StringTable.TString> = LinkedHashSet()
        DEBUG!!.collectStrings(INUSE)
        USERFLAGDEFS!!.forEach(Consumer { flag: UserFlag -> flag.collectStrings(INUSE) })
        SCRIPTS!!.forEach(Consumer { obj: Pex -> obj.collectStrings(INUSE) })
        STRINGS!!.rebuildStringTable(INUSE)
    }

    /**
     * Tries to disassemble the script.
     *
     * @param level Partial disassembly flag.
     * @param code The code strings.
     */
    fun disassemble(code: MutableList<String?>, level: AssemblyLevel?) {
        SCRIPTS!!.forEach(Consumer { v: Pex -> v.disassemble(code, level) })
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
        SCRIPTS?.forEach(Consumer { obj: Pex? -> buf.append("\n\nOBJECT\n").append(obj).append('\n') })
        return buf.toString()
    }

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
            return IString.get(COMPILED)
        }
    var GAME: Game? = null
    var HEADER: Header? = null
    var STRINGS: StringTable? = null
    var DEBUG: DebugInfo? = null
    var USERFLAGDEFS: MutableList<UserFlag>? = null
    var SCRIPTS: MutableList<Pex>? = null

    /**
     * Describes the header of a PexFile file. Useless beyond that.
     *
     */
    inner class Header(input: ByteBuffer) {
        /**
         * Write the object to a `ByteBuffer`.
         *
         * @param output The `ByteBuffer` to write.
         * @throws IOException IO errors aren't handled at all, they are simply
         * passed on.
         */
        @Throws(IOException::class)
        fun write(output: ByteBuffer) {
            output.putInt(magic)
            output.putInt(version)
            output.putLong(compilationTime)
            BufferUtil.putWString(output, soureFilename)
            BufferUtil.putWString(output, userName)
            BufferUtil.putWString(output, machineName)
        }

        /**
         * @return The size of the `Header`, in bytes.
         */
        fun calculateSize(): Int {
            return 22 + soureFilename.length + userName.length + machineName.length
        }

        /**
         * Pretty-prints the Header.
         *
         * @return A string representation of the Header.
         */
        override fun toString(): String {
            return """${soureFilename} compiled at ${compilationTime} by ${userName} on ${machineName}.
"""
        }

        private var magic = 0
        private var version = 0

        /**
         * @return The compilation date of the `PexFile`.
         */
        var compilationTime: Long
        var soureFilename = ""
        private var userName = ""
        private var machineName = ""

        /**
         * Creates a Header by reading from a DataInput.
         *
         * @param input A ByteBuffer for a Skyrim PEX file.
         * @throws IOException Exceptions aren't handled.
         */
        init {
            magic = input.int
            version = input.int
            compilationTime = input.long
            soureFilename = BufferUtil.getUTF(input)
            userName = BufferUtil.getUTF(input)
            machineName = BufferUtil.getUTF(input)
        }
    }

    /**
     * Describe the debugging info section of a PEX file.
     *
     */
    inner class DebugInfo(input: ByteBuffer, strings: StringTable) {
        /**
         * Write the object to a `ByteBuffer`.
         *
         * @param output The `ByteBuffer` to write.
         * @throws IOException IO errors aren't handled at all, they are simply
         * passed on.
         */
        @Throws(IOException::class)
        fun write(output: ByteBuffer) {
            output.put(hasDebugInfo)
            if (hasDebugInfo.toInt() != 0) {
                output.putLong(modificationTime)
                output.putShort(DEBUGFUNCTIONS.size.toShort())
                for (function in DEBUGFUNCTIONS) {
                    function.write(output)
                }
                if (GAME!!.isFO4) {
                    output.putShort(PROPERTYGROUPS.size.toShort())
                    for (function in PROPERTYGROUPS) {
                        function.write(output)
                    }
                    output.putShort(STRUCTORDERS.size.toShort())
                    for (function in STRUCTORDERS) {
                        function.write(output)
                    }
                }
            }
        }

        /**
         * Removes all debug info.
         */
        fun clear() {
            hasDebugInfo = 0
            DEBUGFUNCTIONS.clear()
            PROPERTYGROUPS.clear()
            STRUCTORDERS.clear()
        }

        /**
         * Collects all of the strings used by the DebugInfo and adds them to a
         * set.
         *
         * @param strings The set of strings.
         */
        fun collectStrings(strings: MutableSet<StringTable.TString>) {
            DEBUGFUNCTIONS.forEach(Consumer { f: DebugFunction -> f.collectStrings(strings) })
            PROPERTYGROUPS.forEach(Consumer { f: PropertyGroup -> f.collectStrings(strings) })
            STRUCTORDERS.forEach(Consumer { f: StructOrder -> f.collectStrings(strings) })
        }

        /**
         * @return The size of the `DebugInfo`, in bytes.
         */
        fun calculateSize(): Int {
            var sum = 1
            if (hasDebugInfo.toInt() != 0) {
                sum += 8
                sum += 2 + DEBUGFUNCTIONS.stream().mapToInt { obj: DebugFunction -> obj.calculateSize() }.sum()
                if (GAME!!.isFO4) {
                    sum += 2 + PROPERTYGROUPS.stream().mapToInt { obj: PropertyGroup -> obj.calculateSize() }.sum()
                    sum += 2 + STRUCTORDERS.stream().mapToInt { obj: StructOrder -> obj.calculateSize() }.sum()
                }
            }
            return sum
        }

        /**
         * Pretty-prints the DebugInfo.
         *
         * @return A string representation of the DebugInfo.
         */
        override fun toString(): String {
            val buf = StringBuilder()
            buf.append("DEBUGINFO\n")
            DEBUGFUNCTIONS.forEach(Consumer { function: DebugFunction? ->
                buf.append('\t').append(function).append('\n')
            })
            buf.append('\n')
            return buf.toString()
        }

        private var hasDebugInfo: Byte
        private var modificationTime: Long = 0
        private val DEBUGFUNCTIONS: ArrayList<DebugFunction>
        private val PROPERTYGROUPS: ArrayList<PropertyGroup>
        private val STRUCTORDERS: ArrayList<StructOrder>

        /**
         * Creates a DebugInfo by reading from a DataInput.
         *
         * @param input A datainput for a Skyrim PEX file.
         * @param strings The `StringTable` for the
         * `PexFile`.
         * @throws IOException Exceptions aren't handled.
         */
        init {
            hasDebugInfo = input.get()
            DEBUGFUNCTIONS = ArrayList(0)
            PROPERTYGROUPS = ArrayList(0)
            STRUCTORDERS = ArrayList(0)
            if (hasDebugInfo.toInt() == 0) {
            } else {
                modificationTime = input.long
                val functionCount = java.lang.Short.toUnsignedInt(input.short)
                DEBUGFUNCTIONS.ensureCapacity(functionCount)
                for (i in 0 until functionCount) {
                    DEBUGFUNCTIONS.add(DebugFunction(input, strings))
                }
                if (GAME!!.isFO4) {
                    val propertyCount = java.lang.Short.toUnsignedInt(input.short)
                    PROPERTYGROUPS.ensureCapacity(propertyCount)
                    for (i in 0 until propertyCount) {
                        PROPERTYGROUPS.add(PropertyGroup(input, strings))
                    }
                    val orderCount = java.lang.Short.toUnsignedInt(input.short)
                    STRUCTORDERS.ensureCapacity(orderCount)
                    for (i in 0 until orderCount) {
                        STRUCTORDERS.add(StructOrder(input, strings))
                    }
                }
            }
        }
    }

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
            val MAGIC = data.getInt(0)
            return when (MAGIC) {
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
                    Files.size(scriptFile).toInt()
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
        fun writeScript(script: PexFile, scriptFile: Path) {
            assert(!Files.exists(scriptFile) || Files.isRegularFile(scriptFile))
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
        Objects.requireNonNull(input)
        Objects.requireNonNull(game)
        try {
            GAME = game
            HEADER = Header(input)
            STRINGS = StringTable(input)
            DEBUG = DebugInfo(input, STRINGS!!)
            var flagCount = java.lang.Short.toUnsignedInt(input.short)
            USERFLAGDEFS = mutableListOf()
            while (0 < flagCount) {
                USERFLAGDEFS!!.add(UserFlag(input, STRINGS!!))
                flagCount--
            }
            var scriptCount = java.lang.Short.toUnsignedInt(input.short)
            check(scriptCount >= 1) { "Pex files must contain at least one script." }
            SCRIPTS = mutableListOf()
            while (0 < scriptCount) {
                val pex = Pex(input, game, USERFLAGDEFS!!, STRINGS!!)
                SCRIPTS!!.add(pex)
                scriptCount--
            }
        } catch (ex: IOException) {
            ex.printStackTrace(System.err)
            throw ex
        }
    }
}