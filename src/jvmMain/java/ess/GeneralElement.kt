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
package ess

import ess.ESS.ESSContext
import ess.papyrus.EID
import ess.papyrus.PapyrusContext
import mf.BufferUtil
import resaver.Analysis
import resaver.IString
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.LinkedHashMap
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableMap
import kotlin.collections.Set
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.forEach
import kotlin.collections.set
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

/**
 * A very generalized element. It's not quite as efficient or customizable as
 * other elements, but it's good for elements that can have a range of different
 * members depending on flags.
 *
 * This should generally only be used for elements of which there are not very
 * many.
 *
 * @author Mark Fairchild
 */
open class GeneralElement protected constructor() : Element {
    /**
     *
     * @return The number of sub-elements in the `Element`.
     */
    fun count(): Int {
        return DATA.size
    }//return this.DATA.entrySet().stream()
    //        .collect(Collectors.toMap(IString k -> k.toString(), v -> v));
    /**
     * @return Retrieves a copy of the <name></name>,value> map.
     */
    val values: Map<IString, Any?>
        get() = DATA
    //return this.DATA.entrySet().stream()
    //        .collect(Collectors.toMap(IString k -> k.toString(), v -> v));
    /**
     * Tests whether the `GeneralElement` contains a value for a
     * particular name.
     *
     * @param name The name to search for.
     * @return Retrieves a copy of the <name></name>,value> map.
     */
    fun hasVal(name: Enum<*>): Boolean {
        Objects.requireNonNull(name)
        return this.hasVal(name.toString())
    }

    /**
     * Tests whether the `GeneralElement` contains a value for a
     * particular name.
     *
     * @param name The name to search for.
     * @return Retrieves a copy of the <name></name>,value> map.
     */
    fun hasVal(name: String): Boolean {
        Objects.requireNonNull(name)
        return this.hasVal(IString[name])
    }

    /**
     * Retrieves a value by name.
     *
     * @param name The name to search for.
     * @return Retrieves the value associated with the specified name, or null
     * if there is no match.
     */
    fun getVal(name: String): Any? {
        Objects.requireNonNull(name)
        return this.getVal(IString[name])
    }

    /**
     * Retrieves an `Element` by name.
     *
     * @param name The name to search for.
     * @return Retrieves the value associated with the specified name, or null
     * if there is no match or the match is not an `Element`.
     */
    fun getElement(name: String): Element? {
        Objects.requireNonNull(name)
        val `val` = this.getVal(name)
        return if (`val` is Element) {
            `val`
        } else null
    }

    /**
     * Retrieves a `GeneralElement` by name.
     *
     * @param name The name to search for.
     * @return Retrieves the value associated with the specified name, or null
     * if there is no match or the match is not a `GeneralElement`.
     */
    fun getGeneralElement(name: String): GeneralElement? {
        Objects.requireNonNull(name)
        val `val` = this.getVal(name)
        return if (`val` is GeneralElement) {
            `val`
        } else null
    }

    /**
     * Retrieves a `GeneralElement` by name from an
     * `Enum`.
     *
     * @param name The name to search for.
     * @return Retrieves the value associated with the specified name, or null
     * if there is no match or the match is not a `GeneralElement`.
     */
    fun getElement(name: Enum<*>): Element? {
        return this.getElement(Objects.requireNonNull(name.toString()))
    }

    /**
     * Reads a byte.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The byte.
     */
    fun readByte(input: ByteBuffer, name: String): Byte? {
        Objects.requireNonNull(input)
        Objects.requireNonNull(name)
        val `val` = input.get()
        return addValue(name, `val`)
    }


    /**
     * Reads a short.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The short.
     */
    fun readShort(input: ByteBuffer, name: String): Short? {
        Objects.requireNonNull(input)
        Objects.requireNonNull(name)
        val `val` = input.short
        return addValue(name, `val`)
    }

    /**
     * Reads an int.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The int.
     */
    fun readInt(input: ByteBuffer, name: String): Int? {
        Objects.requireNonNull(input)
        Objects.requireNonNull(name)
        val `val` = input.int
        return addValue(name, `val`)
    }

