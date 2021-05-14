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
 * Describes a the data for a `GlobalData` when it is not parsed.
 *
 * @author Mark Fairchild
 */
class DefaultGlobalDataBlock(data: ByteArray?) : GlobalDataBlock {
    /**
     * @return A read-only view of the data.
     */
    val data: PlatformByteBuffer
        get() {
            val data = PlatformByteBuffer.wrap(DATA)
            data.makeLe()
            return data
        }

    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: PlatformByteBuffer?) {
        output!!.put(DATA)
    }

    /**
     * @see resaver.ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        return DATA.size
    }

    /**
     * @see Object.hashCode
     * @return
     */
    override fun hashCode(): Int {
        var hash = 7
        hash = 89 * hash + DATA.contentHashCode()
        return hash
    }

    /**
     * @see Object.equals
     * @return
     */
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
        val other2 = other as DefaultGlobalDataBlock
        return DATA.contentEquals(other2.DATA)
    }

    private val DATA: ByteArray

    /**
     * Creates a new `DefaultGlobalDataBlock` by supplying it with a
     * byte buffer.
     *
     * @param data The data.
     */
    init {
        if (data == null) {
            throw NullPointerException("data must not be null.")
        }
        DATA = data
    }
}