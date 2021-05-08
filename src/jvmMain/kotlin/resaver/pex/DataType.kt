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
package resaver.pex

import kotlin.Throws
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Describes the six datatypes that appear in PEX files.
 *
 * @author Mark Fairchild
 */
enum class DataType {
    NONE, IDENTIFIER, STRING, INTEGER, FLOAT, BOOLEAN;

    companion object {
        /**
         * Read a `DataType` from an input stream.
         *
         * @param input The input stream.
         * @return The `DataType`.
         */
        @JvmStatic
        @Throws(IOException::class)
        fun read(input: ByteBuffer): DataType {
            val index = UtilityFunctions.toUnsignedInt(input.get())
            if (index < 0 || index >= VALUES.size) {
                throw IOException("Invalid DataType.")
            }
            return VALUES[index]
        }

        private val VALUES = values()
    }
}