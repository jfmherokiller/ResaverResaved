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
package resaver.ess

import resaver.ess.ESS.ESSContext
import resaver.ess.GeneralElement
import java.util.Objects
import resaver.ess.RefID
import java.nio.ByteBuffer

/**
 * Manages the initial data field from a `ChangeForm`.
 *
 * @author Mark Fairchild
 */
internal class ChangeFormInitialData(input: ByteBuffer, initialType: Int, context: ESSContext?) : GeneralElement() {
    /**
     * Creates a new `ChangeFormInitialData`.
     * @param input
     * @param initialType
     */
    init {
        Objects.requireNonNull(input)
        when (initialType) {
            1 -> {
                super.readShort(input, "UNK")
                super.readByte(input, "CELLX")
                super.readByte(input, "CELLY")
                super.readInt(input, "UNK2")
            }
            2 -> {
                super.readShort(input, "UNK")
                super.readShort(input, "UNK1")
                super.readShort(input, "UNK2")
                super.readInt(input, "UNK3")
            }
            3 -> super.readInt(input, "UNK")
            4 -> {
                super.readRefID(input, "CELL", context)
                super.readFloats(input, "POS", 3)
                super.readFloats(input, "ROT", 3)
            }
            5 -> {
                super.readRefID(input, "CELL", context)
                super.readFloats(input, "POS", 3)
                super.readFloats(input, "ROT", 3)
                super.readByte(input, "UNK")
                val ref = super.readRefID(input, "BASE_OBJECT", context)
            }
            6 -> {
                super.readRefID(input, "CELL", context)
                super.readFloats(input, "POS", 3)
                super.readFloats(input, "ROT", 3)
                super.readRefID(input, "STARTING CELL", context)
                super.readShort(input, "UNK1")
                super.readShort(input, "UNK2")
            }
            else -> {
            }
        }
    }
}