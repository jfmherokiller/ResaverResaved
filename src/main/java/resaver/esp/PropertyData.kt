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

import mf.BufferUtil
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.function.Consumer
import java.util.function.Supplier
import java.util.stream.Collectors

/**
 * Stores the data for a script property. Basically a fancy VarArg.
 *
 * @author Mark Fairchild
 */
abstract class PropertyData : Entry {
    /**
     * Stores a Null property data.
     *
     */
    class NullData(input: ByteBuffer?) : PropertyData() {
        override fun write(output: ByteBuffer) {
            //output.putInt(DATA);
        }

        override fun calculateSize(): Int {
            return 0
        }

        override fun toString(): String {
            return "NULL"
        } //final private int DATA;
    }

    /**
     * Stores an Object property data.
     */
    class ObjectData(input: ByteBuffer) : PropertyData() {
        override fun write(output: ByteBuffer) {
            output.putLong(DATA)
        }

        override fun calculateSize(): Int {
            return 8
        }

        override fun toString(): String {
            return String.format("%08x", DATA)
        }

        private val DATA: Long

        init {
            DATA = input.long
        }
    }

    /**
     * Stores a String property data.
     */
    class StringData(input: ByteBuffer?) : PropertyData() {
        override fun write(output: ByteBuffer) {
            output.put(DATA.toByteArray(StandardCharsets.UTF_8))
        }

        override fun calculateSize(): Int {
            return 2 + DATA.length
        }

        override fun toString(): String {
            return DATA
        }

        private val DATA: String

        init {
            DATA = BufferUtil.getUTF(input)
        }
    }

    /**
     * Stores an integer property data.
     */
    class IntData(input: ByteBuffer) : PropertyData() {
        override fun write(output: ByteBuffer) {
            output.putInt(DATA)
        }

        override fun calculateSize(): Int {
            return 4
        }

        override fun toString(): String {
            return DATA.toString()
        }

        private val DATA: Int

        init {
            DATA = input.int
        }
    }

    /**
     * Stores a float property data.
     */
    class FloatData(input: ByteBuffer) : PropertyData() {
        override fun write(output: ByteBuffer) {
            output.putFloat(DATA)
        }

        override fun calculateSize(): Int {
            return 4
        }

        override fun toString(): String {
            return java.lang.Float.toString(DATA)
        }

        private val DATA: Float

        init {
            DATA = input.float
        }
    }

    /**
     * Stores a boolean property data.
     */
    class BoolData(input: ByteBuffer) : PropertyData() {
        override fun write(output: ByteBuffer) {
            output.put(if (DATA) 1.toByte() else 0.toByte())
        }

        override fun calculateSize(): Int {
            return 1
        }

        override fun toString(): String {
            return java.lang.Boolean.toString(DATA)
        }

        private val DATA: Boolean

        init {
            DATA = input.get().toInt() != 0
        }
    }

    /**
     * Stores a variant property data.
     */
    class VarData(input: ByteBuffer) : PropertyData() {
        override fun write(output: ByteBuffer) {
            output.putInt(DATA)
        }

        override fun calculateSize(): Int {
            return 4
        }

        override fun toString(): String {
            return String.format("VAR: %s", DATA)
        }

        private val DATA: Int

        init {
            DATA = input.int
        }
    }

    /**
     * Stores a struct property data.
     */
    class StructData(input: ByteBuffer, ctx: ESPContext?) : PropertyData() {
        override fun write(output: ByteBuffer) {
            output.putInt(MEMBERS.size)
            MEMBERS.forEach(Consumer { p: Property -> p.write(output) })
        }

        override fun calculateSize(): Int {
            return 4 + MEMBERS.stream().mapToInt { v: Property -> v.calculateSize() }.sum()
        }

        override fun toString(): String {
            return MEMBERS.stream()
                .map { v: Property -> v.toString() }
                .collect(Collectors.joining("; ", "{", "}"))
        }

        private val MEMBERS: MutableList<Property>

        init {
            val memberCount = input.int
            MEMBERS = ArrayList(memberCount)
            for (i in 0 until memberCount) {
                val p = Property(input, ctx!!)
                MEMBERS.add(p)
            }
        }
    }

    /**
     * Stores an Array property data.
     *
     * @param <T> The type of PropertyData stored in the array.
    </T> */
    class ArrayData<T : PropertyData?>(input: ByteBuffer, reader: Supplier<T>) : PropertyData() {
        override fun write(output: ByteBuffer) {
            output.putInt(MEMBERS.size)
            MEMBERS.forEach(Consumer { t: T -> t!!.write(output) })
        }

        override fun calculateSize(): Int {
            var sum = 4
            sum += MEMBERS.stream().mapToInt { t: T -> t!!.calculateSize() }.sum()
            return sum
        }

        override fun toString(): String {
            return MEMBERS.stream()
                .map { v: T -> v.toString() }
                .collect(Collectors.joining(", ", "[", "]"))
        }

        private val MEMBERS: MutableList<T>

        init {
            val memberCount = input.int
            MEMBERS = ArrayList(memberCount)
            for (i in 0 until memberCount) {
                val member = reader.get()
                MEMBERS.add(member)
            }
        }
    }

    companion object {
        fun readPropertyData(type: Byte, input: ByteBuffer, ctx: ESPContext?): PropertyData {
            assert(input.hasRemaining() || type.toInt() == 0) { "No input available, type = $type" }
            return when (type.toInt()) {
                0 -> NullData(input)
                1 -> ObjectData(input)
                2 -> StringData(input)
                3 -> IntData(input)
                4 -> FloatData(input)
                5 -> BoolData(input)
                6 -> VarData(input)
                7 -> StructData(input, ctx)
                11 -> ArrayData(input) { ObjectData(input) }
                12 -> ArrayData(input) { StringData(input) }
                13 -> ArrayData(
                    input
                ) { IntData(input) }
                14 -> ArrayData(
                    input
                ) { FloatData(input) }
                15 -> ArrayData(input) { BoolData(input) }
                16 -> ArrayData(input) { VarData(input) }
                17 -> ArrayData(
                    input
                ) { StructData(input, ctx) }
                else -> throw IllegalStateException(String.format("Invalid property type: %d", type))
            }
        }
    }
}