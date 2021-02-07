/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package johnkennedypena;

import javax.swing.*;
import java.awt.*;
/**
 *
 * @author ACER
 */
public class windowModified_ extends Canvas{
    public windowModified_(int width, int height, String title, TestClassForPixelArt game) throws Exception {
        JFrame frame = new JFrame(title);
        
        frame.setPreferredSize(new Dimension(width, height));
        frame.setMaximumSize(new Dimension(width, height));
        frame.setMinimumSize(new Dimension(width, height));
        
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(true);
        frame.setLocationRelativeTo(null);
        frame.add(game);
        frame.setVisible(true);
        
        game.start();
    }
}
