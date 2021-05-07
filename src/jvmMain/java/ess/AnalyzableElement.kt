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

/**
 * An abstract base class for adding functionality to instances of
 * `Element`.
 *
 * @author Mark Fairchild
 */
interface AnalyzableElement : Element {
    /**
     * Generates some information about the element.
     *
     * @param analysis Information about mods.
     * @param save The full set of savegame papyrus data.
     * @return A string to show users about the element.
     */
    fun getInfo(analysis: resaver.Analysis?, save: ess.ESS?): String?

    /**
     * Evaluates whether the element could have originated from the specified
     * `Mod`.
     *
     * @param analysis Analysis of mods, from a profile.
     * @param mod The `Mod` to check for.
     * @return A flag indicating if the element could plausibly have originated
     * in the specified `Mod`.
     */
    fun matches(analysis: resaver.Analysis?, mod: String?): Boolean {
        return false
    }
}