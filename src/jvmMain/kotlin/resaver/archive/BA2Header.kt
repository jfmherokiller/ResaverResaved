/*
 * Copyright 2018 Mark.
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
package resaver.archive

import PlatformByteBuffer
import java.io.IOException

/**
 * Describes a BA2 header.
 *
 * @author Mark
 */
class BA2Header(input: PlatformByteBuffer) {
    val TYPE: Type
    val VERSION: Int
    val SUBTYPE: BA2Subtype
    val MAGIC1: ByteArray
    val MAGIC2: ByteArray
    val FILE_COUNT: Int
    val NAMETABLE_OFFSET: Long

    companion object {
        const val SIZE = 24
    }

    /**
     * Creates a new `BA2Header` by reading from a
     * `ByteBuffer`.
     *
     * @param input The file from which to readFully.
     * @throws IOException
     */
    init {
        MAGIC1 = ByteArray(4)
        input[MAGIC1]
        VERSION = input.getInt()
        MAGIC2 = ByteArray(4)
        input[MAGIC2]
        val type: Type
        try {
            val magic = String(MAGIC1)
            type = Type.valueOf(magic)
            if (type !== Type.BTDX) {
                throw IOException("Invalid archive format: ${String(MAGIC1)}")
            }
        } catch (ex: IllegalArgumentException) {
            throw IOException("Invalid archive format: ${String(MAGIC1)}", ex)
        }
        val subtype: BA2Subtype
        subtype = try {
            val magic = String(MAGIC2)
            BA2Subtype.valueOf(magic)
        } catch (ex: IllegalArgumentException) {
            throw IOException("Invalid archive format: ${String(MAGIC2)}", ex)
        }
        TYPE = type
        SUBTYPE = subtype
        FILE_COUNT = input.getInt()
        NAMETABLE_OFFSET = input.getLong()
    }
}