    /**
     * Reads an long.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The long.
     */
    fun readLong(input: ByteBuffer, name: String): Long? {
        Objects.requireNonNull(input)
        Objects.requireNonNull(name)
        val `val` = input.long
        return addValue(name, `val`)
    }

    /**
     * Reads a float.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The float.
     */
    fun readFloat(input: ByteBuffer, name: String): Float? {
        Objects.requireNonNull(input)
        Objects.requireNonNull(name)
        val `val` = input.float
        return addValue(name, `val`)
    }

    /**
     * Reads a zstring.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The string.
     */
    fun readZString(input: ByteBuffer, name: String): String? {
        Objects.requireNonNull(input)
        Objects.requireNonNull(name)
        val `val` = BufferUtil.getZString(input)!!
        return addValue(name, `val`)
    }

    /**
     * Reads a VSVal.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param reader The element reader.
     * @param <T> The element type.
     * @return The element.
    </T> */
    inline fun <reified T : Element?> readElement(input: ByteBuffer?, name: Enum<*>, reader: ElementReader<T>): T? {
        return this.readElement(input, name.toString(), reader)
    }

    /**
     * Reads a VSVal.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param reader The element reader.
     * @param <T> The element type.
     * @return The element.
    </T> */
    inline fun <reified T : Element?> readElement(input: ByteBuffer?, name: String, reader: ElementReader<T>): T? {
        Objects.requireNonNull(input)
        Objects.requireNonNull(name)
        val element = reader.read(input)
        return addValue(name, element)
    }

    /**
     * Reads a 32bit ID.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param context The Papyrus context data.
     * @return The ID.
     */
    fun readID32(input: ByteBuffer, name: String, context: PapyrusContext): EID? {
        Objects.requireNonNull(input)
        Objects.requireNonNull(name)
        return this.readElement(input, name) { i: ByteBuffer? -> context.readEID32(input) }
    }

    /**
     * Reads a 64bit ID.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param context The Papyrus context data.
     * @return The ID.
     */
    fun readID64(input: ByteBuffer, name: String, context: PapyrusContext): EID? {
        Objects.requireNonNull(input)
        Objects.requireNonNull(name)
        return this.readElement(input, name) { i: ByteBuffer? -> context.readEID64(input) }
    }

    /**
     * Reads a refid.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param context The `ESSContext`.
     * @return The RefID.
     */
    fun readRefID(input: ByteBuffer, name: String, context: ESSContext): RefID? {
        Objects.requireNonNull(input)
        Objects.requireNonNull(name)
        return this.readElement(input, name) { i: ByteBuffer? -> context.readRefID(input) }
    }

    /**
     * Reads a VSVal.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The RefID.
     */
    fun readVSVal(input: ByteBuffer, name: String): VSVal? {
        Objects.requireNonNull(input)
        Objects.requireNonNull(name)
        val `val` = VSVal(input)
        return addValue(name, `val`)
    }

    /**
     * Reads a fixed-length byte array.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param size The size of the array.
     * @return The array.
     */
    fun readBytes(input: ByteBuffer, name: String, size: Int): ByteArray? {
        Objects.requireNonNull(input)
        Objects.requireNonNull(name)
        require(size >= 0) { "Negative array count: $size" }
        require(256 >= size) { "Excessive array count: $size" }
        val `val` = ByteArray(size)
        input[`val`]
        return addValue(name, `val`)
    }

    /**
     * Reads a fixed-length short array.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param size The size of the array.
     * @return The array.
     */
    fun readShorts(input: ByteBuffer, name: String, size: Int): ShortArray? {
        Objects.requireNonNull(input)
        Objects.requireNonNull(name)
        require(size >= 0) { "Negative array count: $size" }
        require(256 >= size) { "Excessive array count: $size" }
        val `val` = ShortArray(size)
        for (i in 0 until size) {
            `val`[i] = input.short
        }
        return addValue(name, `val`)
    }

