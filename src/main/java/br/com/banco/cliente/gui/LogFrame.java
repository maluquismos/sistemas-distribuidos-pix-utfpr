package br.com.banco.cliente.gui;

import javax.swing.*;
import java.awt.*;

public class LogFrame extends JFrame {
    private final JTextArea logArea;

    public LogFrame() {
        super("Logs do Cliente");
        
        logArea = new JTextArea(20, 100);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        getContentPane().add(new JScrollPane(logArea));
        
        pack();
        // Posiciona a janela de log no canto, por exemplo
        setLocation(100, 100); 
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Evita fechar a aplicação por aqui
    }

    public void addLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}