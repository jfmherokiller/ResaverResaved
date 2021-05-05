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

import resaver.ess.papyrus.PapyrusElementMap.PapyrusElementReader
import java.nio.ByteBuffer

/**
 *
 * @author Mark Fairchild
 */
class ArrayMap : PapyrusElementMap<ArrayInfo> {
    internal constructor(input: ByteBuffer?, context: PapyrusContext?) : super(
        input,
        PapyrusElementReader<ArrayInfo> { b: ByteBuffer? -> ArrayInfo(b, context) }) {
    }

    internal constructor() {}
}