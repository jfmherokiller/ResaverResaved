/*
 * Copyright 2017 Mark Fairchild.
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

import ess.ESS.ESSContext
import java.nio.ByteBuffer



/**
 *
 * @author Mark
 */
class GlobalVariableTable : Element, GlobalDataBlock {
    /**
     * Creates a new `GlobalVariableTable`.
     *
     * @param input The input data.
     * @param context The `ESSContext` info.
     */
    constructor(input: ByteBuffer?, context: ESSContext?) {
        COUNT = input?.let { ess.VSVal(it) }!!
        val C = COUNT.value
        VARIABLES = ArrayList(C)
        for (i in 0 until C) {
            val `var` = GlobalVariable(input, context!!)
            VARIABLES.add(`var`)
        }
    }

    /**
     * Creates a new empty `GlobalVariableTable`.
     */
    constructor() {
        COUNT = ess.VSVal(0)
        VARIABLES = emptyList()
    }

    /**
     * @see resaver.ess.Element.write
     * @param output The output stream.
     */
    override fun write(output: ByteBuffer?) {
        COUNT.write(output)
        VARIABLES.forEach { `var`: GlobalVariable -> `var`.write(output) }
    }

    /**
     * @see resaver.ess.Element.calculateSize
     * @return The size of the `Element` in bytes.
     */
    override fun calculateSize(): Int {
        var sum = COUNT.calculateSize()
        sum += VARIABLES.sumOf { obj: GlobalVariable -> obj.calculateSize() }
        return sum
    }

    override fun hashCode(): Int {
        var hash = 3
        hash = 17 * hash + COUNT.hashCode()
        hash = 17 * hash + VARIABLES.hashCode()
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
        val other2 = other as GlobalVariableTable
        if (COUNT != other2.COUNT) {
            return false
        }
        return VARIABLES == other2.VARIABLES
    }

    /**
     * @return The `GlobalVariable` list.
     */
    val variables: List<GlobalVariable>
        get() = VARIABLES
    private val COUNT: ess.VSVal
    private val VARIABLES: List<GlobalVariable>
}