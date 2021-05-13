/*
 * Copyright 2020 Mark Fairchild.
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

import java.nio.ByteBuffer

/**
 *
 * @author Mark Fairchild
 */
enum class CompressionType : Element {
    UNCOMPRESSED, ZLIB, LZ4;

    override fun write(output: ByteBuffer?) {
        output?.putShort(VALUE)
    }

    override fun calculateSize(): Int {
        return 2
    }

    val isCompressed: Boolean
        get() = this != UNCOMPRESSED
    private val VALUE: Short = super.ordinal.toShort()

    companion object {

        fun read(input: ByteBuffer): CompressionType {
            return values()[input.short.toInt()]
        }
    }

}