    /**
     * Reads a fixed-length int array.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param size The size of the array.
     * @return The array.
     */
    fun readInts(input: ByteBuffer, name: String, size: Int): IntArray? {
        Objects.requireNonNull(input)
        Objects.requireNonNull(name)
        require(size >= 0) { "Negative array count: $size" }
        require(256 >= size) { "Excessive array count: $size" }
        val `val` = IntArray(size)
        for (i in 0 until size) {
            `val`[i] = input.int
        }
        return addValue(name, `val`)
    }

    /**
     * Reads a fixed-length long array.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param size The size of the array.
     * @return The array.
     */
    fun readLongs(input: ByteBuffer, name: String, size: Int): LongArray? {
        Objects.requireNonNull(input)
        Objects.requireNonNull(name)
        require(size >= 0) { "Negative array count: $size" }
        require(256 >= size) { "Excessive array count: $size" }
        val `val` = LongArray(size)
        for (i in 0 until size) {
            `val`[i] = input.long
        }
        return addValue(name, `val`)
    }

    /**
     * Reads a fixed-length float array.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param size The size of the array.
     * @return The array.
     */
    fun readFloats(input: ByteBuffer, name: String, size: Int): FloatArray? {
        Objects.requireNonNull(input)
        Objects.requireNonNull(name)
        require(size >= 0) { "Negative array count: $size" }
        require(256 >= size) { "Excessive array count: $size" }
        val `val` = FloatArray(size)
        for (i in 0 until size) {
            `val`[i] = input.float
        }
        return addValue(name, `val`)
    }

    /**
     * Reads a fixed-length element array.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @param size The size of the array.
     * @param reader The element reader.
     * @return The array.
     * @param <T> The element type.
    </T> */
    fun <T : Element?> readElements(
        input: ByteBuffer,
        name: String,
        size: Int,
        reader: ElementReader<T>
    ): MutableList<Element>? {
        Objects.requireNonNull(input)
        Objects.requireNonNull(name)
        Objects.requireNonNull(reader)
        require(size >= 0) { "Negative array count: $size" }
        require(256 >= size) { "Excessive array count: $size" }
        val `val` = mutableListOf<Element>()
        for (i in 0 until size) {
            val element = reader.read(input) as Element
            `val`.add(i, element)
        }
        return addValue(name, `val`)
    }

    /**
     * Reads a fixed-length byte array using a VSVal.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The array.
     */
    fun readBytesVS(input: ByteBuffer, name: String): ByteArray? {
        Objects.requireNonNull(input)
        Objects.requireNonNull(name)
        val COUNT = readVSVal(input, name + "_COUNT")
        if (COUNT != null) {
            require(COUNT.value >= 0) { "Negative array count: $COUNT" }
        }
        return COUNT?.let { this.readBytes(input, name, it.value) }
    }

    /**
     * Reads a fixed-length short array using a VSVal.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The array.
     */
    fun readShortsVS(input: ByteBuffer, name: String): ShortArray? {
        Objects.requireNonNull(input)
        Objects.requireNonNull(name)
        val COUNT = readVSVal(input, name + "_COUNT")
        if (COUNT != null) {
            require(COUNT.value >= 0) { "Negative array count: $COUNT" }
        }
        return COUNT?.let { readShorts(input, name, it.value) }
    }

    /**
     * Reads a fixed-length int array using a VSVal.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The array.
     */
    fun readIntsVS(input: ByteBuffer, name: String): IntArray? {
        Objects.requireNonNull(input)
        Objects.requireNonNull(name)
        val COUNT = readVSVal(input, name + "_COUNT")
        if (COUNT != null) {
            require(COUNT.value >= 0) { "Negative array count: $COUNT" }
        }
        return COUNT?.let { readInts(input, name, it.value) }
    }

    /**
     * Reads a fixed-length long array using a VSVal.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The array.
     */
    fun readLongsVS(input: ByteBuffer, name: String): LongArray? {
        Objects.requireNonNull(input)
        Objects.requireNonNull(name)
        val COUNT = readVSVal(input, name + "_COUNT")
        if (COUNT != null) {
            require(COUNT.value >= 0) { "Negative array count: $COUNT" }
        }
        return COUNT?.let { readLongs(input, name, it.value) }
    }

