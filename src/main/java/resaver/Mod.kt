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
package resaver

import resaver.archive.ArchiveParser.Companion.createParser
import resaver.esp.StringsFile
import resaver.ess.Plugin
import resaver.ess.PluginInfo
import java.io.FileNotFoundException
import java.io.IOException
import java.io.Serializable
import java.nio.BufferUnderflowException
import java.nio.channels.FileChannel
import java.nio.file.*
import java.util.*
import java.util.function.Predicate
import java.util.function.ToDoubleFunction
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.math.sqrt

/**
 * Describes a mod as a 4-tuple of a directory, a list of plugins, a list of
 * archives, and a list of script files.
 *
 * @author Mark Fairchild
 */
class Mod(game: Game?, dir: Path) : Serializable {
    /**
     * An estimate of the total amount of data that would be scanned to read
     * this mod. The Math.sqrt operation is applied, to model the fact that
     * reading short files isn't much faster than reading long files.
     *
     * @return The size.
     */
    fun getSize(): Int {
        val toSize: ToDoubleFunction<Path> = ToDoubleFunction { f: Path? ->
            try {
                return@ToDoubleFunction sqrt(Files.size(f!!).toDouble())
            } catch (ex: IOException) {
                return@ToDoubleFunction 0.0
            }
        }
        size = 0.0
        val sum = SCRIPT_FILES.sumOf { toSize.applyAsDouble(it) }
        this.size += sum
        var result = 0.0
        for (STRINGS_FILE in STRINGS_FILES) {
            val v = toSize.applyAsDouble(STRINGS_FILE)
            result += v
        }
        size += result
        var sum1 = 0.0
        for (PLUGIN_FILE in PLUGIN_FILES) {
            val v = toSize.applyAsDouble(PLUGIN_FILE)
            sum1 += v
        }
        size += sum1
        var result1 = 0.0
        for (ARCHIVE_FILE in ARCHIVE_FILES) {
            val v = toSize.applyAsDouble(ARCHIVE_FILE)
            result1 += v
        }
        size += result1
        return size.toInt()
    }

    /**
     *
     * @return Returns true if the mod contains no plugins, archives, or loose
     * script files.
     */
    val isEmpty: Boolean
        get() = ARCHIVE_FILES.isEmpty() && PLUGIN_FILES.isEmpty() && SCRIPT_FILES.isEmpty()

    /**
     * @return The number of archives.
     */
    fun getNumArchives(): Int {
        return ARCHIVE_FILES.size
    }

    /**
     * @return The number of loose script files.
     */
    fun getNumLooseScripts(): Int {
        return SCRIPT_FILES.size
    }

    /**
     * @return The number of loose script files.
     */
    fun getNumLooseStrings(): Int {
        return STRINGS_FILES.size
    }

    /**
     * @return The number of ESP/ESM files.
     */
    fun getNumESPs(): Int {
        return PLUGIN_FILES.size
    }

    /**
     * @return The name of the mod.
     */
    fun getName(): String? {
        return MODNAME
    }

    /**
     * @return The abbreviated name of the mod.
     */
    fun getShortName(): String? {
        return SHORTNAME
    }

    /**
     * @return A list of the names of the esp files in the mod.
     */
    fun getESPNames(): List<String> {
        val NAMES: MutableList<String> = ArrayList(PLUGIN_FILES.size)
        for (v in PLUGIN_FILES) {
            NAMES.add(v.fileName.toString())
        }
        return NAMES
    }

    /**
     * Finds the `Plugin` corresponding to a
     * `StringsFile`.
     *
     * @param stringsFilePath
     * @param language
     * @param plugins
     * @return
     */
    private fun getStringsFilePlugin(stringsFilePath: Path, language: String, plugins: PluginInfo): Plugin? {
        val SSREGEX = String.format("_%s\\.(il|dl)?strings", language)
        val FILENAME = stringsFilePath.fileName.toString()
        val pathPluginMap = plugins.paths
        val pathPluginMap1 = plugins.paths
        for (path in Arrays.asList(
            Paths.get(FILENAME.replace(SSREGEX.toRegex(), ".esm")),
            Paths.get(FILENAME.replace(SSREGEX.toRegex(), ".esp")),
            Paths.get(FILENAME.replace(SSREGEX.toRegex(), ".esl"))
        )) {
            if (pathPluginMap1.containsKey(path)) {
                return pathPluginMap[path]
            }
        }
        return null
    }

