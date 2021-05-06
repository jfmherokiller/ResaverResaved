/*
 * Copyright 2018 Mark Fairchild.
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
package mf



/**
 *
 * @author Mark Fairchild
 * @param <T>
</T> */
class Duad<T>(var A: T, var B: T) {

    override fun toString(): String {
        return "($A, $B)"
    }

    override fun hashCode(): Int {
        var hash = 5
        hash = 97 * hash + A.hashCode()
        hash = 97 * hash + B.hashCode()
        return hash
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }
        val other2 = other as Duad<*>
        return A == other2.A && B == other2.B
    }

    companion object {
        @JvmStatic
        fun <T> make(a: T, b: T): Duad<T> {
            return Duad(a, b)
        }
    }
}