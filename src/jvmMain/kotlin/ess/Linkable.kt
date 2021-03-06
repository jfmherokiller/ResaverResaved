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

import ess.papyrus.EID
import ess.papyrus.TString

/**
 * Describes anything that can produce a block of HTML containing a link.
 *
 * @author Mark Fairchild
 */
interface Linkable {
    /**
     * Creates an HTML link representation.
     *
     * @param target A target within the `Linkable`.
     * @return HTML link representation.
     */
    fun toHTML(target: Element?): String?

    companion object {
        /**
         * Makes a link url in a standard way, with a target.
         * @param type
         * @param address
         * @param text
         * @return
         */
        fun makeLink(type: String?, address: Any?, text: String?): String {
            return "<a href=\"$type://$address\">$text</a>"
        }

        /**
         * Makes a link url in a standard way, with a target.
         * @param type
         * @param address
         * @param target
         * @param text
         * @return
         */
        fun makeLink(type: String?, address: Any?, target: Int, text: String?): String {
            return "<a href=\"$type://$address[$target]\">$text</a>"
        }

        /**
         * Makes a link url in a standard way, with two target2.
         * @param type
         * @param address
         * @param target1
         * @param target2
         * @param text
         * @return
         */

        fun makeLink(type: String?, address: Any?, target1: Int, target2: Int, text: String?): String {
            return "<a href=\"$type://$address[$target1][$target2]\">$text</a>"
        }


        fun makeLink(thread: String, ID: EID, toString: String): String {
            return "<a href=\"$thread://$ID\">$toString</a>"
        }


        fun makeLink(frame: String, ID: EID, frameIndex: Int, toString: String): String {
            return "<a href=\"$frame://$ID[$frameIndex]\">$toString</a>"
        }


        fun makeLink(script: String, REFTYPE: TString, toString: String): String {
            return "<a href=\"$script://$REFTYPE\">$toString</a>"
        }


        fun makeLink(string: String, INDEX: Int, toString: String): String {
            return "<a href=\"$string://$INDEX\">$toString</a>"
        }
    }
}