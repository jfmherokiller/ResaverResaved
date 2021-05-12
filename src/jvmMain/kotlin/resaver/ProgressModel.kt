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
package resaver

import mu.KLoggable
import mu.KLogger
import javax.swing.DefaultBoundedRangeModel
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 *
 * @author Mark Fairchild
 */
class ProgressModel @JvmOverloads constructor(max: Int = 18) : DefaultBoundedRangeModel(0, 0, 0, max) {
    constructor(max: Double) : this(max.roundToInt())

    @Synchronized
    fun modifyValue(delta: Int) {
        super.setValue(value + delta)
        //LOG.info(String.format("Progress: %d/%d (%d)", this.getValue(), this.getMaximum(), delta));
    }

    @Synchronized
    fun modVSq(delta: Double) {
        modifyValue(sqrt(delta).toInt())
    }

    @Synchronized
    override fun setValue(n: Int) {
        super.setValue(n)
    }

    companion object:KLoggable {
        override val logger: KLogger
            get() = logger()
    }
}