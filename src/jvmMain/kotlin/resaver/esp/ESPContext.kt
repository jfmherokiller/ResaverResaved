/*
 * Copyright 2016 Mark Fairchild
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

import resaver.IString
import ess.Plugin

/**
 * Stores the information that ESP elements require to read and write themselves
 * to and from files.
 *
 * SAMPLE checkpointer:
 * ctx.check("Skyrim.esm", "MGEF", "0010fc14", "VMAD");
 *
 *
 * @author Mark Fairchild
 */
class ESPContext(game: resaver.Game, plugin: Plugin, tes4: RecordTes4?) {
    fun pushContext(ctx: CharSequence) {
        CONTEXT.addLast(IString[ctx.toString()])
    }

    fun popContext() {
        CONTEXT.removeLast()
    }

    fun check(vararg levels: String): Boolean {
        var matches = 0
        for (l: String in levels) {
            val level = IString[l]
            if (!CONTEXT.contains(level)) {
                return false
            } else {
                matches++
            }
        }
        return true
    }

    /**
     * Remaps formIDs. If the formID's master is not available, the plugin field
     * of the formid will be set to 255.
     *
     * @param id The ID to remap.
     * @return
     */
    fun remapFormID(id: Int): Int {
        return TES4?.remapFormID(id, this) ?: id
    }

    override fun toString(): String {
        return CONTEXT.toString()
    }


    val GAME: resaver.Game = game


    val TES4: RecordTes4? = tes4
    private val CONTEXT: ArrayDeque<IString> = ArrayDeque()


    val PLUGIN_INFO: PluginData = PluginData(plugin)

    /**
     * Create a new `ESSContext` from an ESS `Header`.
     *
     * @param game
     * @param plugin
     * @param tes4
     */
    init {
        pushContext(plugin.NAME)
    }
}