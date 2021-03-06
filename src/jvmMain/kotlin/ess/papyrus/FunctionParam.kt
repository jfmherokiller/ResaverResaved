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
package ess.papyrus

import PlatformByteBuffer

/**
 * Describes a function parameter in a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
/**
 * @see MemberDesc.MemberData
 * @param input The input stream.
 * @param context The `PapyrusContext` info.
 * @throws PapyrusFormatException
 */
class FunctionParam

    (input: PlatformByteBuffer?, context: PapyrusContext?) : MemberDesc(input, context) {
    /**
     * @return String representation.
     */
    override fun toString(): String {
        return "(param) ${super.toString()}"
    }
}