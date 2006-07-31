/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.ldap.gui;


import java.awt.BorderLayout;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JDialog;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.BoxLayout;
import javax.swing.JLabel;


public class ShutdownDialog extends JDialog
{
    private static final long serialVersionUID = -6681747075037789868L;

    private JPanel jContentPane = null;
    private JPanel inputsPanel = null;
    private JPanel buttonsPanel = null;
    private JButton sendButton = null;
    private JButton cancelButton = null;
    private JPanel jPanel = null;
    private JPanel jPanel1 = null;
    private JLabel jLabel = null;
    private JTextField timeOfflineField = null;
    private JLabel jLabel1 = null;
    private JTextField delayField = null;
    private boolean canceled = true;


    /**
     * This is the default constructor
     */
    public ShutdownDialog()
    {
        super();
        initialize();
    }


    public boolean isSendCanceled()
    {
        return canceled;
    }


    public int getTimeOffline()
    {
        return Integer.parseInt( timeOfflineField.getText() );
    }


    public int getDelay()
    {
        return Integer.parseInt( delayField.getText() );
    }


    public boolean isCanceled()
    {
        return canceled;
    }


    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize()
    {
        this.setSize( 248, 171 );
        this.setTitle( "Shutdown Parameters" );
        this.setContentPane( getJContentPane() );
    }


    /**
     * This method initializes jContentPane
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJContentPane()
    {
        if ( jContentPane == null )
        {
            jContentPane = new JPanel();
            jContentPane.setLayout( new BorderLayout() );
            jContentPane.add( getJPanel(), java.awt.BorderLayout.CENTER );
            jContentPane.add( getJPanel2(), java.awt.BorderLayout.SOUTH );
        }
        return jContentPane;
    }


    /**
     * This method initializes jPanel	
     * 	
     * @return javax.swing.JPanel	
     */
    private JPanel getJPanel()
    {
        if ( inputsPanel == null )
        {
            inputsPanel = new JPanel();
            inputsPanel.setLayout( null );
            inputsPanel.setBorder( javax.swing.BorderFactory
                .createEtchedBorder( javax.swing.border.EtchedBorder.RAISED ) );
            inputsPanel.add( getJPanel3(), null );
            inputsPanel.add( getJPanel1(), null );
        }
        return inputsPanel;
    }


    /**
     * This method initializes jPanel	
     * 	
     * @return javax.swing.JPanel	
     */
    private JPanel getJPanel2()
    {
        if ( buttonsPanel == null )
        {
            buttonsPanel = new JPanel();
            buttonsPanel.add( getJButton(), null );
            buttonsPanel.add( getJButton2(), null );
        }
        return buttonsPanel;
    }


    /**
     * This method initializes jButton	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getJButton()
    {
        if ( sendButton == null )
        {
            sendButton = new JButton();
            sendButton.setText( "Send" );
            sendButton.addActionListener( new java.awt.event.ActionListener()
            {
                public void actionPerformed( java.awt.event.ActionEvent e )
                {
                    int timeOffline = 0;
                    try
                    {
                        timeOffline = Integer.parseInt( timeOfflineField.getText() );
                        if ( timeOffline > 720 || timeOffline < 0 )
                        {
                            JOptionPane.showMessageDialog( ShutdownDialog.this,
                                "Time Offline is out of range: 0 ... 720", "Range Problem", JOptionPane.ERROR_MESSAGE );
                            timeOfflineField.setText( "" );
                            return;
                        }
                    }
                    catch ( NumberFormatException nfe )
                    {
                        JOptionPane.showMessageDialog( ShutdownDialog.this,
                            "The value for Time Offline is not a number", "Not a Number", JOptionPane.ERROR_MESSAGE );
                        timeOfflineField.setText( "" );
                        return;
                    }
                    int delay = 0;
                    try
                    {
                        delay = Integer.parseInt( delayField.getText() );
                        if ( delay > 86400 || delay < 0 )
                        {
                            JOptionPane.showMessageDialog( ShutdownDialog.this, "Delay is out of range: 0 ... 86400",
                                "Range Problem", JOptionPane.ERROR_MESSAGE );
                            delayField.setText( "" );
                            return;
                        }
                    }
                    catch ( NumberFormatException nfe )
                    {
                        JOptionPane.showMessageDialog( ShutdownDialog.this, "Delay is not a number", "Not a Number",
                            JOptionPane.ERROR_MESSAGE );
                        delayField.setText( "" );
                        return;
                    }
                    canceled = false;
                    setVisible( false );
                    dispose();
                }
            } );
        }
        return sendButton;
    }


    /**
     * This method initializes jButton	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getJButton2()
    {
        if ( cancelButton == null )
        {
            cancelButton = new JButton();
            cancelButton.setText( "Cancel" );
            cancelButton.setSelected( true );
            cancelButton.addActionListener( new java.awt.event.ActionListener()
            {
                public void actionPerformed( java.awt.event.ActionEvent e )
                {
                    canceled = true;
                    setVisible( false );
                    dispose();
                    return;
                }
            } );
        }
        return cancelButton;
    }


    /**
     * This method initializes jPanel	
     * 	
     * @return javax.swing.JPanel	
     */
    private JPanel getJPanel3()
    {
        if ( jPanel == null )
        {
            jLabel = new JLabel();
            jLabel.setText( "Minutes Offline: " );
            jPanel = new JPanel();
            jPanel.setLayout( new BoxLayout( getJPanel3(), BoxLayout.X_AXIS ) );
            jPanel.setBounds( new java.awt.Rectangle( 35, 28, 163, 16 ) );
            jPanel.add( jLabel, null );
            jPanel.add( getJTextField(), null );
        }
        return jPanel;
    }


    /**
     * This method initializes jPanel1	
     * 	
     * @return javax.swing.JPanel	
     */
    private JPanel getJPanel1()
    {
        if ( jPanel1 == null )
        {
            jLabel1 = new JLabel();
            jLabel1.setText( "Seconds Delay: " );
            jPanel1 = new JPanel();
            jPanel1.setLayout( new BoxLayout( getJPanel1(), BoxLayout.X_AXIS ) );
            jPanel1.setBounds( new java.awt.Rectangle( 42, 57, 156, 16 ) );
            jPanel1.add( jLabel1, null );
            jPanel1.add( getJTextField1(), null );
        }
        return jPanel1;
    }


    /**
     * This method initializes jTextField	
     * 	
     * @return javax.swing.JTextField	
     */
    private JTextField getJTextField()
    {
        if ( timeOfflineField == null )
        {
            timeOfflineField = new JTextField();
        }
        return timeOfflineField;
    }


    /**
     * This method initializes jTextField1	
     * 	
     * @return javax.swing.JTextField	
     */
    private JTextField getJTextField1()
    {
        if ( delayField == null )
        {
            delayField = new JTextField();
        }
        return delayField;
    }

} //  @jve:decl-index=0:visual-constraint="10,10"
