/*
 * Copyright 2018 Mark.
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

import javax.swing.JMenuItem

/**
 * A `JMenuItem` that has a parameter and whose display string is
 * formatted.
 *
 * @author Mark Fairchild
 * @since 2018-09-22
 */
class JValueMenuItem<T>(private var format: String, private var value: T) : JMenuItem(
    String.format(
        format, value
    )
) {
    override fun setText(newFormat: String) {
        format = newFormat
        updateText()
    }

    fun setValue(newValue: T) {
        value = newValue
        updateText()
    }

    fun getValue(): T {
        return value
    }

    private fun updateText() {
        val formatted: String = if (value == null) {
            String.format(format, "none")
        } else {
            String.format(format, value)
        }
        super.setText(formatted)
    }

    /**
     *
     */
    override fun updateUI() {
        updateText()
        super.updateUI()
    }
}