package br.com.banco.cliente.util;

import javax.swing.*;
import java.awt.*;

public class IconUtil {
    public static ImageIcon loadIcon(String path, int width, int height) {
        try {
            ImageIcon icon = new ImageIcon(IconUtil.class.getResource("/icons/" + path));
            Image img = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(img);
        } catch (Exception e) {
            System.err.println("Não foi possível carregar o ícone: " + path);
            return null;
        }
    }
}