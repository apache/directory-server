/*
 * $Id: IndexDialog.java,v 1.3 2003/03/13 18:27:24 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */
package org.apache.eve.backend.jdbm.gui;

import javax.swing.JDialog;
import java.awt.Frame;
import java.awt.Panel;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;
import org.apache.eve.backend.jdbm.index.Index;
import org.apache.eve.backend.Cursor;
import javax.swing.JOptionPane;
import org.apache.regexp.RE;
import org.apache.eve.backend.jdbm.index.IndexRecord;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import org.apache.avalon.framework.ExceptionUtil;
import javax.swing.JTextArea;
import org.apache.ldap.common.util.StringTools;

public class IndexDialog extends JDialog
{
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
    public IndexDialog(Frame parent, boolean modal, Index a_index)
    {
        super(parent, modal) ;
        m_index = a_index ;
        initGUI() ;
    }

    /**
     * This method is called from within the constructor to initialize the
     * form.
     */
    private void initGUI()
    {
        addWindowListener(
        new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog() ;
            }
        });
        pack() ;
        setTitle("Index On Attribute '" + m_index.getAttribute() + "'") ;
        setBounds(new java.awt.Rectangle(0, 0, 512, 471));
        getContentPane().add(m_mainPnl, java.awt.BorderLayout.CENTER);
        m_mainPnl.setLayout(new java.awt.BorderLayout());
        m_mainPnl.add(m_tabbedPane, java.awt.BorderLayout.CENTER);
        m_tabbedPane.add(m_listPnl, "Listing");
        m_listPnl.setLayout(new java.awt.GridBagLayout());
        m_listPnl.add(m_cursorPnl,
        new java.awt.GridBagConstraints(0, 0, 1, 1, 1.0, 0.15, java.awt.GridBagConstraints.NORTH, java.awt.GridBagConstraints.BOTH,
        new java.awt.Insets(15, 0, 30, 0), 0, 0));
        m_listPnl.add(m_resultsPnl,
        new java.awt.GridBagConstraints(0, 1, 1, 1, 1.0, 0.8, java.awt.GridBagConstraints.CENTER, java.awt.GridBagConstraints.BOTH,
        new java.awt.Insets(0, 0, 0, 0), 0, 0));
        m_listPnl.add(m_buttonPnl,
        new java.awt.GridBagConstraints(0, 2, 1, 1, 1.0, 0.05, java.awt.GridBagConstraints.CENTER, java.awt.GridBagConstraints.BOTH,
        new java.awt.Insets(0, 0, 0, 0), 0, 0));
        m_cursorPnl.setLayout(new java.awt.GridBagLayout());
        m_cursorPnl.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createLineBorder(
        new java.awt.Color(153, 153, 153), 1),
        "Display Cursor Constraints", javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
        new java.awt.Font("SansSerif", 0, 14), new java.awt.Color(60, 60, 60)));
        m_cursorPnl.add(jLabel1,
        new java.awt.GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, java.awt.GridBagConstraints.WEST, java.awt.GridBagConstraints.NONE,
        new java.awt.Insets(0, 15, 0, 10), 0, 0));
        m_cursorPnl.add(m_keyText,
        new java.awt.GridBagConstraints(1, 1, 1, 1, 0.4, 0.0, java.awt.GridBagConstraints.WEST, java.awt.GridBagConstraints.BOTH,
        new java.awt.Insets(5, 5, 5, 236), 0, 0));
        m_cursorPnl.add(jLabel2,
        new java.awt.GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, java.awt.GridBagConstraints.WEST, java.awt.GridBagConstraints.NONE,
        new java.awt.Insets(0, 15, 0, 10), 0, 0));
        m_cursorPnl.add(m_cursorType,
        new java.awt.GridBagConstraints(1, 0, 1, 1, 0.4, 0.0, java.awt.GridBagConstraints.WEST, java.awt.GridBagConstraints.NONE,
        new java.awt.Insets(5, 5, 5, 0), 0, 0));
        m_resultsPnl.setLayout(new java.awt.BorderLayout());
        m_resultsPnl.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createLineBorder(
        new java.awt.Color(153, 153, 153), 1), "Scan Results", javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
        new java.awt.Font("SansSerif", 0, 14), new java.awt.Color(60, 60, 60)));
        m_resultsPnl.add(jScrollPane2, java.awt.BorderLayout.CENTER);
        jScrollPane2.getViewport().add(m_resultsTbl);
        m_buttonPnl.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 15, 5));
        m_buttonPnl.add(m_doneBut);
        m_buttonPnl.add(m_scanBut);
        m_doneBut.setText("Done");
        m_doneBut.setName("Done");
        m_doneBut.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                closeDialog() ;
            }
        }) ;

        jLabel1.setText("Key Constraint:");
        m_keyText.setText("");
        m_keyText.setMinimumSize(new java.awt.Dimension(130, 20));
        m_keyText.setPreferredSize(new java.awt.Dimension(130, 20));
        m_keyText.setMaximumSize(new java.awt.Dimension(130, 20));
        m_keyText.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 14));
        m_keyText.setSize(new java.awt.Dimension(130, 20));
        jLabel2.setText("Cursor Type:");
        m_cursorType.setMaximumSize(new java.awt.Dimension(32767, 20));
        m_cursorType.setMinimumSize(new java.awt.Dimension(126, 20));
        m_cursorType.setPreferredSize(new java.awt.Dimension(130, 20));
        DefaultComboBoxModel l_comboModel = new DefaultComboBoxModel() ;
        l_comboModel.addElement(DEFAULT_CURSOR) ;
        l_comboModel.addElement(EQUALITY_CURSOR) ;
        l_comboModel.addElement(GREATER_CURSOR) ;
        l_comboModel.addElement(LESS_CURSOR) ;
        l_comboModel.addElement(REGEX_CURSOR) ;
        m_cursorType.setModel(l_comboModel) ;
        m_cursorType.setMaximumRowCount(5);
        m_scanBut.setText("Scan");
        m_scanBut.setName("Scan");
        m_scanBut.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doScan(m_keyText.getText(),
                    (String) m_cursorType.getSelectedItem()) ;
            }
        }) ;

        doScan(null, DEFAULT_CURSOR) ;
    }


    private void closeDialog()
    {
        setVisible(false) ;
        dispose() ;
    }


    public boolean doScan(String a_key, String a_cursorType)
    {
        if(a_key == null || a_key.trim().equals("")) {
            a_key = null ;
        }

        if(a_key == null && a_cursorType != DEFAULT_CURSOR) {
            JOptionPane.showMessageDialog(null, "Cannot use a " +
                a_cursorType + " cursor type with a null key constraint.",
                "Missing Key Constraint", JOptionPane.ERROR_MESSAGE) ;
            return false ;
        }

        try {
            Cursor l_cursor = null ;

            if(a_cursorType == EQUALITY_CURSOR) {
                l_cursor = m_index.getCursor(a_key) ;
            } else if(a_cursorType == GREATER_CURSOR) {
                l_cursor = m_index.getCursor(a_key, true) ;
            } else if(a_cursorType == LESS_CURSOR) {
                l_cursor = m_index.getCursor(a_key, false) ;
            } else if(a_cursorType == REGEX_CURSOR) {
                RE l_regex = StringTools.getRegex(a_key) ;
                int l_starIndex = a_key.indexOf('*') ;

                if(l_starIndex > 0) {
                    String l_prefix = a_key.substring(0, l_starIndex) ;
                    System.out.println("Regex prefix = " + l_prefix) ;
                    l_cursor = m_index.getCursor(l_regex, l_prefix) ;
                } else {
                    l_cursor = m_index.getCursor(l_regex) ;
                }
            } else {
                l_cursor = m_index.getCursor() ;
            }

            Object [] l_cols = new Object [2] ;
            Object [] l_row = null ;
            l_cols[0] = "Keys (Attribute Value)" ;
            l_cols[1] = "Values (Entry Id)" ;
            DefaultTableModel l_model = new DefaultTableModel(l_cols, 0) ;
            int l_count = 0 ;
            while(l_cursor.hasMore()) {
                IndexRecord l_rec = (IndexRecord) l_cursor.next() ;
                l_row = new Object [2] ;
                l_row[0] = l_rec.getIndexKey() ;
                l_row[1] = l_rec.getEntryId() ;
                l_model.addRow(l_row) ;
                l_count++ ;
            } ;

            m_resultsTbl.setModel(l_model) ;
            m_resultsPnl.setBorder(
                javax.swing.BorderFactory.createTitledBorder(
                javax.swing.BorderFactory.createLineBorder(
                new java.awt.Color(153, 153, 153), 1),
                "Scan Results: " + l_count,
                javax.swing.border.TitledBorder.LEADING,
                javax.swing.border.TitledBorder.TOP,
                new java.awt.Font("SansSerif", 0, 14),
                new java.awt.Color(60, 60, 60))) ;

            if(this.isVisible()) {
                this.validate() ;
            }
        } catch(Exception e) {
            String l_msg = ExceptionUtil.printStackTrace(e) ;
            if(l_msg.length() > 1024) {
                l_msg = l_msg.substring(0, 1024)
                    + "\n. . . TRUNCATED . . ." ;
            }
            l_msg = "Error while scanning index "
                + "on attribute " + m_index.getAttribute() + " using a "
                + a_cursorType + " cursor type with a key constraint of '"
                + a_key + "':\n" + l_msg ;
            JTextArea l_area = new JTextArea() ;
            l_area.setText(l_msg) ;
            JOptionPane.showMessageDialog(null, l_area, "Index Scan Error",
                JOptionPane.ERROR_MESSAGE) ;
            return false ;
        }

        return true ;
    }
}
