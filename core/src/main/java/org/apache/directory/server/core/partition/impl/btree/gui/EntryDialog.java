/*
 *   Copyright 2004 The Apache Software Foundation
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
package org.apache.directory.server.core.partition.impl.btree.gui;


import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.naming.directory.Attributes;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;


/**
 * Allows for operations on entries.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class EntryDialog extends JDialog
{
    private static final long serialVersionUID = 3761684611092001592L;

    private JPanel m_namePnl = new JPanel();
    private JPanel m_attrPnl = new JPanel();
    private JPanel m_buttonPnl = new JPanel();
    private JPanel m_rdnPnl = new JPanel();
    private JPanel m_dnPnl = new JPanel();
    private JLabel m_rdnLbl = new JLabel();
    private JComboBox m_rdnChoice = new JComboBox();
    private JTextField m_dnText = new JTextField();
    private JScrollPane m_attrScrollPnl = new JScrollPane();
    private JTable m_attrTbl = new JTable();
    private JButton m_doneBut = new JButton();
    private JButton m_cancelBut = new JButton();


    //    private String m_opMode = "Add" ;
    //    private String m_dn ;
    //    private String m_rdn ;
    //    private Attributes m_entry ;

    /**
     * Creates new form JDialog
     *  
     * @param a_parent
     * @param a_modal
     */
    public EntryDialog( Frame parent, boolean modal )
    {
        super( parent, modal );
        initGUI();
    }


    /** 
     * This method is called from within the constructor to initialize the form.
     */
    private void initGUI()
    {
        addWindowListener( new java.awt.event.WindowAdapter()
        {
            public void windowClosing( java.awt.event.WindowEvent evt )
            {
                closeDialog();
            }
        } );
        pack();
        setBounds( new java.awt.Rectangle( 0, 0, 447, 364 ) );
        setTitle( "Entry Dialog" );
        getContentPane().setLayout( new java.awt.GridBagLayout() );
        getContentPane().add(
            m_namePnl,
            new java.awt.GridBagConstraints( 0, 0, 1, 1, 1.0, 0.0, java.awt.GridBagConstraints.NORTH,
                java.awt.GridBagConstraints.HORIZONTAL, new java.awt.Insets( 5, 5, 5, 5 ), 0, 0 ) );
        getContentPane().add(
            m_attrPnl,
            new java.awt.GridBagConstraints( 0, 1, 1, 1, 1.0, 1.0, java.awt.GridBagConstraints.CENTER,
                java.awt.GridBagConstraints.BOTH, new java.awt.Insets( 5, 5, 5, 5 ), 0, 0 ) );
        getContentPane().add(
            m_buttonPnl,
            new java.awt.GridBagConstraints( 0, 2, 1, 1, 1.0, 0.05, java.awt.GridBagConstraints.CENTER,
                java.awt.GridBagConstraints.HORIZONTAL, new java.awt.Insets( 0, 0, 0, 20 ), 0, 0 ) );
        m_namePnl.setBorder( javax.swing.BorderFactory.createTitledBorder( javax.swing.BorderFactory.createLineBorder(
            new java.awt.Color( 153, 153, 153 ), 1 ), "Naming", javax.swing.border.TitledBorder.LEADING,
            javax.swing.border.TitledBorder.TOP, new java.awt.Font( "SansSerif", 0, 14 ), new java.awt.Color( 60, 60,
                60 ) ) );
        m_namePnl.setLayout( new javax.swing.BoxLayout( m_namePnl, javax.swing.BoxLayout.Y_AXIS ) );
        m_namePnl.add( m_rdnPnl );
        m_namePnl.add( m_dnPnl );
        m_rdnLbl.setText( "Rdn:" );
        m_rdnPnl.setLayout( new java.awt.GridBagLayout() );
        m_rdnPnl.add( m_rdnChoice, new java.awt.GridBagConstraints( 1, 0, 1, 1, 1.0, 0.0,
            java.awt.GridBagConstraints.WEST, java.awt.GridBagConstraints.NONE, new java.awt.Insets( 0, 10, 0, 0 ), 0,
            0 ) );
        m_rdnPnl.add( m_rdnLbl, new java.awt.GridBagConstraints( 0, 0, 1, 1, 0.0, 0.0,
            java.awt.GridBagConstraints.WEST, java.awt.GridBagConstraints.NONE, new java.awt.Insets( 0, 10, 0, 0 ), 0,
            0 ) );
        m_dnPnl.setLayout( new java.awt.GridBagLayout() );
        m_dnPnl.add( m_dnText, new java.awt.GridBagConstraints( 1, 0, 1, 1, 1.0, 0.0, java.awt.GridBagConstraints.WEST,
            java.awt.GridBagConstraints.HORIZONTAL, new java.awt.Insets( 0, 5, 0, 0 ), 0, 0 ) );
        m_dnText.setText( "unknown" );
        m_dnText.setEditable( false );
        m_dnText.setBorder( javax.swing.BorderFactory.createTitledBorder( javax.swing.BorderFactory.createLineBorder(
            new java.awt.Color( 153, 153, 153 ), 1 ), "Dn", javax.swing.border.TitledBorder.LEADING,
            javax.swing.border.TitledBorder.TOP, new java.awt.Font( "SansSerif", 0, 14 ), new java.awt.Color( 60, 60,
                60 ) ) );
        m_rdnChoice.setEditable( true );
        m_rdnChoice.setMaximumRowCount( 6 );

        m_rdnChoice.setSize( new java.awt.Dimension( 130, 24 ) );
        m_attrPnl.setLayout( new java.awt.BorderLayout() );
        m_attrPnl.add( m_attrScrollPnl, java.awt.BorderLayout.CENTER );
        m_attrScrollPnl.getViewport().add( m_attrTbl );
        m_attrTbl.setBounds( new java.awt.Rectangle( 78, 60, 32, 32 ) );
        m_attrTbl.setEditingColumn( 1 );
        m_attrTbl.setCellSelectionEnabled( true );
        m_doneBut.setText( "Done" );
        m_buttonPnl.setLayout( new java.awt.FlowLayout( java.awt.FlowLayout.RIGHT, 10, 5 ) );
        m_buttonPnl.add( m_doneBut );
        m_buttonPnl.add( m_cancelBut );
        m_cancelBut.setText( "Cancel" );
        m_cancelBut.addActionListener( new ActionListener()
        {
            public void actionPerformed( ActionEvent a_evt )
            {
                closeDialog();
            }
        } );
        m_attrScrollPnl.setBorder( javax.swing.BorderFactory.createTitledBorder( javax.swing.BorderFactory
            .createLineBorder( new java.awt.Color( 153, 153, 153 ), 1 ), "Attributes",
            javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP, new java.awt.Font(
                "SansSerif", 0, 14 ), new java.awt.Color( 60, 60, 60 ) ) );
    }


    /** Closes the dialog */
    private void closeDialog()
    {
        setVisible( false );
        dispose();
    }


    public void setDn( String a_dn )
    {
        //        m_dn = a_dn ;
        m_dnText.setText( a_dn );
    }


    public void setRdn( String a_rdn )
    {
        //        m_rdn = a_rdn ;
        // m_rdnChoice.setSelectedItem(  ) ;
    }


    public void setEntry( Attributes a_entry )
    {
        //        m_entry = a_entry ;
    }
}
