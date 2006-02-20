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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.naming.directory.Attributes;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import org.apache.directory.shared.ldap.message.LockableAttributesImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Allows for operations on entries.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AddEntryDialog extends JDialog implements ActionListener
{
    private static final Logger log = LoggerFactory.getLogger( AddEntryDialog.class );

    private static final long serialVersionUID = 3544671793504663604L;

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
    private JPopupMenu m_popup;

    private Attributes m_childEntry = new LockableAttributesImpl();


    /**
     * Creates new entry addition dialog.
     *  
     * @param parent the parent frame
     * @param modal whether or not to go modal on the dialog
     */
    public AddEntryDialog(Frame parent, boolean modal)
    {
        super( parent, modal );
        m_childEntry.put( "objectClass", "top" );
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
        setTitle( "Add New Entry" );
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

        m_attrTbl.setModel( new AttributesTableModel( m_childEntry, null, null, true ) );

        //
        // Build the table's popup menu
        //

        m_popup = new JPopupMenu();
        JMenuItem l_menuItem = new JMenuItem( "Add" );
        l_menuItem.setActionCommand( "Add" );
        l_menuItem.addActionListener( this );
        m_popup.add( l_menuItem );
        l_menuItem = new JMenuItem( "Delete" );
        l_menuItem.setActionCommand( "Delete" );
        l_menuItem.addActionListener( this );
        m_popup.add( l_menuItem );

        // Add listener to components that can bring up popup menus.
        m_attrTbl.addMouseListener( new PopupListener() );

        setUpEditor( m_attrTbl );
    }


    private void setUpEditor( JTable l_table )
    {
        //Set up the editor for the integer cells.
        final JTextField l_textField = new JTextField();

        DefaultCellEditor l_textEditor = new DefaultCellEditor( l_textField )
        {
            private static final long serialVersionUID = 3256727286014554675L;


            //Override DefaultCellEditor's getCellEditorValue method
            //to return an Integer, not a String:
            public Object getCellEditorValue()
            {
                if ( log.isDebugEnabled() )
                    log.debug( "Editor returning '" + l_textField.getText() + "'" );
                return l_textField.getText();
            }
        };

        l_table.setDefaultEditor( String.class, l_textEditor );
    }

    class PopupListener extends MouseAdapter
    {
        public void mousePressed( MouseEvent e )
        {
            maybeShowPopup( e );
        }


        public void mouseReleased( MouseEvent e )
        {
            maybeShowPopup( e );
        }


        private void maybeShowPopup( MouseEvent e )
        {
            if ( e.isPopupTrigger() )
            {
                m_popup.show( e.getComponent(), e.getX(), e.getY() );
            }
        }
    }


    public void actionPerformed( ActionEvent a_event )
    {
        String l_cmd = a_event.getActionCommand();
        AttributesTableModel l_model = ( AttributesTableModel ) m_attrTbl.getModel();
        int l_row = m_attrTbl.getSelectedRow();
        log.debug( l_cmd );

        if ( l_row >= l_model.getRowCount() || l_row < 0 )
        {
            JOptionPane.showMessageDialog( this, "Row needs to be selected to apply operation" );
        }

        if ( l_cmd.equals( "Add" ) )
        {
            l_model.insert( l_row, "xxxx", "xxxx" );
        }
        else if ( l_cmd.equals( "Delete" ) )
        {
            l_model.delete( l_row );
        }
        else
        {
            JOptionPane.showMessageDialog( this, "Unrecognized action - abandoning action processing." );
        }
    }


    /** Closes the dialog */
    private void closeDialog()
    {
        setVisible( false );
        dispose();
    }


    public void setParentDn( String dn )
    {
        m_dnText.setText( dn );
    }


    public Attributes getChildEntry()
    {
        return m_childEntry;
    }


    public String getChildDn()
    {
        return m_dnText.getText();
    }
}