    /**
     * Reads a fixed-length float array using a VSVal.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The array.
     */
    fun readFloatsVS(input: ByteBuffer, name: String): FloatArray? {
        Objects.requireNonNull(input)
        Objects.requireNonNull(name)
        val COUNT = readVSVal(input, name + "_COUNT")
        if (COUNT != null) {
            require(COUNT.value >= 0) { "Negative array count: $COUNT" }
        }
        return COUNT?.let { readFloats(input, name, it.value) }
    }

    /**
     * Reads an array of elements using a supplier functional.
     *
     * @param input The inputstream.
     * @param reader
     * @param name The name of the new element.
     * @param <T> The element type.
     * @return The array.
    </T> */
    fun <T : Element?> readVSElemArray(input: ByteBuffer, name: String, reader: ElementReader<T>): MutableList<Element>? {
        Objects.requireNonNull(input)
        Objects.requireNonNull(reader)
        Objects.requireNonNull(name)
        val COUNT = readVSVal(input, name + "_COUNT")
        if (COUNT != null) {
            require(COUNT.value >= 0) { "Negative array count: $COUNT" }
        }
        val `val` = mutableListOf<Element>()
        if (COUNT != null) {
            for (i in 0 until COUNT.value) {
                val e = reader.read(input) as Element
                `val`.add(i,e)
            }
        }
        return addValue(name, `val`)
    }

    /**
     * Reads an array of elements using a supplier functional.
     *
     * @param input The inputstream.
     * @param reader
     * @param name The name of the new element.
     * @param <T> The element type.
     * @return The array.
    </T> */
    fun <T : Element?> read32ElemArray(input: ByteBuffer, name: String, reader: ElementReader<T>): MutableList<Element>? {
        Objects.requireNonNull(input)
        Objects.requireNonNull(reader)
        Objects.requireNonNull(name)
        val COUNT = readInt(input, name + "_COUNT")
        if (COUNT != null) {
            require(COUNT >= 0) { "Count is negative: $COUNT" }
        }
        val `val` = mutableListOf<Element>()
        for (i in 0 until COUNT!!) {
            val e = reader.read(input) as Element
            `val`.add(i,e)
        }
        return addValue(name, `val`)
    }

    /**
     * Adds an object value.
     *
     * @param name The name of the new element.
     * @param `val` The value.
     */
    inline fun <reified T: Any> addValue(name: String, `val`: T?): T? {
        var b = false
        var converted:Any? = null
        for (type in SUPPORTED) {
            val casted = type.safeCast(`val`)
            if (casted != null) {
                b = true
                converted = casted
                break
            }
        }
        if (`val` != null) {
            check(b) { String.format("Invalid type for %s: %s", name, `val`::class) }
        }
        DATA[IString[name]] = converted
        return converted as T
    }
//    private fun <T> addValue2(name: String, `val`: T): T {
//        var b = true
//        for (type in SUPPORTED) {
//            if (type.isInstance(`val`)) {
//                b = false
//                break
//            }
//        }
//        check(!b) { String.format("Invalid type for %s: %s", name, `val`.javaClass) }
//        DATA[IString[name]] = `val`
//        return `val`
//    }

    /**
     * @see Element.write
     * @param output output buffer
     */
    override fun write(output: ByteBuffer?) {
        DATA.values.forEach { v: Any? ->
            when (v) {
                is Element -> {
                    v.write(output)
                }
                is Byte -> {
                    output?.put((v as Byte?)!!)
                }
                is Short -> {
                    output?.putShort((v as Short?)!!)
                }
                is Int -> {
                    output?.putInt((v as Int?)!!)
                }
                is Float -> {
                    output?.putFloat((v as Float?)!!)
                }
                is String -> {
                    BufferUtil.putZString(output!!, (v as String?)!!)
                }
                is ByteArray -> {
                    output?.put(v as ByteArray?)
                }
                is ShortArray -> {
                    for (s in v) {
                        output?.putShort(s)
                    }
                }
                is IntArray -> {
                    for (i in v) {
                        output?.putInt(i)
                    }
                }
                is FloatArray -> {
                    for (f in v) {
                        output?.putFloat(f)
                    }
                }
                is Array<*> -> {
                    for (e in v) {
                        (e as Element).write(output)
                    }
                }
                else -> checkNotNull(v) { "Null element!" }
            }
            throw IllegalStateException("Unknown element: " + v.javaClass)
        }
    }

