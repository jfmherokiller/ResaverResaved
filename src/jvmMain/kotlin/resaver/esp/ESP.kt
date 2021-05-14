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
package resaver.esp

import PlatformByteBuffer
import ess.Plugin
import ess.PluginInfo
import mu.KLoggable
import mu.KLogger
import resaver.Entry
import resaver.Game
import resaver.esp.Record.Companion.readRecord
import resaver.esp.Record.Companion.skimRecord
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.channels.ClosedByInterruptException
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * Describes a Skyrim PEX script and will readFully and write it from iostreams.
 *
 * @author Mark Fairchild
 */
class ESP(input: PlatformByteBuffer, game: Game, plugin: Plugin, name: String?, plugins: PluginInfo?) : Entry {
    /**
     * Write the ESP to a `ByteBuffer`.
     *
     * @param output The `ByteBuffer` to write.
     */
    override fun write(output: PlatformByteBuffer?) {
        RECORDS.forEach { record: Record -> record.write(output) }
    }

    /**
     * @return The calculated size of the field.
     * @see Entry.calculateSize
     */
    override fun calculateSize(): Int {
        var sum = 0
        val result = RECORDS.sumOf { it.calculateSize() }
        sum += result
        return sum
    }

    /**
     * Pretty-prints the ESP.
     *
     * @return A string representation of the ESP.
     */
    override fun toString(): String {
        val BUF = StringBuilder()
        RECORDS.forEach { record: Record -> BUF.append(record.toString()) }
        return BUF.toString()
    }

    private val RECORDS: MutableList<Record>

    companion object:KLoggable {
        /**
         * Skims a mod file and extracts EDIDs and ids.
         *
         * Exceptions are not handled. At all. Not even a little bit.
         *
         * @param path The mod file to readFully, which must exist and be readable.
         * @param game The game whose mods are being read.
         * @param plugin The `Plugin` corresponding to the
         * `ESP`.
         * @param plugins The list of plugins, for correcting FormIDs.
         * @return The PluginData.
         *
         * @throws FileNotFoundException
         * @throws IOException
         * @throws ClosedByInterruptException
         * @throws BufferUnderflowException
         */
        @Throws(FileNotFoundException::class, IOException::class, ClosedByInterruptException::class)
        fun skimPlugin(path: Path, game: Game, plugin: Plugin, plugins: PluginInfo?): PluginData {
            assert(Files.isReadable(path))
            assert(Files.isRegularFile(path))
            val NAME = path.fileName.toString()

            // Prepare input stream.
            try {
                FileChannel.open(path, StandardOpenOption.READ).use { input ->
                    val BUFFER = PlatformByteBuffer.allocateDirect(input.size().toInt())
                    BUFFER.readFileChannel(input)
                    BUFFER.makeLe()
                    BUFFER.flip()
                    val TES4 = RecordTes4(BUFFER, plugin, plugins, ESPContext(game, plugin, null))
                    val CTX = ESPContext(game, plugin, TES4)
                    while (BUFFER.hasRemaining()) {
                        skimRecord(BUFFER, CTX)
                    }
                    return CTX.PLUGIN_INFO
                }
            } catch (ex: FileNotFoundException) {
                logger.warn{ex.message}
                throw ex
            } catch (ex: ClosedByInterruptException) {
                throw ex
            } catch (ex: IOException) {
                logger.warn{ex.message}
                throw IOException("Error reading plugin: $NAME", ex)
            }
        }
        override val logger: KLogger
            get() = logger()
    }

    /**
     * Creates an ESP by reading from a `ByteBuffer`.
     *
     * @param input A `ByteBuffer` for a Skyrim PEX file.
     * @param game The game whose mods are being read.
     * @param plugin The `Plugin` corresponding to the
     * `ESP`.
     * @param name The name of the plugin.
     * @param plugins The list of plugins, for correcting FormIDs.
     * @throws IOException Exceptions aren't handled.
     */
    init {
        assert(input.hasRemaining())
        RECORDS = mutableListOf()
        val TES4 = RecordTes4(input, plugin, plugins, ESPContext(game, plugin, null))
        val CTX = ESPContext(game, plugin, TES4)
        CTX.pushContext(plugin.NAME)
        RECORDS.add(TES4)
        while (input.hasRemaining()) {
            val record = readRecord(input, CTX)
            RECORDS.add(record)
        }
    }
}