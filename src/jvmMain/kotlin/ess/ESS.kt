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
package ess

import ess.ChangeForm.Companion.verifyIdentical
import ess.Header.Companion.verifyIdentical
import ess.papyrus.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import mf.BufferUtil
import mf.Counter
import mf.Timer
import mf.Timer.Companion.startNew
import mu.KLoggable
import mu.KLogger
import okio.ExperimentalFileSystem
import okio.FileSystem
import okio.Path.Companion.toPath
import resaver.Analysis
import resaver.Game
import resaver.ListException
import resaver.gui.FilterTreeModel
import java.io.IOException
import java.nio.Buffer
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.function.Predicate
import java.util.regex.Pattern
import java.util.zip.CRC32
import java.util.zip.DataFormatException
import kotlin.collections.ArrayDeque
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.collections.LinkedHashMap
/**
 * Describes a Skyrim or Fallout4 savegame.
 *
 * @author Mark Fairchild
 */
class ESS private constructor(buffer: ByteBuffer, saveFile: Path, model: ModelBuilder) : Element {
    /**
     * Writes the `ESS` to a `ByteBuffer`.
     *
     * @param channel The output channel for the savegame.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun write(channel: FileChannel) {
        val COMPRESSION = header.getCompression()

        // Write the header, with a litte of extra room for compression prefixes.
        val headerBlock = ByteBuffer.allocate(header.calculateSize() + 8).order(ByteOrder.LITTLE_ENDIAN)
        header.write(headerBlock)
        (headerBlock as Buffer).flip()
        channel.write(headerBlock)
        headerBlock.compact()

        // Write the body to a ByteBuffer.
        val UNCOMPRESSED_LEN = calculateSize()
        val UNCOMPRESSED = ByteBuffer.allocate(UNCOMPRESSED_LEN).order(ByteOrder.LITTLE_ENDIAN)
        this.write(UNCOMPRESSED)
        (UNCOMPRESSED as Buffer).flip()

        // Do the decompression, if necessary.
        if (COMPRESSION.isCompressed) {
            val COMPRESSED: ByteBuffer = when (COMPRESSION) {
                CompressionType.ZLIB -> BufferUtil.deflateZLIB(UNCOMPRESSED, UNCOMPRESSED_LEN)
                CompressionType.LZ4 -> BufferUtil.deflateLZ4(UNCOMPRESSED, UNCOMPRESSED_LEN)
                else -> throw IOException("Unknown compression type: $COMPRESSION")
            }
            headerBlock.putInt(UNCOMPRESSED.limit())
            headerBlock.putInt(COMPRESSED.limit())
            (headerBlock as Buffer).flip()
            channel.write(headerBlock)
            channel.write(COMPRESSED)
        } else {
            channel.write(UNCOMPRESSED)
        }
    }

    /**
     * Writes the body of the `ESS` to a `ByteBuffer`. The
     * header and compression prefixes are not written.
     *
     * @param output The output stream for the savegame.
     */
    override fun write(output: ByteBuffer?) {
        // Write the form version.
        output?.put(formVersion)

        // Write the version string.
        if (null != versionString) {
            if (output != null) {
                BufferUtil.putWString(output, versionString!!)
            }
        }

        // Write the PLUGIN info section.
        pluginInfo.write(output)
        logger.info {"Writing savegame: wrote plugin table."}

        // Rebuild and then write the file location table.
        fLT!!.rebuild(this)
        fLT.write(output)
        logger.info {"Writing savegame: rebuilt and wrote file location table."}
        TABLE1.forEach { data: GlobalData ->
            try {
                data.write(output)
                logger.info {"Writing savegame: \tGlobalData type ${data.type}."}
            } catch (ex: RuntimeException) {
                throw ElementException("GlobalDataTable1", ex, data)
            }
        }
        logger.info {"Writing savegame: wrote GlobalDataTable #1."}
        TABLE2.forEach { data: GlobalData ->
            try {
                data.write(output)
                logger.info {"Writing savegame: \tGlobalData type ${data.type}."}
            } catch (ex: RuntimeException) {
                throw ElementException("GlobalDataTable2", ex, data)
            }
        }
        logger.info {"Writing savegame: wrote GlobalDataTable #2."}
        CHANGEFORMS.values.forEach { form: ChangeForm? ->
            try {
                form!!.write(output)
            } catch (ex: RuntimeException) {
                throw ElementException("Error writing ChangeForm", ex, form!!)
            }
        }
        logger.info {"Writing savegame: wrote changeform table."}
        TABLE3.forEach { data: GlobalData ->
            try {
                data.write(output)
                logger.info {"Writing savegame: \tGlobalData type ${data.type}."}
            } catch (ex: RuntimeException) {
                throw ElementException("GlobalDataTable3", ex, data)
            }
        }
        logger.info {"Writing savegame: wrote GlobalDataTable #3."}
        output?.putInt(FORMIDARRAY!!.size)
        if (FORMIDARRAY != null) {
            for (formID in FORMIDARRAY) {
                output?.putInt(formID)
            }
        }
        logger.info {"Writing savegame: wrote formid array."}
        output?.putInt(visitedWorldspaceArray!!.size)
        if (visitedWorldspaceArray != null) {
            for (formID in visitedWorldspaceArray) {
                output?.putInt(formID)
            }
        }
        logger.info {"Writing savegame: wrote visited worldspace array."}
        output?.put(unknown3)
        logger.info {"Writing savegame: wrote unknown block."}
    }

