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

import ess.papyrus.Definition
import javax.swing.event.TableModelListener
import javax.swing.table.TableModel

/**
 * A table model for papyrus scripts.
 *
 * @author Mark Fairchild
 */
class DefinitionTableModel(def: Definition?) : TableModel {
    override fun getRowCount(): Int {
        return DEFINITION.members?.size!!
    }

    override fun getColumnCount(): Int {
        return 3
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        assert(0 <= rowIndex && rowIndex < this.rowCount)
        val member = DEFINITION.members?.get(rowIndex)
        return when (columnIndex) {
            0 -> rowIndex
            1 -> member?.type
            2 -> member?.name
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
        return false
    }

    override fun setValueAt(aValue: Any, rowIndex: Int, columnIndex: Int) {
        throw UnsupportedOperationException("Not supported.")
    }

    override fun addTableModelListener(l: TableModelListener) {}
    override fun removeTableModelListener(l: TableModelListener) {}
    private val DEFINITION: Definition = def!!
    private val COLUMNNAMES = arrayOf("#", "Type", "Name")

    companion object {
        private val COLUMNTYPES = arrayOf<Class<*>>(Int::class.java, String::class.java, String::class.java)
    }

}