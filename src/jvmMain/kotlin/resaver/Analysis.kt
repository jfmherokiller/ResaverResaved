/*
 * Copyright 2017 Mark Fairchild.
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

import resaver.esp.StringTable
import ess.Plugin
/**
 * Combines the results of script analysis and ESP analysis.
 *
 * @author Mark Fairchild
 */
class Analysis(profileAnalysis: resaver.Mod.Analysis, espInfos: MutableMap<Plugin?, resaver.esp.PluginData>, strings: StringTable) :
    resaver.Mod.Analysis() {
    fun getName(plugin: Plugin?, formID: Int): String? {
        return if (ESP_INFOS.containsKey(plugin)) ESP_INFOS[plugin]!!.getName(formID, STRINGS) else null
    }

    fun find(searchTerm: String?): Set<Int> {
        return ESP_INFOS.values
            .map { v: resaver.esp.PluginData -> v.getID(searchTerm!!, STRINGS) }
            .filter { obj: Set<Int>? -> obj.isNullOrEmpty() }
            .flatten().toSet()
    }

    val scriptDataSize: Long
        get() = ESP_INFOS.values.sumOf { obj: resaver.esp.PluginData -> obj.scriptDataSize }
    val ESP_INFOS: MutableMap<Plugin?, resaver.esp.PluginData> = espInfos
    val STRINGS: StringTable = strings

    companion object {
        /**
         * For serialization.
         */
        private const val serialVersionUID = 0x171f2b1L
    }

    /**
     * Creates a new `Analysis`.
     *
     * @param profileAnalysis
     * @param espInfos
     * @param strings
     */
    init {
        MODS.addAll(profileAnalysis.MODS)
        SCRIPTS.putAll(profileAnalysis.SCRIPTS)
        ESPS.putAll(profileAnalysis.ESPS)
        SCRIPT_ORIGINS.putAll(profileAnalysis.SCRIPT_ORIGINS)
    }
}