    /**
     * Reads the data for the `Mod`, consisting of
     * `StringsFile` objects and `PexFile` objects.
     *
     * @param language The language for the string tables.
     * @param plugins The `PluginInfo` from an `ESS`.
     * @return A `ModReadResults`.
     */
    fun readData(plugins: PluginInfo, language: String): ModReadResults {
        Objects.requireNonNull(language)
        val LANG = "_" + language.toLowerCase()
        val GLOB = "glob:**$LANG.*strings"
        val MATCHER = FS.getPathMatcher(GLOB)
        val ARCHIVE_ERRORS: MutableList<Path> = LinkedList()
        val STRINGSFILE_ERRORS: MutableList<Path> = LinkedList()

        // Read the archives.
        val STRINGSFILES: MutableList<StringsFile> = LinkedList()
        val SCRIPT_ORIGINS: MutableMap<Path, Path> = hashMapOf()
        for (archivePath in ARCHIVE_FILES) {
            try {
                FileChannel.open(archivePath, StandardOpenOption.READ).use { channel ->
                    createParser(archivePath, channel).use { PARSER ->
                        val ARCHIVE_STRINGSFILES: MutableList<StringsFile> = LinkedList()
                        for ((path, input) in PARSER!!.getFiles(Paths.get("strings"), MATCHER)!!) {
                            if (input.isPresent) {
                                val PLUGIN = path?.let { getStringsFilePlugin(it, language, plugins) }
                                if (PLUGIN != null) {
                                    try {
                                        val STRINGSFILE = StringsFile.readStringsFile(path, PLUGIN, input.get())
                                        ARCHIVE_STRINGSFILES.add(STRINGSFILE)
                                    } catch (ex: BufferUnderflowException) {
                                        STRINGSFILE_ERRORS.add(archivePath.fileName)
                                    }
                                }
                            } else {
                                STRINGSFILE_ERRORS.add(archivePath.fileName)
                            }
                        }
                        val ARCHIVE_SCRIPTS = PARSER.getFilenames(Paths.get("scripts"), GLOB_SCRIPT)
                        SCRIPT_ORIGINS.putAll(ARCHIVE_SCRIPTS!!)
                        STRINGSFILES.addAll(ARCHIVE_STRINGSFILES)
                        var stringsCount = 0
                        for (s in ARCHIVE_STRINGSFILES) {
                            val i = s.TABLE.size
                            stringsCount += i
                        }
                        val scriptsCount = ARCHIVE_SCRIPTS.size
                        if (stringsCount > 0 || scriptsCount > 0) {
                            val fmt = "Read %5d scripts and %5d strings from %d stringsfiles in %s of \"%s\""
                            val msg = String.format(
                                fmt,
                                scriptsCount,
                                stringsCount,
                                ARCHIVE_STRINGSFILES.size,
                                archivePath.fileName,
                                SHORTNAME
                            )
                            LOG.info(msg)
                        }
                    }
                }
            } catch (ex: IOException) {
                ARCHIVE_ERRORS.add(archivePath.fileName)
            }
        }

        // Read the loose stringtable files.
        val LOOSE_STRINGSFILES: List<StringsFile> = STRINGS_FILES
            .filter { path: Path? -> MATCHER.matches(path) }
            .map { path: Path ->
                try {
                    val PLUGIN = getStringsFilePlugin(path, language, plugins)
                    if (PLUGIN != null) {
                        return@map StringsFile.readStringsFile(path, PLUGIN)
                    } else {
                        return@map null
                    }
                } catch (ex: IOException) {
                    STRINGSFILE_ERRORS.add(path)
                    LOG.severe(String.format("Mod \"%s\": error while reading \"%s\".", SHORTNAME, path))
                    return@map null
                }
            }.filterNotNull().toList()

        // Read the loose stringtable files.
        val LOOSE_SCRIPTS: MutableMap<Path, Path> = hashMapOf()
        for (p in SCRIPT_FILES) {
            if (GLOB_SCRIPT.matches(p)) {
                check(LOOSE_SCRIPTS.put(p, p.fileName) == null) { "Duplicate key" }
            }
        }
        SCRIPT_ORIGINS.putAll(LOOSE_SCRIPTS)
        var stringsCount = 0
        for (s in LOOSE_STRINGSFILES) {
            val i = s.TABLE.size
            stringsCount += i
        }
        val scriptsCount = LOOSE_SCRIPTS.size
        if (stringsCount > 0 || scriptsCount > 0) {
            val fmt = "Read %5d scripts and %5d strings from %d stringsfiles in loose files of \"%s\""
            val msg = String.format(fmt, scriptsCount, stringsCount, LOOSE_STRINGSFILES.size, SHORTNAME)
            LOG.info(msg)
        }
        return ModReadResults(SCRIPT_ORIGINS, STRINGSFILES, ARCHIVE_ERRORS, null, STRINGSFILE_ERRORS)
    }

