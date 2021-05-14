/*
 * Copyright 2020 Mark.
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
package ess.papyrus

import PlatformByteBuffer
import resaver.ListException

/**
 *
 * @author Mark
 * @param <T>
</T> */
open class PapyrusElementMap<T : HasID?> : LinkedHashMap<EID?, T>, PapyrusElement {
    internal constructor(input: PlatformByteBuffer, reader: PapyrusElementReader<T>) {
        try {
            val count = input.getInt()
            for (i in 0 until count) {
                try {
                    val element = reader.read(input)
                    this[element!!.iD] = element
                } catch (ex: PapyrusFormatException) {
                    throw ListException(i, count, ex)
                }
            }
        } catch (ex: ListException) {
            throw PapyrusElementException("Failed to read elements.", ex, this)
        }
    }

    internal constructor() {}

    override fun calculateSize(): Int {
        var sum = 0
        for (t in this.values) {
            val calculateSize = t!!.calculateSize()
            sum += calculateSize
        }
        return 4 + sum
    }

    override fun write(output: PlatformByteBuffer?) {
        output?.putInt(this.size)
        this.values.forEach { v: T -> v!!.write(output) }
    }

    internal fun interface PapyrusElementReader<T> {
        @Throws(PapyrusFormatException::class, PapyrusElementException::class)
        fun read(input: PlatformByteBuffer?): T
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}