/*
 * $Id: BackendFrame.java,v 1.3 2003/03/13 18:27:22 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm.gui ;


import java.io.File ;

import javax.swing.JTree ;
import javax.swing.JMenu ;
import javax.swing.JTable ;
import javax.swing.JFrame ;
import javax.swing.JPanel ;
import javax.swing.JLabel ;
import javax.swing.JMenuBar ;
import javax.swing.JMenuItem ;
import javax.swing.JSplitPane ;
import javax.swing.JSeparator ;
import javax.swing.JScrollPane ;
import javax.swing.JTabbedPane ;
import javax.swing.JFileChooser ;
import javax.swing.tree.TreePath ;
import javax.swing.tree.TreeModel ;
import javax.swing.tree.DefaultTreeModel ;
import javax.swing.event.TreeSelectionEvent ;
import javax.swing.event.TreeSelectionListener ;

import java.awt.Dimension ;
import java.awt.BorderLayout ;
import java.awt.event.ActionEvent ;
import java.awt.event.ActionListener ;

import org.apache.eve.backend.jdbm.Database ;
import org.apache.eve.backend.jdbm.LdapEntryImpl ;

import org.apache.eve.Kernel ;
import java.util.HashMap;
import java.awt.Toolkit;
import org.apache.eve.backend.jdbm.search.SearchEngine;
import org.apache.eve.backend.jdbm.JdbmModule;
import org.apache.eve.backend.Backend;
import org.apache.ldap.common.filter.FilterParserImpl;
import org.apache.ldap.common.filter.FilterParser;
import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.filter.FilterParserMonitorAdapter;

import javax.swing.JOptionPane;
import java.util.Iterator;
import org.apache.eve.backend.jdbm.index.Index;
import javax.swing.tree.TreeNode;
import org.apache.eve.backend.jdbm.search.DefaultOptimizer;
import javax.swing.JTextArea;
import java.util.Stack;
import java.math.BigInteger;
import org.apache.eve.backend.jdbm.index.IndexRecord;
import javax.swing.table.DefaultTableModel;
import org.apache.eve.backend.Cursor;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;


public class BackendFrame
    extends JFrame
    implements LogEnabled
{
    private JLabel statusBar = new JLabel("Ready") ;
    private JPanel m_mainPnl = new JPanel() ;
    private JSplitPane m_splitPane = new JSplitPane() ;
    private JTabbedPane m_tabbedPane = new JTabbedPane() ;
    private JPanel m_entryPnl = new JPanel() ;
    private JPanel m_idxPnl = new JPanel() ;
    private JScrollPane m_treePane = new JScrollPane() ;
    private JTree m_tree = new JTree() ;
    private JScrollPane m_entryPane = new JScrollPane() ;
    private JTable m_entryTbl = new JTable() ;
    private JScrollPane m_idxPane = new JScrollPane() ;
    private JTable m_idxTbl = new JTable() ;
    private JMenu m_searchMenu = new JMenu();
    private JMenuItem m_annotate = new JMenuItem();
    private JMenuItem m_run = new JMenuItem();
    private JMenuItem m_debug = new JMenuItem();
    private JMenu m_indices = new JMenu();
    private Database m_database = null ;
    private boolean m_doCleanUp = false ;
    private HashMap m_nodes = new HashMap() ;
    private EntryNode m_root = null ;
    private Kernel m_kernel = null ;
    private Logger m_logger = null ;

    /**
     * Creates new form JFrame
     */
    public BackendFrame() {
        initGUI() ;
        pack() ;
    }


    /**
     * This method is called from within the constructor to initialize the form
     */
    private void initGUI() {
        m_mainPnl.setBorder(null) ;
        m_mainPnl.setLayout(new java.awt.BorderLayout()) ;
        m_mainPnl.add(m_splitPane, java.awt.BorderLayout.CENTER) ;
        m_splitPane.add(m_tabbedPane, javax.swing.JSplitPane.RIGHT) ;
        m_splitPane.add(m_treePane, javax.swing.JSplitPane.LEFT) ;
        m_tabbedPane.add(m_entryPnl, "Entry Attributes") ;
        m_tabbedPane.add(m_idxPnl, "Entry Indices") ;

        m_entryPnl.setLayout(new java.awt.BorderLayout());
        m_entryPnl.add(m_entryPane, java.awt.BorderLayout.CENTER);

        m_idxPnl.setLayout(new java.awt.BorderLayout());
        m_idxPnl.add(m_idxPane, java.awt.BorderLayout.CENTER);

        getContentPane().setLayout(new java.awt.BorderLayout());
        JPanel content = new JPanel();
        content.setPreferredSize(new java.awt.Dimension(798, 461));
        content.setLayout(new java.awt.BorderLayout());
        content.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        content.add(m_mainPnl, java.awt.BorderLayout.NORTH);
        getContentPane().add(content, BorderLayout.CENTER);
        // set title
        setTitle("Jdbm DB Backend Viewer");
        // add status bar
        getContentPane().add(statusBar, BorderLayout.SOUTH);
        // add menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu m_backendMenu = new JMenu("File");
        m_backendMenu.setMnemonic('F');
        // create Exit menu item
        JMenuItem m_exit = new JMenuItem("Exit");
        m_exit.setMnemonic('E');
        m_exit.setBackground(new java.awt.Color(205,205,205));
        m_exit.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    exitForm() ;
                }
            });
        // create About menu item
        JMenu m_helpMenu = new JMenu("Help");
        m_helpMenu.setMnemonic('H');
        JMenuItem m_about = new JMenuItem("About");
        m_about.setMnemonic('A');
        m_about.setBackground(new java.awt.Color(205,205,205));
        m_about.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    AboutDialog aboutDialog = new AboutDialog (BackendFrame.this, true);
                    Dimension frameSize = getSize();
                    Dimension aboutSize = aboutDialog.getPreferredSize();
                    int x = getLocation().x + (frameSize.width - aboutSize.width) / 2;
                    int y = getLocation().y + (frameSize.height - aboutSize.height) / 2;
                    if (x < 0) x = 0;
                    if (y < 0) y = 0;
                    aboutDialog.setLocation(x, y);
                    aboutDialog.setVisible(true);
                }
            });
        m_helpMenu.setBackground(new java.awt.Color(205,205,205));
        m_helpMenu.add(m_about);
        // create Open menu item
        String l_startPath = null ;
        if(File.separatorChar == '/') {
            l_startPath = "../projects/ldapd-test" ;
        } else {
            l_startPath = "..\\projects\\ldapd-test" ;
        }

        final JFileChooser fc = new JFileChooser(l_startPath);
        JMenuItem m_open = new JMenuItem("Open");
        m_open.setMnemonic('O');
        m_open.setName("Open");
        m_open.setActionCommand("Open");
        m_open.setBackground(new java.awt.Color(205,205,205));
        m_open.addActionListener(
            new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    int returnVal = fc.showOpenDialog(BackendFrame.this);
                    if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION) {
                        File file = fc.getSelectedFile() ;
                        try {
							loadDatabase("ldapd.test.Harness", file.getParent()) ;
                        } catch(Exception ex) {
                            ex.printStackTrace() ;
                        }
                    } else {
                        // Write your code here what to do if user has canceled Open dialog
                    }
                }
            });
        m_backendMenu.setName("Backend");
        m_backendMenu.setBackground(new java.awt.Color(205,205,205));
        m_backendMenu.add(m_open);
        // create Save menu item
        // create Print menu item
        m_backendMenu.add(m_exit);
        menuBar.setBackground(new java.awt.Color(196,197,203));
        menuBar.add(m_backendMenu);
        menuBar.add(m_searchMenu);
        menuBar.add(m_indices);
        menuBar.add(m_helpMenu);
        // sets menu bar
        setJMenuBar(menuBar);
        setBounds(new java.awt.Rectangle(0, 0, 802, 515));
        setSize(new java.awt.Dimension(802,515));
        setResizable(true);
        addWindowListener(
            new java.awt.event.WindowAdapter() {
                public void windowClosing(java.awt.event.WindowEvent evt) {
                    exitForm() ;
                }
            });
        m_treePane.getViewport().add(m_tree);
        m_tree.setBounds(new java.awt.Rectangle(6,184,82,80));
        m_tree.setShowsRootHandles(true);
        m_tree.setToolTipText("Jdbm DB DIT");
        m_tree.setScrollsOnExpand(true) ;
        m_tree.getSelectionModel().addTreeSelectionListener(
            new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent e) {
                    TreePath l_path = e.getNewLeadSelectionPath() ;

                    if(l_path == null) {
                        return ;
                    }

                    Object l_last = l_path.getLastPathComponent() ;
                    try {
                        if(l_last instanceof EntryNode) {
                            displayEntry(((EntryNode) l_last).getLdapEntry()) ;
                        }
                    } catch(Exception ex) {
                        ex.printStackTrace() ;
                    }
                }
        }) ;

        m_entryPane.getViewport().add(m_entryTbl);
        m_entryTbl.setBounds(new java.awt.Rectangle(321,103,32,32));

        m_idxPane.getViewport().add(m_idxTbl);
        m_idxTbl.setBounds(new java.awt.Rectangle(429,134,32,32));

        m_treePane.setSize(new java.awt.Dimension(285, 435));
        m_treePane.setPreferredSize(new java.awt.Dimension(285, 403));
        m_searchMenu.setText("Search") ;
        m_searchMenu.setName("Search") ;
        m_searchMenu.setBackground(new java.awt.Color(205,205,205));
        m_searchMenu.add(m_run) ;
        m_searchMenu.add(m_debug) ;
        m_searchMenu.add(m_annotate) ;

        ActionListener l_searchHandler = new ActionListener()
        {
            public void actionPerformed(ActionEvent an_event) {
                System.out.println("action command = " + an_event.getActionCommand()) ;
				doFilterDialog(an_event.getActionCommand()) ;
            }
        } ;

        m_annotate.setText(FilterDialog.ANNOTATE_MODE) ;
        m_annotate.setName(FilterDialog.ANNOTATE_MODE) ;
        m_annotate.setActionCommand(FilterDialog.ANNOTATE_MODE) ;
        m_annotate.setBackground(new java.awt.Color(205,205,205));
        m_annotate.addActionListener(l_searchHandler) ;

        m_run.setText(FilterDialog.RUN_MODE) ;
        m_run.setName(FilterDialog.RUN_MODE) ;
        m_run.setActionCommand(FilterDialog.RUN_MODE) ;
        m_run.setBackground(new java.awt.Color(205,205,205));
        m_run.addActionListener(l_searchHandler) ;

        m_debug.setText(FilterDialog.DEBUG_MODE) ;
        m_debug.setName(FilterDialog.DEBUG_MODE) ;
        m_debug.setActionCommand(FilterDialog.DEBUG_MODE) ;
        m_debug.setBackground(new java.awt.Color(205,205,205));
        m_debug.addActionListener(l_searchHandler) ;

        m_indices.setText("Indices") ;
        m_indices.setName("Indices") ;
        m_indices.setBackground(new java.awt.Color(205,205,205));
    }


    public void enableLogging(Logger a_logger)
    {
        m_logger = a_logger ;
    }


    public void launch()
    {
        //Center the frame on screen
        Dimension l_screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension l_frameSize = getSize() ;
        l_frameSize.height = ((l_frameSize.height > l_screenSize.height)
            ? l_screenSize.height : l_frameSize.height);
        l_frameSize.width = ((l_frameSize.width > l_screenSize.width)
            ? l_screenSize.width : l_frameSize.width);
        setLocation((l_screenSize.width - l_frameSize.width) / 2,
            (l_screenSize.height - l_frameSize.height) / 2);
        setVisible(true) ;
    }


    /**
     * Exit the Application
     */
    private void exitForm() {
        this.setEnabled(false) ;
        this.setVisible(false) ;
        this.dispose() ;

        if(m_doCleanUp && m_database != null) {
            m_database.sync() ;
            m_database.close() ;
	        System.exit(0) ;
        }
    }


    public void doFilterDialog(final String a_mode)
    {
        final FilterDialog l_dialog = new FilterDialog(a_mode, this, true) ;

        if(this.m_tree.getSelectionModel().getSelectionPath() != null) {
            TreePath l_path = m_tree.getSelectionModel().getSelectionPath() ;
            Object l_last = l_path.getLastPathComponent() ;
            String l_base = null ;
            if(l_last instanceof EntryNode) {
                LdapEntryImpl l_entry = ((EntryNode) l_last).getLdapEntry() ;
                l_base = l_entry.getEntryDN() ;
            } else {
                l_base = m_database.getSuffix().toString() ;
            }

            l_dialog.setBase(l_base) ;
        } else {
            l_dialog.setBase(m_database.getSuffix().toString()) ;
        }

        l_dialog.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent an_event) {
                String l_cmd = an_event.getActionCommand() ;

                try {
                    if(l_cmd.equals(FilterDialog.SEARCH_CMD)) {
                        if(a_mode == FilterDialog.RUN_MODE) {
                            doRun(l_dialog.getFilter(), l_dialog.getScope(),
                                l_dialog.getBase(), l_dialog.getLimit()) ;
                        } else if(a_mode == FilterDialog.DEBUG_MODE) {
                            doDebug(l_dialog.getFilter(), l_dialog.getScope(),
                                 l_dialog.getBase(), l_dialog.getLimit()) ;
                        } else if(a_mode == FilterDialog.ANNOTATE_MODE) {
                            if(doAnnotate(l_dialog.getFilter())) {
                                // continue
                            } else {
                                // We failed don't loose users filter buf
                                // allow user to make edits.
                                return ;
                            }
                        } else {
                            throw new RuntimeException("Unrecognized mode.") ;
                        }
                    } else if(l_cmd.equals(FilterDialog.CANCEL_CMD)) {
                        // Do nothing! Just exit dialog.
                    } else {
                        throw new
                            RuntimeException("Unrecognized FilterDialog command: "
                                + l_cmd) ;
                    }
                } catch(Exception e) {
                    e.printStackTrace() ;
                }

				l_dialog.setVisible(false) ;
				l_dialog.dispose() ;
            }
        }) ;

        //Center the frame on screen
        l_dialog.setSize(456, 256) ;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = l_dialog.getSize();
        frameSize.height = ((frameSize.height > screenSize.height)
            ? screenSize.height : frameSize.height);
        frameSize.width = ((frameSize.width > screenSize.width)
            ? screenSize.width : frameSize.width);
        l_dialog.setLocation((screenSize.width - frameSize.width) / 2,
            (screenSize.height - frameSize.height) / 2);
        l_dialog.setEnabled(true) ;
        l_dialog.setVisible(true) ;
    }


    public boolean doRun(String a_filter, String a_scope, String a_base,
        String a_limit)
        throws Exception
    {
        System.out.println("Search attempt using filter '" + a_filter + "' "
            + "with scope '" + a_scope + "' and a return limit of '" + a_limit
            + "'") ;
        SearchEngine l_engine = new SearchEngine() ;
        l_engine.enableLogging(m_logger) ;
        FilterParser l_parser = new FilterParserImpl() ;
        l_parser.setFilterParserMonitor( new FilterParserMonitorAdapter() ) ;
        ExprNode l_root = null ;

        try {
            l_root = l_parser.parse(a_filter) ;
        } catch(Exception e) {
            JTextArea l_text = new JTextArea() ;
            String l_msg = e.getMessage() ;

            if(l_msg.length() > 1024) {
                l_msg = l_msg.substring(0, 1024) + "\n. . . truncated . . ." ;
            }

            l_text.setText(l_msg) ;
            l_text.setEnabled(false) ;
            JOptionPane.showMessageDialog(null, l_text, "Syntax Error",
                JOptionPane.ERROR_MESSAGE) ;
            return false ;
        }

        int l_scope = -1 ;

        if(a_scope == FilterDialog.BASE_SCOPE) {
	        l_scope = Backend.BASE_SCOPE ;
        } else if(a_scope == FilterDialog.SINGLE_SCOPE) {
            l_scope = Backend.SINGLE_SCOPE ;
        } else if(a_scope == FilterDialog.SUBTREE_SCOPE) {
            l_scope = Backend.SUBTREE_SCOPE ;
        } else {
            throw new RuntimeException("Unexpected scope parameter: " +
                a_scope) ;
        }

        int l_limit = Integer.MAX_VALUE ;
        if(!a_limit.equals(FilterDialog.UNLIMITED)) {
            l_limit = Integer.parseInt(a_limit) ;
        }

        Cursor l_cursor =
            l_engine.search(m_database, l_root, a_base, l_scope) ;
        String [] l_cols = new String [2] ;
        l_cols[0] = "id" ;
        l_cols[1] = "dn" ;
        DefaultTableModel l_tableModel = new DefaultTableModel(l_cols, 0) ;
        Object [] l_row = new Object[2] ;
        int l_count = 0 ;
        while(l_cursor.hasMore() && l_count < l_limit) {
            IndexRecord l_rec = (IndexRecord) l_cursor.next() ;
            l_row[0] = l_rec.getEntryId() ;
            l_row[1] = m_database.getEntryDn((BigInteger) l_row[0]) ;
            l_tableModel.addRow(l_row) ;
            l_count++ ;
        }

        SearchResultDialog l_results = new SearchResultDialog(this, false) ;
        StringBuffer l_buf = new StringBuffer() ;
        l_buf.append("base: ").append(a_base).append('\n') ;
        l_buf.append("scope: ").append(a_scope).append('\n') ;
        l_buf.append("limit: ").append(a_limit).append('\n') ;
        l_buf.append("total: ").append(l_count).append('\n') ;
        l_buf.append("filter:\n").append(a_filter).append('\n') ;
        l_results.setFilter(l_buf.toString()) ;

	    TreeNode l_astRoot = new ASTNode(null, l_root) ;
	    TreeModel l_treeModel = new DefaultTreeModel(l_astRoot, true) ;
        l_results.setTreeModel(l_treeModel) ;
        l_results.setTableModel(l_tableModel) ;
        l_results.setVisible(true) ;
        return true ;
    }


    public void doDebug(String a_filter, String a_scope, String a_base,
        String a_limit)
    {
        System.out.println("Search attempt using filter '" + a_filter + "' "
            + "with scope '" + a_scope + "' and a return limit of '" + a_limit
            + "'") ;
    }


    public void selectTreeNode(BigInteger a_id)
    {
        Stack l_stack = new Stack() ;
        TreeNode l_parent = (EntryNode) m_nodes.get(a_id) ;
        while(l_parent != null && (l_parent != l_parent.getParent())) {
            l_stack.push(l_parent) ;
            l_parent = l_parent.getParent() ;
        }


        Object [] l_comps = null ;

        if(l_stack.size() == 0) {
            l_comps = new Object[1] ;
            l_comps[0] = m_root ;
        } else {
            l_comps = new Object[l_stack.size()] ;
        }

        for(int ii = 0; l_stack.size() > 0 && ii < l_comps.length; ii++) {
            l_comps[ii] = l_stack.pop() ;
        }

        TreePath l_path = new TreePath(l_comps) ;
        m_tree.scrollPathToVisible(l_path) ;
        m_tree.getSelectionModel().setSelectionPath(l_path) ;
        m_tree.validate() ;
    }


    public boolean doAnnotate(String a_filter)
        throws Exception
    {
		FilterParser l_parser = new FilterParserImpl() ;
        l_parser.setFilterParserMonitor( new FilterParserMonitorAdapter() );
        ExprNode l_root = null ;

        try {
            l_root = l_parser.parse(a_filter) ;
        } catch(Exception e) {
            JTextArea l_text = new JTextArea() ;
            String l_msg = e.getMessage() ;

            if(l_msg.length() > 1024) {
                l_msg = l_msg.substring(0, 1024) + "\n. . . truncated . . ." ;
            }

            l_text.setText(l_msg) ;
            l_text.setEnabled(false) ;
            JOptionPane.showMessageDialog(null, l_text, "Syntax Error",
                JOptionPane.ERROR_MESSAGE) ;
            return false ;
        }

		AnnotatedFilterTreeDialog l_treeDialog = new
			AnnotatedFilterTreeDialog(BackendFrame.this, false) ;
		l_treeDialog.setFilter(a_filter) ;

        DefaultOptimizer l_optimizer = new DefaultOptimizer() ;
        l_optimizer.annotate(this.m_database, l_root) ;
		TreeNode l_astRoot = new ASTNode(null, l_root) ;
		TreeModel l_model = new DefaultTreeModel(l_astRoot, true) ;
		l_treeDialog.setModel(l_model) ;
        l_treeDialog.setVisible(true) ;
        return true ;
    }


    public void showIndexDialog(String a_index)
        throws Exception
    {
        System.out.println("Got request to show index dialog for " + a_index) ;
        Index l_index = (Index) m_database.getIndex(a_index) ;

        if(l_index != null) {
            IndexDialog l_dialog = new IndexDialog(this, false, l_index) ;
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension frameSize = l_dialog.getSize();
            frameSize.height = ((frameSize.height > screenSize.height)
                ? screenSize.height : frameSize.height);
            frameSize.width = ((frameSize.width > screenSize.width)
                ? screenSize.width : frameSize.width);
            l_dialog.setLocation((screenSize.width - frameSize.width) / 2,
                (screenSize.height - frameSize.height) / 2);
            l_dialog.setEnabled(true) ;
            l_dialog.setVisible(true) ;
        }
    }


    public void buildIndicesMenu(Database a_database)
    {
        ActionListener l_listener = new ActionListener()
        {
            public void actionPerformed(ActionEvent a_event) {
                try {
                    showIndexDialog(a_event.getActionCommand()) ;
                } catch(Exception e) {
                    e.printStackTrace() ;
                }
            }
        } ;

        JMenuItem l_item = null ;
        String [] l_sdi = Database.SYS_INDICES ;
        Iterator l_udi = a_database.getUDIAttributes() ;

        for(int ii = 0; ii < l_sdi.length; ii++) {
			l_item = new JMenuItem() ;
            l_item.setBackground(new java.awt.Color(205,205,205));
            m_indices.add(l_item) ;
            l_item.setText(l_sdi[ii]) ;
            l_item.setName(l_sdi[ii]) ;
            l_item.setActionCommand(l_sdi[ii]) ;
            l_item.addActionListener(l_listener) ;
        }

        m_indices.add(new JSeparator()) ;

        while(l_udi.hasNext()) {
            String l_index = (String) l_udi.next() ;
			l_item = new JMenuItem() ;
            l_item.setBackground(new java.awt.Color(205,205,205));
            m_indices.add(l_item) ;
            l_item.setText(l_index) ;
            l_item.setName(l_index) ;
            l_item.setActionCommand(l_index) ;
            l_item.addActionListener(l_listener) ;
        }
    }


    void displayEntry(LdapEntryImpl a_entry)
        throws Exception
    {
        MultiMapModel l_model = new MultiMapModel(a_entry) ;
        this.m_entryTbl.setModel(l_model) ;

        if(m_database != null) {
            l_model = new MultiMapModel(
                m_database.getIndices(a_entry.getEntryID())) ;
            this.m_idxTbl.setModel(l_model) ;
        } else {
            this.m_idxTbl.setModel(null) ;
        }

        this.validate() ;
    }


    public void loadDatabase(Database a_database)
        throws Exception
    {
        m_database = a_database ;
        m_doCleanUp = false ;
        load() ;
    }


    public void loadDatabase(String a_kernelClass, String a_dirPath)
        throws Exception
    {
        m_doCleanUp = true ;

        m_kernel = (Kernel) Class.forName(a_kernelClass).newInstance() ;
        m_kernel.setRoot(a_dirPath) ;
        m_kernel.bootStrap("backend0") ;
        m_logger = m_kernel.getLogger() ;
        JdbmModule l_backend = (JdbmModule)
            m_kernel.getServiceManager().lookup(JdbmModule.ROLE) ;
        m_database = l_backend.getDatabase() ;
        load() ;
    }


    private void load()
        throws Exception
    {
        boolean doFiltered = false ;
        m_nodes = new HashMap() ;

        int l_option =
            JOptionPane.showConfirmDialog(null, "Would you like to filter "
            + "leaf nodes on load?", "Use Filter?",
            JOptionPane.OK_CANCEL_OPTION) ;
        doFiltered = l_option == JOptionPane.OK_OPTION ;

        if(doFiltered) {
            SearchEngine l_engine = new SearchEngine() ;
            final FilterDialog l_dialog =
                new FilterDialog(FilterDialog.LOAD_MODE, this, true) ;
            l_dialog.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    l_dialog.setVisible(false) ;
                    l_dialog.dispose() ;
                }
            }) ;

            l_dialog.setBase(m_database.getSuffix().toString()) ;
            l_dialog.setScope(FilterDialog.SUBTREE_SCOPE) ;

            //Center the frame on screen
            l_dialog.setSize(456, 256) ;
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension frameSize = l_dialog.getSize();
            frameSize.height = ((frameSize.height > screenSize.height)
                ? screenSize.height : frameSize.height);
            frameSize.width = ((frameSize.width > screenSize.width)
                ? screenSize.width : frameSize.width);
            l_dialog.setLocation((screenSize.width - frameSize.width) / 2,
                (screenSize.height - frameSize.height) / 2);
            l_dialog.setEnabled(true) ;
            l_dialog.setVisible(true) ;

            FilterParser l_parser = new FilterParserImpl() ;
            l_parser.setFilterParserMonitor( new FilterParserMonitorAdapter());
            ExprNode l_exprNode = l_parser.parse(l_dialog.getFilter()) ;

            int l_scope = -1 ;
            String l_scopeStr = l_dialog.getScope() ;
            if(l_scopeStr == FilterDialog.BASE_SCOPE) {
                l_scope = Backend.BASE_SCOPE ;
            } else if(l_scopeStr == FilterDialog.SINGLE_SCOPE) {
                l_scope = Backend.SINGLE_SCOPE ;
            } else if(l_scopeStr == FilterDialog.SUBTREE_SCOPE) {
                l_scope = Backend.SUBTREE_SCOPE ;
            } else {
                throw new RuntimeException("Unrecognized scope") ;
            }

            l_exprNode =
                l_engine.addScopeNode(l_exprNode, l_dialog.getBase(), l_scope) ;
            m_root = new EntryNode(null, m_database,
                m_database.getSuffixEntry(), m_nodes, l_exprNode, l_engine) ;
        } else {
            m_root = new EntryNode(null, m_database,
                m_database.getSuffixEntry(), m_nodes) ;
        }

        DefaultTreeModel l_model = new DefaultTreeModel(m_root) ;
        m_tree.setModel(l_model) ;
        buildIndicesMenu(m_database) ;
        if(this.isVisible()) {
            m_tree.validate() ;
        }
    }
}
