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
 * Describes a user flag from a PEX file.
 *
 * @author Mark Fairchild
 */
class UserFlag internal constructor(input: ByteBuffer, strings: StringTable) {
    /**
     * Write the object to a `ByteBuffer`.
     *
     * @param output The `ByteBuffer` to write.
     * @throws IOException IO errors aren't handled at all, they are simply
     * passed on.
     */
    @Throws(IOException::class)
    fun write(output: ByteBuffer) {
        NAME.write(output)
        output.put(FLAGINDEX)
    }

    /**
     * Collects all of the strings used by the UserFlag and adds them to a set.
     *
     * @param strings The set of strings.
     */
    fun collectStrings(strings: MutableSet<StringTable.TString?>) {
        strings.add(NAME)
    }

    /**
     * Tests if a userFlags field includes this UserFlag.
     *
     * @param userFlags A userFlags field.
     * @return True if the field includes this UserFlag, false otherwise.
     */
    fun matches(userFlags: Int): Boolean {
        return userFlags ushr FLAGINDEX.toInt() and 1 != 0
    }

    /**
     * @return The size of the `UserFlag`, in bytes.
     */
    fun calculateSize(): Int {
        return 3
    }

    /**
     * Pretty-prints the UserFlag.
     *
     * @return A string representation of the UserFlag.
     */
    override fun toString(): String {
        val FORMAT = "%s"
        return String.format(FORMAT, NAME)
    }

    private val NAME: StringTable.TString = strings.read(input)
    private val FLAGINDEX: Byte = input.get()

}