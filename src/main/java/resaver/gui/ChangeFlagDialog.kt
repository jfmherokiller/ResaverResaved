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
package resaver.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.Arrays;
import mf.BiIntConsumer;

/**
 *
 * @author Mark
 */
public class ChangeFlagDialog extends JDialog {

    /**
     *
     * @param parent
     * @param mask
     * @param filter
     * @param done
     */
    public ChangeFlagDialog(SaveWindow parent, int mask, int filter, BiIntConsumer done) {
        super(parent, "", true);
        this.FLAGS = new JButton[32];

        this.mask = mask;
        this.filter = filter;

        int m = mask;
        int f = filter;

        for (int i = 0; i < 32; i++) {
            this.FLAGS[i] = new JButton();
        }

        this.maskField = new JFormattedTextField(new mf.Hex32Formatter());
        this.filterField = new JFormattedTextField(new mf.Hex32Formatter());

        this.maskField.setValue(this.mask);
        this.filterField.setValue(this.filter);
        this.FILTER = new JButton("Set Filter");
        this.CLEAR = new JButton("Clear");
        this.CANCEL = new JButton("Cancel");

        this.maskField.setColumns(8);
        this.filterField.setColumns(8);
        this.maskField.addPropertyChangeListener("value", e -> updateMask());
        this.filterField.addPropertyChangeListener("value", e -> updateFilter());
        super.setLayout(new BorderLayout());

        final JPanel TOP = new JPanel(new FlowLayout());
        final JPanel CENTER = new JPanel(new GridLayout(2, 32));
        final JPanel BOTTOM = new JPanel(new FlowLayout());
        super.add(TOP, BorderLayout.NORTH);
        super.add(BOTTOM, BorderLayout.SOUTH);
        super.add(CENTER, BorderLayout.CENTER);
        TOP.add(new JLabel("Mask"));
        TOP.add(this.maskField);
        TOP.add(new JLabel("Filter"));
        TOP.add(this.filterField);
        BOTTOM.add(this.FILTER);
        BOTTOM.add(this.CLEAR);
        BOTTOM.add(this.CANCEL);

        for (int i = 0; i < 32; i++) {
            CENTER.add(new JLabel(Integer.toString(31 - i), JLabel.CENTER));
        }
        for (int i = 0; i < 32; i++) {
            JButton b = this.FLAGS[31 - i];
            CENTER.add(b);
            b.addActionListener(e -> flagToggle((JButton) e.getSource()));
        }

        this.FILTER.addActionListener(e -> {
            this.setVisible(false);
            done.consume(this.mask, this.filter);
        });

        this.CANCEL.addActionListener(e -> this.setVisible(false));

        this.CLEAR.addActionListener(e -> {
            this.mask = 0;
            this.filter = 0;
            this.update();
        });

        super.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.update();
    }

    /**
     *
     */
    private void update() {
        int m = this.mask;
        int f = this.filter;

        for (int i = 0; i < 32; i++) {
            JButton flag = this.FLAGS[i];
            int x = m & 0x1;
            int v = f & 0x1;
            m >>>= 1;
            f >>>= 1;
            if (x == 0) {
                flag.setText("x");
            } else if (v == 0) {
                flag.setText("0");
            } else {
                flag.setText("1");
            }
        }
    }

    /**
     *
     */
    private void updateMask() {
        try {
            this.mask = (Integer) this.maskField.getValue();
            this.update();
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Illegal mask and/or field.", ex);
        }
    }

    /**
     *
     */
    private void updateFilter() {
        try {
            this.filter = (Integer) this.filterField.getValue();
            this.update();
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Illegal mask and/or field.", ex);
        }
    }

    /**
     *
     * @param b
     */
    private void flagToggle(JButton b) {
        int i = Arrays.asList(this.FLAGS).indexOf(b);

        switch (b.getText()) {
            case LABEL_X:
                this.setFlag(i, 0);
                break;
            case LABEL_0:
                this.setFlag(i, 1);
                break;
            case LABEL_1:
                this.setFlag(i, -1);
                break;
            default:
                throw new IllegalStateException("Illegal value.");
        }
    }

    /**
     *
     * @param i
     * @param val
     */
    private void setFlag(int i, int val) {
        System.out.printf("button %d, %d\n", i, val);
        JButton flag = this.FLAGS[i];
        int clearBit = ~(0x1 << i);
        int setBit = 0x1 << i;

        switch (val) {
            case -1:
                this.mask &= clearBit;
                this.filter &= clearBit;
                flag.setText(LABEL_X);
                break;
            case 0:
                this.mask |= setBit;
                this.filter &= clearBit;
                flag.setText(LABEL_0);
                break;
            case 1:
                this.mask |= setBit;
                this.filter |= setBit;
                flag.setText(LABEL_1);
                break;
            default:
                throw new IllegalStateException("Illegal flag value.");
        }

        this.maskField.setValue(this.mask);
        this.filterField.setValue(this.filter);
    }

    @Override
    public String toString() {
        return String.format("%08x / %08x", this.mask, this.filter);
    }

    private int mask;
    private int filter;
    final private JFormattedTextField maskField;
    final private JFormattedTextField filterField;
    final private JButton[] FLAGS;
    final private JButton FILTER;
    final private JButton CLEAR;
    final private JButton CANCEL;

    static final private String LABEL_X = "x";
    static final private String LABEL_0 = "0";
    static final private String LABEL_1 = "1";

}
