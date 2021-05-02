/*
 * Copyright 2017 Mark.
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
package resaver.ess.papyrus

import resaver.ess.Element
import resaver.ess.Flags
import resaver.ess.GeneralElement
import java.nio.ByteBuffer
import java.util.*
import java.util.logging.Logger

/**
 *
 * @author Mark Fairchild
 */
class OtherData(input: ByteBuffer?, context: PapyrusContext) : GeneralElement(), PapyrusElement {
    val arrays: List<Array<Element>>
        get() = listOf(
            ARRAY1,
            ARRAY2,
            ARRAY3,
            ARRAY4,
            SCRIPTS,
            ARRAY4A,
            ARRAY4B,
            ARRAY4C,
            ARRAY4D,
            ARRAY5,
            ARRAY6,
            ARRAY7,
            ARRAY8,
            ARRAY9!!,
            ARRAY10!!,
            ARRAY11!!,
            ARRAY12!!,
            ARRAY13!!,
            ARRAY14!!,
            ARRAY15!!
        )
    val ARRAY1: Array<Element>
    val ARRAY1A: Array<Element>
    val ARRAY2: Array<Element>
    val ARRAY3: Array<Element>
    val ARRAY4: Array<Element>
    val SCRIPTS: Array<Element>
    val ARRAY4A: Array<Element>
    val ARRAY4B: Array<Element>
    val ARRAY4C: Array<Element>
    val ARRAY4D: Array<Element>
    val ARRAY5: Array<Element>
    val ARRAY6: Array<Element>
    val ARRAY7: Array<Element>
    val ARRAY8: Array<Element>
    val ARRAY9: Array<Element>?
    val ARRAY10: Array<Element>?
    val ARRAY11: Array<Element>?
    val ARRAY12: Array<Element>?
    val ARRAY13: Array<Element>?
    val ARRAY14: Array<Element>?
    val ARRAY15: Array<Element>?

    internal class Array1(input: ByteBuffer?, context: PapyrusContext) : GeneralElement() {
        override fun toString(): String {
            return if (null == THREAD) {
                "INVALID (" + super.toString() + ")"
            } else {
                THREAD.toString() + "(" + super.toString() + ")"
            }
        }

        private val THREAD: ActiveScript?

        init {
            val ID1 = super.readID32(input, "ID1", context)
            val ID2 = super.readID32(input, "ID2", context)
            THREAD = context.findActiveScript(ID2)
        }
    }

    internal class Array1A(input: ByteBuffer?, context: PapyrusContext) : GeneralElement() {
        override fun toString(): String {
            return if (null == THREAD) {
                "INVALID (" + super.toString() + ")"
            } else {
                THREAD.toString() + "(" + super.toString() + ")"
            }
        }

        private val THREAD: ActiveScript?

        init {
            super.readID32(input, "ID1", context)
            val ID =
                if (context.game.isFO4) super.readID64(input, "ID2", context) else super.readID32(input, "ID2", context)
            THREAD = context.findActiveScript(ID)
        }
    }

    internal class Array2(input: ByteBuffer?, context: PapyrusContext) : GeneralElement() {
        override fun toString(): String {
            return if (THREAD == null) {
                "null(${super.toString()})"
            } else {
                "$THREAD(${super.toString()})"
            }
        }

        private val THREAD: ActiveScript?

        init {
            super.readID32(input, "ID1", context)
            val ID =
                if (context.game.isFO4) super.readID64(input, "ID2", context) else super.readID32(input, "ID2", context)
            THREAD = context.findActiveScript(ID)
        }
    }

    internal class Array3(input: ByteBuffer?, context: PapyrusContext?) : GeneralElement() {
        init {
            super.readByte(input, "type")
            super.readShort(input, "str1")
            super.readShort(input, "unk1")
            super.readShort(input, "str2")
            super.readInt(input, "unk2")
            super.readElement(input, "flags") { input: ByteBuffer? -> Flags.readShortFlags(input) }
            super.readRefID(input, "refID", context)
        }
    }

