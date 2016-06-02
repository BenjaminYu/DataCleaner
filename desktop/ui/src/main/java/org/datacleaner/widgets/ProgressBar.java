/**
 * DataCleaner (community edition)
 * Copyright (C) 2014 Neopost - Customer Information Management
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.datacleaner.widgets;

import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.WindowConstants;

public class ProgressBar {
    private ProgressBarDialog _dialog;
    private Thread _thread;

    public void show() {
        _dialog = new ProgressBarDialog();
        _thread = new Thread(_dialog);
        _thread.start();
    }

    public void hide() {
        _dialog.setVisible(false);
        WindowEvent closingEvent = new WindowEvent(_dialog, WindowEvent.WINDOW_CLOSING);
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(closingEvent);
    }

    public void update(String text) {
        _dialog.setText(text);
    }

    private class ProgressBarDialog extends JDialog implements Runnable {
        private static final String PATH = "/images/status/loading.gif";
        private static final int MARGIN = 10;
        private static final int FRAME_HEIGHT = 25;
        private final JLabel _label = new JLabel("", JLabel.CENTER);

        private ProgressBarDialog() {
            setUndecorated(true);
            setAlwaysOnTop(true);
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

            final URL imageUrl = getClass().getResource(PATH);
            final ImageIcon image = new ImageIcon(imageUrl);
            _label.setIcon(image);
            setText("");
            add(_label);
        }

        public void run() {
            setVisible(true);
        }

        private void setText(String text) {
            _label.setText(text);
            setSize(_label.getPreferredSize().width + 2 * MARGIN, FRAME_HEIGHT + MARGIN);
            setLocationRelativeTo(null);
        }
    }
}
