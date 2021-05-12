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

import java.awt.Dimension
import javax.swing.JScrollPane

/**
 *
 * @author Mark
 */
class TextDialog(newText: String?) : JScrollPane() {
    private val TEXT: InfoPane = InfoPane(newText, null)

    /**
     * Creates a new `TextDialog` with a default preferred size of
     * 600x400 and a block of HTML formatted text to display.
     *
     * @param newText HTML formatted text.
     */
    init {
        TEXT.text = newText
        super.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_AS_NEEDED)
        super.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS)
        super.setViewportView(TEXT)
        super.setPreferredSize(Dimension(600, 400))
    }
}