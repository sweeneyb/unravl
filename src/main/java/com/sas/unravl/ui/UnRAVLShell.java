/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sas.unravl.ui;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

/**
 *
 * @author David.Biesack@sas.com
 */
public class UnRAVLShell {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        JFrame frame = UnRAVLFrame.main(args);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

}