    /**
     *
     * @return
     */
    fun getAnalysis(): Analysis {
        val ANALYSIS = Analysis()
        ANALYSIS.MODS.add(this)
        ANALYSIS.ESPS[MODNAME] = TreeSet(getESPNames())
        return ANALYSIS
    }

    /**
     * @return
     */
    override fun toString(): String {
        return MODNAME!!
    }

    /**
     * @return A copy of the list of ESP files.
     */
    fun getESPFiles(): List<Path> {
        return ArrayList(PLUGIN_FILES)
    }

    /**
     * @return A copy of the list of archive files.
     */
    fun getArchiveFiles(): List<Path> {
        return ArrayList(ARCHIVE_FILES)
    }

    /**
     * @return A copy of the list of PEX files.
     */
    fun getPexFiles(): List<Path> {
        return ArrayList(SCRIPT_FILES)
    }

    /**
     * @see Object.hashCode
     * @return
     */
    override fun hashCode(): Int {
        return directory.hashCode()
    }

    /**
     * @see Object.equals
     * @param obj
     * @return
     */
    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        if (obj == null) {
            return false
        }
        if (javaClass != obj.javaClass) {
            return false
        }
        val other = obj as Mod
        return directory == other.directory
    }

    /**
     * @return The directory storing the `Mod`.
     */
    val directory: Path
    private val SCRIPT_PATH: Path
    private val STRING_PATH: Path
    private var MODNAME: String
    private val SHORTNAME: String?
    private val PLUGIN_FILES: List<Path>
    private val ARCHIVE_FILES: List<Path>
    private val SCRIPT_FILES: List<Path>
    private val STRINGS_FILES: List<Path>

    var size: Double = 0.0

    /**
     * The status of an individual mod within a profile.
     */
    enum class Status {
        CHECKED, UNCHECKED, DISABLED
    }

    /**
     * Stores data relating strings, scripts, and functions to their
     *
     * @author Mark Fairchild
     */
    open class Analysis : Serializable {
        /**
         * List: (Mod name)
         */
        @JvmField
        val MODS: MutableSet<Mod> = LinkedHashSet()

        /**
         * Map: (IString) -> File
         */
        val SCRIPTS: MutableMap<IString, Path> = LinkedHashMap()

        /**
         * Map: (Mod name) -> (Lisp[ESP name])
         */
        val ESPS: MutableMap<String, SortedSet<String>> = LinkedHashMap()

        /**
         * Map: (IString) -> (List: (Mod name))
         */
        @JvmField
        val SCRIPT_ORIGINS: MutableMap<IString, SortedSet<String>> = LinkedHashMap()

        /**
         * Map: (IString) -> (List: (Mod name))
         */
        val STRUCT_ORIGINS: MutableMap<IString, SortedSet<String>> = LinkedHashMap()

        /**
         * Merges analyses.
         *
         * @param sub anaysis to merge
         * @return Analysis
         */
        fun merge(sub: Analysis): Analysis {
            MODS.addAll(sub.MODS)
            SCRIPTS.putAll(sub.SCRIPTS)
            for ((k, v) in sub.ESPS) {
                ESPS.merge(k, v) { l1: SortedSet<String>, l2: SortedSet<String>? ->
                    l1.addAll(l2!!)
                    l1
                }
            }
            for ((key, value) in sub.SCRIPT_ORIGINS) {
                SCRIPT_ORIGINS.merge(key, value) { l1: SortedSet<String>, l2: SortedSet<String>? ->
                    l1.addAll(l2!!)
                    l1
                }
            }
            for ((name, list) in sub.STRUCT_ORIGINS) {
                STRUCT_ORIGINS.merge(name, list) { l1: SortedSet<String>, l2: SortedSet<String>? ->
                    l1.addAll(l2!!)
                    l1
                }
            }
            return this
        }
    }

    /**
     * An exception that indicates a stringtable file couldn't be read.
     */
    inner class ModReadResults(
        scriptOrigins: Map<Path, Path>,
        strings: List<StringsFile>?,
        archiveErrors: List<Path>?,
        scriptErrors: List<Path>?,
        stringsErrors: List<Path>?
    ) {
        val errorFiles: Stream<Path>
            get() = Stream.of(ARCHIVE_ERRORS, SCRIPT_ERRORS, STRINGS_ERRORS)
                .flatMap { obj: List<Path> -> obj.stream() }
        val MOD: Mod
        val SCRIPT_ORIGINS: Map<Path, Path>
        val STRINGSFILES: List<StringsFile>
        val ARCHIVE_ERRORS: List<Path>
        val SCRIPT_ERRORS: List<Path>
        val STRINGS_ERRORS: List<Path>

        /**
         * Creates a new `StringsReadError` with a list of
         * stringtable files and archive files that were corrupt.
         *
         * @param scriptOrigins the origin of the script
         * @param strings list of strings files
         * @param archiveErrors The list of names of the archives that were
         * unreadable.
         * @param scriptErrors The list of names of the script files that were
         * unreadable.
         * @param stringsErrors The list of names of the stringtables that were
         * unreadable.
         * @see Exception.Exception
         */
        init {
            //super(String.format("Some data could not be read: %d archives, %d scripts, %d stringtables", archives.size(), scripts.size(), strings.size()));
            MOD = this@Mod
            SCRIPT_ORIGINS = scriptOrigins!!
            STRINGSFILES = Collections.unmodifiableList(strings ?: emptyList())
            ARCHIVE_ERRORS = Collections.unmodifiableList(archiveErrors ?: emptyList())
            SCRIPT_ERRORS = Collections.unmodifiableList(scriptErrors ?: emptyList())
            STRINGS_ERRORS = Collections.unmodifiableList(stringsErrors ?: emptyList())
        }
    }

    companion object {
        /**
         * Creates a new `Mod` from a `File` representing the
         * directory containing the mod's files. The directory will be scanned to
         * create lists of all the important files.
         *
         * @param game The game.
         * @param dir The directory containing the mod.
         * @return The `Mod`, or null if it couldn't be created.
         */
        @JvmStatic
        fun createMod(game: Game?, dir: Path): Mod? {
            return try {
                Mod(game, dir)
            } catch (ex: FileNotFoundException) {
                LOG.warning(String.format("Couldn't read mod: %s\n%s", dir, ex.message))
                null
            } catch (ex: IOException) {
                LOG.log(Level.WARNING, String.format("Couldn't read mod: %s", dir), ex)
                null
            }
        }

        private val SCRIPTS_SUBDIR = Paths.get("scripts")
        private val STRINGS_SUBDIR = Paths.get("strings")
        private val LOG = Logger.getLogger(Mod::class.java.canonicalName)
        private val FS = FileSystems.getDefault()
        val GLOB_CREATIONCLUB = FS.getPathMatcher("glob:**\\cc*.{esm,esp,esl,bsa,ba2}")
        val GLOB_INTEREST = FS.getPathMatcher("glob:**.{esm,esp,esl,bsa,ba2}")
        val GLOB_PLUGIN = FS.getPathMatcher("glob:**.{esm,esp,esl}")
        val GLOB_ARCHIVE = FS.getPathMatcher("glob:**.{bsa,ba2}")
        @JvmField
        val GLOB_SCRIPT = FS.getPathMatcher("glob:**.pex")
        val GLOB_STRINGS = FS.getPathMatcher("glob:**.{strings,ilstrings,dlstrings}")
        val GLOB_ALL = FS.getPathMatcher("glob:**.{esm,esp,esl,bsa,ba2,pex,strings,ilstrings,dlstrings}")
        val GLOB_EXE = FS.getPathMatcher("glob:{skyrim.exe,skyrimse.exe,fallout4.exe}")
    }

    /**
     * Creates a new `Mod` from a `File` representing the
     * directory containing the mod's files. The directory will be scanned to
     * create lists of all the important files.
     *
     * @param game The game.
     * @param dir The directory containing the mod.
     * @throws IllegalArgumentException Thrown if the directory doesn't exist,
     * isn't a directory, or isn't readable.
     * @throws IOException Thrown if there is a problem reading data.
     */
    init {
        Objects.requireNonNull(game)
        Objects.requireNonNull(dir)
        directory = dir
        if (!Files.exists(dir)) {
            throw FileNotFoundException("Directory doesn't exists: $dir")
        }
        if (!Files.isDirectory(dir)) {
            throw IOException("Directory isn't actual a directory: $dir")
        }

        // Check if the parent directory is the game directory.
        val PARENT = dir.parent
        if (Files.list(PARENT).map { obj: Path -> obj.fileName }
                .anyMatch { path: Path? -> GLOB_EXE.matches(path) }) {
            MODNAME = PARENT.fileName.toString()
        } else {
            MODNAME = dir.fileName.toString()
        }
        SHORTNAME = if (MODNAME!!.length < 25) MODNAME else MODNAME!!.substring(0, 22) + "..."

        // Collect all files of relevance.
        SCRIPT_PATH = directory.resolve(SCRIPTS_SUBDIR)
        STRING_PATH = directory.resolve(STRINGS_SUBDIR)
        PLUGIN_FILES =
            if (Files.exists(directory)) Files.list(directory).filter { path: Path? -> GLOB_PLUGIN.matches(path) }
                .collect(Collectors.toList()) else emptyList()
        ARCHIVE_FILES =
            if (Files.exists(directory)) Files.list(directory).filter { path: Path? -> GLOB_ARCHIVE.matches(path) }
                .collect(Collectors.toList()) else emptyList()
        STRINGS_FILES =
            if (Files.exists(STRING_PATH)) Files.walk(STRING_PATH).filter { path: Path? -> GLOB_STRINGS.matches(path) }
                .collect(Collectors.toList()) else emptyList()
        SCRIPT_FILES =
            if (Files.exists(SCRIPT_PATH)) Files.walk(SCRIPT_PATH).filter { path: Path? -> GLOB_SCRIPT.matches(path) }
                .collect(Collectors.toList()) else emptyList()

        // Print out some status information.
        if (!Files.exists(directory)) {
            LOG.warning(String.format("Mod \"%s\" doesn't exist.", MODNAME))
        } else {
            if (PLUGIN_FILES.isEmpty()) {
                LOG.fine(String.format("Mod \"%s\" contains no plugins.", MODNAME))
            } else {
                LOG.fine(String.format("Mod \"%s\" contains %d plugins.", MODNAME, PLUGIN_FILES.size))
            }
            if (ARCHIVE_FILES.isEmpty()) {
                LOG.fine(String.format("Mod \"%s\" contains no archives.", MODNAME))
            } else {
                LOG.fine(String.format("Mod \"%s\" contains %d archives.", MODNAME, ARCHIVE_FILES.size))
            }
            if (STRINGS_FILES.isEmpty()) {
                LOG.fine(String.format("Mod \"%s\" contains no loose localization files.", MODNAME))
            } else {
                LOG.fine(String.format("Mod \"%s\" contains %d loose localization files.", MODNAME, STRINGS_FILES.size))
            }
            if (SCRIPT_FILES.isEmpty()) {
                LOG.fine(String.format("Mod \"%s\" contains no loose scripts.", MODNAME))
            } else {
                LOG.fine(String.format("Mod \"%s\" contains %d loose scripts.", MODNAME, SCRIPT_FILES.size))
            }
        }
    }
}