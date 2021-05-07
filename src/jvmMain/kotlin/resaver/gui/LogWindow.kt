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

import java.awt.Dimension
import java.util.logging.Handler
import java.util.logging.LogRecord
import javax.swing.JScrollPane
import javax.swing.JTextArea

/**
 * Displays progress messages in a JFrame. It receives messages via the Java
 * logging system.
 *
 * To use it, a `Handler` must be retrieved using the
 * `getHandler` method and attached to an instance of
 * `Logger`.
 *
 * @see java.util.logging.Logger
 *
 * @see java.util.logging.Handler
 *
 *
 * @author Mark
 */
class LogWindow : JScrollPane() {
    /**
     * @return A `Handler` for the Java logging system.
     */
    val handler: Handler
        get() = HANDLER

    /**
     * This class handles the job of receiving log messages and displaying them.
     */
    private inner class LogWindowHandler : Handler() {
        override fun publish(record: LogRecord) {
            TEXT.append(record.message.trimIndent())
            TEXT.caretPosition = TEXT.document.length
        }

        override fun flush() {}
        override fun close() {}
    }

    private val HANDLER: LogWindowHandler = LogWindowHandler()
    private val TEXT: JTextArea = JTextArea()

    /**
     * Creates a new `LogWindow` with a default preferred size of
     * 480x400.
     */
    init {
        TEXT.wrapStyleWord = true
        TEXT.lineWrap = true
        super.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_AS_NEEDED)
        super.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS)
        super.setViewportView(TEXT)
        TEXT.font = TEXT.font.deriveFont(12.0f)
        //super.add(SCROLLER);
        super.setPreferredSize(Dimension(600, 400))
    }
}