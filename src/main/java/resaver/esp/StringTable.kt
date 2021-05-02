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

import resaver.ess.Plugin
import java.util.*
import java.util.regex.Pattern

/**
 * A StringTable stores reads and stores strings from the mod stringtables;
 * mostly just applies to Skyrim.esm and the DLCs.
 *
 * @author Mark Fairchild
 */
class StringTable {
    /**
     *
     * @param stringsFiles
     * @param plugin
     */
    fun populateFromFiles(stringsFiles: Collection<StringsFile>, plugin: Plugin) {
        Objects.requireNonNull(stringsFiles)
        val SUBTABLE = TABLE.computeIfAbsent(plugin) { p: Plugin? -> HashMap() }
        stringsFiles.stream().forEach { stringsFile: StringsFile -> SUBTABLE.putAll(stringsFile.TABLE) }
    }

    /**
     * Retrieves a string using its formid.
     *
     * @param plugin
     * @param stringID
     * @return
     */
    operator fun get(plugin: Plugin?, stringID: Int): String? {
        return TABLE.getOrDefault(plugin, emptyMap<Int, String>())[stringID]
    }

    /**
     * The reference for accessing the stringtable.
     */
    val TABLE: MutableMap<Plugin, MutableMap<Int?, String?>>

    private enum class Type(regex: Pattern) {
        STRINGS(
            Pattern.compile(
                ".+\\.STRINGS$",
                Pattern.CASE_INSENSITIVE
            )
        ),
        ILSTRINGS(Pattern.compile(".+\\.ILSTRINGS$", Pattern.CASE_INSENSITIVE)), DLSTRINGS(
            Pattern.compile(".+\\.DLSTRINGS$", Pattern.CASE_INSENSITIVE)
        );

        val REGEX: Pattern

        companion object {
            fun match(filename: String?): Type? {
                if (STRINGS.REGEX.asPredicate().test(filename)) {
                    return STRINGS
                }
                if (ILSTRINGS.REGEX.asPredicate().test(filename)) {
                    return ILSTRINGS
                }
                return if (DLSTRINGS.REGEX.asPredicate().test(filename)) {
                    DLSTRINGS
                } else null
            }
        }

        init {
            REGEX = Objects.requireNonNull(regex)
        }
    }

    init {
        TABLE = HashMap()
    }
}