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

import ess.Plugin.Companion.readFullPlugin
import ess.Plugin.Companion.readLitePlugin
import java.nio.ByteBuffer
import java.nio.file.Path
import java.nio.file.Paths


import java.util.stream.Stream
import kotlin.streams.toList

/**
 * Describes a savegame's list of plugins.
 *
 * @author Mark Fairchild
 */
class PluginInfo(input: ByteBuffer, supportsESL: Boolean) : Element {
    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer?) {
        output!!.putInt(calculateSize() - 4)
        output.put(PLUGINS_FULL!!.size.toByte())
        PLUGINS_FULL.forEach { p: Plugin -> p.write(output) }
        if (hasLite()) {
            output.putShort(PLUGINS_LITE!!.size.toShort())
            PLUGINS_LITE!!.forEach { p: Plugin -> p.write(output) }
        }
    }

    /**
     * @see resaver.ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        var sum = 4
        sum += 1
        sum += PLUGINS_FULL!!.sumBy { obj: Plugin -> obj.calculateSize() }
        if (hasLite()) {
            sum += 2
            sum += PLUGINS_LITE!!.sumBy { obj: Plugin -> obj.calculateSize() }
        }
        return sum
    }

    /**
     * @return The list of all plugins.
     */
    val allPlugins: List<Plugin>
        get() = stream().toList()

    /**
     * @return The list of all plugins.
     */
    fun stream(): Stream<Plugin> {
        return Stream.concat(fullPlugins.stream(), litePlugins.stream())
    }

    /**
     * @return A flag indicating whether there is a lightweight plugin table.
     */
    fun hasLite(): Boolean {
        return null != PLUGINS_LITE
    }

    /**
     * @return The list of plugins.
     */
    val fullPlugins: List<Plugin>
        get() = PLUGINS_FULL.orEmpty()

    /**
     * @return The list of lightweight plugins.
     */
    val litePlugins: List<Plugin>
        get() = if (hasLite()) {
            PLUGINS_LITE.orEmpty()
        } else {
            emptyList()
        }

    /**
     * @return The total number of lite and full plugins.
     */
    val size: Int
        get() = fullPlugins.size + litePlugins.size

    /**
     * Creates a string representation of the `PluginInfo`.
     *
     * @see Object.toString
     * @return
     */
    override fun toString(): String {
        if (null == PLUGINS_FULL) {
            return "NOT INITIALIZED"
        }
        val BUF = StringBuilder()
        BUF.append(String.format("%d plugins", fullPlugins.size))
        if (hasLite()) {
            BUF.append(String.format(", %d lightweight plugins", litePlugins.size))
        }
        return BUF.toString()
    }

    /**
     * @see Object.hashCode
     * @return
     */
    override fun hashCode(): Int {
        var hash = 7
        hash = 89 * hash + PLUGINS_FULL.hashCode()
        hash = 89 * hash + PLUGINS_LITE.hashCode()
        return hash
    }

    /**
     * @see Object.equals
     * @return
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }
        val other2 = other as PluginInfo
        return PLUGINS_FULL?.toTypedArray().contentDeepEquals(other2.PLUGINS_FULL?.toTypedArray())
                && PLUGINS_LITE?.toTypedArray().contentDeepEquals(other2.PLUGINS_LITE?.toTypedArray())
    }

    private val PLUGINS_FULL: MutableList<Plugin>?
    private var PLUGINS_LITE: MutableList<Plugin>? = null

    /**
     * @return A `Map` for matching a `Path` to a
     * corresponding `Plugin`.
     */
    val paths: Map<Path, Plugin>

    companion object {
        /**
         * Make an absolute formID for a plugin. Any existing plugin info in the
         * formID will be discarded.
         *
         * @param plugin
         * @param id
         * @return
         */
        @JvmStatic
        fun makeFormID(plugin: Plugin, id: Int): Int {
            return if (plugin.LIGHTWEIGHT) {
                -0x2000000 or (plugin.INDEX shl 12) or (id and 0xfff)
            } else {
                plugin.INDEX shl 24 or (id and 0xffffff)
            }
        }
    }

    /**
     * Creates a new `PluginInfo` by reading from a
     * `ByteBuffer`. No error handling is performed.
     *
     * @param input The input stream.
     * @param supportsESL Whether to load a lightweight plugins table.
     * @throws IOException
     */
    init {
        val pluginInfoSize = input.int
        val numberOfFull = java.lang.Byte.toUnsignedInt(input.get())
        require(!(numberOfFull < 0 || numberOfFull >= 256)) { "Invalid full plugin count: $numberOfFull" }
        PLUGINS_FULL = mutableListOf()
        for (i in 0 until numberOfFull) {
            val p = readFullPlugin(input, i)
            PLUGINS_FULL.add(p)
        }
        if (supportsESL) {
            val numberOfLite = input.short.toInt()
            require(!(numberOfLite < 0 || numberOfLite >= 4096)) { "Invalid lite plugin count: $numberOfLite" }
            PLUGINS_LITE = mutableListOf()
            for (i in 0 until numberOfLite) {
                val p = readLitePlugin(input, i)
                PLUGINS_LITE!!.add(p)
            }
        } else {
            PLUGINS_LITE = null
        }
        paths = stream().toList().associateBy { p -> Paths.get(p.NAME) }
        check(pluginInfoSize + 4 == calculateSize()) {
            String.format(
                "PluginInfoSize = %d, but read %d",
                pluginInfoSize,
                calculateSize()
            )
        }
    }
}