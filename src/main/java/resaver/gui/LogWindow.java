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
package resaver.gui;

import javax.swing.*;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Displays progress messages in a JFrame. It receives messages via the Java
 * logging system.
 *
 * To use it, a <code>Handler</code> must be retrieved using the
 * <code>getHandler</code> method and attached to an instance of
 * <code>Logger</code>.
 *
 * @see java.util.logging.Logger
 * @see java.util.logging.Handler
 *
 * @author Mark
 */
public class LogWindow extends JScrollPane { 

    /**
     * Creates a new <code>LogWindow</code> with a default preferred size of
     * 480x400.
     */
    public LogWindow() {
        this.HANDLER = new LogWindowHandler();
        this.TEXT = new JTextArea();
        this.TEXT.setWrapStyleWord(true);
        this.TEXT.setLineWrap(true);
        super.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        super.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        super.setViewportView(this.TEXT);
        this.TEXT.setFont(this.TEXT.getFont().deriveFont(12.0f));
        //super.add(SCROLLER);
        super.setPreferredSize(new java.awt.Dimension(600, 400));
    }

    /**
     * @return A <code>Handler</code> for the Java logging system.
     */
    public Handler getHandler() {
        return this.HANDLER;
    }

    /**
     * This class handles the job of receiving log messages and displaying them.
     */
    private class LogWindowHandler extends Handler {

        public LogWindowHandler() {

        }

        @Override
        public void publish(LogRecord record) {
            LogWindow.this.TEXT.append(record.getMessage() + "\n");
            LogWindow.this.TEXT.setCaretPosition(LogWindow.this.TEXT.getDocument().getLength());
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }

    final private LogWindowHandler HANDLER;
    final private JTextArea TEXT;

}
