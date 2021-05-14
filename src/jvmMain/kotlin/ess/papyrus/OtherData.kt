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
package ess.papyrus

import PlatformByteBuffer
import ess.Element
import ess.Flags
import mu.KLoggable
import mu.KLogger

/**
 *
 * @author Mark Fairchild
 */

class OtherData(input: PlatformByteBuffer, context: PapyrusContext) : ess.GeneralElement(), PapyrusElement {
    val arrays: List<Array<Element>>
        get() = listOfNotNull(
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
            ARRAY9,
            ARRAY10,
            ARRAY11,
            ARRAY12,
            ARRAY13,
            ARRAY14,
            ARRAY15
        )
    val ARRAY1: Array<Element> = input?.let { read32ElemArray(it, "Array1") { `in`: PlatformByteBuffer? -> Array1(`in`, context) } }
        ?.toTypedArray()!!
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

    internal class Array1(input: PlatformByteBuffer?, context: PapyrusContext) : ess.GeneralElement() {
        override fun toString(): String {
            return if (null == THREAD) {
                "INVALID (" + super.toString() + ")"
            } else {
                THREAD.toString() + "(" + super.toString() + ")"
            }
        }

        private val THREAD: ActiveScript?

        init {
            val ID1 = input?.let { super.readID32(it, "ID1", context) }
            val ID2 = input?.let { super.readID32(it, "ID2", context) }
            THREAD = context.findActiveScript(ID2)
        }
    }

    internal class Array1A(input: PlatformByteBuffer?, context: PapyrusContext) : ess.GeneralElement() {
        override fun toString(): String {
            return if (null == THREAD) {
                "INVALID (" + super.toString() + ")"
            } else {
                THREAD.toString() + "(" + super.toString() + ")"
            }
        }

        private val THREAD: ActiveScript?

        init {
            if (input != null) {
                super.readID32(input, "ID1", context)
            }
            val ID =
                if (context.game?.isFO4 == true) input?.let { super.readID64(it, "ID2", context) } else input?.let {
                    super.readID32(
                        it, "ID2", context)
                }
            THREAD = context.findActiveScript(ID)
        }
    }

    internal class Array2(input: PlatformByteBuffer?, context: PapyrusContext) : ess.GeneralElement() {
        override fun toString(): String {
            return if (THREAD == null) {
                "null(${super.toString()})"
            } else {
                "$THREAD(${super.toString()})"
            }
        }

        private val THREAD: ActiveScript?

        init {
            if (input != null) {
                super.readID32(input, "ID1", context)
            }
            val ID =
                if (context.game?.isFO4 == true) input?.let { super.readID64(it, "ID2", context) } else input?.let {
                    super.readID32(
                        it, "ID2", context)
                }
            THREAD = context.findActiveScript(ID)
        }
    }

    internal class Array3(input: PlatformByteBuffer?, context: PapyrusContext?) : ess.GeneralElement() {
        init {
            if (input != null) {
                super.readByte(input, "type")
            }
            if (input != null) {
                super.readShort(input, "str1")
            }
            if (input != null) {
                super.readShort(input, "unk1")
            }
            if (input != null) {
                super.readShort(input, "str2")
            }
            if (input != null) {
                super.readInt(input, "unk2")
            }
            super.readElement(input, "flags") { input: PlatformByteBuffer? -> input?.let { Flags.readShortFlags(it) } }
            if (input != null) {
                if (context != null) {
                    super.readRefID(input, "refID", context)
                }
            }
        }
    }

    internal class Array4(input: PlatformByteBuffer?, context: PapyrusContext?) : ess.GeneralElement() {
        init {
            if (input != null) {
                super.readShort(input, "str1")
            }
            if (input != null) {
                super.readShort(input, "unk1")
            }
            if (input != null) {
                super.readByte(input, "unk2")
            }
            if (input != null) {
                super.readShort(input, "str2")
            }
            if (input != null) {
                super.readInt(input, "unk3")
            }
            super.readElement(input, "flags") { input: PlatformByteBuffer? -> input?.let { Flags.readShortFlags(it) } }
            if (context != null) {
                if (input != null) {
                    super.readRefID(input, "refID", context)
                }
            }
        }
    }

