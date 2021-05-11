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
package resaver.gui


import resaver.Analysis
import ess.ESS.ESSContext
import ess.Flags
import ess.papyrus.*
import java.awt.*
import java.awt.event.ActionEvent
import java.nio.Buffer
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import javax.swing.*
import javax.swing.border.TitledBorder
import javax.swing.event.CaretEvent
import javax.swing.event.HyperlinkListener
import javax.swing.text.*

/**
 *
 * @author Mark
 */
class DataAnalyzer(newData: ByteBuffer, save: ess.ESS) : JSplitPane(HORIZONTAL_SPLIT) {
    private fun initComponents() {
        setLeftComponent(SCROLLER)
        setRightComponent(SIDEPANE)
        resizeWeight = 1.0
        TEXTPANE.isEditable = false
        TEXTPANE.caret.isVisible = true
        SCROLLER.minimumSize = Dimension(200, 200)
        SCROLLER.preferredSize = Dimension(200, 200)
        SCROLLER.border = BorderFactory.createTitledBorder("Raw Data")
        SIDEPANE.border = BorderFactory.createTitledBorder("Interpretation")
        SIDEPANE.layout = FlowLayout()
        val SIDE_INT = JPanel(GridBagLayout())
        SIDEPANE.add(SIDE_INT)
        val C1 = GridBagConstraints()
        val C2 = GridBagConstraints()
        C1.anchor = GridBagConstraints.LINE_END
        C2.anchor = GridBagConstraints.LINE_START
        C2.fill = GridBagConstraints.HORIZONTAL
        C1.gridx = 0
        C1.gridy = 0
        C2.gridx = 1
        C2.gridy = 0
        C1.ipadx = 5
        C1.ipady = 5
        SIDE_INT.add(JLabel("Data Type"), C1)
        SIDE_INT.add(JLabel("Data Value"), C2)
        for (type in DataType.values()) {
            val CONTAINER1 = JPanel(FlowLayout(FlowLayout.RIGHT))
            val CONTAINER2 = JPanel(FlowLayout(FlowLayout.LEFT))
            val DATAFIELD = JTextPane()
            val LABEL = JLabel(type.name)
            DATAFIELD.background = Color.WHITE
            DATAFIELD.border = BorderFactory.createEtchedBorder()
            DATAFIELD.contentType = "text/html"
            LABEL.labelFor = DATAFIELD
            CONTAINER1.add(LABEL)
            CONTAINER2.add(DATAFIELD)
            C1.gridy++
            C2.gridy++
            SIDE_INT.add(LABEL, C1)
            SIDE_INT.add(DATAFIELD, C2)
            FIELDS[type] = DATAFIELD
        }
        for (i in 0..5) {
            val CONTAINER1 = JPanel(FlowLayout(FlowLayout.RIGHT))
            val CONTAINER2 = JPanel(FlowLayout(FlowLayout.LEFT))
            val DATAFIELD = JTextField(8)
            val COLOR = JButton("COLOR")
            DATAFIELD.background = Color.WHITE
            DATAFIELD.border = BorderFactory.createEtchedBorder()
            CONTAINER1.add(DATAFIELD)
            CONTAINER2.add(COLOR)
            C1.gridy++
            C2.gridy++
            SIDE_INT.add(DATAFIELD, C1)
            SIDE_INT.add(COLOR, C2)
            SEARCH[DATAFIELD] = COLOR
            DATAFIELD.addActionListener { l: ActionEvent? -> updateFormatting() }
            COLOR.addActionListener { l: ActionEvent ->
                val source = l.source as JButton
                val color = JColorChooser.showDialog(this, "Select Color", source.background)
                source.background = color
            }
        }
        TEXTPANE.addCaretListener { e: CaretEvent? -> updateFormatting() }
        TEXTPANE.addCaretListener { e: CaretEvent? -> updateFormatting() }
        refill()
    }

    fun addHyperlinkListener(listener: HyperlinkListener?) {
        FIELDS.values.forEach { field: JTextPane -> field.addHyperlinkListener(listener) }
    }

    fun refill() {
        //this.currentSlice = this.DATA.slice(this.DATA.position() - OFFSET, SIZE);
        currentSlice = DATA.slice()
        currentSlice.limit(SIZE)
        currentSlice.order(ByteOrder.LITTLE_ENDIAN)
        try {
            val DOC: StyledDocument = DefaultStyledDocument()
            while (currentSlice.hasRemaining()) {
                val B = currentSlice.get()
                val STR = String.format("%02x ", B)
                DOC.insertString(DOC.length, STR, BINARY)
            }
            (currentSlice as Buffer).flip()
            TEXTPANE.document = DOC
            if (DATA.limit() > 0) {
                TEXTPANE.caretPosition = 1
                TEXTPANE.caretPosition = 0
            }
        } catch (ex: BadLocationException) {
            ex.printStackTrace(System.err)
            assert(false)
        }
    }