    /**
     * @see Element.calculateSize
     * @return
     */
    override fun calculateSize(): Int {
        return header.calculateSize() + calculateBodySize()
    }

    /**
     * @see Element.calculateSize
     * @return
     */
    fun calculateBodySize(): Int {
        var sum = 1 // form version
        if (null != versionString) {
            sum += versionString!!.length + 2
        }
        sum += pluginInfo.calculateSize()
        sum += fLT!!.calculateSize()
        val sum1 = TABLE1.sumOf { it.calculateSize() }
        sum += sum1
        val result1 = TABLE2.sumOf { it.calculateSize() }
        sum += result1
        val result = CHANGEFORMS.values.sumOf { it!!.calculateSize() }
        sum += result
        val sum2 = TABLE3.sumOf { it.calculateSize() }
        sum += sum2
        sum += 4
        sum += if (FORMIDARRAY == null) 0 else 4 * FORMIDARRAY.size
        sum += 4
        sum += if (visitedWorldspaceArray == null) 0 else 4 * visitedWorldspaceArray.size
        sum += unknown3.size
        return sum
    }

    /**
     * @param analysis The analysis data.
     */
    fun addNames(analysis: Analysis) {
        for (v in REFIDS.values) {
            v.addNames(analysis)
        }
    }

    /**
     * @return The list of change forms.
     */
    val changeForms: LinkedHashMap<RefID?, ChangeForm?>
        get() = CHANGEFORMS

    /**
     * @return The array of form IDs.
     */
    val formIDs: IntArray
        get() = FORMIDARRAY ?: IntArray(0)

    /**
     * @return The `GlobalVariableTable`.
     */
    val globals: GlobalVariableTable
        get() = GLOBALS

    /**
     * @return A flag indicating whether there is stored cosave data.
     */
    fun hasCosave(): Boolean {
        return COSAVE != null
    }

    /**
     * NOT IMPLEMENTED.
     *
     * Removes all `ChangeForm` objects with havok entries.
     *
     * @return The number of forms removed.
     */
    fun resetHavok(): Int {
        for (form in CHANGEFORMS.values) {
            //form.
        }
        return 0
    }

    /**
     * @return A flag indicating if the savefile has a truncation error.
     */
    val isTruncated: Boolean
        get() = truncated || papyrus!!.isTruncated


    /**
     * NOT IMPLEMENTED.
     *
     * Removes null entries from form lists.
     *
     * @return An array containing two ints; the first is the number of entries
     * that were removed, and the second is the number of forms that had entries
     * remvoed.
     */
    fun cleanseFormLists(): IntArray {
        val entries = 0
        val forms = 0
        /*
        for (ChangeForm form : this.CHANGEFORMS) {
            form.g
            ChangeFormData data = form.getData();
            if (!(data instanceof ChangeFormFLST)) {
                continue;
            }

            ChangeFormFLST flst = (ChangeFormFLST) data;
            int removed = flst.cleanse();

            if (removed > 0) {
                entries += removed;
                forms++;
            }
        }
         */return intArrayOf(entries, forms)
    }

    /**
     * Removes all script instances that are associated with non-existent
     * created forms.
     *
     * @return The elements that were removed.
     */
    fun removeNonexistentCreated(): Set<PapyrusElement> {
        val NONEXISTENT: MutableSet<PapyrusElement> = mutableSetOf()
        for (v in papyrus!!.scriptInstances
            .values) {
            if (v.refID.type === RefID.Type.CREATED) {
                if (!changeForms.containsKey(v.refID)) {
                    NONEXISTENT.add(v)
                }
            }
        }
        return papyrus.removeElements(NONEXISTENT)
    }

    /**
     * Removes a `Element` collection.
     *
     * @param elements The elements to remove.
     * @return The elements that were removed.
     */
    fun removeElements(elements: Collection<Element?>): MutableSet<Element?> {
        val ELEM1: MutableSet<ChangeForm> = HashSet()
        for (element in elements) {
            if (element is ChangeForm) {
                ELEM1.add(element)
            }
        }
        val ELEM2: MutableSet<PapyrusElement> = HashSet()
        for (v in elements) {
            if (v is PapyrusElement) {
                ELEM2.add(v)
            }
        }
        val REMOVED: MutableSet<Element?> = HashSet()
        REMOVED.addAll(removeChangeForms(ELEM1))
        REMOVED.addAll(papyrus!!.removeElements(ELEM2))
        return REMOVED
    }

    /**
     * Removes a `Set` of `ChangeForm`.
     *
     * @param forms The elements to remove.
     * @return The number of elements removed.
     */
    fun removeChangeForms(forms: Collection<ChangeForm>?): Set<ChangeForm?> {
        if (null == forms || forms.contains<ChangeForm?>(null)) {
            throw NullPointerException("The set of forms to be removed must not be null and must not contain null.")
        }
        val FORMS = ArrayDeque(forms)
        val REMOVED: MutableSet<ChangeForm?> = mutableSetOf()
        while (FORMS.isNotEmpty()) {
            val FORM = FORMS.removeLast()
            REMOVED.add(CHANGEFORMS.remove(FORM.refID))
        }
        REMOVED.remove(null)
        return REMOVED
    }

    /**
     * @return Flag indicating whether the game has a 32bit string model.
     */
    val isFO4: Boolean
        get() = header.GAME?.isFO4 == true

