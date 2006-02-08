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
package org.apache.directory.server.core.partition.impl.btree.gui ;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;


/**
 * A dialog for the filter.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class FilterDialog extends JDialog
{
	private static final long serialVersionUID = 3760565295319626294L;

    public static final String RUN_MODE = "Run" ;
    public static final String LOAD_MODE = "Load" ;
    public static final String DEBUG_MODE = "Debug" ;
	public static final String ANNOTATE_MODE = "Annotate" ;

    public static final String UNLIMITED = "Unlimited" ;

	public static final String BASE_SCOPE = "Base Object" ;
	public static final String SINGLE_SCOPE = "Single Level" ;
	public static final String SUBTREE_SCOPE = "Subtree Level" ;

    public static final String LOAD_CMD = "Load" ;
    public static final String SEARCH_CMD = "Search" ;
    public static final String CANCEL_CMD = "Cancel" ;

    private JPanel m_northPnl = new JPanel() ;
    private JPanel m_centerPnl = new JPanel() ;
    private JTextArea m_filterText = new JTextArea() ;
    private JLabel m_scopeLbl = new JLabel() ;
    private JComboBox m_scopeChoice = new JComboBox() ;
    private JLabel m_limitLbl = new JLabel() ;
    private JTextField m_limitField = new JTextField() ;
    private JPanel m_southPnl = new JPanel() ;
    private JButton m_searchBut = new JButton() ;
    private JButton m_cancelBut = new JButton() ;
    private JScrollPane m_scrollPane = new JScrollPane() ;
    private final String m_mode ;
    private JTextField m_baseText = new JTextField();
    private JPanel m_basePnl = new JPanel();
    private JLabel jLabel1 = new JLabel();

    /** Creates new form JDialog */
    public FilterDialog(String a_mode, JFrame parent, boolean modal)
    {
        super(parent, modal) ;
        m_mode = a_mode ;
        initGUI() ;
    }


	public void addActionListener(ActionListener l_listener)
    {
        m_searchBut.addActionListener(l_listener) ;
        m_cancelBut.addActionListener(l_listener) ;
    }



	/**
     * This method is called from within the constructor to initialize the form
     */
    private void initGUI() {
        m_baseText.setText("");
        addWindowListener(
            new WindowAdapter() {
                public void windowClosing(WindowEvent evt) {
                    closeDialog(evt);
                }
            }) ;
        pack() ;

        getContentPane().setLayout(new java.awt.GridBagLayout()) ;
        getContentPane().add(m_northPnl,
        new java.awt.GridBagConstraints(0, 0, 1, 1, 0.9, 0.0, java.awt.GridBagConstraints.NORTH, java.awt.GridBagConstraints.BOTH,
        new java.awt.Insets(5, 5, 6, 0), 0, 0));
        getContentPane().add(m_centerPnl,
        new GridBagConstraints(0, 1, 1, 1, 0.9, 0.9,
        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
        new Insets(10, 10, 10, 10), 0, 0));
        getContentPane().add(m_southPnl,
	        new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0,
        	GridBagConstraints.SOUTH, GridBagConstraints.BOTH,
    	    new Insets(0, 0, 2, 0), 0, 0)) ;
        m_northPnl.setLayout(new GridBagLayout()) ;
        m_northPnl.setBorder(null) ;
        m_northPnl.add(m_scopeLbl,
        new java.awt.GridBagConstraints(0, 0, 1, 1, 0.2, 0.0, java.awt.GridBagConstraints.CENTER, java.awt.GridBagConstraints.NONE,
        new java.awt.Insets(5, 0, 5, 0), 0, 0));
        m_northPnl.add(m_scopeChoice,
        new java.awt.GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, java.awt.GridBagConstraints.CENTER, java.awt.GridBagConstraints.HORIZONTAL,
        new java.awt.Insets(9, 0, 7, 5), 0, 0));
        m_northPnl.add(m_limitLbl,
	        new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER, GridBagConstraints.NONE,
        	new Insets(5, 10, 5, 5), 0, 0)) ;
        m_northPnl.add(m_limitField,
        new java.awt.GridBagConstraints(3, 0, 1, 1, 1.0, 0.0, java.awt.GridBagConstraints.CENTER, java.awt.GridBagConstraints.HORIZONTAL,
        new java.awt.Insets(11, 0, 9, 10), 0, 0));
        m_northPnl.add(m_basePnl,
        new java.awt.GridBagConstraints(0, 1, 4, 1, 0.0, 0.0, java.awt.GridBagConstraints.CENTER, java.awt.GridBagConstraints.BOTH,
        new java.awt.Insets(5, 10, 5, 10), 0, 0));
        m_filterText.setText("") ;
        m_filterText.setBorder(null) ;
        m_centerPnl.setLayout(new BorderLayout()) ;
        m_centerPnl.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(
        		new Color(153, 153, 153), 1), "Search Filter",
                TitledBorder.LEADING, TitledBorder.TOP,
        		new Font("SansSerif", 0, 14), new Color(60, 60, 60))) ;
        m_scrollPane.getViewport().add(m_filterText);
        m_centerPnl.add(m_scrollPane, BorderLayout.CENTER) ;
        m_scopeLbl.setText("Scope:") ;
        m_scopeLbl.setFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 14));
        m_scopeChoice.setSize(new java.awt.Dimension(115, 25));
        m_scopeChoice.setMaximumSize(new Dimension(32767,25)) ;
        m_scopeChoice.setMinimumSize(new java.awt.Dimension(115, 25));
        m_scopeChoice.setPreferredSize(new Dimension(115, 25)) ;
		m_scopeChoice.addItem(BASE_SCOPE) ;
		m_scopeChoice.addItem(SINGLE_SCOPE) ;
		m_scopeChoice.addItem(SUBTREE_SCOPE) ;

        m_limitLbl.setText("Limit:") ;
        m_limitField.setText("Unlimited") ;
        m_limitField.setHorizontalAlignment(JTextField.CENTER) ;
        m_southPnl.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 5)) ;
        m_southPnl.add(m_searchBut) ;

        if ( m_mode != LOAD_MODE ) 
        {
            m_searchBut.setText( SEARCH_CMD ) ;
            m_searchBut.setActionCommand( SEARCH_CMD ) ;
            m_southPnl.add( m_cancelBut ) ;
        } 
        else 
        {
            m_searchBut.setText( LOAD_CMD ) ;
            m_searchBut.setActionCommand( LOAD_CMD ) ;
        }

        m_cancelBut.setText(CANCEL_CMD) ;
        m_cancelBut.setActionCommand(CANCEL_CMD) ;
        setBounds(new java.awt.Rectangle(0,0,595,331));
        m_basePnl.setLayout(new java.awt.GridBagLayout());
        m_basePnl.add(jLabel1,
        new java.awt.GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, java.awt.GridBagConstraints.WEST, java.awt.GridBagConstraints.NONE,
        new java.awt.Insets(0, 0, 0, 0), 0, 0));
        m_basePnl.add(m_baseText,
        new java.awt.GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, java.awt.GridBagConstraints.EAST, java.awt.GridBagConstraints.HORIZONTAL,
        new java.awt.Insets(5, 5, 5, 0), 0, 0));
        jLabel1.setText("Search Base:");
        jLabel1.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 14));

        if(m_mode == RUN_MODE) {
	        setTitle("Search Filter Dialog: Execute mode") ;
        } else if(m_mode == LOAD_MODE) {
            setTitle("Search Filter Dialog: Load mode") ;
        } else if(m_mode == DEBUG_MODE) {
	        setTitle("Search Filter Dialog: Debug mode") ;
        } else if(m_mode == ANNOTATE_MODE) {
	        setTitle("Search Filter Dialog: Annotate mode") ;
            this.m_scopeChoice.setEnabled(false) ;
            this.m_limitField.setEnabled(false) ;
            this.m_baseText.setEnabled(false) ;
        } else {
            throw new RuntimeException("Unrecognized mode.") ;
        }
    }


    /**
     * Closes the dialog
     */
    public void closeDialog(WindowEvent evt)
    {
        setVisible(false) ;
        dispose() ;
    }


	public String getScope()
    {
		int l_selected = m_scopeChoice.getSelectedIndex() ;
        return (String) m_scopeChoice.getItemAt(l_selected) ;
    }


    /*
	public int getScope()
    {
		int l_selected = m_scopeChoice.getSelectedIndex() ;
        String l_scope = (String) m_scopeChoice.getItemAt(l_selected) ;

		if(l_scope == BASE_SCOPE) {
			return Backend.BASE_SCOPE ;
        } else if(l_scope == SINGLE_SCOPE) {
            return Backend.SINGLE_SCOPE ;
        } else if(l_scope == SUBTREE_SCOPE) {
            return Backend.SUBTREE_SCOPE ;
        }

        throw new RuntimeException("Unexpected scope parameter: " + l_scope) ;
    }
	*/

    public String getLimit()
    {
		return m_limitField.getText() ;
    }

/*
    public String getLimit()
    {
        String l_limit = m_limitField.getText() ;

		if(l_limit.equals(UNLIMITED)) {
            return -1 ;
        }

        return Integer.parseInt(l_limit) ;
    }
*/

    public String getFilter()
    {
		return this.m_filterText.getText() ;
    }


    public void setBase(String a_base)
    {
        this.m_baseText.setText(a_base) ;
    }


    public void setScope(String a_scope)
    {
        this.m_scopeChoice.setSelectedItem(a_scope) ;
    }


    public String getBase()
    {
        return this.m_baseText.getText() ;
    }
}
