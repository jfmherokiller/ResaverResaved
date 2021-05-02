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

import resaver.pex.TokenGenerator
import resaver.IString

/**
 * Generates a sequence of identifiers. Every `TokenGenerator`
 * generates the same sequence.
 *
 * @author Mark Fairchild
 */
class TokenGenerator
/**
 * Creates a new `TokenGenerator`.
 *
 */
    : Cloneable {
    /**
     * Creates a copy of the `TokenGenerator`; it will return
     * the same sequence of identifiers as the original.
     *
     * @return A copy of the `TokenGenerator`.
     */
    public override fun clone(): TokenGenerator {
        val copy = TokenGenerator()
        copy.index = index
        return copy
    }

    /**
     * Produces the next identifier in the sequence.
     * @return An identifier string.
     */
    operator fun next(): IString {
        return IString.format("%s%03d", PREFIX, index++)
    }

    /**
     * @return String representation.
     */
    override fun toString(): String {
        return String.format("TokenGenerator(%s) %d", PREFIX, index)
    }

    private var index = 0

    companion object {
        private const val PREFIX = "QQZQQ"
    }
}