package SystemServices;

import SystemServices.ui.MainWindow;

import javax.swing.*;

public class App {
    public static void main(String[] args) {
        System.setProperty("sun.awt.X11.appClass", "SystemServices");
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}