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
package org.apache.ldap.server.partition.impl.btree.gui ;


import java.awt.Frame;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.naming.NamingEnumeration;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import org.apache.ldap.common.util.ExceptionUtils;
import org.apache.ldap.common.util.StringTools;
import org.apache.ldap.server.partition.impl.btree.Index;
import org.apache.ldap.server.partition.impl.btree.IndexRecord;
import org.apache.regexp.RE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A dialog showing index values.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class IndexDialog extends JDialog
{
    private static final Logger log = LoggerFactory.getLogger(IndexDialog.class);

    private static final long serialVersionUID = 3689917253680445238L;

    public static final String DEFAULT_CURSOR = "Default" ;
    public static final String EQUALITY_CURSOR = "Equality" ;
    public static final String GREATER_CURSOR = "Greater" ;
    public static final String LESS_CURSOR = "Less" ;
    public static final String REGEX_CURSOR = "Regex" ;

    private Panel m_mainPnl = new Panel();
    private JTabbedPane m_tabbedPane = new JTabbedPane();
    private JPanel m_listPnl = new JPanel();
    private JPanel m_cursorPnl = new JPanel();
    private JPanel m_resultsPnl = new JPanel();
    private JScrollPane jScrollPane2 = new JScrollPane();
    private JTable m_resultsTbl = new JTable();
    private JPanel m_buttonPnl = new JPanel();
    private JButton m_doneBut = new JButton();
    private JLabel jLabel1 = new JLabel();
    private JTextField m_keyText = new JTextField();
    private JLabel jLabel2 = new JLabel();
    private JComboBox m_cursorType = new JComboBox();
    private JButton m_scanBut = new JButton();
    private Index m_index = null ;

    /** Creates new form JDialog */
    public IndexDialog( Frame parent, boolean modal, Index a_index )
    {
        super ( parent, modal ) ;
        m_index = a_index ;
        initGUI() ;
    }

    /**
     * This method is called from within the constructor to initialize the
     * form.
     */
    private void initGUI()
    {
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog();
            }
        });

        pack();
        setTitle("Index On Attribute '" + m_index.getAttribute() + "'");
        setBounds(new java.awt.Rectangle(0, 0, 512, 471));
        getContentPane().add(m_mainPnl, java.awt.BorderLayout.CENTER);
        m_mainPnl.setLayout(new java.awt.BorderLayout());
        m_mainPnl.add(m_tabbedPane, java.awt.BorderLayout.CENTER);
        m_tabbedPane.add(m_listPnl, "Listing");
        m_listPnl.setLayout(new java.awt.GridBagLayout());
        m_listPnl.add(
            m_cursorPnl,
            new java.awt.GridBagConstraints(
                0,
                0,
                1,
                1,
                1.0,
                0.15,
                java.awt.GridBagConstraints.NORTH,
                java.awt.GridBagConstraints.BOTH,
                new java.awt.Insets(15, 0, 30, 0),
                0,
                0));
        m_listPnl.add(
            m_resultsPnl,
            new java.awt.GridBagConstraints(
                0,
                1,
                1,
                1,
                1.0,
                0.8,
                java.awt.GridBagConstraints.CENTER,
                java.awt.GridBagConstraints.BOTH,
                new java.awt.Insets(0, 0, 0, 0),
                0,
                0));
        m_listPnl.add(
            m_buttonPnl,
            new java.awt.GridBagConstraints(
                0,
                2,
                1,
                1,
                1.0,
                0.05,
                java.awt.GridBagConstraints.CENTER,
                java.awt.GridBagConstraints.BOTH,
                new java.awt.Insets(0, 0, 0, 0),
                0,
                0));
        m_cursorPnl.setLayout(new java.awt.GridBagLayout());
        m_cursorPnl.setBorder(
            javax.swing.BorderFactory.createTitledBorder(
                javax.swing.BorderFactory.createLineBorder(
                    new java.awt.Color(153, 153, 153),
                    1),
                "Display Cursor Constraints",
                javax.swing.border.TitledBorder.LEADING,
                javax.swing.border.TitledBorder.TOP,
                new java.awt.Font("SansSerif", 0, 14),
                new java.awt.Color(60, 60, 60)));
        m_cursorPnl.add(
            jLabel1,
            new java.awt.GridBagConstraints(
                0,
                1,
                1,
                1,
                0.0,
                0.0,
                java.awt.GridBagConstraints.WEST,
                java.awt.GridBagConstraints.NONE,
                new java.awt.Insets(0, 15, 0, 10),
                0,
                0));
        m_cursorPnl.add(
            m_keyText,
            new java.awt.GridBagConstraints(
                1,
                1,
                1,
                1,
                0.4,
                0.0,
                java.awt.GridBagConstraints.WEST,
                java.awt.GridBagConstraints.BOTH,
                new java.awt.Insets(5, 5, 5, 236),
                0,
                0));
        m_cursorPnl.add(
            jLabel2,
            new java.awt.GridBagConstraints(
                0,
                0,
                1,
                1,
                0.0,
                0.0,
                java.awt.GridBagConstraints.WEST,
                java.awt.GridBagConstraints.NONE,
                new java.awt.Insets(0, 15, 0, 10),
                0,
                0));
        m_cursorPnl.add(
            m_cursorType,
            new java.awt.GridBagConstraints(
                1,
                0,
                1,
                1,
                0.4,
                0.0,
                java.awt.GridBagConstraints.WEST,
                java.awt.GridBagConstraints.NONE,
                new java.awt.Insets(5, 5, 5, 0),
                0,
                0));
        m_resultsPnl.setLayout(new java.awt.BorderLayout());
        m_resultsPnl.setBorder(
            javax.swing.BorderFactory.createTitledBorder(
                javax.swing.BorderFactory.createLineBorder(
                    new java.awt.Color(153, 153, 153),
                    1),
                "Scan Results",
                javax.swing.border.TitledBorder.LEADING,
                javax.swing.border.TitledBorder.TOP,
                new java.awt.Font("SansSerif", 0, 14),
                new java.awt.Color(60, 60, 60)));
        m_resultsPnl.add(jScrollPane2, java.awt.BorderLayout.CENTER);
        jScrollPane2.getViewport().add(m_resultsTbl);
        m_buttonPnl.setLayout(
            new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 15, 5));
        m_buttonPnl.add(m_doneBut);
        m_buttonPnl.add(m_scanBut);
        m_doneBut.setText("Done");
        m_doneBut.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                closeDialog();
            }
        });

        jLabel1.setText("Key Constraint:");
        m_keyText.setText("");
        m_keyText.setMinimumSize(new java.awt.Dimension(130, 20));
        m_keyText.setPreferredSize(new java.awt.Dimension(130, 20));
        m_keyText.setMaximumSize(new java.awt.Dimension(130, 20));
        m_keyText.setFont(
            new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 14));
        m_keyText.setSize(new java.awt.Dimension(130, 20));
        jLabel2.setText("Cursor Type:");
        m_cursorType.setMaximumSize(new java.awt.Dimension(32767, 20));
        m_cursorType.setMinimumSize(new java.awt.Dimension(126, 20));
        m_cursorType.setPreferredSize(new java.awt.Dimension(130, 20));
        DefaultComboBoxModel l_comboModel = new DefaultComboBoxModel();
        l_comboModel.addElement(DEFAULT_CURSOR);
        l_comboModel.addElement(EQUALITY_CURSOR);
        l_comboModel.addElement(GREATER_CURSOR);
        l_comboModel.addElement(LESS_CURSOR);
        l_comboModel.addElement(REGEX_CURSOR);
        m_cursorType.setModel(l_comboModel);
        m_cursorType.setMaximumRowCount(5);
        m_scanBut.setText("Scan");
        m_scanBut.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                doScan(
                    m_keyText.getText(),
                    (String) m_cursorType.getSelectedItem());
            }
        });

        doScan(null, DEFAULT_CURSOR);
    }


    private void closeDialog()
    {
        setVisible( false ) ;
        dispose() ;
    }


    public boolean doScan( String a_key, String a_scanType )
    {
        if ( a_key == null || a_key.trim().equals( "" ) ) 
        {
            a_key = null ;
        }

        if ( a_key == null && a_scanType != DEFAULT_CURSOR ) 
        {
            JOptionPane.showMessageDialog( null, "Cannot use a " +
                a_scanType + " scan type with a null key constraint.",
                "Missing Key Constraint", JOptionPane.ERROR_MESSAGE ) ;
            return false ;
        }

        try 
        {
            NamingEnumeration l_list = null ;

            if ( a_scanType == EQUALITY_CURSOR ) 
            {
                l_list = m_index.listIndices( a_key ) ;
            } 
            else if ( a_scanType == GREATER_CURSOR ) 
            {
                l_list = m_index.listIndices( a_key, true ) ;
            } 
            else if ( a_scanType == LESS_CURSOR ) 
            {
                l_list = m_index.listIndices( a_key, false ) ;
            } 
            else if ( a_scanType == REGEX_CURSOR ) 
            {
                RE l_regex = StringTools.getRegex( a_key ) ;
                int l_starIndex = a_key.indexOf( '*' ) ;

                if ( l_starIndex > 0 ) 
                {
                    String l_prefix = a_key.substring( 0, l_starIndex ) ;

                    if (log.isDebugEnabled())
                        log.debug( "Regex prefix = " + l_prefix ) ;

                    l_list = m_index.listIndices( l_regex, l_prefix ) ;
                } 
                else 
                {
                    l_list = m_index.listIndices( l_regex ) ;
                }
            } 
            else 
            {
                l_list = m_index.listIndices() ;
            }

            Object [] l_cols = new Object [2] ;
            Object [] l_row = null ;
            l_cols[0] = "Keys ( Attribute Value )" ;
            l_cols[1] = "Values ( Entry Id )" ;
            DefaultTableModel l_model = new DefaultTableModel( l_cols, 0 ) ;
            int l_count = 0 ;
            while( l_list.hasMore() )
            {
                IndexRecord l_rec = ( IndexRecord ) l_list.next() ;
                l_row = new Object [2] ;
                l_row[0] = l_rec.getIndexKey() ;
                l_row[1] = l_rec.getEntryId() ;
                l_model.addRow( l_row ) ;
                l_count++ ;
            }

            m_resultsTbl.setModel( l_model ) ;
            m_resultsPnl.setBorder(
                javax.swing.BorderFactory.createTitledBorder(
                javax.swing.BorderFactory.createLineBorder(
                new java.awt.Color( 153, 153, 153 ), 1 ),
                "Scan Results: " + l_count,
                javax.swing.border.TitledBorder.LEADING,
                javax.swing.border.TitledBorder.TOP,
                new java.awt.Font( "SansSerif", 0, 14 ),
                new java.awt.Color( 60, 60, 60 ) ) ) ;

            if ( isVisible() ) 
            {
                validate() ;
            }
        } 
        catch ( Exception e ) 
        {
            String l_msg = ExceptionUtils.getStackTrace( e );

            if ( l_msg.length() > 1024 ) 
            {
                l_msg = l_msg.substring( 0, 1024 )
                    + "\n. . . TRUNCATED . . ." ;
            }

            l_msg = "Error while scanning index "
                + "on attribute " + m_index.getAttribute() + " using a "
                + a_scanType + " cursor type with a key constraint of '"
                + a_key + "':\n" + l_msg ;
                
            JTextArea l_area = new JTextArea() ;
            l_area.setText( l_msg ) ;
            JOptionPane.showMessageDialog( null, l_area, "Index Scan Error",
                    JOptionPane.ERROR_MESSAGE ) ;
            return false ;
        }

        return true ;
    }
}