    /**
     * @see Element.calculateSize
     * @return
     */
    override fun calculateSize(): Int {
        if (DATA.containsValue(null)) {
            throw NullPointerException("GeneralElement may not contain null.")
        }
        var sum = 0
        for (v in DATA.values) {
            when (v) {
                is Element -> {
                    sum += v.calculateSize()
                }
                is Byte -> {
                    sum += 1
                }
                is Short -> {
                    sum += 2
                }
                is Int -> {
                    sum += 4
                }
                is Float -> {
                    sum += 4
                }
                is String -> {
                    sum += 1 + v.toByteArray().size
                }
                is ByteArray -> {
                    sum += 1 * v.size
                }
                is ShortArray -> {
                    sum += 2 * v.size
                }
                is IntArray -> {
                    sum += 4 * v.size
                }
                is FloatArray -> {
                    sum += 4 * v.size
                }
                is Array<*> -> {
                    sum += v.map { i:Any? -> i as Element}.sumOf { i -> i.calculateSize() }
                }
                else -> checkNotNull(v) {
                    "Null element!"
                }
            }
                throw IllegalStateException("Unknown element: " + v.javaClass)
        }
        return sum
    }

    /**
     * @see java.lang.Object.hashCode
     * @return
     */
    override fun hashCode(): Int {
        return Objects.hashCode(DATA)
    }

    /**
     * @see java.lang.Object.equals
     * @return
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        } else if (other == null) {
            return false
        } else if (javaClass != other.javaClass) {
            return false
        }
        val other2 = other as GeneralElement
        return DATA == other2.DATA
    }

    /**
     *
     * @return String representation.
     */
    fun toTextBlock(): String {
        val joiner = StringJoiner(", ", "[", "]")
        for (n in DATA.keys) {
            val format = String.format("%s=%s", n, getVal(n))
            joiner.add(format)
        }
        return joiner.toString()
    }

    /**
     * @param level Number of tabs by which to indent.
     * @return String representation.
     */
    open fun toString(level: Int): String {
        return this.toString(null, level)
    }

    /**
     * @param name A name to display.
     * @param level Number of tabs by which to indent.
     * @return String representation.
     */
    protected fun toString(name: String?, level: Int): String {
        val BUF = StringBuilder()
        if (DATA.keys.isEmpty()) {
            indent(BUF, level)
            if (null != name) {
                BUF.append(name)
            }
            BUF.append("{}")
            return BUF.toString()
        }
        indent(BUF, level)
        if (null != name) {
            BUF.append(name)
        }
        BUF.append("{\n")
        DATA.forEach { (key: IString, `val`: Any?) ->
            if (`val` is GeneralElement) {
                val str = `val`.toString(key.toString(), level + 1)
                BUF.append(str)
                BUF.append('\n')
            } else if (`val` is Array<*> && `val`.all { i:Any? -> i is Element? }) {
                val str = `val`.mapNotNull { i: Any? -> i as Element? }
                    .toTypedArray().let { eaToString(key, level + 1, it) }
                BUF.append(str)
                BUF.append('\n')
            } else {
                indent(BUF, level + 1)
                val str: String = when (`val`) {
                    is Byte -> {
                        String.format("%02x", java.lang.Byte.toUnsignedInt((`val` as Byte?)!!))
                    }
                    is Short -> {
                        String.format("%04x", java.lang.Short.toUnsignedInt((`val` as Short?)!!))
                    }
                    is Int -> {
                        String.format("%08x", Integer.toUnsignedLong((`val` as Int?)!!))
                    }
                    is Long -> {
                        String.format("%16x", `val`)
                    }
                    is Array<*> -> {
                        Arrays.toString(`val` as Array<*>?)
                    }
                    is BooleanArray -> {
                        Arrays.toString(`val` as BooleanArray?)
                    }
                    is ByteArray -> {
                        Arrays.toString(`val` as ByteArray?)
                    }
                    is CharArray -> {
                        Arrays.toString(`val` as CharArray?)
                    }
                    is DoubleArray -> {
                        Arrays.toString(`val` as DoubleArray?)
                    }
                    is FloatArray -> {
                        Arrays.toString(`val` as FloatArray?)
                    }
                    is IntArray -> {
                        Arrays.toString(`val` as IntArray?)
                    }
                    is LongArray -> {
                        Arrays.toString(`val` as LongArray?)
                    }
                    is ShortArray -> {
                        Arrays.toString(`val` as ShortArray?)
                    }
                    else -> {
                        Objects.toString(`val`)
                    }
                }
                BUF.append(String.format("%s=%s\n", key, str))
            }
        }
        indent(BUF, level)
        BUF.append("}")
        return BUF.toString()
    }

