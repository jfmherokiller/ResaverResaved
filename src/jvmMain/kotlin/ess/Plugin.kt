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

import resaver.Analysis
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.*


/**
 * Abstraction for plugins.
 *
 * @author Mark Fairchild
 */
class Plugin private constructor(name: String, index: Int, lightweight: Boolean) : Element, AnalyzableElement, Linkable,
    Comparable<Plugin?> {
    /**
     * @see Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer?) {
        if (output != null) {
            mf.BufferUtil.putWString(output, NAME)
        }
    }

    /**
     * @see Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        return 2 + NAME.toByteArray(StandardCharsets.ISO_8859_1).size
    }

    /**
     * @see Linkable.toHTML
     * @param target A target within the `Linkable`.
     * @return
     */
    override fun toHTML(target: Element?): String {
        return Linkable.makeLink("plugin", NAME, this.toString())
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        return NAME
    }

    /**
     * @return String representation.
     */
    fun indexName(): String {
        return if (LIGHTWEIGHT) String.format("FE%03x: $NAME", INDEX) else String.format("%02x: $NAME", INDEX)
    }

    /**
     * Finds all of the changeforms associated with this Plugin.
     *
     * @param save The savefile to search.
     * @return A set of changeforms.
     */
    fun getChangeForms(save: ESS?): Set<ChangeForm> {
        return save!!.changeForms.values
            .filter { form: ChangeForm? -> form?.refID?.PLUGIN === this }.toList().filterNotNull().toSet()
    }

    /**
     * Finds all of the scriptinstances associated with this Plugin.
     *
     * @param save The savefile to search.
     * @return A set of scriptinstances.
     */
    fun getInstances(save: ESS?): Set<ess.papyrus.ScriptInstance> {
        val INSTANCES: Set<ess.papyrus.ScriptInstance> = save!!.papyrus?.scriptInstances?.values
            ?.filter { instance: ess.papyrus.ScriptInstance -> instance.refID != null }
            ?.filter { instance: ess.papyrus.ScriptInstance -> instance.refID.PLUGIN === this }?.toList()!!.toSet()
        return INSTANCES
    }

    /**
     * @see AnalyzableElement.getInfo
     * @param analysis
     * @param save
     * @return
     */
    override fun getInfo(analysis: Analysis?, save: ESS?): String {
        val BUILDER: StringBuilder = StringBuilder()
        if (LIGHTWEIGHT) {
            BUILDER.append("<html><h3>LITE PLUGIN</h3>")
            BUILDER.append("<p>Name: ").append(NAME).append("</p>")
            BUILDER.append("<p>Index: FE:").append(INDEX).append("</p>")
        } else {
            BUILDER.append("<html><h3>FULL PLUGIN</h3>")
            BUILDER.append("<p>Name: ").append(NAME).append("</p>")
            BUILDER.append("<p>Index: ").append(INDEX).append("</p>")
        }
        val FORMS: Set<ChangeForm> = getChangeForms(save)
        val INSTANCES: Set<ess.papyrus.ScriptInstance> = getInstances(save)
        BUILDER.append("<p>").append(FORMS.size).append(" ChangeForms.</p>")
        if (FORMS.size < 100) {
            BUILDER.append("<ul>")
            FORMS.forEach { form: ChangeForm -> BUILDER.append("<li>").append(form.toHTML(null)) }
            BUILDER.append("</ul>")
        }
        BUILDER.append("<p>").append(INSTANCES.size).append(" ScriptInstances.</p>")
        if (INSTANCES.size < 100) {
            BUILDER.append("<ul>")
            INSTANCES.forEach { instance: ess.papyrus.ScriptInstance ->
                BUILDER.append("<li>").append(instance.toHTML(null))
            }
            BUILDER.append("</ul>")
        }
        if (null != analysis) {
            val PROVIDERS: MutableList<String> = mutableListOf()
            val espFilter = { esp: String -> esp.equals(NAME, ignoreCase = true) }
            analysis.ESPS.forEach { (mod: String, esps: Set<String>) ->
                esps.sorted().filter(espFilter).forEach { esp: String? -> PROVIDERS.add(mod) }
            }
            if (PROVIDERS.isNotEmpty()) {
                val probableProvider: String = PROVIDERS[PROVIDERS.size - 1]
                val modFilter = { e: Set<String?> -> e.contains(probableProvider) }
                val numScripts: Int = analysis.SCRIPT_ORIGINS.values.filter(modFilter).count()
                BUILDER.append(String.format("<p>%d scripts.</p>", numScripts))
                BUILDER.append("<p>The plugin probably came from mod \"$probableProvider\".</p>")
                if (PROVIDERS.size > 1) {
                    BUILDER.append("<p>Full list of providers:</p><ul>")
                    PROVIDERS.forEach { mod: String? -> BUILDER.append("<li>$mod") }
                    BUILDER.append("</ul>")
                }
            }
        }
        BUILDER.append("</html>")
        return BUILDER.toString()
    }

    /**
     * @see AnalyzableElement.matches
     * @param analysis
     * @param mod
     * @return
     */
    override fun matches(analysis: Analysis?, mod: String?): Boolean {
        val filter = { esp: String -> esp.equals(NAME, ignoreCase = true) }
        val PROVIDERS: MutableList<String?> = mutableListOf()
        analysis!!.ESPS.forEach { (m: String?, esps: Set<String>) ->
            esps.sorted().filter(filter).forEach { esp: String? -> PROVIDERS.add(m) }
        }
        return PROVIDERS.contains(mod)
    }

    /**
     * The name field.
     */
    val NAME: String = name

    /**
     * The index field.
     */
    val INDEX: Int = index

    /**
     *
     */
    val LIGHTWEIGHT: Boolean = lightweight

    /**
     * @see Comparable.compareTo
     * @param other
     * @return
     */
    override fun compareTo(other: Plugin?): Int {
        if (null == other) {
            return 1
        }
        return NAME.compareTo(other.NAME, ignoreCase = true)
    }

    /**
     * @see Object.hashCode
     * @return
     */
    override fun hashCode(): Int {
        var hash = 3
        hash = 29 * hash + (NAME.lowercase(Locale.getDefault())).hashCode()
        return hash
    }

    /**
     * @see Object.equals
     * @return
     */
    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> {
                true
            }
            other == null -> {
                false
            }
            javaClass != other.javaClass -> {
                false
            }
            else -> {
                val other2: Plugin = other as Plugin
                NAME.equals(other2.NAME, ignoreCase = true)
            }
        }
    }

    companion object {

        var PROTOTYPE: Plugin = Plugin("Unofficial Skyrim Legendary Edition Patch", -1, false)

        /**
         * Creates a new `Plugin` by reading from an input stream.
         *
         * @param input The input stream.
         * @param index The index of the plugin.
         * @return The new Plugin.
         */
        fun readFullPlugin(input: ByteBuffer?, index: Int): Plugin {
            if (index < 0 || index > 255) {
                throw IllegalArgumentException("Invalid index: $index")
            }
            val name: String = input?.let { mf.BufferUtil.getWString(it).toString() }!!
            return Plugin(name, index, false)
        }

        /**
         * Creates a new `Plugin` by reading from an input stream.
         *
         * @param input The input stream.
         * @param index The index of the plugin.
         * @return The new Plugin.
         * @throws IOException
         */
        @JvmStatic
        @Throws(IOException::class)
        fun readLitePlugin(input: ByteBuffer?, index: Int): Plugin {
            if (index < 0 || index >= 4096) {
                throw IllegalArgumentException("Invalid index: $index")
            }
            val name: String = input?.let { mf.BufferUtil.getWString(it) }!!
            return Plugin(name, index, true)
        }
    }

}