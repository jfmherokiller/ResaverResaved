/*
 * Copyright 2016 Mark.
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

import resaver.ess.papyrus.*
import javax.swing.AbstractCellEditor
import javax.swing.table.TableCellEditor
import javax.swing.JTable
import java.lang.IllegalStateException
import javax.swing.JTextField
import javax.swing.JFormattedTextField
import java.text.NumberFormat
import javax.swing.JComboBox
import java.awt.Component
import javax.swing.JFormattedTextField.AbstractFormatter
import kotlin.Throws
import java.lang.NumberFormatException
import java.text.ParseException

/**
 * A `TableCellEditor` implementation for table cells that contain
 * `Variable` objects.
 *
 * @author Mark Fairchild
 */
class VariableCellEditor(context: PapyrusContext) : AbstractCellEditor(), TableCellEditor {
    override fun getCellEditorValue(): Any {
        return subeditor.cellEditorValue
    }

    override fun getTableCellEditorComponent(
        table: JTable,
        value: Any,
        isSelected: Boolean,
        row: kotlin.Int,
        column: kotlin.Int
    ): Component {
        return when (value) {
            is VarStr -> {
                subeditor = STR
                subeditor.getTableCellEditorComponent(table, value, isSelected, row, column)
            }
            is VarInt -> {
                subeditor = INT
                subeditor.getTableCellEditorComponent(table, value, isSelected, row, column)
            }
            is VarFlt -> {
                subeditor = FLT
                subeditor.getTableCellEditorComponent(table, value, isSelected, row, column)
            }
            is VarBool -> {
                subeditor = BOOL
                subeditor.getTableCellEditorComponent(table, value, isSelected, row, column)
            }
            is VarRef -> {
                subeditor = REF
                subeditor.getTableCellEditorComponent(table, value, isSelected, row, column)
            }
            else -> {
                throw IllegalStateException()
            }
        }
    }

    private val STR: Str
    private val INT: Int
    private val FLT: Flt
    private val BOOL: Bool
    private val REF: Ref
    private var subeditor: TableCellEditor
    private val CONTEXT: PapyrusContext

    /**
     * Subclass that handles strings.
     */
    private inner class Str : AbstractCellEditor(), TableCellEditor {
        override fun getCellEditorValue(): VarStr {
            val text = EDITER.text
            return VarStr(text, CONTEXT)
        }

        override fun getTableCellEditorComponent(
            table: JTable,
            value: Any,
            isSelected: Boolean,
            row: kotlin.Int,
            column: kotlin.Int
        ): Component? {
            if (value !is VarStr) {
                return null
            }
            `var` = value
            EDITER.text = `var`!!.value.toString()
            return EDITER
        }

        private var `var`: VarStr? = null
        private val EDITER: JTextField = JTextField(10)

    }

    /**
     * Subclass that handles integers.
     */
    private inner class Int : AbstractCellEditor(), TableCellEditor {
        override fun getCellEditorValue(): VarInt {
            val value = EDITER.value as Number
            return VarInt(value.toInt())
        }

        override fun getTableCellEditorComponent(
            table: JTable,
            value: Any,
            isSelected: Boolean,
            row: kotlin.Int,
            column: kotlin.Int
        ): Component? {
            if (value is VarInt) {
                EDITER.value = value.value
                return EDITER
            }
            return null
        }

        private val EDITER: JFormattedTextField = JFormattedTextField(NumberFormat.getIntegerInstance())

        init {
            EDITER.columns = 5
        }
    }

    /**
     * Subclass that handles floats.
     */
    private inner class Flt : AbstractCellEditor(), TableCellEditor {
        override fun getCellEditorValue(): VarFlt {
            val value = EDITER.value as Number
            return VarFlt(value.toFloat())
        }

        override fun getTableCellEditorComponent(
            table: JTable,
            value: Any,
            isSelected: Boolean,
            row: kotlin.Int,
            column: kotlin.Int
        ): Component? {
            if (value !is VarFlt) {
                return null
            }
            EDITER.value = value.value
            return EDITER
        }

        private val EDITER: JFormattedTextField = JFormattedTextField(NumberFormat.getNumberInstance())

        init {
            EDITER.columns = 5
        }
    }

    /**
     * Subclass that handles booleans.
     */
    private inner class Bool : AbstractCellEditor(), TableCellEditor {
        override fun getCellEditorValue(): VarBool {
            val value = EDITER.selectedItem as Boolean
            return VarBool(value)
        }

        override fun getTableCellEditorComponent(
            table: JTable,
            value: Any,
            isSelected: Boolean,
            row: kotlin.Int,
            column: kotlin.Int
        ): Component? {
            if (value !is VarBool) {
                return null
            }
            EDITER.selectedItem = value.value
            return EDITER
        }

        private val EDITER: JComboBox<Boolean> = JComboBox(arrayOf(java.lang.Boolean.TRUE, java.lang.Boolean.FALSE))

        init {
            EDITER.prototypeDisplayValue = java.lang.Boolean.FALSE
        }
    }

    /**
     * Subclass that handles integers.
     */
    private inner class Ref : AbstractCellEditor(), TableCellEditor {
        override fun getCellEditorValue(): VarRef {
            val v = EDITER.value as Long
            return original!!.derive(v, CONTEXT)
        }

        override fun getTableCellEditorComponent(
            table: JTable,
            value: Any,
            isSelected: Boolean,
            row: kotlin.Int,
            column: kotlin.Int
        ): Component? {
            if (value is VarRef) {
                original = value
                eid = original!!.ref
                EDITER.value = eid!!.longValue()
                return EDITER
            }
            return null
        }

        private val EDITER: JFormattedTextField
        private var original: VarRef? = null
        private var eid: EID? = null
        private val FORMATTER: AbstractFormatter = object : AbstractFormatter() {
            @Throws(ParseException::class)
            override fun stringToValue(text: String?): Any {
                return try {
                    java.lang.Long.parseUnsignedLong(text, 16)
                } catch (ex: NumberFormatException) {
                    throw ParseException(text, 0)
                }
            }

            @Throws(ParseException::class)
            override fun valueToString(value: Any?): String {
                return if (value is Number) {
                    if (eid!!.is4Byte()) {
                        Integer.toHexString(value.toInt())
                    } else {
                        java.lang.Long.toHexString(value.toLong())
                    }
                } else {
                    throw ParseException(value.toString(), 0)
                }
            }
        }

        init {
            EDITER = JFormattedTextField(FORMATTER)
            EDITER.columns = 16
        }
    }

    init {
        STR = Str()
        INT = Int()
        FLT = Flt()
        BOOL = Bool()
        REF = Ref()
        subeditor = STR
        CONTEXT = context
    }
}