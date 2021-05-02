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

import java.util.Objects
import java.util.function.Function

/**
 *
 * @author Mark Fairchild
 * @param <TypeA>
 * @param <TypeB>
</TypeB></TypeA> */
class Pair<TypeA, TypeB>(val A: TypeA, val B: TypeB) {
    fun <C, D> map(f1: Function<TypeA, C>, f2: Function<TypeB, D>): Pair<C, D> {
        return make(f1.apply(A), f2.apply(B))
    }

    override fun toString(): String {
        return "Pair{A=$A, B=$B}"
    }

    override fun hashCode(): Int {
        var hash = 5
        hash = 97 * hash + Objects.hashCode(A)
        hash = 97 * hash + Objects.hashCode(B)
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
        val other = other as Pair<*, *>
        return A == other.A && B == other.B
    }

    companion object {
        fun <A> make(a: A): Pair<A, A?> {
            return Pair(a, null)
        }

        fun <A, B> make(a: A, b: B): Pair<A, B> {
            return Pair(a, b)
        }
    }
}