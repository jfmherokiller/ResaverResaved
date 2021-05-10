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
package mf

import kotlin.jvm.Volatile
import java.util.function.Supplier
import kotlin.jvm.Synchronized
import java.util.Objects

/**
 *
 * @author Mark
 */
class Lazy<T> {
    @Volatile
    private var value: T? = null
    fun getOrCompute(supplier: Supplier<T>): T? {
        val result = value // Just one volatile read 
        return result ?: maybeCompute(supplier)
    }

    @Synchronized
    private fun maybeCompute(supplier: Supplier<T>): T? {
        if (value == null) {
            value = Objects.requireNonNull(supplier.get())
        }
        return value
    }
}