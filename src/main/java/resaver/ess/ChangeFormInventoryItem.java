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
package resaver.ess;

import java.nio.ByteBuffer;

/**
 * Manages an inventory item.
 *
 * @author Mark Fairchild
 */
public class ChangeFormInventoryItem extends GeneralElement {

    /**
     * Creates a new <code>ChangeFormInventoryItem</code> by reading from a
     * <code>LittleEndianDataOutput</code>. No error handling is performed.
     *
     * @param input The input stream.
     * @param context The <code>ESSContext</code> info.
     */
    public ChangeFormInventoryItem(ByteBuffer input, ESS.ESSContext context) {
        super.readRefID(input, "ITEM", context);
        super.readInt(input, "COUNT");
        super.readElement(input, "EXTRA", in -> new ChangeFormExtraData(in, context));
    }

}
