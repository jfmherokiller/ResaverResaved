/*
 * Copyright 2016 Mark Fairchild
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

import java.nio.ByteBuffer
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Supplier

/**
 * Describes a BSA file record.
 *
 * @author Mark Fairchild
 */
class BSAFileRecord(input: ByteBuffer, header: BSAHeader, names: Supplier<String>) {
    override fun toString(): String {
        return NAME ?: String.format("%d bytes at offset %d", FILESIZE, OFFSET)
    }

    val path: Path?
        get() = try {
            Paths.get(NAME)
        } catch (ex: InvalidPathException) {
            null
        }
    val NAMEHASH: Long
    val FILESIZE: Int
    val OFFSET: Int
    val ISCOMPRESSED: Boolean
    val NAME: String

    companion object {
        const val SIZE = 16
    }

    /**
     * Creates a new `FileRecord` by reading from a
     * `LittleEndianDataInput`. The name field will be set to null.
     *
     * @param input The file from which to readFully.
     * @param header
     * @param names
     */
    init {
        NAMEHASH = input.long
        val size = input.int
        val BIT30 = 1 shl 30
        val compressToggle = size and BIT30 != 0
        FILESIZE = size and BIT30.inv()
        OFFSET = input.int
        ISCOMPRESSED = header.ISCOMPRESSED xor compressToggle
        NAME = names.get()
    }
}