    internal class Array4(input: ByteBuffer?, context: PapyrusContext?) : GeneralElement() {
        init {
            super.readShort(input, "str1")
            super.readShort(input, "unk1")
            super.readByte(input, "unk2")
            super.readShort(input, "str2")
            super.readInt(input, "unk3")
            super.readElement(input, "flags") { input: ByteBuffer? -> Flags.readShortFlags(input) }
            super.readRefID(input, "refID", context)
        }
    }

    internal class Array4A(input: ByteBuffer?, context: PapyrusContext?) : GeneralElement()
    internal class Array4B(input: ByteBuffer?, context: PapyrusContext?) : GeneralElement() {
        init {
            super.readByte(input, "unk1")
            super.readShort(input, "unk2")
            super.readShort(input, "unk3")
            super.readRefID(input, "ref1", context)
            super.readRefID(input, "ref2", context)
            super.readRefID(input, "ref3", context)
            super.readRefID(input, "ref4", context)
        }
    }

    internal class Array4C(input: ByteBuffer?, context: PapyrusContext?) : GeneralElement() {
        init {
            val FLAG = super.readByte(input, "flag")
            super.readInt(input, "data")
            super.readRefID(input, "ref", context)
            if (FLAG in 0..6) {
                super.readInts(input, "data1array", 3)
            }
            if (FLAG.toInt() == 0) {
                super.readInts(input, "data2array", 4)
            }
            if (FLAG in 0..3) {
                super.readByte(input, "unk")
            }
        }
    }

    internal class Array4D(input: ByteBuffer?, context: PapyrusContext?) : GeneralElement() {
        init {
            super.readByte(input, "flag1")
            super.readInt(input, "unk2")
            super.readByte(input, "flag2")
            super.readRefID(input, "ref", context)
        }
    }

    internal class Array5(input: ByteBuffer?, context: PapyrusContext?) : GeneralElement() {
        init {
            super.readShort(input, "unk1")
            super.readShort(input, "unk2")
            super.readRefID(input, "ref1", context)
            super.readRefID(input, "ref2", context)
            super.readRefID(input, "ref3", context)
            super.readShort(input, "unk4")
        }
    }

    internal class Array6(input: ByteBuffer?, context: PapyrusContext?) : GeneralElement() {
        init {
            super.readShort(input, "unk")
            super.readElement(input, "flags") { input: ByteBuffer? -> Flags.readShortFlags(input) }
            super.readRefID(input, "ref", context)
        }
    }

    internal class Array7(input: ByteBuffer?, context: PapyrusContext?) : GeneralElement() {
        init {
            super.readShort(input, "unk")
            super.readElement(input, "flags") { input: ByteBuffer? -> Flags.readShortFlags(input) }
            super.readRefID(input, "ref", context)
        }
    }

    internal class Array8(input: ByteBuffer?, context: PapyrusContext) : GeneralElement() {
        init {
            super.readShort(input, "unk")
            super.readShort(input, "type")
            super.readRefID(input, "ref", context)
            val COUNT1 = super.readInt(input, "count1")
            val COUNT2 = super.readInt(input, "count2")
            super.readElements(input, "refArray1", COUNT1) { input: ByteBuffer? -> context.readRefID(input) }
            super.readElements(input, "refArray2", COUNT2) { input: ByteBuffer? -> context.readRefID(input) }
        }
    }

    internal class LString(input: ByteBuffer?) : GeneralElement() {
        override fun toString(): String {
            return STRING
        }

        val STRING: String

        init {
            val COUNT = readInt(input, "COUNT")
            assert(0 <= COUNT)
            val BYTES = super.readBytes(input, "MEMBERS", COUNT)
            STRING = String(BYTES)
        }
    }

    companion object {
        private val LOG = Logger.getLogger(OtherData::class.java.canonicalName)
    }

