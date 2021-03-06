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

import PlatformByteBuffer

/**
 * Describes the ChangeForm flags for a ChangeForm.
 *
 * @author Mark Fairchild
 */
/**
 * Creates a new `ChangeFormFlags`.
 *
 * @param input The input stream.
 */
class ChangeFormFlags(input: PlatformByteBuffer) : Element {
    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: PlatformByteBuffer?) {
        output!!.putInt(flags)
        output.putShort(unknown)
    }

    /**
     * @see resaver.ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        return 6
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        val BUF = StringBuilder()
        val S = flags.toUInt().toString(2)
        var idx = 0
        while (idx < 32 - S.length) {
            if (idx > 0 && idx % 4 == 0) {
                BUF.append(' ')
            }
            BUF.append('0')
            idx++
        }
        while (idx < 32) {
            if (idx > 0 && idx % 4 == 0) {
                BUF.append(' ')
            }
            BUF.append(S[idx - 32 + S.length])
            idx++
        }
        return BUF.toString()
    }

    /**
     * @return The flag field.
     */
    val flags: Int = input.getInt()

    /**
     * @return The unknown field.
     */
    val unknown: Short = input.getShort()
}