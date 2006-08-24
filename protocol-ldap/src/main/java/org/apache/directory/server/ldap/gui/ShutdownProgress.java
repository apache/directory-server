/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.ldap.gui;


import java.awt.BorderLayout;
import java.awt.Cursor;

import javax.swing.JPanel;
import javax.swing.JDialog;
import javax.swing.JButton;
import javax.swing.JProgressBar;


public class ShutdownProgress extends JDialog implements Runnable
{
    private static final long serialVersionUID = 1L;
    private JPanel jContentPane = null;
    private JPanel jPanel = null;
    private JButton jButton = null;
    private JProgressBar jProgressBar = null;
    private long timeMillis = 0;
    private boolean bypass = false;


    public void setTime( long millis )
    {
        this.timeMillis = millis;
    }


    public void run()
    {
        setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
        jProgressBar.setEnabled( true );
        jProgressBar.setMinimum( 0 );
        jProgressBar.setMaximum( ( int ) timeMillis );
        jProgressBar.setValue( 0 );
        jProgressBar.setStringPainted( true );
        final long startTime = System.currentTimeMillis();
        while ( System.currentTimeMillis() - startTime < timeMillis && !bypass )
        {
            try
            {
                Thread.sleep( 100 );
            }
            catch ( InterruptedException e )
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            jProgressBar.setString( ( timeMillis - ( System.currentTimeMillis() - startTime ) ) / 1000
                + " seconds remaining ..." );
            jProgressBar.setValue( jProgressBar.getValue() + 100 );
            this.repaint();
        }

        setCursor( null );
        setVisible( false );
        dispose();
    }


    /**
     * This is the default constructor
     */
    public ShutdownProgress()
    {
        super();
        initialize();
    }


    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize()
    {
        this.setSize( 300, 104 );
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
            jContentPane.add( getJPanel(), java.awt.BorderLayout.SOUTH );
            jContentPane.add( getJProgressBar(), java.awt.BorderLayout.CENTER );
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
        if ( jPanel == null )
        {
            jPanel = new JPanel();
            jPanel.add( getJButton(), null );
        }
        return jPanel;
    }


    /**
     * This method initializes jButton	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getJButton()
    {
        if ( jButton == null )
        {
            jButton = new JButton();
            jButton.setText( "Bypass Delay" );
            jButton.setText( "Bypass Delay" );
            jButton.addActionListener( new java.awt.event.ActionListener()
            {
                public void actionPerformed( java.awt.event.ActionEvent e )
                {
                    bypass = true;
                }
            } );
        }
        return jButton;
    }


    /**
     * This method initializes jProgressBar	
     * 	
     * @return javax.swing.JProgressBar	
     */
    private JProgressBar getJProgressBar()
    {
        if ( jProgressBar == null )
        {
            jProgressBar = new JProgressBar();
        }
        return jProgressBar;
    }

} //  @jve:decl-index=0:visual-constraint="10,10"
