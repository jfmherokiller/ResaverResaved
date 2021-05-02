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

import java.util.Objects
import java.io.IOException
import java.lang.IllegalArgumentException
import java.nio.ByteBuffer

/**
 * Describes a BSA header.
 *
 * @author Mark Fairchild
 */
class BSAHeader(input: ByteBuffer, name: String) {
    override fun toString(): String {
        return NAME
    }

    val NAME: String
    val TYPE: Type
    val MAGIC: ByteArray
    val VERSION: Int
    val FOLDER_OFFSET: Int
    val ARCHIVE_FLAGS: Int
    val FOLDER_COUNT: Int
    val FILE_COUNT: Int
    val TOTAL_FOLDERNAME_LENGTH: Int
    val TOTAL_FILENAME_LENGTH: Int
    val FILE_FLAGS: Int
    val INCLUDE_DIRECTORYNAMES: Boolean
    val INCLUDE_FILENAMES: Boolean
    val ISCOMPRESSED: Boolean
    val EMBED_FILENAME: Boolean

    companion object {
        const val SIZE = 36
    }

    /**
     * Creates a new `BSAHeader` by reading from a
     * `LittleEndianDataInput`.
     *
     * @param input The file from which to readFully.
     * @param name The filename.
     * @throws IOException
     */
    init {
        Objects.requireNonNull(input)
        NAME = Objects.requireNonNull(name)
        MAGIC = ByteArray(4)
        input[MAGIC]
        val type: Type
        try {
            val magic = String(MAGIC)
            type = Type.valueOf(magic.trim { it <= ' ' })
            if (type !== Type.BSA) {
                throw IOException("Invalid archive format: " + String(MAGIC))
            }
        } catch (ex: IllegalArgumentException) {
            throw IOException("Invalid archive format: " + String(MAGIC), ex)
        }
        TYPE = type
        VERSION = input.int
        FOLDER_OFFSET = input.int
        ARCHIVE_FLAGS = input.int
        FOLDER_COUNT = input.int
        FILE_COUNT = input.int
        TOTAL_FOLDERNAME_LENGTH = input.int
        TOTAL_FILENAME_LENGTH = input.int
        FILE_FLAGS = input.int
        INCLUDE_DIRECTORYNAMES = ARCHIVE_FLAGS and 0x1 != 0
        INCLUDE_FILENAMES = ARCHIVE_FLAGS and 0x2 != 0
        ISCOMPRESSED = ARCHIVE_FLAGS and 0x4 != 0
        EMBED_FILENAME = ARCHIVE_FLAGS and 0x100 != 0
    }
}