    /**
     * Tests whether the `GeneralElement` contains a value for a
     * particular name.
     *
     * @param name The name to search for.
     * @return Retrieves a copy of the <name></name>,value> map.
     */
    fun hasVal(name: IString): Boolean {
        Objects.requireNonNull(name)
        return DATA.containsKey(name)
    }

    /**
     * Retrieves a value by name.
     *
     * @param name The name to search for.
     * @return Retrieves the value associated with the specified name, or null
     * if there is no match.
     */
    private fun getVal(name: IString): Any? {
        Objects.requireNonNull(name)
        return DATA[name]
    }

    /**
     * @see AnalyzableElement.getInfo
     * @param analysis
     * @param save
     * @return
     */
    open fun getInfo(analysis: Analysis?, save: ESS?): String {
        val BUF = StringBuilder()
        BUF.append("<table border=1>")
        DATA.forEach { (key: IString, `val`: Any?) ->
            if (`val` is Linkable) {
                val STR = `val`.toHTML(null)
                BUF.append(String.format("<td>%s</td><td>%s</td></tr>", key, STR))
            } else if (`val` is List<*>) {
                val STR = analysis?.let {
                    if (save != null) {
                        formatList(key.toString(), `val`, it, save)
                    }
                }
                BUF.append(String.format("<td>%s</td><td>%s</td></tr>", key, STR))
            } else if (`val` is GeneralElement) {
                val STR = analysis?.let {
                    if (save != null) {
                        formatGeneralElement(key.toString(), `val`, it, save)
                    }
                }
                BUF.append(String.format("<td>%s</td><td>%s</td></tr>", key, STR))
            } else {
                BUF.append(String.format("<td>%s</td><td>%s</td></tr>", key, `val`))
            }
        }
        BUF.append("</table>")
        return BUF.toString()
    }

    /**
     * Stores the actual data.
     */
    public val DATA: MutableMap<IString, Any?>