    private fun updateFormatting() {
        val DOC = TEXTPANE.styledDocument
        val CARET = TEXTPANE.caret
        val TEXT_POS = CARET.dot
        val TEXT_MARK = CARET.mark
        if (TEXT_POS == TEXT_MARK) {
            val DATA_POS = TEXT_POS / 3
            val TEXT_START = DATA_POS * 3
            val BUFFER_POS = 0
            if (TEXT_POS % 3 == 1) {
                TEXTPANE.caretPosition = TEXT_START + 3
                return
            } else if (TEXT_POS % 3 == 2) {
                TEXTPANE.caretPosition = TEXT_START
                return
            }
            HIGHLIGHTS.forEach { h: Highlight ->
                val style = COLORS.computeIfAbsent(h.COLOR) { color: Color ->
                    val s = DOC.addStyle("binary_$color", BINARY)
                    StyleConstants.setBackground(s, color)
                    s
                }
                DOC.setCharacterAttributes(h.C1 * 3, h.C2 * 3, style, false)
            }
            DOC.setCharacterAttributes(0, DOC.length, BINARY, true)
            DOC.setCharacterAttributes(BUFFER_POS, 2, DATAPOS, false)
            DOC.setCharacterAttributes(TEXT_START, 2, CURSOR, false)
            SEARCH.forEach { (field: JTextField, button: JButton) ->
                val color = button.background
                val style = COLORS.computeIfAbsent(color) { c: Color ->
                    val s = DOC.addStyle("binary_$c", BINARY)
                    StyleConstants.setBackground(s, c)
                    s
                }
                val text = field.text.lowercase(locale = Locale.getDefault())
                val length = text.length
                if (length > 0) {
                    var i = 0
                    while (i < DOC.length - length) {
                        try {
                            if (DOC.getText(i, length) == text) {
                                DOC.setCharacterAttributes(i, length, style, false)
                            }
                        } catch (ex: BadLocationException) {
                            val breakpoint = 0
                        }
                        i += 3
                    }
                }
            }

            /*for (int i = 0; i < DOC.getLength() / 3; i++) {
                this.currentSlice.s
                ByteBuffer slice = this.DATA.slice(i, DOC.getLength() - i);
                try {
                    String f = mf.BufferUtil.getWString(slice);
                    if (validString(f)) {
                        DOC.setCharacterAttributes(i * 3, 6 + 3 * f.length(), STRING, false);
                    }
                } catch (BufferUnderflowException ex) {

                }
            }*/try {
                val COLUMN = (TEXT_POS - Utilities.getRowStart(TEXTPANE, TEXT_POS)) / 3
                val TITLE = String.format("[%2x], Offset = %02x (%d) bytes", COLUMN, DATA_POS, DATA_POS)
                (SCROLLER.border as TitledBorder).title = TITLE
                SCROLLER.updateUI()
            } catch (ex: BadLocationException) {
                val TITLE = String.format("Offset = %02x (%d) bytes", DATA_POS, DATA_POS)
                (SCROLLER.border as TitledBorder).title = TITLE
                SCROLLER.updateUI()
            }
            fillFields(DATA_POS)
        } else {
            val start = TEXT_POS.coerceAtMost(TEXT_MARK)
            val end = TEXT_POS.coerceAtLeast(TEXT_MARK)
            val dst = start / 3
            val den = end / 3
            val newStart = dst * 3
            val newEnd = den * 3
            if (newStart != start) {
                TEXTPANE.selectionStart = newStart
            }
            if (newEnd != end) {
                TEXTPANE.selectionEnd = newEnd
            }
        }
    }

    /**
     * Data types that will be interpreted.
     */
    private enum class DataType {
        Integer_10,
        Integer_16,
        Integer_2,
        Float,
        Boolean,
        RefID,
        EID32,
        EID64,
        TString,
        BString,
        WString,
        LString,
        ZString,
        Variable
    }

    /**
     *
     * @param c1
     * @param c2
     * @param color
     */
    fun addHighlight(c1: Int, c2: Int, color: Color) {
        val len = TEXTPANE.document.length
        require(!(c1 < 0 || c1 >= len))
        require(!(c2 < 0 || c2 >= len))
        HIGHLIGHTS.add(Highlight(c1, c2, color))
    }