    internal class Array4A(input: PlatformByteBuffer?, context: PapyrusContext?) : ess.GeneralElement()
    internal class Array4B(input: PlatformByteBuffer?, context: PapyrusContext?) : ess.GeneralElement() {
        init {
            if (input != null) {
                super.readByte(input, "unk1")
            }
            if (input != null) {
                super.readShort(input, "unk2")
            }
            if (input != null) {
                super.readShort(input, "unk3")
            }
            if (input != null) {
                if (context != null) {
                    super.readRefID(input, "ref1", context)
                }
            }
            if (input != null) {
                if (context != null) {
                    super.readRefID(input, "ref2", context)
                }
            }
            if (input != null) {
                if (context != null) {
                    super.readRefID(input, "ref3", context)
                }
            }
            if (input != null) {
                if (context != null) {
                    super.readRefID(input, "ref4", context)
                }
            }
        }
    }

    internal class Array4C(input: PlatformByteBuffer?, context: PapyrusContext?) : ess.GeneralElement() {
        init {
            val FLAG = input?.let { super.readByte(it, "flag") }!!
            super.readInt(input, "data")
            if (context != null) {
                super.readRefID(input, "ref", context)
            }
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

    internal class Array4D(input: PlatformByteBuffer?, context: PapyrusContext?) : ess.GeneralElement() {
        init {
            if (input != null) {
                super.readByte(input, "flag1")
            }
            if (input != null) {
                super.readInt(input, "unk2")
            }
            if (input != null) {
                super.readByte(input, "flag2")
            }
            if (input != null) {
                if (context != null) {
                    super.readRefID(input, "ref", context)
                }
            }
        }
    }

    internal class Array5(input: PlatformByteBuffer?, context: PapyrusContext?) : ess.GeneralElement() {
        init {
            if (input != null) {
                super.readShort(input, "unk1")
            }
            if (input != null) {
                super.readShort(input, "unk2")
            }
            if (input != null) {
                if (context != null) {
                    super.readRefID(input, "ref1", context)
                }
            }
            if (input != null) {
                if (context != null) {
                    super.readRefID(input, "ref2", context)
                }
            }
            if (input != null) {
                if (context != null) {
                    super.readRefID(input, "ref3", context)
                }
            }
            if (input != null) {
                super.readShort(input, "unk4")
            }
        }
    }

    internal class Array6(input: PlatformByteBuffer?, context: PapyrusContext?) : ess.GeneralElement() {
        init {
            if (input != null) {
                super.readShort(input, "unk")
            }
            super.readElement(input, "flags") { input: PlatformByteBuffer? -> input?.let { Flags.readShortFlags(it) } }
            if (context != null) {
                if (input != null) {
                    super.readRefID(input, "ref", context)
                }
            }
        }
    }

    internal class Array7(input: PlatformByteBuffer?, context: PapyrusContext?) : ess.GeneralElement() {
        init {
            if (input != null) {
                super.readShort(input, "unk")
            }
            super.readElement(input, "flags") { input: PlatformByteBuffer? -> input?.let { Flags.readShortFlags(it) } }
            if (context != null) {
                if (input != null) {
                    super.readRefID(input, "ref", context)
                }
            }
        }
    }

    internal class Array8(input: PlatformByteBuffer?, context: PapyrusContext) : ess.GeneralElement() {
        init {
            if (input != null) {
                super.readShort(input, "unk")
            }
            if (input != null) {
                super.readShort(input, "type")
            }
            if (input != null) {
                super.readRefID(input, "ref", context)
            }
            val COUNT1 = input?.let { super.readInt(it, "count1") }
            val COUNT2 = input?.let { super.readInt(it, "count2") }
            if (COUNT1 != null) {
                super.readElements(input, "refArray1", COUNT1) { input: PlatformByteBuffer? -> input?.let { context.readRefID(it) } }
            }
            if (COUNT2 != null) {
                super.readElements(input, "refArray2", COUNT2) { input: PlatformByteBuffer? -> input?.let { context.readRefID(it) } }
            }
        }
    }

    internal class LString(input: PlatformByteBuffer?) : ess.GeneralElement() {
        override fun toString(): String {
            return STRING
        }

        val STRING: String

        init {
            val COUNT = input?.let { readInt(it, "COUNT") }
            assert(0 <= COUNT!!)
            val BYTES = super.readBytes(input, "MEMBERS", COUNT)
            STRING = String(BYTES)
        }
    }

    companion object:KLoggable {
        override val logger: KLogger
            get() = logger()

    }

    /*static final private int[] SIZES_SLE = {8, 8, 8, 16, 16, -1, 17, };
    static final private int[] SIZES_SSE = {};
    static final private int[] SIZES_FO4 = {};*/
    init {
        logger.info{String.format("Read ARRAY1, %d elements.", ARRAY1.size)}
        ARRAY1A = input?.let { read32ElemArray(it, "Array1a") { `in`: PlatformByteBuffer? -> Array1A(`in`, context) } }
            ?.toTypedArray()!!
        logger.info{"Read ARRAY1A, ${ARRAY1A.size} elements."}
        ARRAY2 = read32ElemArray(input, "Array2") { `in`: PlatformByteBuffer? -> Array2(`in`, context) }.toTypedArray()
        logger.info{"Read ARRAY2, ${ARRAY2.size} elements."}
        ARRAY3 = read32ElemArray(input, "Array3") { `in`: PlatformByteBuffer? -> Array3(`in`, context) }.toTypedArray()
        logger.info{"Read ARRAY3, ${ARRAY3.size} elements."}
        ARRAY4 = read32ElemArray(input, "Array4") { `in`: PlatformByteBuffer? -> Array4(`in`, context) }.toTypedArray()
        logger.info{"Read ARRAY4, ${ARRAY4.size} elements."}
        SCRIPTS = read32ElemArray(input, "Scripts") { input: PlatformByteBuffer? -> LString(input) }.toTypedArray()
        logger.info{"Read SCRIPTS, ${SCRIPTS.size} element."}
        ARRAY4A = read32ElemArray(input, "Array4A") { `in`: PlatformByteBuffer? -> Array4A(`in`, context) }.toTypedArray()
        logger.info{"Read ARRAY4A, ${ARRAY4A.size} elements."}
        ARRAY4B = read32ElemArray(input, "Array4b") { `in`: PlatformByteBuffer? -> Array4B(`in`, context) }.toTypedArray()
        logger.info{"Read ARRAY4B, ${ARRAY4B.size} elements."}
        ARRAY4C = read32ElemArray(input, "Array4c") { `in`: PlatformByteBuffer? -> Array4C(`in`, context) }.toTypedArray()
        logger.info{"Read ARRAY4C, ${ARRAY4C.size} elements."}
        ARRAY4D = read32ElemArray(input, "Array4d") { `in`: PlatformByteBuffer? -> Array4D(`in`, context) }.toTypedArray()
        logger.info{"Read ARRAY4D, ${ARRAY4D.size} elements."}
        ARRAY5 = read32ElemArray(input, "Array5") { `in`: PlatformByteBuffer? -> Array5(`in`, context) }.toTypedArray()
        logger.info{"Read ARRAY5, ${ARRAY5.size} elements."}
        ARRAY6 = read32ElemArray(input, "Array6") { `in`: PlatformByteBuffer? -> Array6(`in`, context) }.toTypedArray()
        logger.info{"Read ARRAY6, ${ARRAY6.size} elements."}
        ARRAY7 = read32ElemArray(input, "Array7") { `in`: PlatformByteBuffer? -> Array7(`in`, context) }.toTypedArray()
        logger.info{"Read ARRAY7, ${ARRAY7.size} elements."}
        ARRAY8 = read32ElemArray(input, "Array8") { `in`: PlatformByteBuffer? -> Array8(`in`, context) }.toTypedArray()
        logger.info{"Read ARRAY8, ${ARRAY8.size} elements."}
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