    companion object {
        /**
         * Appends `n` indents to a `StringBuilder`.
         *
         * @param b
         * @param n
         */
        private fun indent(b: StringBuilder, n: Int) {
            for (i in 0 until n) {
                b.append('\t')
            }
        }

        /**
         * Creates a string representation of an `ElementArrayList`.
         *
         * @param name A name to display.
         * @param level Number of tabs by which to indent.
         * @return String representation.
         */
        private fun eaToString(name: IString?, level: Int, list: Array<Element>): String {
            val BUF = StringBuilder()
            if (list.isEmpty()) {
                indent(BUF, level)
                if (null != name) {
                    BUF.append(name)
                }
                BUF.append("[]")
                return BUF.toString()
            }
            indent(BUF, level)
            if (null != name) {
                BUF.append(name)
            }
            BUF.append("[\n")
            for (e in list) {
                if (e is GeneralElement) {
                    val str = e.toString(level + 1)
                    BUF.append(str).append('\n')
                } else if (e != null) {
                    indent(BUF, level + 1)
                    val str = e.toString()
                    BUF.append(str).append('\n')
                } else {
                    BUF.append("null")
                }
            }
            indent(BUF, level)
            BUF.append("]")
            return BUF.toString()
        }

        private fun formatElement(key: String, `val`: Any?, analysis: Analysis, save: ESS): String {
            val BUF = StringBuilder()
            if (`val` == null) {
                BUF.append(String.format("%s: <NULL>", key))
            } else if (`val` is Linkable) {
                val STR = `val`.toHTML(null)
                BUF.append(String.format("%s: %s", key, STR))
            } else if (`val` is List<*>) {
                val STR = formatList(key, `val`, analysis, save)
                BUF.append(String.format("%s: %s", key, STR))
            } else if (`val`.javaClass.isArray) {
                if (`val` is Array<*>) {
                    BUF.append(String.format("%s: %s", key, Arrays.toString(`val` as Array<Any?>?)))
                } else if (`val` is BooleanArray) {
                    BUF.append(String.format("%s: %s", key, Arrays.toString(`val` as BooleanArray?)))
                } else if (`val` is ByteArray) {
                    BUF.append(String.format("%s: %s", key, Arrays.toString(`val` as ByteArray?)))
                } else if (`val` is CharArray) {
                    BUF.append(String.format("%s: %s", key, Arrays.toString(`val` as CharArray?)))
                } else if (`val` is DoubleArray) {
                    BUF.append(String.format("%s: %s", key, Arrays.toString(`val` as DoubleArray?)))
                } else if (`val` is FloatArray) {
                    BUF.append(String.format("%s: %s", key, Arrays.toString(`val` as FloatArray?)))
                } else if (`val` is IntArray) {
                    BUF.append(String.format("%s: %s", key, Arrays.toString(`val` as IntArray?)))
                } else if (`val` is LongArray) {
                    BUF.append(String.format("%s: %s", key, Arrays.toString(`val` as LongArray?)))
                } else if (`val` is ShortArray) {
                    BUF.append(String.format("%s: %s", key, Arrays.toString(`val` as ShortArray?)))
                }
                val LIST = `val` as List<*>
                val STR = formatList(key, LIST, analysis, save)
                BUF.append(String.format("%s: %s", key, STR))
            } else if (`val` is GeneralElement) {
                val STR = formatGeneralElement(key, `val`, analysis, save)
                BUF.append(String.format("%s: %s", key, STR))
            } else {
                BUF.append(String.format("%s: %s", key, `val`))
            }
            return BUF.toString()
        }

        private fun formatGeneralElement(key: String, gen: GeneralElement, analysis: Analysis, save: ESS): String {
            val BUF = StringBuilder()
            gen.values.forEach { (k: IString, v: Any?) ->
                val S = formatElement(k.toString(), v, analysis, save)
                BUF.append(String.format("<p>%s</p>", S))
            }
            //BUF.append("</ol>");
            return BUF.toString()
        }

        private fun formatList(key: String, list: List<*>, analysis: Analysis, save: ESS): String {
            val BUF = StringBuilder()
            //BUF.append(String.format("<p>%s</p>", key));
            for ((i, `val`) in list.withIndex()) {
                val K = i.toString()
                val S = formatElement(K, `val`, analysis, save)
                BUF.append(String.format("<p>%s</p>", S))
            }
            //BUF.append("<");
            return BUF.toString()
        }

        val SUPPORTED: Set<KClass<*>> = hashSetOf(
                Element::class,
                Byte::class,
                Short::class,
                Int::class,
                Float::class,
                String::class,
                ByteArray::class,
                ShortArray::class,
                IntArray::class,
                LongArray::class,
                FloatArray::class,
                Array<Any>::class
        )
    }

    /**
     * Create a new `GeneralElement`.
     */
    init {
        DATA = LinkedHashMap()
    }
}