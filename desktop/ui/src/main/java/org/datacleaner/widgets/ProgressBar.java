package org.datacleaner.widgets;

import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProgressBar {
    private static final Logger logger = LoggerFactory.getLogger(ProgressBar.class);
    private ProgressBarFrame _frame;
    private Thread _thread;

    private class ProgressBarFrame extends JFrame implements Runnable {
        private static final String PATH = "/images/status/loading.gif";
        private static final int MARGIN = 10;
        private static final int FRAME_HEIGHT = 25;
        private final JLabel _label = new JLabel("", JLabel.CENTER);

        private ProgressBarFrame() {
            setUndecorated(true);
            setAlwaysOnTop(true);
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

            final URL imageUrl = getClass().getResource(PATH);
            final ImageIcon image = new ImageIcon(imageUrl);
            _label.setIcon(image);
            setText("");
            add(_label);
        }

        public void run() {
            setVisible(true);
        }

        private void terminate() {
            setVisible(false);
            dispose();
        }

        private void setText(String text) {
            _label.setText(text);
            setSize(_label.getPreferredSize().width + 2 * MARGIN, FRAME_HEIGHT + MARGIN);
            setLocationRelativeTo(null);
        }
    }

    public void show() {
        _frame = new ProgressBarFrame();
        _thread = new Thread(_frame);
        _thread.start();
    }

    public void hide() {
        try {
            _frame.terminate();
            _thread.interrupt();
            _thread.join();
        } catch (InterruptedException e) {
            logger.error("An error occurred during progress bar hiding. ", e.getMessage());
        }
    }

    public void update(String text) {
        _frame.setText(text);
    }
}
