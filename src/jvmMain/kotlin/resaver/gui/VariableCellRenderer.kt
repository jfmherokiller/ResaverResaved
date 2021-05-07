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
package resaver.gui

import ess.papyrus.*
import java.awt.Color
import java.awt.Component
import java.awt.Font
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.JTable

/**
 * Renderer for cells showing variables, to allow hot-linking of references.
 *
 * @author Mark Fairchld
 */
internal class VariableCellRenderer : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        return if (value is ess.papyrus.Variable) {
            val VAR = value
            val STR = value.toValueString()
            val C = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            C.foreground = DEFAULT_COLOR
            C.font = DEFAULT_FONT
            if (VAR is VarRef) {
                val REF = VAR
                if (REF.isNull) {
                    C.foreground = NULL_COLOR
                } else if (null == REF.referent) {
                    C.foreground = INVALID_COLOR
                    C.font = INVALID_FONT
                }
            }
            C
        } else {
            val C = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            C.foreground = DEFAULT_COLOR
            C.font = DEFAULT_FONT
            C
        }
    }

    private val DEFAULT_COLOR: Color = super.getForeground()?: Color.BLACK
    private val INVALID_COLOR: Color = Color.RED
    private val NULL_COLOR: Color = Color.BLUE
    private val DEFAULT_FONT: Font = super.getFont()
    private val INVALID_FONT: Font = super.getFont().deriveFont(Font.ITALIC)

}