    /**
     * Fill the interpretation fields.
     *
     * @param dataPos
     */
    private fun fillFields(dataPos: Int) {
        for (type in DataType.values()) {
            val FIELD = FIELDS[type]
            FIELD!!.text = ""
            FIELD.background = Color.WHITE
            val BUF = StringBuilder()
            BUF.append("<code>")
            val MAXLEN = 40
            currentSlice.position(dataPos)
            try {
                when (type) {
                    DataType.Integer_10 -> {
                        val `val` = currentSlice.int
                        BUF.append(`val`)
                    }
                    DataType.Integer_16 -> {
                        val `val` = currentSlice.int
                        val str = String.format("%08x", `val`)
                        BUF.append(str)
                    }
                    DataType.Integer_2 -> {
                        val `val`: Flags = Flags.readIntFlags(currentSlice)
                        BUF.append(`val`)
                    }
                    DataType.Float -> {
                        val `val` = currentSlice.float
                        BUF.append(`val`)
                    }
                    DataType.Boolean -> {
                        val `val` = currentSlice.get().toInt()
                        BUF.append(`val` != 0)
                    }
                    DataType.BString -> {
                        var `val` = mf.BufferUtil.getBString(currentSlice)
                        if (`val`?.let { validString(it) }?.not() == true) {
                            FIELD.background = Color.LIGHT_GRAY
                        } else {
                            if (`val` != null) {
                                if (`val`.length > MAXLEN) {
                                    `val` = `val`.substring(0, MAXLEN - 3) + "..."
                                }
                            }
                            BUF.append(`val`)
                        }
                    }
                    DataType.WString -> {
                        var `val` = mf.BufferUtil.getWString(currentSlice)
                        if (`val`?.let { validString(it).not() } == true) {
                            FIELD.background = Color.LIGHT_GRAY
                        } else {
                            if (`val` != null) {
                                if (`val`.length > MAXLEN) {
                                    `val` = `val`.substring(0, MAXLEN - 3) + "..."
                                }
                            }
                            BUF.append(`val`)
                        }
                    }
                    DataType.LString -> {
                        var `val` = mf.BufferUtil.getLString(currentSlice)
                        if (`val`?.let { validString(it).not() } == true) {
                            FIELD.background = Color.LIGHT_GRAY
                        } else {
                            if (`val` != null) {
                                if (`val`.length > MAXLEN) {
                                    `val` = `val`.substring(0, MAXLEN - 3) + "..."
                                }
                            }
                            BUF.append(`val`)
                        }
                    }
                    DataType.ZString -> {
                        var `val` = mf.BufferUtil.getZString(currentSlice)
                        if (`val`?.let { validString(it).not() } == true) {
                            FIELD.background = Color.LIGHT_GRAY
                        } else {
                            if (`val` != null) {
                                if (`val`.length > MAXLEN) {
                                    `val` = `val`.substring(0, MAXLEN - 3) + "..."
                                }
                            }
                            BUF.append(`val`)
                        }
                    }
                    DataType.RefID -> if (ESS_CONTEXT != null) {
                        val `val` = ESS_CONTEXT.readRefID(currentSlice)
                        BUF.append(`val`.toHTML(null))
                    }
                    DataType.EID32 -> if (PAPYRUS_CONTEXT != null) {
                        val `val` = currentSlice.int
                        if (null != SAVE) {
                            val link = PAPYRUS_CONTEXT.broadSpectrumSearch(`val`)
                            if (null != link) {
                                BUF.append(link.javaClass.name).append(": ").append(link.toHTML(null))
                                break
                            }
                        }
                        BUF.append(PAPYRUS_CONTEXT.makeEID32(`val`))
                        FIELD.background = Color.LIGHT_GRAY
                    }
                    DataType.EID64 -> if (PAPYRUS_CONTEXT != null) {
                        val `val` = currentSlice.long
                        if (null != SAVE) {
                            val link = PAPYRUS_CONTEXT.broadSpectrumSearch(`val`)
                            if (null != link) {
                                BUF.append(link.javaClass.name).append(": ").append(link.toHTML(null))
                                break
                            }
                        }
                        BUF.append(PAPYRUS_CONTEXT.makeEID64(`val`))
                        FIELD.background = Color.LIGHT_GRAY
                    }
                    DataType.TString -> if (PAPYRUS_CONTEXT != null) {
                        if (null != SAVE) {
                            val str = PAPYRUS_CONTEXT.readTString(currentSlice)
                            BUF.append(str.toHTML(null))
                        } else {
                            FIELD.background = Color.LIGHT_GRAY
                        }
                    }
                    DataType.Variable -> if (PAPYRUS_CONTEXT != null) {
                        val `var` = Variable.read(currentSlice, PAPYRUS_CONTEXT)
                        BUF.append(`var`.toHTML(null))
                        when (`var`) {
                            is VarArray -> {
                                addHighlight(dataPos, `var`.calculateSize(), Color.PINK)
                            }
                            is VarRef -> {
                                addHighlight(dataPos, `var`.calculateSize(), Color.GREEN)
                            }
                            is VarNull -> {
                                //this.addHighlight(dataPos, var.calculateSize(), Color.GREEN);
                            }
                            is VarNull -> {
                                addHighlight(dataPos, `var`.calculateSize(), Color.YELLOW)
                            }
                        }
                    }
                }
                BUF.append("</code>")
                FIELD.text = BUF.toString()
            } catch (ex: PapyrusFormatException) {
                FIELD.background = Color.LIGHT_GRAY
            } catch (ex: BufferUnderflowException) {
                FIELD.background = Color.LIGHT_GRAY
            } catch (ex: NullPointerException) {
                FIELD.background = Color.LIGHT_GRAY
            }
        }
    }

