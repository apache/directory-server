/*
 * $Id: JdbmBackendViewer.java,v 1.2 2003/03/13 18:27:24 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm.gui ;

import javax.swing.UIManager ;
import java.awt.Dimension ;
import java.awt.Toolkit ;
import java.io.File ;
import javax.swing.JFileChooser ;

import org.apache.log.Priority;
import org.apache.log.Hierarchy;


public class JdbmBackendViewer
{
    public JdbmBackendViewer()
    {
        BackendFrame frame = new BackendFrame() ;

        String l_startPath = null ;
        if(File.separatorChar == '/') {
            l_startPath = "../projects" ;
        } else {
            l_startPath = "..\\projects" ;
        }

        final JFileChooser fc = new JFileChooser(l_startPath) ;
        int returnVal = fc.showOpenDialog(frame);
        if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile() ;
            try {
                frame.loadDatabase("ldapd.test.Harness", file.getParent()) ;
            } catch(Exception ex) {
                ex.printStackTrace() ;
            }
        } else {
            // Write your code here what to do if user has canceled Open dialog
        }

        //Center the frame on screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = frame.getSize();
        frameSize.height = ((frameSize.height > screenSize.height)
            ? screenSize.height : frameSize.height);
        frameSize.width = ((frameSize.width > screenSize.width)
            ? screenSize.width : frameSize.width);
        frame.setLocation((screenSize.width - frameSize.width) / 2,
            (screenSize.height - frameSize.height) / 2);
        frame.setVisible(true);
        System.out.println(frameSize) ;
    }


    public static void main(String[] argv) {
        // set up system Look&Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e) {
            System.out.println("Could not set look and feel to use " +
                UIManager.getSystemLookAndFeelClassName() + ".") ;
            //e.printStackTrace() ;
        }
        new JdbmBackendViewer ();
    }
}
