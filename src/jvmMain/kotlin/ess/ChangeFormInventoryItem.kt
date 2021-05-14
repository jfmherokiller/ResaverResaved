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
package ess

import PlatformByteBuffer
import ess.ESS.ESSContext

/**
 * Manages an inventory item.
 *
 * @author Mark Fairchild
 */
class ChangeFormInventoryItem(input: PlatformByteBuffer?, context: ESSContext?) : ess.GeneralElement() {
    /**
     * Creates a new `ChangeFormInventoryItem` by reading from a
     * `LittleEndianDataOutput`. No error handling is performed.
     *
     * @param input The input stream.
     * @param context The `ESSContext` info.
     */
    init {
        if (context != null) {
            if (input != null) {
                super.readRefID(input, "ITEM", context)
            }
        }
        if (input != null) {
            super.readInt(input, "COUNT")
        }
        super.readElement(input, "EXTRA") { `in`: PlatformByteBuffer? ->
            ChangeFormExtraData(
                `in`!!, context!!
            )
        }
    }
}