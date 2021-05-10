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

import kotlin.Throws
import java.nio.ByteBuffer

/**
 *
 * @author Mark
 */
interface SeparateData {
    @Throws(PapyrusFormatException::class, PapyrusElementException::class)
    fun readData(input: ByteBuffer?, context: PapyrusContext?)
    fun writeData(input: ByteBuffer?)
}