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
package org.apache.directory.server.core.partition.impl.btree.gui;


import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.tree.TreeModel;


/**
 * Dialog for showing annotated filter trees.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AnnotatedFilterTreeDialog extends JDialog
{
    private static final long serialVersionUID = 3690476917916513074L;
    private JPanel jPanel1 = new JPanel();
    private JTree jTree1 = new JTree();
    private JPanel jPanel2 = new JPanel();
    private JPanel jPanel3 = new JPanel();
    private JTextArea jTextArea1 = new JTextArea();
    private JScrollPane jScrollPane1 = new JScrollPane();
    private JButton jButton1 = new JButton();


    /** Creates new form JDialog */
    public AnnotatedFilterTreeDialog(Frame parent, boolean modal)
    {
        super( parent, modal );
        initGUI();
    }


    /** This method is called from within the constructor to initialize the form. */
    private void initGUI()
    {
        addWindowListener( new java.awt.event.WindowAdapter()
        {
            public void windowClosing( java.awt.event.WindowEvent evt )
            {
                closeDialog( evt );
            }
        } );
        pack();
        getContentPane().setLayout( new java.awt.GridBagLayout() );
        getContentPane().add(
            jPanel1,
            new java.awt.GridBagConstraints( 0, 0, 1, 1, 1.0, 0.1, java.awt.GridBagConstraints.NORTH,
                java.awt.GridBagConstraints.BOTH, new java.awt.Insets( 10, 5, 5, 5 ), 0, 0 ) );
        getContentPane().add(
            jPanel2,
            new java.awt.GridBagConstraints( 0, 1, 1, 1, 1.0, 0.8, java.awt.GridBagConstraints.CENTER,
                java.awt.GridBagConstraints.BOTH, new java.awt.Insets( 5, 5, 5, 5 ), 0, 0 ) );
        getContentPane().add(
            jPanel3,
            new java.awt.GridBagConstraints( 0, 2, 1, 1, 1.0, 0.1, java.awt.GridBagConstraints.SOUTH,
                java.awt.GridBagConstraints.HORIZONTAL, new java.awt.Insets( 0, 0, 0, 0 ), 0, 0 ) );
        jPanel1.setLayout( new java.awt.BorderLayout( 10, 10 ) );
        jPanel1.setBorder( javax.swing.BorderFactory.createTitledBorder( javax.swing.BorderFactory.createLineBorder(
            new java.awt.Color( 153, 153, 153 ), 1 ), "Search Filter", javax.swing.border.TitledBorder.LEADING,
            javax.swing.border.TitledBorder.TOP, new java.awt.Font( "SansSerif", 0, 14 ), new java.awt.Color( 60, 60,
                60 ) ) );
        jPanel1.add( jTextArea1, java.awt.BorderLayout.CENTER );
        jScrollPane1.getViewport().add( jTree1 );
        jTree1.setBounds( new java.awt.Rectangle( 238, 142, 82, 80 ) );
        jTextArea1.setText( "" );
        jTextArea1.setEditable( false );
        setBounds( new java.awt.Rectangle( 0, 0, 485, 414 ) );
        jPanel2.setLayout( new java.awt.BorderLayout() );
        jPanel2.setBorder( javax.swing.BorderFactory.createTitledBorder( javax.swing.BorderFactory.createLineBorder(
            new java.awt.Color( 153, 153, 153 ), 1 ), "Filter Expression Tree",
            javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP, new java.awt.Font(
                "SansSerif", 0, 14 ), new java.awt.Color( 60, 60, 60 ) ) );
        jPanel2.add( jScrollPane1, java.awt.BorderLayout.CENTER );
        jButton1.setText( "Done" );
        jButton1.setActionCommand( "Done" );
        jButton1.addActionListener( new ActionListener()
        {
            public void actionPerformed( ActionEvent a_event )
            {
                AnnotatedFilterTreeDialog.this.setVisible( false );
                AnnotatedFilterTreeDialog.this.dispose();
            }
        } );
        jButton1.setHorizontalAlignment( javax.swing.SwingConstants.CENTER );
        jButton1.setAlignmentX( 0.5f );
        jButton1.setHorizontalTextPosition( javax.swing.SwingConstants.CENTER );
        jPanel3.setPreferredSize( new java.awt.Dimension( 79, 41 ) );
        jPanel3.setMinimumSize( new java.awt.Dimension( 79, 41 ) );
        jPanel3.setSize( new java.awt.Dimension( 471, 35 ) );
        jPanel3.setToolTipText( "" );
        jPanel3.add( jButton1 );
    }


    /** Closes the dialog */
    private void closeDialog( WindowEvent evt )
    {
        evt.getWindow();
        setVisible( false );
        dispose();
    }


    public void setModel( TreeModel a_model )
    {
        this.jTree1.setModel( a_model );
    }


    public void setFilter( String a_filter )
    {
        this.jTextArea1.setText( a_filter );
    }
}