    /**
     * @return Flag indicating whether the game has a 32bit string model.
     */
    val isSkyrim: Boolean
        get() = header.GAME?.isSkyrim == true

    /**
     * @return Flag indicating whether the game has a 32bit string model.
     */
    val isStr32: Boolean
        get() = when (header.GAME) {
            Game.FALLOUT4, Game.FALLOUT_VR -> formVersion > 61
            Game.SKYRIM_LE -> false
            else -> true
        }

    /**
     * @return Flag indicating whether the game is CC enabled.
     */
    fun supportsESL(): Boolean {
        return when (header.GAME) {
            Game.FALLOUT4, Game.FALLOUT_VR -> formVersion >= 68
            Game.SKYRIM_SW, Game.SKYRIM_SE, Game.SKYRIM_VR -> formVersion >= 78
            Game.SKYRIM_LE -> false
            else -> false
        }
    }

    /**
     * @return Flag indicating whether the game supports savefile compression.
     */
    fun supportsCompression(): Boolean {
        return when (header.GAME) {
            Game.SKYRIM_SW, Game.SKYRIM_SE, Game.SKYRIM_VR -> true
            Game.FALLOUT4, Game.FALLOUT_VR, Game.SKYRIM_LE -> false
            else -> false
        }
    }

    /**
     * @see AnalyzableElement.getInfo
     * @param analysis
     * @return
     */
    fun getInfo(analysis: Analysis?): String {
        val BUILDER = StringBuilder()
        BUILDER.append("<h3>${originalFile.fileName}</h3>")
        val race = header.RACEID.toString().replace("Race", "")
        val name = header.NAME.toString()
        val level = header.LEVEL
        val gender = if (header.SEX.toInt() == 0) "male" else "female"
        val location = header.LOCATION.toString()
        val gameDate = header.GAMEDATE.toString()
        val xp = header.CURRENT_XP
        val nexp = header.NEEDED_XP + header.CURRENT_XP
        val time = header.FILETIME
        val millis = time / 10000L - 11644473600000L
        val DATE = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
        BUILDER.append(String.format("<h3>$name the level $level $race $gender, in $location on $gameDate (%1.0f/%1.0f xp).</h3>", xp, nexp))
        BUILDER.append("<fixed><ul><li>Save number ${header.SAVENUMBER}, created on ${DATE.dayOfMonth}/${DATE.month}/${DATE.year} ${DATE.hour}:${DATE.minute}.</li>")
        BUILDER.append("<li>Version string: $versionString</li>")
        BUILDER.append(String.format("<li>Form version: %d</li>", formVersion))
        val actualSize = calculateSize() / 1048576.0f
        val papyrusSize = papyrus!!.calculateSize() / 1048576.0f
        var sum = 0
        for (changeForm in CHANGEFORMS.values) {
            val calculateSize = changeForm!!.calculateSize()
            sum += calculateSize
        }
        val changeFormsSize = sum / 1048576.0f
        if (header.getCompression().isCompressed) {
            try {
                val fileSize = Files.size(originalFile) / 1048573.0f
                BUILDER.append(
                    String.format(
                        "<li>Total size: %1.1f mb (%1.1f mb with ${header.getCompression()})</li>",
                        actualSize,
                        fileSize)
                )
            } catch (ex: IOException) {
                logger.warn(ex) {"Error retrieving savefile size on disk."}
                BUILDER.append(String.format("<li>Total size: %1.1f mb</li>", actualSize))
            }
        } else {
            BUILDER.append(String.format("<li>Total size: %1.1f mb</li>", actualSize))
        }
        BUILDER.append(String.format("<li>Papyrus size: %1.1f mb</li>", papyrusSize))
        BUILDER.append(String.format("<li>ChangeForms size: %1.1f mb</li>", changeFormsSize))
        if (analysis != null) {
            BUILDER.append(
                String.format(
                    "<li>Total ScriptData in load order: %1.1f mb</li>",
                    analysis.scriptDataSize / 1048576.0f
                )
            )
        }
        BUILDER.append("</ul></fixed>")
        return BUILDER.toString()
    }

    /**
     * Retrieves the plugin corresponding to a formID.
     *
     * @param formID
     * @return
     */
    fun getPluginFor(formID: Int): Plugin? {
        val FULL = pluginInfo.fullPlugins
        val LITE = pluginInfo.litePlugins
        val INDEX = formID ushr 24
        val SUBINDEX = formID and 0xFFFFFF ushr 12
        return if (INDEX in 0..0xfd && INDEX < FULL.size) {
            FULL[INDEX]
        } else if (INDEX == 0xFE && SUBINDEX >= 0 && SUBINDEX < LITE.size) {
            if (pluginInfo.hasLite()) LITE[SUBINDEX] else null
        } else {
            null
        }
    }

    /**
     * @return Returns a new `ESSContext`.
     */
    val context: ESSContext
        get() = ESSContext(this)

    /**
     * Creates a new `RefID` directly.
     *
     * @param val The 3-byte value with which to create the `RefID`.
     * @return The new `RefID`.
     */
    fun make(`val`: Int): RefID {
        val r = RefID(`val`, this)
        return REFIDS.computeIfAbsent(`val`) { v: Int? -> RefID(`val`, this) }
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        return header.GAME?.NAME ?: ""
        /*if (null != this.ORIGINAL_FILE) {
            return this.ORIGINAL_FILE.getFileName().toString();
        } else {
            return "NO FILENAME";
        }*/
    }

