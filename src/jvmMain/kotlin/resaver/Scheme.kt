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

/**
 * Describe an IString mapping.
 *
 * This exists solely for convenience, to avoid having to write
 * Map<IString></IString>, IString> repeatedly..
 *
 * @author Mark
 */
class Scheme : HashMap<IString, IString> {
    /**
     * Creates a new empty Scheme.
     */
    constructor() : super() {}

    /**
     * Creates a new Scheme containing the contents of an existing sScheme.
     *
     * @param m The existing Scheme whose contents should be copied.
     */
    constructor(m: Map<IString, IString>) : super(m) {}

    /**
     * @see java.util.HashMap.clone
     * @return
     */
    override fun clone(): Scheme {
        return super.clone() as Scheme
    }

    /**
     * @see Object.hashCode
     * @return
     */
    override fun hashCode(): Int {
        val flatMap = super.hashCode() + this.flatMap { keys + values }.sumOf { it.hashCode() }
        return flatMap
        //return o.hashCode();
    }

    /**
     * @see Object.equals
     * @param other
     * @return
     */
    override fun equals(other: Any?): Boolean {
        return this === other
    } // This is used to generate an identity hashcode rather than the value
    // hashcode that HashMap normally produces.
    //final private Object o = new Object();
}