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

import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.event.TableModelEvent
import javax.swing.JViewport
import ess.AnalyzableElement
import ess.papyrus.*
import javax.swing.table.DefaultTableModel
import java.awt.event.*
import javax.swing.JPopupMenu
import javax.swing.JMenuItem

/**
 * Describes a JTable specialized for displaying variable tables.
 *
 * @author Mark Fairchild
 */
class VariableTable(window: SaveWindow?) : JTable() {
    /**
     * Initializes the table's components.
     */
    private fun initComponent() {
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        this.model.addTableModelListener { e: TableModelEvent? -> WINDOW.setModified() }
        TABLE_POPUP_MENU.add(MI_FIND)
        MI_FIND.addActionListener { e: ActionEvent? ->
            val viewRow = selectedRow
            val modelRow = convertRowIndexToModel(viewRow)
            val column = model.columnCount - 1
            val o = model.getValueAt(modelRow, column)
            assert(o is Variable)
            val `var` = o as Variable
            if (`var`.hasRef() && `var`.ref?.isZero!!.not()) {
                WINDOW.findElement(`var`.referent)
            }
        }
        addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    val row = rowAtPoint(e.point)
                    val col = columnAtPoint(e.point)
                    setRowSelectionInterval(row, row)
                    val modelRow = convertRowIndexToModel(row)
                    val column = model.columnCount - 1
                    val o = model.getValueAt(modelRow, column)
                    assert(o is Variable)
                    val `var` = o as Variable
                    if (`var`.hasRef() && `var`.ref?.isZero!!.not()) {
                        TABLE_POPUP_MENU.show(e.component, e.x, e.y)
                    }
                }
            }
        })
    }

    /**
     *
     * @param index
     */
    fun scrollSelectionToVisible(index: Int) {
        if (index < 0) {
            return
        }
        getSelectionModel().setSelectionInterval(index, index)
        if (parent !is JViewport) {
            return
        }
        val PARENT = parent as JViewport
        val CELL_RECTANGLE = getCellRect(index, 0, true)
        val POINT = PARENT.viewPosition
        CELL_RECTANGLE.setLocation(CELL_RECTANGLE.x - POINT.x, CELL_RECTANGLE.y - POINT.y)
        scrollRectToVisible(CELL_RECTANGLE)
    }

    /**
     *
     * @param element
     * @return
     */
    fun isSupported(element: AnalyzableElement?): Boolean {
        return (element is ScriptInstance
                || element is StructInstance
                || element is Struct
                || element is StackFrame
                || element is ArrayInfo
                || element is FunctionMessageData
                || element is SuspendedStack
                || element is FunctionMessage
                || element is Reference)
    }

    /**
     * Clears the table.
     */
    fun clearTable() {
        this.model = DefaultTableModel()
    }

    /**
     * Displays a `AnalyzableElement` using an appropriate model.
     *
     * @param element The `PapyrusElement` to display.
     * @param context The `PapyrusContext` info.
     */
    fun displayElement(element: AnalyzableElement?, context: PapyrusContext) {
        if (element is ArrayInfo) {
            displayArray(element, context)
        } else if (element is HasVariables) {
            displayVariableTable(element as HasVariables, context)
        } else if (element is Definition) {
            displayDefinition(element, context)
        } else if (element is FunctionMessageData) {
            displayVariableTable(element, context)
        } else if (element is SuspendedStack) {
            if (element.hasMessage()) {
                element.message?.let { displayVariableTable(it, context) }
            } else {
                clearTable()
            }
        } else if (element is FunctionMessage) {
            if (element.hasMessage()) {
                element.message?.let { displayVariableTable(it, context) }
            } else {
                clearTable()
            }
        } else {
            clearTable()
        }
    }

    /**
     * Displays a `Definition` using an appropriate model.
     *
     * @param def The `Definition` to display.
     * @param context The `PapyrusContext` info.
     */
    private fun displayDefinition(def: Definition, context: PapyrusContext) {
        setDefaultRenderer(Variable::class.java, VariableCellRenderer())
        setDefaultEditor(Variable::class.java, VariableCellEditor(context))
        this.model = DefinitionTableModel(def)
        getColumn(getColumnName(0)).minWidth = 25
        getColumn(getColumnName(0)).maxWidth = 25
    }

    /**
     * Displays a `ScriptInstance` using an appropriate model.
     *
     * @param instance The `PapyrusElement` to display.
     * @param context The `PapyrusContext` info.
     */
    private fun displayVariableTable(instance: HasVariables, context: PapyrusContext) {
        setDefaultRenderer(Variable::class.java, VariableCellRenderer())
        setDefaultEditor(Variable::class.java, VariableCellEditor(context))
        this.model = VariableTableModel(instance)
        getColumn(getColumnName(0)).minWidth = 25
        getColumn(getColumnName(0)).maxWidth = 25
        getColumn(getColumnName(1)).minWidth = 120
        getColumn(getColumnName(1)).maxWidth = 120
    }

    /**
     * Displays an `ArrayInfo` using an appropriate model.
     *
     * @param array The `PapyrusElement` to display.
     * @param context The `PapyrusContext` info.
     */
    private fun displayArray(array: ArrayInfo, context: PapyrusContext) {
        setDefaultRenderer(Variable::class.java, VariableCellRenderer())
        setDefaultEditor(Variable::class.java, VariableCellEditor(context))
        this.model = ArrayTableModel(array)
        getColumn(getColumnName(0)).minWidth = 25
        getColumn(getColumnName(0)).maxWidth = 25
        getColumn(getColumnName(1)).minWidth = 120
        getColumn(getColumnName(1)).maxWidth = 120
    }

    private val TABLE_POPUP_MENU: JPopupMenu = JPopupMenu("Table")
    private val MI_FIND: JMenuItem = JMenuItem("Find", KeyEvent.VK_F)
    private val WINDOW: SaveWindow = window!!

    /**
     * Creates a new `VariableTable`.
     *
     * @param window
     */
    init {
        initComponent()
    }
}