    val cosave: ByteArray?
        get() = COSAVE
    val table1: List<GlobalData>
        get() = TABLE1
    val table2: List<GlobalData>
        get() = TABLE2
    val table3: List<GlobalData>
        get() = TABLE3

    /**
     * @return The value of the header field.
     */
    val header: Header
    val formVersion: Byte
    var versionString: String? = null

    /**
     * @return The list of plugins.
     */
    val pluginInfo: PluginInfo
    val fLT: FileLocationTable?
    private val TABLE1: MutableList<GlobalData>
    private val TABLE2: MutableList<GlobalData>
    private val CHANGEFORMS: LinkedHashMap<RefID?, ChangeForm?>
    private val TABLE3: MutableList<GlobalData>
    private val FORMIDARRAY: IntArray?
    val visitedWorldspaceArray: IntArray?
    val unknown3: ByteArray
    private val COSAVE: ByteArray?

    /**
     * @return The original file containing the `ESS` when it was
     * readRefID from the disk.
     */
    val originalFile: Path

    /**
     * @return The digest of the `ESS` when it was readRefID from the
     * disk.
     */
    val digest: Long

    /**
     * @return The papyrus section.
     */
    val papyrus: Papyrus?

    /**
     * @return The `GlobalVariableTable`.
     */
    val animations: AnimObjects
    private val GLOBALS: GlobalVariableTable
    private val REFIDS: MutableMap<Int, RefID>
    val analysis: Analysis? = null
    private var truncated = false

    /**
     * Stores the results of a load or save operation.
     */
    inner class Result(backup: Path?, timer: Timer, model: FilterTreeModel?) {
        val ESS: ESS = this@ESS
        val GAME: Game? = header.GAME
        val SAVE_FILE: Path = originalFile
        val BACKUP_FILE: Path? = backup
        val TIME_S: Double = timer.elapsed / 1.0e9
        val SIZE_MB: Double
        val MODEL: FilterTreeModel?

        init {
            val size: Double = try {
                Files.size(SAVE_FILE) / 1048576.0
            } catch (ex: IOException) {
                Double.NEGATIVE_INFINITY
            }
            SIZE_MB = size
            MODEL = model
        }
    }

    /**
     * A factory class for making and reading `RefID`.
     */
    open class ESSContext {
        /**
         * Creates a new `ESSContext` for the specified
         * `ESS` object.
         *
         * @param ess
         */
        constructor(ess: ESS?) {
            eSS = ess!!
        }

        /**
         * Creates a new `ESSContext` for the specified
         * `ESS` object.
         *
         * @param context
         */
        constructor(context: ESSContext?) {
            eSS = context!!.eSS
        }

        /**
         * Creates a new `RefID` by reading from a
         * `LittleEndianDataOutput`. No error handling is performed.
         *
         * @param input The input stream.
         * @return The new `RefID`.
         */
        fun readRefID(input: ByteBuffer): RefID {
            val B1 = input.get().toInt()
            val B2 = input.get().toInt()
            val B3 = input.get().toInt()
            val VAL = (B1 and 0xFF shl 16
                    or (B2 and 0xFF shl 8)
                    or (B3 and 0xFF))
            return makeRefID(VAL)
        }

        /**
         * Creates a new `RefID` directly.
         *
         * @param val The 3-byte value with which to create the
         * `RefID`.
         * @return The new `RefID`.
         */
        fun makeRefID(`val`: Int): RefID {
            return eSS.make(`val`)
        }
        val game: Game?
            get() = this.eSS.header.GAME
        /**
         * @return A flag indicating whether the `ESS` has 32-bit
         * strings.
         */
        val isStr32: Boolean
            get() = eSS.isStr32

        /**
         * Does a very general search for an ID.
         *
         * @param number The data to search for.
         * @return Any match of any kind.
         */
        open fun broadSpectrumSearch(number: Number): Linkable? {
            try {
                val ref = makeRefID(number.toInt())
                if (eSS.CHANGEFORMS.containsKey(ref)) {
                    return eSS.CHANGEFORMS[ref]
                }
            } catch (ex: RuntimeException) {
                logger.warn(ex) {"RuntimeException during BroadSpectrumMatch."}
            }
            return null
        }

        /**
         * Finds the `ChangeForm` corresponding to a
         * `RefID`.
         *
         * @param refID The `RefID`.
         * @return The corresponding `ChangeForm` or null if it was
         * not found.
         */
        fun getChangeForm(refID: RefID): ChangeForm? {
            return eSS.changeForms[refID]
        }

        /**
         * @return The `Path` of the original save file.
         */
        val path: Path
            get() = eSS.originalFile

        /**
         * @return The `ESS` itself. May not be full constructed.
         */
        protected val eSS: ESS
    }

