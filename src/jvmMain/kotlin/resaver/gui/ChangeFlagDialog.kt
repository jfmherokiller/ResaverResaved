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
package resaver.gui

import mf.BiIntConsumer
import mf.Hex32Formatter
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.event.ActionEvent
import java.beans.PropertyChangeEvent
import javax.swing.*

/**
 *
 * @author Mark
 */
class ChangeFlagDialog(parent: SaveWindow?, mask: Int, filter: Int, done: BiIntConsumer) : JDialog(parent, "", true) {
    /**
     *
     */
    private fun update() {
        var m = mask
        var f = filter
        for (i in 0..31) {
            val flag = FLAGS[i]
            val x = m and 0x1
            val v = f and 0x1
            m = m ushr 1
            f = f ushr 1
            when {
                x == 0 -> {
                    flag!!.text = "x"
                }
                v == 0 -> {
                    flag!!.text = "0"
                }
                else -> {
                    flag!!.text = "1"
                }
            }
        }
    }

    /**
     *
     */
    private fun updateMask() {
        try {
            mask = maskField.value as Int
            this.update()
        } catch (ex: NumberFormatException) {
            throw IllegalStateException("Illegal mask and/or field.", ex)
        }
    }

    /**
     *
     */
    private fun updateFilter() {
        try {
            filter = filterField.value as Int
            this.update()
        } catch (ex: NumberFormatException) {
            throw IllegalStateException("Illegal mask and/or field.", ex)
        }
    }

    /**
     *
     * @param b
     */
    private fun flagToggle(b: JButton) {
        val i = listOf(*FLAGS).indexOf(b)
        when (b.text) {
            LABEL_X -> setFlag(i, 0)
            LABEL_0 -> setFlag(i, 1)
            LABEL_1 -> setFlag(i, -1)
            else -> throw IllegalStateException("Illegal value.")
        }
    }

    /**
     *
     * @param i
     * @param val
     */
    private fun setFlag(i: Int, `val`: Int) {
        System.out.printf("button %d, %d\n", i, `val`)
        val flag = FLAGS[i]
        val clearBit = (0x1 shl i).inv()
        val setBit = 0x1 shl i
        when (`val`) {
            -1 -> {
                mask = mask and clearBit
                filter = filter and clearBit
                flag!!.text = LABEL_X
            }
            0 -> {
                mask = mask or setBit
                filter = filter and clearBit
                flag!!.text = LABEL_0
            }
            1 -> {
                mask = mask or setBit
                filter = filter or setBit
                flag!!.text = LABEL_1
            }
            else -> throw IllegalStateException("Illegal flag value.")
        }
        maskField.value = mask
        filterField.value = filter
    }

    override fun toString(): String {
        return String.format("%08x / %08x", mask, filter)
    }

    private var mask: Int
    private var filter: Int
    private val maskField: JFormattedTextField
    private val filterField: JFormattedTextField
    private val FLAGS: Array<JButton?> = arrayOfNulls(32)
    private val FILTER: JButton
    private val CLEAR: JButton
    private val CANCEL: JButton

    companion object {
        private const val LABEL_X = "x"
        private const val LABEL_0 = "0"
        private const val LABEL_1 = "1"
    }

    /**
     *
     * @param parent
     * @param mask
     * @param filter
     * @param done
     */
    init {
        this.mask = mask
        this.filter = filter
        val m = mask
        val f = filter
        for (i in 0..31) {
            FLAGS[i] = JButton()
        }
        maskField = JFormattedTextField(Hex32Formatter())
        filterField = JFormattedTextField(Hex32Formatter())
        maskField.value = this.mask
        filterField.value = this.filter
        FILTER = JButton("Set Filter")
        CLEAR = JButton("Clear")
        CANCEL = JButton("Cancel")
        maskField.columns = 8
        filterField.columns = 8
        maskField.addPropertyChangeListener("value") { e: PropertyChangeEvent? -> updateMask() }
        filterField.addPropertyChangeListener("value") { e: PropertyChangeEvent? -> updateFilter() }
        super.setLayout(BorderLayout())
        val TOP = JPanel(FlowLayout())
        val CENTER = JPanel(GridLayout(2, 32))
        val BOTTOM = JPanel(FlowLayout())
        super.add(TOP, BorderLayout.NORTH)
        super.add(BOTTOM, BorderLayout.SOUTH)
        super.add(CENTER, BorderLayout.CENTER)
        TOP.add(JLabel("Mask"))
        TOP.add(maskField)
        TOP.add(JLabel("Filter"))
        TOP.add(filterField)
        BOTTOM.add(FILTER)
        BOTTOM.add(CLEAR)
        BOTTOM.add(CANCEL)
        for (i in 0..31) {
            CENTER.add(JLabel((31 - i).toString(), JLabel.CENTER))
        }
        for (i in 0..31) {
            val b = FLAGS[31 - i]
            CENTER.add(b)
            b!!.addActionListener { e: ActionEvent -> flagToggle(e.source as JButton) }
        }
        FILTER.addActionListener { e: ActionEvent? ->
            this.isVisible = false
            done.consume(this.mask, this.filter)
        }
        CANCEL.addActionListener { e: ActionEvent? -> this.isVisible = false }
        CLEAR.addActionListener { e: ActionEvent? ->
            this.mask = 0
            this.filter = 0
            this.update()
        }
        super.setDefaultCloseOperation(DISPOSE_ON_CLOSE)
        this.update()
    }
}