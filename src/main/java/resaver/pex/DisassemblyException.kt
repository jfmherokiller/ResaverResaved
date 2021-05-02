/*
 * Copyright 2016 Mark.
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

import java.lang.Exception
import java.util.ArrayList
import kotlin.jvm.JvmOverloads
import java.util.Objects
import java.util.Collections

/**
 *
 * @author Mark Fairchild
 */
class DisassemblyException @JvmOverloads constructor(
    message: String?,
    partial: List<String> = emptyList(),
    pdel: Int = 0,
    cause: Throwable? = null
) : Exception(message, cause) {
    /**
     * @return The partial disassembly.
     */
    val partial: List<String>

    /**
     * @return The pointer-delta.
     */
    val ptrDelta: Int
    /**
     * Creates a new instance of `DisassemblyException` with a
     * message, a partial disassembly, and a cause.
     *
     * @param message The exception message.
     * @param partial The partial disassembly.
     * @param pdel The point-delta.
     * @param cause
     */
    /**
     * Creates a new instance of `DisassemblyException` with a
     * message, an empty partial disassembly, and no cause.
     *
     * @param message
     */
    init {
        Objects.requireNonNull(partial)
        this.partial = Collections.unmodifiableList(ArrayList(partial))
        ptrDelta = pdel
    }
}