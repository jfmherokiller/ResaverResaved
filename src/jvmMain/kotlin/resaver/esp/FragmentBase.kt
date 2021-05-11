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
package resaver.esp

/**
 * The base class for script fragments.
 *
 * @author Mark Fairchild
 */
abstract class FragmentBase : Entry {
    companion object {
        /**
         * Taken from an algorithm posted on Stack Overflow. Accessed on 2016/04/22.
         *
         * @see  http://stackoverflow.com/questions/109023/how-to-count-the-number-of-set-bits-in-a-32-bit-integer
         *
         *
         * @param i The integer to count the bits.
         * @return The number of bits.
         */
        fun NumberOfSetBits(i: Int): Int {
            return i.countOneBits()
        }
    }
}