    companion object:KLoggable {
        /**
         * Reads a savegame and creates an `ESS` object to represent it.
         *
         * Exceptions are not handled. At all. Not even a little bit.
         *
         * @param saveFile The file containing the savegame.
         * @param model A `ModelBuilder`.
         * @return A `Result` object with details about results.
         * @throws IOException
         */
        @OptIn(ExperimentalFileSystem::class)
        @JvmStatic
        @Throws(IOException::class)
        fun readESS(saveFile: Path, model: ModelBuilder): Result {

            // Timer, for analyzing stuff.
            val TIMER = startNew("reading savefile")

            // Doublecheck that the savefile has a correct extension.
            if (!Game.FILTER_ALL.accept(saveFile.toFile())) {
                throw IOException("Filename extension not recognized: $saveFile")
            }

            // Read the savefile.
            // If the F4SE co-save is present, readRefID it too.
            try {
                //try (LittleEndianInputStream input = LittleEndianInputStream.openCtxDig(saveFile)) {
            return FilePart(saveFile.toString().toPath(),model,TIMER)
                //return FileParsing(saveFile, model, TIMER)
            } catch (ex: IOException) {
                val msg = "Failed to load $saveFile\n${ex.message}"
                throw IOException(msg, ex)
            } catch (ex: DataFormatException) {
                val msg = "Failed to load $saveFile\n${ex.message}"
                throw IOException(msg, ex)
            }
        }
        @OptIn(ExperimentalFileSystem::class)
        private fun FilePart(saveFile: okio.Path, model: ModelBuilder, TIMER: Timer):Result {
            val fileSystem: FileSystem = FileSystem.SYSTEM
            val entireFileByteString = fileSystem.read(saveFile) {
                readByteString()
            }
            var outbuffer = ByteBuffer.allocate(entireFileByteString.size).order(ByteOrder.LITTLE_ENDIAN)
            outbuffer = outbuffer.put(entireFileByteString.asByteBuffer())
            outbuffer.flip()
            val ESS = ESS(outbuffer, saveFile.toNioPath(), model)
            val TREEMODEL = model.finish(ESS)
            TIMER.stop()
            val SIZE = ESS.calculateSize() / 1048576.0f
            logger.info {String.format("Savegame read: %.1f mb in ${TIMER.formattedTime} ($saveFile).", SIZE)}
            return ESS.Result(null, TIMER, TREEMODEL)
        }
//        private fun FileParsing(saveFile: Path, model: ModelBuilder, TIMER: Timer): Result {
//            FileChannel.open(saveFile, StandardOpenOption.READ).use { channel: FileChannel ->
//                val saveSize = Files.size(saveFile).toInt()
//                val input = ByteBuffer.allocate(saveSize).order(ByteOrder.LITTLE_ENDIAN)
//                channel.read(input)
//                (input as Buffer).flip()
//                val ESS = ESS(input, saveFile, model)
//                val TREEMODEL = model.finish(ESS)
//                TIMER.stop()
//                val SIZE = ESS.calculateSize() / 1048576.0f
//                logger.info {String.format("Savegame read: %.1f mb in ${TIMER.formattedTime} ($saveFile).", SIZE)}
//                return ESS.Result(null, TIMER, TREEMODEL)
//            }
//        }

        /**
         * Writes out a savegame.
         *
         * Exceptions are not handled. At all. Not even a little bit.
         *
         * @param ess The `ESS` object.
         * @param saveFile The file into which to write the savegame.
         * @return A `Result` object with details about results.
         * @throws IOException
         */
        @Throws(IOException::class)
        fun writeESS(ess: ESS, saveFile: Path): Result {
            if (ess.truncated) {
                throw IOException("${ess.originalFile.fileName} is truncated and can't be saved.")
            }
            val TIMER = startNew("writing savefile")
            val GAME: Game = ess.header.GAME!!
            var backup: Path? = null
            if (Files.exists(saveFile)) {
                backup = makeBackupFile(saveFile)
            }
            if (ess.COSAVE != null) {
                val filename = saveFile.fileName.toString()
                val cosaveName = filename.replace("${GAME.SAVE_EXT}${"$".toRegex()}", GAME.COSAVE_EXT)
                val COSAVE_FILE = saveFile.resolveSibling(cosaveName)
                if (Files.exists(COSAVE_FILE)) {
                    makeBackupFile(COSAVE_FILE)
                }
                Files.write(COSAVE_FILE, ess.COSAVE)
            }
            FileChannel.open(
                saveFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
            ).use { channel -> ess.write(channel) }
            val SIZE = Files.size(saveFile) / 1048576.0f
            TIMER.stop()
            logger.info {String.format("Savegame written: %.1f mb in ${TIMER.formattedTime} ($saveFile).",SIZE)}
            return ess.Result(backup, TIMER, null)
        }

        /**
         * Verifies that two instances of `ESS` are identical.
         *
         * @param ess1 The first `ESS`.
         * @param ess2 The second `ESS`.
         * @throws IllegalStateException Thrown if the two instances of
         * `ESS` are not equal.
         */
        @JvmStatic
        @Throws(IllegalStateException::class)
        fun verifyIdentical(ess1: ESS, ess2: ESS) {
            check(ess1.calculateBodySize() == ess2.calculateBodySize()) {
                String.format(
                    "Body size mismatch: %d vs %d.",
                    ess1.calculateBodySize(),
                    ess2.calculateBodySize()
                )
            }
            check(ess1.calculateSize() == ess2.calculateSize()) {
                String.format(
                    "Total size mismatch: %d vs %d.",
                    ess1.calculateSize(),
                    ess2.calculateSize()
                )
            }
            verifyIdentical(ess1.header, ess2.header)
            ess1.changeForms.forEach { (refID: RefID?, cf1: ChangeForm?): Map.Entry<RefID?, ChangeForm?> ->
                if (cf1 != null) {
                    verifyIdentical(
                        cf1, ess2.changeForms[refID]!!
                    )
                }
            }
            val PAP1 = ess1.papyrus
            val PAP2 = ess2.papyrus
            check(PAP1!!.header == PAP2!!.header) {
                String.format(
                    "Papyrus header mismatch: %d vs %d.",
                    PAP1.header,
                    PAP2.header
                )
            }
            check(PAP1.stringTable.containsAll(PAP2.stringTable)) { "StringTable mismatch." }
            val BUF1 = ByteBuffer.allocate(PAP1.calculateSize())
            val BUF2 = ByteBuffer.allocate(PAP2.calculateSize())
            PAP1.write(BUF1)
            PAP2.write(BUF2)

            check(BUF1.array().contentEquals(BUF2.array())) { "Papyrus mismatch." }
        }

        val THREAD = Predicate { v: Element? -> v is ActiveScript }
        val OWNABLE = Predicate { v: Element? ->
            (v is ActiveScript
                    || v is StackFrame
                    || v is ArrayInfo)
        }

        val DELETABLE = Predicate { v: Element? ->
            (v is Definition
                    || v is GameElement
                    || v is ArrayInfo
                    || v is ActiveScript
                    || v is ChangeForm
                    || v is SuspendedStack)
        }
        val PURGEABLE = Predicate { v: Element? -> v is Plugin }

        /**
         * Creates a backup of a file.
         *
         * @param file The file to backup.
         * @return The backup file.
         * @throws IOException
         */
        @Throws(IOException::class)
        private fun makeBackupFile(file: Path): Path {
            val newdate = Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
            val TIME = "${newdate.year}.${newdate.monthNumber}.${newdate.dayOfWeek.value}.${newdate.hour}.${newdate.minute}.${newdate.second}"
            val FILENAME = file.fileName.toString()
            val REGEX = Pattern.compile("^(.+)\\.([ -~]+)$")
            val MATCHER = REGEX.matcher(FILENAME)
            val NEWNAME: String = if (MATCHER.matches()) {
                val NAME = MATCHER.group(1)
                val EXT = MATCHER.group(2)
                "$NAME.$TIME.$EXT"
            } else {
                "$FILENAME.$TIME"
            }
            val NEWFILE = file.resolveSibling(NEWNAME)
            Files.copy(file, NEWFILE, StandardCopyOption.REPLACE_EXISTING)
            return NEWFILE
        }

        override val logger: KLogger
            get() = logger()
    }

