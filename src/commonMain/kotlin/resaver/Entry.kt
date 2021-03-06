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
package resaver

import PlatformByteBuffer

/**
 * A base interface describing anything that can be read from an ESP.
 *
 * @author Mark Fairchild
 */
interface Entry {
    /**
     * Writes the Entry.
     *
     * @param output The `ByteBuffer` to write.
     */
    fun write(output: PlatformByteBuffer?)

    /**
     * Calculates the size of the Entry.
     *
     * @return The size of the Field in bytes.
     */
    fun calculateSize(): Int

    companion object {
        /**
         *
         * @param buffer
         * @param newLimit
         * @return
         */

        fun advancingSlice(buffer: PlatformByteBuffer, newLimit: Int): PlatformByteBuffer {
            // Make the new slice.
            val newSlice = buffer.slice()
            newSlice.makeLe()
            newSlice.limit(newLimit)

            // Advance the original.
            buffer.position(buffer.position() + newLimit)
            return newSlice
        }
    }
}