    /*static final private int[] SIZES_SLE = {8, 8, 8, 16, 16, -1, 17, };
    static final private int[] SIZES_SSE = {};
    static final private int[] SIZES_FO4 = {};*/
    init {
        ARRAY1 = read32ElemArray(input, "Array1") { `in`: ByteBuffer? -> Array1(`in`, context) }
        LOG.info(String.format("Read ARRAY1, %d elements.", ARRAY1.size))
        ARRAY1A = read32ElemArray(input, "Array1a") { `in`: ByteBuffer? -> Array1A(`in`, context) }
        LOG.info(String.format("Read ARRAY1A, %d elements.", ARRAY1A.size))
        ARRAY2 = read32ElemArray(input, "Array2") { `in`: ByteBuffer? -> Array2(`in`, context) }
        LOG.info(String.format("Read ARRAY2, %d elements.", ARRAY2.size))
        ARRAY3 = read32ElemArray(input, "Array3") { `in`: ByteBuffer? -> Array3(`in`, context) }
        LOG.info(String.format("Read ARRAY3, %d elements.", ARRAY3.size))
        ARRAY4 = read32ElemArray(input, "Array4") { `in`: ByteBuffer? -> Array4(`in`, context) }
        LOG.info(String.format("Read ARRAY4, %d elements.", ARRAY4.size))
        SCRIPTS = read32ElemArray(input, "Scripts") { `in`: ByteBuffer? -> LString(`in`) }
        LOG.info(String.format("Read SCRIPTS, %d element.", SCRIPTS.size))
        ARRAY4A = read32ElemArray(input, "Array4A") { `in`: ByteBuffer? -> Array4A(`in`, context) }
        LOG.info(String.format("Read ARRAY4A, %d elements.", ARRAY4A.size))
        ARRAY4B = read32ElemArray(input, "Array4b") { `in`: ByteBuffer? -> Array4B(`in`, context) }
        LOG.info(String.format("Read ARRAY4B, %d elements.", ARRAY4B.size))
        ARRAY4C = read32ElemArray(input, "Array4c") { `in`: ByteBuffer? -> Array4C(`in`, context) }
        LOG.info(String.format("Read ARRAY4C, %d elements.", ARRAY4C.size))
        ARRAY4D = read32ElemArray(input, "Array4d") { `in`: ByteBuffer? -> Array4D(`in`, context) }
        LOG.info(String.format("Read ARRAY4D, %d elements.", ARRAY4D.size))
        ARRAY5 = read32ElemArray(input, "Array5") { `in`: ByteBuffer? -> Array5(`in`, context) }
        LOG.info(String.format("Read ARRAY5, %d elements.", ARRAY5.size))
        ARRAY6 = read32ElemArray(input, "Array6") { `in`: ByteBuffer? -> Array6(`in`, context) }
        LOG.info(String.format("Read ARRAY6, %d elements.", ARRAY6.size))
        ARRAY7 = read32ElemArray(input, "Array7") { `in`: ByteBuffer? -> Array7(`in`, context) }
        LOG.info(String.format("Read ARRAY7, %d elements.", ARRAY7.size))
        ARRAY8 = read32ElemArray(input, "Array8") { `in`: ByteBuffer? -> Array8(`in`, context) }
        LOG.info(String.format("Read ARRAY8, %d elements.", ARRAY8.size))
        ARRAY9 = null
        //LOG.info(String.format("Read ARRAY9, %d elements.", this.ARRAY9.length));
        ARRAY10 = null
        //LOG.info(String.format("Read ARRAY10, %d elements.", this.ARRAY10.length));
        ARRAY11 = null
        //LOG.info(String.format("Read ARRAY11, %d elements.", this.ARRAY11.length));
        ARRAY12 = null
        //LOG.info(String.format("Read ARRAY12, %d elements.", this.ARRAY12.length));
        ARRAY13 = null
        //LOG.info(String.format("Read ARRAY13, %d elements.", this.ARRAY13.length));
        ARRAY14 = null
        //.info(String.format("Read ARRAY14, %d elements.", this.ARRAY14.length));
        ARRAY15 = null
        //LOG.info(String.format("Read ARRAY15, %d elements.", this.ARRAY15.length));
    }
}