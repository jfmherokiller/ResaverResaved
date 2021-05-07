/*
 * Copyright 2020 Mark.
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
import resaver.ResaverFormatting
import java.nio.ByteBuffer


/**
 * Describes 3-byte formIDs from Skyrim savegames.
 *
 * @author Mark Fairchild
 */
class RefID internal constructor(private val DATA: Int, ess: ess.ESS) : Element, Linkable, Comparable<RefID?> {
    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer?) {
        output!!.put((DATA shr 16).toByte())
        output.put((DATA shr 8).toByte())
        output.put((DATA shr 0).toByte())
    }

    /**
     * @see resaver.ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        return 3
    }

    /**
     * @return The type of RefID.
     */
    val type: Type
        get() {
            return when (0x3 and DATA ushr 22) {
                0 -> Type.FORMIDX
                1 -> Type.DEFAULT
                2 -> Type.CREATED
                3 -> Type.INVALID
                else -> Type.INVALID
            }
        }

    /**
     * @return The value portion of the RefID, which is guaranteed to be non-negative.
     */
    private val valPart: Int
        get() = DATA and 0x3FFFFF

    /**
     * @return A flag indicating if the RefID is zero.
     */
    val isZero: Boolean
        get() = this.valPart == 0

    /**
     * Adds the EDID/FULL field for the RefID.
     *
     * @param analysis The analysis data.
     */
    fun addNames(analysis: Analysis) {
        name = analysis.getName(PLUGIN, FORMID)
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        return when {
            FORMID == 0 -> {
                "$type:${ResaverFormatting.zeroPad6(DATA)}"
            }
            null != name -> {
                "${ResaverFormatting.zeroPad8(FORMID)} ($name)"
            }
            else -> {
                ResaverFormatting.zeroPad8(FORMID)
            }
        }
    }

    /**
     * @see resaver.ess.Linkable.toHTML
     * @param target A target within the `Linkable`.
     * @return
     */
    override fun toHTML(target: Element?): String {
        val HEX = String.format("%06x", DATA)
        return Linkable.makeLink("refid", HEX, this.toString())
    }

    override fun compareTo(other: RefID?): Int {
        return Integer.compareUnsigned(DATA, other?.DATA ?: 0)
    }

    override fun hashCode(): Int {
        return Integer.hashCode(DATA)
    }

    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> {
                true
            }
            other == null -> {
                false
            }
            else -> {
                other is RefID && other.DATA == DATA
            }
        }
    }

    @JvmField
    var FORMID = 0
    @JvmField
    var PLUGIN: Plugin? = null

    /**
     * @return The name field, if any.
     */
    var name: String?
        private set

    /**
     * The four types of RefIDs.
     */
    enum class Type {
        FORMIDX, DEFAULT, CREATED, INVALID
    }

    /**
     * Creates a new `RefID` directly.
     *
     * @param newData
     * @param ess The savefile for context.
     */
    init {
        if (isZero) {
            FORMID = 0
            PLUGIN = null
        } else {
            val PLUGINS = ess.pluginInfo
            when (type) {
                Type.DEFAULT -> {
                    PLUGIN = PLUGINS.fullPlugins[0]
                    FORMID = valPart
                }
                Type.CREATED -> {
                    FORMID = -0x1000000 or valPart
                    PLUGIN = null
                }
                Type.FORMIDX -> {
                    assert(valPart > 0) { "Invalid form index: $valPart" }
                    val FORM_INDEX = valPart - 1
                    if (FORM_INDEX < ess.formIDs.size) {
                        FORMID = ess.formIDs[FORM_INDEX]
                        PLUGIN = ess.getPluginFor(FORMID)
                    } else {
                        FORMID = -1
                        PLUGIN = null
                    }
                }
                else -> {
                    FORMID = 0
                    PLUGIN = null
                }
            }
        }
        name = if (ess.analysis == null) null else ess.analysis.getName(PLUGIN, FORMID)
    }
}