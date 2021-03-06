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
 * Describes a component of a Skyrim savegame.
 *
 * @author Mark Fairchild
 */
interface Element {
    /**
     * Write the `Element` to an output stream.
     * @param output The output stream.
     */
    fun write(output: PlatformByteBuffer?)

    /**
     * @return The size of the `Element` in bytes.
     */
    fun calculateSize(): Int
}