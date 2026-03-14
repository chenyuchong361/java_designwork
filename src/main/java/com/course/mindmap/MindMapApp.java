package com.course.mindmap;

import com.course.mindmap.ui.MainFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class MindMapApp {
    private MindMapApp() {
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}

