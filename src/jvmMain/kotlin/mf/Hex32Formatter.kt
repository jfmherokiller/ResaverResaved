/*
 * Copyright 2019 Mark.
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

import javax.swing.JFormattedTextField.AbstractFormatter
import kotlin.Throws
import java.text.ParseException

/**
 *
 * @author Mark
 */
class Hex32Formatter : AbstractFormatter() {
    @Throws(ParseException::class)
    override fun stringToValue(text: String): Int {
        return try {
            UtilityFunctions.parseUnsignedInt(text, 16)
        } catch (ex: NumberFormatException) {
            throw ParseException(text, 0)
        }
    }

    @Throws(ParseException::class)
    override fun valueToString(value: Any?): String {
        return if (value is Number) {
            val i = value.toInt()
            String.format("%08x", i)
        } else {
            throw ParseException(value.toString(), 0)
        }
    }
}