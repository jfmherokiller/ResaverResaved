/*
 * Copyright 2020 Mark.
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

import javax.swing.event.HyperlinkListener
import javax.swing.JTextPane

/**
 * Displays HTML formatted text and supports a hyperlink listener.
 *
 * @author Mark Fairchild
 */
class InfoPane(text: String?, listener: HyperlinkListener?) : JTextPane() {
    /**
     *
     * @param text
     */
    override fun setText(text: String) {
        super.setText(text)
        super.setCaretPosition(0)
    }

    /**
     * @param text
     * @param listener
     */
    init {
        super.setEditable(false)
        super.setContentType("text/html")
        if (text != null) {
            this.text = text;
        }
        if (listener != null) {
            super.addHyperlinkListener(listener);
        }
    }
}