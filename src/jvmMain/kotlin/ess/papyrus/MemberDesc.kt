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
package ess.papyrus

import resaver.ListException
import java.nio.ByteBuffer


/**
 * Describes a script member in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
open class MemberDesc(input: ByteBuffer?, context: PapyrusContext?) : PapyrusElement, Comparable<MemberDesc> {
    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer?) {
        name.write(output)
        type.write(output)
    }

    /**
     * @see resaver.ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        return name.calculateSize() + type.calculateSize()
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        return "$type $name"
    }

    override fun compareTo(other: MemberDesc): Int {
        return TString.compare(name, other.name)
    }

    /**
     * @return The ID of the papyrus element.
     */
    val name: TString = input?.let { context?.readTString(it) } ?: TString.makeUnindexed("")

    /**
     * @return The type of the array.
     */
    val type: TString = input?.let { context?.readTString(it) } ?: TString.makeUnindexed("")

    companion object {
        /**
         * Creates a new `List` of `Variable` by reading from
         * a `ByteBuffer`.
         *
         * @param input The input stream.
         * @param count The number of variables.
         * @param context The `PapyrusContext` info.
         * @return The new `List` of `MemberDesc`.
         * @throws ListException
         */
        @Throws(ListException::class)
        fun readList(input: ByteBuffer?, count: Int, context: PapyrusContext): List<MemberDesc> {
            val DESCS: MutableList<MemberDesc> = mutableListOf()
            for (i in 0 until count) {
                try {
                    val desc = MemberDesc(input, context)
                    DESCS.add(desc)
                } catch (ex: PapyrusFormatException) {
                    throw ListException(i, count, ex)
                }
            }
            return DESCS
        }
    }

}