    /**
     * Creates a new `ESS` by reading from a `ByteBuffer`.
     *
     * @param buffer The input stream for the savegame.
     * @param saveFile The file containing the `ESS`.
     * @param model A `ModelBuilder`.
     * @throws IOException
     */
    init {
        REFIDS = hashMapOf()
        originalFile = saveFile
        logger.info {"Reading savegame."}

        // Read the header. This includes the magic string.
        header = Header(buffer, saveFile)
        val GAME: Game = header.GAME!!
        logger.info {"Reading savegame: read header."}

        // Determine the filename of the co-save.
        val filename = saveFile.fileName.toString()
        val cosaveName = filename.replace("${GAME.SAVE_EXT}${"$".toRegex()}", GAME.COSAVE_EXT)
        val cosaveFile = saveFile.resolveSibling(cosaveName)
        COSAVE = if (Files.exists(cosaveFile)) Files.readAllBytes(cosaveFile) else null

        // Store the offset where the header ends and the body begins.
        val startingOffset = buffer.position()

        // This is the stream that will be used for the remainder of the 
        // constructor.
        val INPUT: ByteBuffer

        // Do the decompression, if necessary.
        val COMPRESSION = header.getCompression()
        if (COMPRESSION.isCompressed) {
            val UNCOMPRESSED_LEN = buffer.int
            val COMPRESSED_LEN = buffer.int
            if (UNCOMPRESSED_LEN < 0 || COMPRESSED_LEN < 0) {
                throw IOException("Compression error. You might need to set [SAVEGAME]uiCompression=1 in SkyrimCustom.ini.")
            }

            //final ByteBuffer UNCOMPRESSED = ByteBuffer.allocate(UNCOMPRESSED_LEN);
            val COMPRESSED = ByteBuffer.allocate(COMPRESSED_LEN)
            COMPRESSED.put(buffer)
            (COMPRESSED as Buffer).flip()
            COMPRESSED.order(ByteOrder.LITTLE_ENDIAN)
            check(!buffer.hasRemaining()) { "Some data was not compressed." }
            when (COMPRESSION) {
                CompressionType.ZLIB -> {
                    logger.info {"ZLIB DECOMPRESSION"}
                    INPUT = BufferUtil.inflateZLIB(COMPRESSED, UNCOMPRESSED_LEN, COMPRESSED_LEN)
                    INPUT.order(ByteOrder.LITTLE_ENDIAN)
                }
                CompressionType.LZ4 -> {
                    logger.info {"LZ4 DECOMPRESSION"}
                    INPUT = BufferUtil.inflateLZ4(COMPRESSED, UNCOMPRESSED_LEN)
                    INPUT.order(ByteOrder.LITTLE_ENDIAN)
                }
                else -> throw IOException("Unknown compression type: $COMPRESSION")
            }
        } else {
            logger.info {"NO FILE COMPRESSION"}
            INPUT = buffer.slice()
            INPUT.order(ByteOrder.LITTLE_ENDIAN)
        }

        // sanity check
        val headerSize = header.calculateSize()
        check(headerSize == startingOffset) {
            String.format(
                "Error reading header: position mismatch %d instead of %d.",
                headerSize,
                startingOffset
            )
        }

        // Make a CRC for the ESS header block.
        val CRC32 = CRC32()
        (buffer as Buffer).position(0)
        (buffer as Buffer).limit(startingOffset)
        CRC32.update(buffer)

        // Update the CRC with the ESS body block.
        CRC32.update(INPUT)
        (INPUT as Buffer).flip()
        digest = CRC32.value
        val SUM = Counter(buffer.capacity())
        SUM.addCountListener { sum: Int ->
            check(!(truncated || sum != INPUT.position())) {
                String.format(
                    "Position mismatch; counted %d but actual %d in %s.",
                    sum,
                    INPUT.position(),
                    saveFile.fileName
                )
            }
        }

        // Read the form version.
        formVersion = INPUT.get()
        SUM.click()
        logger.info {
            String.format(
                "Detected %s with form version %d, %s in %s.",
                GAME,
                formVersion,
                COMPRESSION,
                saveFile.parent.relativize(saveFile)
            )
        }
        when (GAME) {
            Game.SKYRIM_LE -> {
                require(formVersion >= 73) { "Invalid formVersion: $formVersion" }
                versionString = null
            }
            Game.SKYRIM_SE, Game.SKYRIM_SW, Game.SKYRIM_VR -> {
                require(formVersion >= 77) { "Invalid formVersion: $formVersion" }
                versionString = null
            }
            Game.FALLOUT4, Game.FALLOUT_VR -> {
                require(formVersion >= 60) { "Invalid formVersion: $formVersion" }
                versionString = BufferUtil.getWString(INPUT)
                SUM.click(2 + versionString!!.length)
            }
            else -> throw IllegalArgumentException("Unrecognized game.")
        }

        // Read the PLUGIN info section.
        pluginInfo = PluginInfo(INPUT, supportsESL())
        SUM.click(pluginInfo.calculateSize())
        logger.info {"Reading savegame: read plugin table."}

        // Add the plugins to the model.
        model.addPluginInfo(pluginInfo)

        // Read the file location table.
        fLT = FileLocationTable(INPUT, GAME)
        TABLE1 = ArrayList(fLT.TABLE1COUNT)
        TABLE2 = ArrayList(fLT.TABLE2COUNT)
        TABLE3 = ArrayList(fLT.TABLE3COUNT)
        CHANGEFORMS = LinkedHashMap(fLT.changeFormCount)
        SUM.click(fLT.calculateSize())
        logger.info {"Reading savegame: read file location table."}

        // Read the FormID table.
        var formIDs: IntArray? = null
        try {
            (INPUT as Buffer).position(fLT.formIDArrayCountOffset - startingOffset)
            val formIDCount = INPUT.int
            formIDs = IntArray(formIDCount)
            for (formIDIndex in 0 until formIDCount) {
                try {
                    formIDs[formIDIndex] = INPUT.int
                } catch (ex: BufferUnderflowException) {
                    throw ListException("Truncation in the FormID array.", formIDIndex, formIDCount, ex)
                }
            }
            logger.info {"Reading savegame: read formid array."}
        } catch (ex: ListException) {
            truncated = true
            logger.error(ex) {"Error while reading FormID array."}
        } catch (ex: IllegalArgumentException) {
            truncated = true
            logger.error(ex) {"Error while reading FormID array."}
        } catch (ex: BufferUnderflowException) {
            truncated = true
            logger.error(ex) {"FormID table missing."}
        } finally {
            FORMIDARRAY = formIDs
        }
        (INPUT as Buffer).position(fLT.table1Offset - startingOffset)

        // Read the first and second sets of data tables.
        val context = context
        for (tableIndex in 0 until fLT.TABLE1COUNT) {
            try {
                val DATA = GlobalData(INPUT, context, model)
                require(!(DATA.type < 0 || DATA.type > 100)) { "Invalid type for Table1: " + DATA.type }
                TABLE1.add(DATA)
                logger.info {"Reading savegame: \tGlobalData type ${DATA.type}."}
                logger.info {"Reading savegame: read global data table 1."}
            } catch (ex: PapyrusException) {
                throw IOException(
                    String.format(
                        "Error; read %d/%d GlobalData from table #2; something stupid happened.",
                        tableIndex,
                        fLT.TABLE2COUNT
                    ), ex
                )
            } catch (ex: RuntimeException) {
                throw IOException(
                    String.format(
                        "Error; read %d/%d GlobalData from table #2.",
                        tableIndex,
                        fLT.TABLE2COUNT
                    ), ex
                )
            }
        }
        var sum = 0
        for (globalData in TABLE1) {
            val i = globalData.calculateSize()
            sum += i
        }
        SUM.click(sum)
        logger.info {"Reading savegame: read GlobalDataTable #1."}
        for (tableIndex in 0 until fLT.TABLE2COUNT) {
            try {
                val DATA = GlobalData(INPUT, context, model)
                require(!(DATA.type < 100 || DATA.type > 1000)) { "Invalid type for Table1: " + DATA.type }
                TABLE2.add(DATA)
                logger.info {"Reading savegame: \tGlobalData type ${DATA.type}."}
            } catch (ex: PapyrusException) {
                throw IOException(
                    String.format(
                        "Error; read %d/%d GlobalData from table #2; something stupid happened.",
                        tableIndex,
                        fLT.TABLE2COUNT
                    ), ex
                )
            } catch (ex: RuntimeException) {
                throw IOException(
                    String.format(
                        "Error; read %d/%d GlobalData from table #2.",
                        tableIndex,
                        fLT.TABLE2COUNT
                    ), ex
                )
            }
        }
        var result = 0
        for (globalData in TABLE2) {
            val i = globalData.calculateSize()
            result += i
        }
        SUM.click(result)
        logger.info {"Reading savegame: read GlobalDataTable #2."}

        // Get the GlobalVariableTable.
        var found = GlobalVariableTable()
        for (globalData in TABLE1) {
            if (globalData.type == 3 && globalData.dataBlock is GlobalVariableTable) {
                val dataBlock = globalData.dataBlock as GlobalVariableTable?
                found = dataBlock!!
                break
            }
        }
        GLOBALS = found
        model.addGlobalVariableTable(GLOBALS)

        // Read the changeforms.
        for (changeFormIndex in 0 until fLT.changeFormCount) {
            try {
                val FORM = ChangeForm(INPUT, context)
                CHANGEFORMS[FORM.refID] = FORM
            } catch (ex: RuntimeException) {
                throw IOException(
                    String.format(
                        "Error; read %d/%d ChangeForm definitions.",
                        changeFormIndex,
                        fLT.changeFormCount
                    ), ex
                )
            }
        }
        model.addChangeForms(CHANGEFORMS)
        var sum1 = 0
        for (changeForm in CHANGEFORMS.values) {
            val i = changeForm!!.calculateSize()
            sum1 += i
        }
        SUM.click(sum1)
        logger.info {"Reading savegame: read changeform table."}

        // Read the third set of data tables.
        var papyrusPartial: Papyrus? = null
        for (tableIndex in 0 until fLT.TABLE3COUNT) {
            try {
                val DATA = GlobalData(INPUT, context, model)
                require(!(DATA.type < 1000 || DATA.type > 1100)) { "Invalid type for Table1: " + DATA.type }
                TABLE3.add(DATA)
                logger.info {"Reading savegame: \tGlobalData type ${DATA.type}."}
            } catch (ex: PapyrusException) {
                logger.error(ex) {"Error reading GlobalData 1001 (Papyrus)."}
                truncated = true
                papyrusPartial = ex.partial
            } catch (ex: RuntimeException) {
                throw IOException(
                    String.format(
                        "Error; read %d/%d GlobalData from table #3.",
                        tableIndex,
                        fLT.TABLE3COUNT
                    ), ex
                )
            }
        }

        // Grab the Papyrus block.
        var found1 = papyrusPartial
        for (globalData in TABLE3) {
            if (globalData.type == 1001 && globalData.dataBlock is Papyrus) {
                val dataBlock = globalData.dataBlock as Papyrus?
                found1 = dataBlock
                break
            }
        }
        papyrus = found1

        // Grab the Animations block.
        var result1 = AnimObjects()
        for (b in TABLE3) {
            if (b.type == 1002 && b.dataBlock is DefaultGlobalDataBlock) {
                val dataBlock = b.dataBlock as DefaultGlobalDataBlock?
                val animObjects = AnimObjects(dataBlock!!.data, context)
                result1 = animObjects
                break
            }
        }
        animations = result1
        model.addAnimations(animations)
        val sum2 = TABLE3.sumOf { it.calculateSize() }
        SUM.click(sum2)
        logger.info {"Reading savegame: read GlobalDataTable #3."}

        // Try to readRefID the visited worldspaces block.
        var visitedWorldSpaces: IntArray? = null
        try {
            // Read the worldspaces-visited table. Skip past the FormID array since
            // it was readRefID earlier.
            val skipFormIDArray = fLT.formIDArrayCountOffset - startingOffset + (4 + 4 * FORMIDARRAY!!.size)
            (INPUT as Buffer).position(skipFormIDArray)
            val worldspaceIDCount = INPUT.int
            visitedWorldSpaces = IntArray(worldspaceIDCount)
            for (worldspaceIndex in 0 until worldspaceIDCount) {
                visitedWorldSpaces[worldspaceIndex] = INPUT.int
            }
            logger.info {"Reading savegame: read visited worldspace array."}
        } catch (ex: BufferUnderflowException) {
            if (!truncated) {
                truncated = true
                logger.error(ex) {"Error reading VisitedWorldSpace array."}
            }
        } catch (ex: IllegalArgumentException) {
            if (!truncated) {
                truncated = true
                logger.error(ex) {"Error reading VisitedWorldSpace array."}
            }
        } finally {
            visitedWorldspaceArray = visitedWorldSpaces
        }

        // Read whatever is left.
        val U3SIZE = INPUT.limit() - INPUT.position()
        logger.info {String.format("Reading savegame: read unknown block. %d bytes present.", U3SIZE)}
        unknown3 = ByteArray(U3SIZE)
        INPUT[unknown3]
        val calculatedBodySize = calculateBodySize().toLong()
        val bodyPosition = INPUT.position().toLong()
        check(calculatedBodySize == bodyPosition) {
            String.format(
                "Missing data, calculated body size is %d but actual body size is %d.",
                calculatedBodySize,
                bodyPosition
            )
        }
        if (!COMPRESSION.isCompressed) {
            val calculatedSize = calculateSize().toLong()
            val fileSize = Files.size(saveFile)
            check(calculatedSize == fileSize) {
                String.format(
                    "Missing data, calculated file size size is %d but actual file size is %d.",
                    calculatedSize,
                    fileSize
                )
            }
        }
    }
}