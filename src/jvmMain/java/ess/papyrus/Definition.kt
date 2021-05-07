/*
 * Copyright 2017 Mark.
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

import ess.AnalyzableElement
import ess.Linkable

/**
 *
 * @author Mark
 */
abstract class Definition : PapyrusElement, AnalyzableElement, Linkable {
    /**
     * @return The name of the papyrus element.
     */
    abstract val name: TString?

    /**
     * @return A flag indicating if the `Definition` is undefined.
     */
    open val isUndefined: Boolean
        get() = false

    /**
     * Increments the instance count.
     */
    fun incrementInstanceCount() {
        instanceCount++
    }

    /**
     * @return The list of member descriptions.
     */
    abstract val members: List<MemberDesc?>?

    /**
     * @return The instance count.
     */
    protected var instanceCount = 0
        private set
}