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
import java.util.stream.Collectors

/**
 * Stores somethin'.
 *
 * @author Mark Fairchild
 */
class PluginData(plugin: Plugin) {
    /**
     * Adds an entry for a record.
     *
     * @param formID The formId of the record.
     * @param fields The record's fields, which will be sampled for suitable
     * names.
     */
    fun addRecord(formID: Int, fields: FieldList) {
        val INFO = Info(fields)
        MAP[formID] = INFO
    }

    /**
     * Calculates the script data size and adds it.
     * @param script
     */
    fun addScriptData(script: Script) {
        scriptDataSize += script.calculateSize().toLong()
    }

    /**
     * @return The number of stored names.
     */
    val nameCount: Long
        get() = MAP.size.toLong()

    /**
     * Adds all of the entries from on `PluginData` to another.
     *
     * @param other
     */
    fun addAll(other: PluginData) {
        MAP.putAll(other.MAP)
        scriptDataSize += other.scriptDataSize
    }

    /**
     *
     * @param searchTerm
     * @param strings
     * @return
     */
    fun getID(searchTerm: String, strings: StringTable?): Set<Int> {
        return TreeSet(
            MAP.keys
                .stream()
                .filter { id: Int -> searchTerm.equals(getName(id, strings), ignoreCase = true) }
                .collect(Collectors.toSet())
        )
    }

    /**
     * Tries to retrieve a suitable name for a formID.
     *
     * @param formID
     * @param strings The StringTable.
     * @return
     */
    fun getName(formID: Int, strings: StringTable?): String? {
        if (!MAP.containsKey(formID)) {
            return null
        }
        val INFO = MAP[formID]
        if (INFO!!.FULL != null) {
            if (INFO.FULL?.hasString() == true) {
                return INFO.FULL.string
            } else if (INFO.FULL!!.hasIndex() && null != strings) {
                val index = INFO.FULL.index
                val lookup = strings[ESPNAME, index]
                if (lookup != null) {
                    return lookup
                }
            }
        }
        if (INFO.NAME != null) {
            val baseID = INFO.NAME.formID
            assert(baseID != formID)
            val baseName = getName(baseID, strings)
            if (null != baseName) {
                return baseName
            }
        }
        return if (null != INFO.EDID) {
            INFO.EDID.value
        } else null
    }

    /**
     * @see Object.toString
     * @return
     */
    override fun toString(): String {
        return ESPNAME.toString()
    }

    /**
     * The actual mapping.
     */
    private val MAP: MutableMap<Int, Info>

    /**
     * The name of the map.
     */
    private val ESPNAME: Plugin
    /**
     * @return The size of the plugin's script data.
     */
    /**
     * Size of the script data.
     */
    var scriptDataSize: Long
        private set

    /**
     * Stores ID information.
     */
    private class Info(fields: FieldList) {
        val FULL: FieldFull?
        val NAME: FieldName?
        val EDID: FieldEDID?

        init {
            var edid: FieldEDID? = null
            var name: FieldName? = null
            var full: FieldFull? = null
            for (field in fields) {
                if (field is FieldFull) {
                    full = field
                } else if (field is FieldName) {
                    name = field
                } else if (field is FieldEDID) {
                    edid = field
                }
            }
            EDID = edid
            NAME = name
            FULL = full
        }
    }

    /**
     * Creates a new `ESPInfo`.
     * Not threadsafe.
     *
     * @param plugin The name of the ESP.
     */
    init {
        MAP = HashMap()
        ESPNAME = plugin
        scriptDataSize = 0
    }
}