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
package org.apache.eve.db.gui ;
 
 
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.WindowEvent;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JButton;


/**
 * Another dialog.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class FilterDialog2 extends Dialog
{
    private JPanel jPanel1 = new JPanel();
    private JPanel m_basePnl = new JPanel();
    private JLabel m_baseLbl = new JLabel();
    private JTextField m_baseTxt = new JTextField();
    private JPanel jPanel2 = new JPanel();
    private JLabel jLabel1 = new JLabel();
    private JTextField m_sizeTxt = new JTextField();
    private JPanel jPanel7 = new JPanel();
    private JLabel jLabel3 = new JLabel();
    private JTextField m_timeTxt = new JTextField();
    private JPanel jPanel4 = new JPanel();
    private JRadioButton m_baseRad = new JRadioButton();
    private JRadioButton m_singleRad = new JRadioButton();
    private JRadioButton m_subtreeRad = new JRadioButton();
    private JPanel jPanel5 = new JPanel();
    private JRadioButton m_never = new JRadioButton();
    private JRadioButton m_always = new JRadioButton();
    private JRadioButton m_finding = new JRadioButton();
    private JRadioButton m_searching = new JRadioButton();
    private JPanel jPanel6 = new JPanel();
    private JTextArea m_filter = new JTextArea();
    private JPanel jPanel3 = new JPanel();
    private JButton m_done = new JButton();
    private JButton m_cancel = new JButton();

    /** Creates new form Dialog */
    public FilterDialog2(Frame parent, boolean modal)
    {
        super(parent, modal);
        initGUI();
        pack();
    }

    /** This method is called from within the init() method to initialize the form. */
    private void initGUI()
    {
        jLabel3.setText("Time:");
        jPanel7.add(jLabel3);
        jPanel7.add(m_timeTxt);
        m_timeTxt.setText("");
        m_timeTxt.setPreferredSize(new java.awt.Dimension(48, 24));
        jLabel1.setText("Size:");
        jPanel2.add(jLabel1);
        jPanel2.add(m_sizeTxt);
        m_sizeTxt.setText("");
        m_sizeTxt.setPreferredSize(new java.awt.Dimension(48, 24));
        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createLineBorder(
        new java.awt.Color(153, 153, 153), 1), "Alias Handling", javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
        new java.awt.Font("SansSerif", 0, 14), new java.awt.Color(60, 60, 60)));
        jPanel5.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 25, 5));
        jPanel5.add(m_never);
        jPanel5.add(m_finding);
        jPanel5.add(m_searching);
        jPanel5.add(m_always);
        m_never.setText("never");
        m_never.setActionCommand("never");
        m_always.setText("always");
        m_always.setSelected(true);
        m_finding.setText("jRadioButton6");
        m_finding.setActionCommand("finding");
        m_searching.setText("deref in searching");
        m_searching.setSelected(false);
        jPanel4.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 20, 5));
        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createLineBorder(
        new java.awt.Color(153, 153, 153), 1), "Scope", javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
        new java.awt.Font("SansSerif", 0, 14), new java.awt.Color(60, 60, 60)));
        jPanel4.add(m_baseRad);
        jPanel4.add(m_singleRad);
        jPanel4.add(m_subtreeRad);
        m_baseRad.setText("base/object level");
        m_baseRad.setBorderPainted(false);
        m_baseRad.setSelected(true);
        m_singleRad.setText("single level");
        m_subtreeRad.setText("subtree level");
        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.X_AXIS));
        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createLineBorder(
        new java.awt.Color(153, 153, 153), 1), "Base and Limits", javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
        new java.awt.Font("SansSerif", 0, 14), new java.awt.Color(60, 60, 60)));
        jPanel1.add(m_basePnl);
        jPanel1.add(jPanel2);
        jPanel1.add(jPanel7);
        m_baseLbl.setText("Base:");
        m_basePnl.add(m_baseLbl);
        m_basePnl.add(m_baseTxt);
        m_baseTxt.setText("");
        m_baseTxt.setPreferredSize(new java.awt.Dimension(220, 24));
        setLayout(new java.awt.GridBagLayout());
        setBounds(new java.awt.Rectangle(0, 0, 485, 390));
        addWindowListener(
        new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });
        add(jPanel1,
        new java.awt.GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, java.awt.GridBagConstraints.CENTER, java.awt.GridBagConstraints.HORIZONTAL,
        new java.awt.Insets(5, 5, 5, 5), 0, 0));
        add(jPanel4,
        new java.awt.GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, java.awt.GridBagConstraints.CENTER, java.awt.GridBagConstraints.HORIZONTAL,
        new java.awt.Insets(5, 5, 5, 5), 0, 0));
        add(jPanel5,
        new java.awt.GridBagConstraints(0, 2, 1, 1, 1.0, 0.0, java.awt.GridBagConstraints.CENTER, java.awt.GridBagConstraints.HORIZONTAL,
        new java.awt.Insets(5, 5, 5, 5), 0, 0));
        add(jPanel6,
        new java.awt.GridBagConstraints(0, 3, 1, 1, 1.0, 1.0, java.awt.GridBagConstraints.CENTER, java.awt.GridBagConstraints.BOTH,
        new java.awt.Insets(5, 5, 5, 5), 0, 0));
        add(jPanel3,
        new java.awt.GridBagConstraints(0, 4, 1, 1, 1.0, 0.0, java.awt.GridBagConstraints.SOUTH, java.awt.GridBagConstraints.BOTH,
        new java.awt.Insets(0, 0, 0, 0), 0, 0));
        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createLineBorder(
        new java.awt.Color(153, 153, 153), 1), "Filter", javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
        new java.awt.Font("SansSerif", 0, 14), new java.awt.Color(60, 60, 60)));
        jPanel6.setLayout(new java.awt.BorderLayout(10, 10));
        jPanel6.add(m_filter, java.awt.BorderLayout.CENTER);
        m_filter.setText("");
        m_done.setText("Done");
        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        jPanel3.add(m_done);
        jPanel3.add(m_cancel);
        m_cancel.setText("Cancel");
    }

    /** Closes the dialog */
    private void closeDialog(WindowEvent evt)
    {
        setVisible(false);
        dispose();
    }
}