    val DATA: ByteBuffer
    private val SAVE: ess.ESS?
    private val ESS_CONTEXT: ESSContext?
    private val PAPYRUS_CONTEXT: PapyrusContext?
    private val ANALYSIS: Analysis?
    private var currentSlice: ByteBuffer
    private val SIZE: Int
    private val SCROLLER: JScrollPane
    private val TEXTPANE: JTextPane
    private val SIDEPANE: JPanel
    private val FIELDS: MutableMap<DataType, JTextPane>
    private val SEARCH: MutableMap<JTextField, JButton>
    private val BINARY: Style
    private val DATAPOS: Style
    private val CURSOR: Style
    private val STRING: Style
    private val FLOAT: Style
    private val COLORS: MutableMap<Color, Style>
    private val HIGHLIGHTS: MutableList<Highlight>

    private class Highlight(val C1: Int, val C2: Int, val COLOR: Color)
    companion object {
        @JvmStatic
        fun showDataAnalyzer(window: Window?, data: ByteBuffer, ess: ess.ESS) {
            val ANALYZER = DataAnalyzer(data, ess)
            val DIALOG = JDialog(window, "Analyze")
            DIALOG.contentPane = ANALYZER
            DIALOG.pack()
            DIALOG.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
            DIALOG.isVisible = true
        }

        /**
         *
         * @param s
         * @return
         */
        fun validString(s: String): Boolean {
            return stringValidity(s) > 0.5
        }

        /**
         *
         * @param s
         * @return
         */
        fun stringValidity(s: String): Double {
            if (s.isEmpty()) {
                return 0.0
            }
            val invalid = s.chars().filter { codePoint: Int -> Character.isISOControl(codePoint) }.count()
            if (invalid > 0) {
                return 0.0
            }
            val valid = s.chars().filter { codePoint: Int -> Character.isLetterOrDigit(codePoint) }.count()
            return valid.toDouble() / s.length
        }
    }

    init {
        DATA = newData.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        SAVE = save
        ANALYSIS = save.analysis
        ESS_CONTEXT = save.context
        PAPYRUS_CONTEXT = if (save.papyrus == null) null else save.papyrus.context
        currentSlice = DATA.slice()
        SIZE = 2048.coerceAtMost(currentSlice.limit())
        currentSlice.limit(SIZE)
        currentSlice.order(ByteOrder.LITTLE_ENDIAN)
        TEXTPANE = JTextPane()
        SCROLLER = JScrollPane(TEXTPANE)
        SIDEPANE = JPanel()
        HIGHLIGHTS = mutableListOf()
        FIELDS = mutableMapOf()
        SEARCH = hashMapOf()
        COLORS = hashMapOf()
        TEXTPANE.document = DefaultStyledDocument()
        val DOC = TEXTPANE.styledDocument
        BINARY = DOC.addStyle("default", null)
        StyleConstants.setFontFamily(BINARY, "Courier New")
        CURSOR = DOC.addStyle("cursor", BINARY)
        StyleConstants.setBackground(CURSOR, Color.CYAN)
        StyleConstants.setBold(CURSOR, true)
        STRING = DOC.addStyle("string", BINARY)
        StyleConstants.setUnderline(STRING, true)
        FLOAT = DOC.addStyle("float", BINARY)
        StyleConstants.setItalic(FLOAT, true)
        DATAPOS = DOC.addStyle("datapos", CURSOR)
        StyleConstants.setBackground(DATAPOS, Color.LIGHT_GRAY)
        initComponents()
    }
}