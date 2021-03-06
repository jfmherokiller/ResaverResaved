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

import ess.papyrus.ArrayInfo
import ess.papyrus.VarType
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener
import javax.swing.table.TableModel

/**
 * A table model for `ArrayInfo`.
 *
 * @author Mark Fairchild
 */
class ArrayTableModel(data: ArrayInfo?) : TableModel {
    override fun getRowCount(): Int {
        return DATA.variables.size
    }

    override fun getColumnCount(): Int {
        return 3
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        assert(0 <= rowIndex && rowIndex < this.rowCount)
        return when (columnIndex) {
            0 -> rowIndex
            1 -> DATA.variables[rowIndex]?.toTypeString()!!
            2 -> DATA.variables[rowIndex]!!
            else -> throw IllegalStateException()
        }
    }

    override fun getColumnName(columnIndex: Int): String {
        return COLUMNNAMES[columnIndex]
    }

    override fun getColumnClass(columnIndex: Int): Class<*> {
        return COLUMNTYPES[columnIndex]
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
        if (columnIndex != 2) {
            return false
        }
        assert(0 <= rowIndex && rowIndex < this.rowCount)
        val `var` = DATA.variables[rowIndex]
        return when (`var`?.type) {
            VarType.STRING, VarType.INTEGER, VarType.FLOAT, VarType.BOOLEAN, VarType.REF -> true
            else -> false
        }
    }

    override fun setValueAt(aValue: Any, rowIndex: Int, columnIndex: Int) {
        if (!isCellEditable(rowIndex, columnIndex)) {
            throw UnsupportedOperationException("Not supported.") //To change body of generated methods, choose Tools | Templates.
        } else if (aValue !is ess.papyrus.Variable) {
            throw UnsupportedOperationException("Not supported.") //To change body of generated methods, choose Tools | Templates.
        }
        DATA.setVariable(rowIndex, aValue)
        fireTableCellUpdate(rowIndex, columnIndex)
    }

    fun fireTableCellUpdate(row: Int, column: Int) {
        val event = TableModelEvent(this, row, row, column, TableModelEvent.UPDATE)
        LISTENERS.forEach { l: TableModelListener -> l.tableChanged(event) }
    }

    override fun addTableModelListener(l: TableModelListener) {
        LISTENERS.add(l)
    }

    override fun removeTableModelListener(l: TableModelListener) {
        LISTENERS.remove(l)
    }

    private val LISTENERS: MutableList<TableModelListener> = mutableListOf()
    private val DATA: ArrayInfo = data!!
    private val COLUMNNAMES = arrayOf("#", "Type", "Value")

    companion object {
        private val COLUMNTYPES = arrayOf(Int::class.java, String::class.java, ess.papyrus.Variable::class.java)
    }

}