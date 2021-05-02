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
package resaver.ess.papyrus

import resaver.ess.papyrus.EID.Companion.pad8
import resaver.ess.papyrus.PapyrusElement
import java.util.Objects
import resaver.ess.papyrus.EID
import java.nio.ByteBuffer

/**
 *
 * @author Mark Fairchild
 */
/**
 * @param input The input stream.
 */
class UnknownThing(input: ByteBuffer) : PapyrusElement {
    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer) {
        Objects.requireNonNull(output)
        output.putInt(VALUE)
    }

    /**
     * @see resaver.ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        return 4
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        return pad8(VALUE)
    }

    private val VALUE: Int


    init {
        VALUE = input.int
    }
}