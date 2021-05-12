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
import mu.KLoggable
import mu.KLogger
import mu.KotlinLogging
import resaver.Analysis
import resaver.IString
import java.nio.ByteBuffer
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
        return this.getElement(name.toString())
    }

    /**
     * Reads a byte.
     *
     * @param input The inputstream.
     * @param name The name of the new element.
     * @return The byte.
     */
    fun readByte(input: ByteBuffer, name: String): Byte? {
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
    inline fun <reified T: Any> addValue(name: String, `val`: T?): T {
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
            check(b) { "Invalid type for $name: ${`val`::class}" }
            if(!b) {
                logger.error {"Invalid type for $name: ${`val`::class}"}
            }
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
                is ArrayList<*> -> {
                    for (e in v) {
                        (e as Element).write(output)
                    }
                }
                else -> checkNotNull(v) { "Null element!" }
            }
            throw IllegalStateException("Unknown element: ${v.javaClass}")
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
                is List<*> -> {
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
        return DATA.hashCode()
    }

    /**
     * @see java.lang.Object.equals
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
                val other2 = other as GeneralElement
                DATA == other2.DATA
            }
        }
    }

    /**
     *
     * @return String representation.
     */
    fun toTextBlock(): String {
        return DATA.keys.joinToString(separator = ", ", prefix = "[", postfix = "]") { n: IString -> "$n=${getVal(n)}" }
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
                        val special = (`val` as Byte?)?.toUInt()?.toLong()
                        String.format("%02x",special!!)
                    }
                    is Short -> {
                        val special = (`val` as Short?)?.toUInt()?.toLong()
                        String.format("%04x",special!!)
                    }
                    is Int -> {
                        val special = (`val` as Int?)?.toUInt()?.toLong()
                        String.format("%08x",special!!)
                    }
                    is Long -> {
                        String.format("%16x", `val`)
                    }
                    is ArrayList<*> -> {
                        (`val` as ArrayList<*>?)?.joinToString(",","[","]").toString()
                    }
                    is Array<*> -> {
                        (`val` as Array<*>?)?.joinToString(",","[","]").toString()
                    }
                    is BooleanArray -> {
                        (`val` as BooleanArray?)?.joinToString(",","[","]").toString()
                    }
                    is ByteArray -> {
                        (`val` as ByteArray?)?.joinToString(",","[","]").toString()
                    }
                    is CharArray -> {
                        (`val` as CharArray?)?.joinToString(",","[","]").toString()
                    }
                    is DoubleArray -> {
                        (`val` as DoubleArray?)?.joinToString(",","[","]").toString()
                    }
                    is FloatArray -> {
                        (`val` as FloatArray?)?.joinToString(",","[","]").toString()
                    }
                    is IntArray -> {
                        (`val` as IntArray?)?.joinToString(",","[","]").toString()
                    }
                    is LongArray -> {
                        (`val` as LongArray?)?.joinToString(",","[","]").toString()
                    }
                    is ShortArray -> {
                        (`val` as ShortArray?)?.joinToString(",","[","]").toString()
                    }
                    else -> {
                        `val`.toString()
                    }
                }
                BUF.append("$key=$str\n")
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
            when (`val`) {
                is Linkable -> {
                    val STR = `val`.toHTML(null)
                    BUF.append("<td>$key</td><td>$STR</td></tr>")
                }
                is List<*> -> {
                    val STR = analysis?.let {
                        if (save != null) {
                            formatList(key.toString(), `val`, it, save)
                        }
                    }
                    BUF.append("<td>$key</td><td>$STR</td></tr>")
                }
                is GeneralElement -> {
                    val STR = analysis?.let {
                        if (save != null) {
                            formatGeneralElement(key.toString(), `val`, it, save)
                        }
                    }
                    BUF.append("<td>$key</td><td>$STR</td></tr>")
                }
                else -> {
                    BUF.append("<td>$key</td><td>$`val`</td></tr>")
                }
            }
        }
        BUF.append("</table>")
        return BUF.toString()
    }

    /**
     * Stores the actual data.
     */
    public val DATA: MutableMap<IString, Any?> = mutableMapOf()

    companion object: KLoggable {
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
                when (e) {
                    is GeneralElement -> {
                        val str = e.toString(level + 1)
                        BUF.append(str).append('\n')
                    }
                    else -> {
                        indent(BUF, level + 1)
                        val str = e.toString()
                        BUF.append(str).append('\n')
                    }
                }
            }
            indent(BUF, level)
            BUF.append("]")
            return BUF.toString()
        }

        private fun formatElement(key: String, `val`: Any?, analysis: Analysis, save: ESS): String {
            val BUF = StringBuilder()
            when {
                `val` == null -> {
                    BUF.append("$key: <NULL>")
                }
                `val` is Linkable -> {
                    val STR = `val`.toHTML(null)
                    BUF.append("$key: $STR")
                }
                `val` is List<*> -> {
                    val STR = formatList(key, `val`, analysis, save)
                    BUF.append("$key: $STR")
                }
                `val`.javaClass.isArray -> {
                    when (`val`) {
                        is Array<*> -> {
                            BUF.append("$key: ${(`val` as Array<*>?)?.joinToString(",", "[", "]").toString()}")
                        }
                        is BooleanArray -> {
                            BUF.append("$key: ${(`val` as BooleanArray?)?.joinToString(",", "[", "]").toString()}")
                        }
                        is ByteArray -> {
                            BUF.append("$key: ${(`val` as ByteArray?)?.joinToString(",", "[", "]").toString()}")
                        }
                        is CharArray -> {
                            BUF.append("$key: ${(`val` as CharArray?)?.joinToString(",", "[", "]").toString()}")
                        }
                        is DoubleArray -> {
                            BUF.append("$key: ${(`val` as DoubleArray?)?.joinToString(",", "[", "]").toString()}")
                        }
                        is FloatArray -> {
                            BUF.append("$key: ${(`val` as FloatArray?)?.joinToString(",", "[", "]").toString()}")
                        }
                        is IntArray -> {
                            BUF.append("$key: ${(`val` as IntArray?)?.joinToString(",", "[", "]").toString()}")
                        }
                        is LongArray -> {
                            BUF.append("$key: ${(`val` as LongArray?)?.joinToString(",", "[", "]").toString()}")
                        }
                        is ShortArray -> {
                            BUF.append("$key: ${(`val` as ShortArray?)?.joinToString(",", "[", "]").toString()}")
                        }
                    }
                    val LIST = `val` as List<*>
                    val STR = formatList(key, LIST, analysis, save)
                    BUF.append("$key: $STR")
                }
                `val` is GeneralElement -> {
                    val STR = formatGeneralElement(key, `val`, analysis, save)
                    BUF.append("$key: $STR")
                }
                else -> {
                    BUF.append("$key: $`val`")
                }
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
            for ((i: Int, `val`: Any?) in list.withIndex()) {
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
                Array<Any>::class,
                ArrayList::class,  //Check other places where lists are being pulled instead of arrays (kotlin being special)
        )
        override val logger: KLogger
            get() = logger()
    }
}