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

import PlatformByteBuffer
import ess.ESS.ESSContext
import ess.papyrus.EID

/**
 *
 * @author Mark
 */
class GlobalVariable(input: PlatformByteBuffer?, context: ESSContext) : Element {
    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: PlatformByteBuffer?) {
        REFID.write(output)
        output!!.putFloat(value)
    }

    /**
     * @see resaver.ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        return 4 + REFID.calculateSize()
    }

    override fun toString(): String {
        val BUF = StringBuilder()
        if (REFID.name != null) {
            BUF.append(REFID.toString())
        } else {
            BUF.append("NAME MISSING")
        }
        BUF.append(" (")
        if (REFID.PLUGIN != null) {
            BUF.append(REFID.PLUGIN!!.NAME)
            BUF.append(":")
        }
        if (REFID.type === RefID.Type.FORMIDX) {
            BUF.append(EID.pad8(REFID.FORMID))
        } else {
            BUF.append(REFID.toString())
        }
        BUF.append(") = ")
        BUF.append(value)
        return BUF.toString()
    }

    override fun hashCode(): Int {
        var hash = 7
        hash = 53 * hash + REFID.hashCode()
        hash = 53 * hash + java.lang.Float.floatToIntBits(value)
        return hash
    }

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
        val other2 = other as GlobalVariable
        return other2.REFID == REFID && other2.value == value
    }

    private val REFID: RefID = context.readRefID(input!!)
    /**
     *
     * @return The value of the `GlobalVariable`.
     */
    /**
     * Sets the value of the `GlobalVariable`.
     * @param newVal The new value of the `GlobalVariable`.
     */
    var value: Float = input?.getFloat() ?: 0.0f

}