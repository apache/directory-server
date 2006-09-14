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


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Stack;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.apache.directory.server.core.partition.impl.btree.BTreePartition;
import org.apache.directory.server.core.partition.impl.btree.Index;
import org.apache.directory.server.core.partition.impl.btree.IndexNotFoundException;
import org.apache.directory.server.core.partition.impl.btree.IndexRecord;
import org.apache.directory.server.core.partition.impl.btree.SearchEngine;

import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.filter.FilterParserImpl;
import org.apache.directory.shared.ldap.ldif.Entry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.apache.directory.shared.ldap.message.DerefAliasesEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.StringTools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The frame for the database.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class PartitionFrame extends JFrame
{
    private static final Logger log = LoggerFactory.getLogger( PartitionFrame.class );

    private static final long serialVersionUID = 4049353102291513657L;

    // Swing Stuff
    private JLabel statusBar = new JLabel( "Ready" );
    private JPanel mainPnl = new JPanel();
    private JSplitPane splitPane = new JSplitPane();
    private JTabbedPane tabbedPane = new JTabbedPane();
    private JPanel entryPnl = new JPanel();
    private JPanel idxPnl = new JPanel();
    private JScrollPane treePane = new JScrollPane();
    private JTree tree = new JTree();
    private JScrollPane entryPane = new JScrollPane();
    private JTable entryTbl = new JTable();
    private JScrollPane idxPane = new JScrollPane();
    private JTable idxTbl = new JTable();
    private JMenu searchMenu = new JMenu();
    private JMenuItem annotate = new JMenuItem();
    private JMenuItem run = new JMenuItem();
    private JMenuItem debug = new JMenuItem();
    private JMenu indices = new JMenu();

    // Non Swing Stuff
    private BTreePartition partition = null;
    private boolean doCleanUp = false;
    private HashMap nodes = new HashMap();
    private EntryNode root = null;
    private SearchEngine eng = null;


    /**
     * Creates new form JFrame
     */
    public PartitionFrame(BTreePartition db, SearchEngine eng) throws NamingException
    {
        partition = db;
        this.eng = eng;

        initialize();
        buildIndicesMenu( partition );
        pack();
        load();
    }


    /**
     * This method is called from within the constructor to initialize the form
     */
    private void initialize() throws NamingException
    {
        mainPnl.setBorder( null );
        mainPnl.setLayout( new java.awt.BorderLayout() );
        mainPnl.add( splitPane, java.awt.BorderLayout.CENTER );
        splitPane.add( tabbedPane, javax.swing.JSplitPane.RIGHT );
        splitPane.add( treePane, javax.swing.JSplitPane.LEFT );
        tabbedPane.add( entryPnl, "Entry Attributes" );
        tabbedPane.add( idxPnl, "Entry Indices" );

        entryPnl.setLayout( new java.awt.BorderLayout() );
        entryPnl.add( entryPane, java.awt.BorderLayout.CENTER );

        idxPnl.setLayout( new java.awt.BorderLayout() );
        idxPnl.add( idxPane, java.awt.BorderLayout.CENTER );

        getContentPane().setLayout( new java.awt.BorderLayout() );
        JPanel content = new JPanel();
        content.setPreferredSize( new java.awt.Dimension( 798, 461 ) );
        content.setLayout( new java.awt.BorderLayout() );
        content.setBorder( javax.swing.BorderFactory.createEtchedBorder() );
        content.add( mainPnl, java.awt.BorderLayout.NORTH );
        getContentPane().add( content, BorderLayout.CENTER );
        // set title
        setTitle( "Partition: " + this.partition.getSuffix().toString() );
        // add status bar
        getContentPane().add( statusBar, BorderLayout.SOUTH );
        // add menu bar
        JMenuBar menuBar = new JMenuBar();

        // --------------------------------------------------------------------
        // 'Backend' Menu
        // --------------------------------------------------------------------

        JMenu backendMenu = new JMenu( "Backend" );
        backendMenu.setText( "Partition" );
        backendMenu.setBackground( new java.awt.Color( 205, 205, 205 ) );
        backendMenu.setMnemonic( 'B' );

        // create Import menu item
        JMenuItem add = new JMenuItem( "Add" );
        backendMenu.add( add );
        add.setMnemonic( 'A' );
        add.setBackground( new java.awt.Color( 205, 205, 205 ) );
        add.addActionListener( new ActionListener()
        {
            public void actionPerformed( ActionEvent e )
            {
                doAddDialog();
            }
        } );

        // create Import menu item
        JMenuItem importItem = new JMenuItem( "Import" );
        backendMenu.add( importItem );
        importItem.setMnemonic( 'I' );
        importItem.setBackground( new java.awt.Color( 205, 205, 205 ) );
        importItem.addActionListener( new ActionListener()
        {
            public void actionPerformed( ActionEvent e )
            {
                doImport();
            }
        } );

        // create Exit menu item
        JMenuItem exit = new JMenuItem( "Exit" );
        backendMenu.add( exit );
        exit.setMnemonic( 'E' );
        exit.setBackground( new java.awt.Color( 205, 205, 205 ) );
        exit.addActionListener( new ActionListener()
        {
            public void actionPerformed( ActionEvent e )
            {
                exitForm();
            }
        } );

        // create About menu item
        JMenu helpMenu = new JMenu( "Help" );
        helpMenu.setMnemonic( 'H' );
        JMenuItem about = new JMenuItem( "About" );
        about.setMnemonic( 'A' );
        about.setBackground( new java.awt.Color( 205, 205, 205 ) );
        about.addActionListener( new ActionListener()
        {
            public void actionPerformed( ActionEvent e )
            {
                AboutDialog aboutDialog = new AboutDialog( PartitionFrame.this, true );
                PartitionFrame.this.centerOnScreen( aboutDialog );
                aboutDialog.setVisible( true );
            }
        } );
        helpMenu.setBackground( new java.awt.Color( 205, 205, 205 ) );
        helpMenu.add( about );

        // create Save menu item
        // create Print menu item
        menuBar.setBackground( new java.awt.Color( 196, 197, 203 ) );
        menuBar.add( backendMenu );
        menuBar.add( searchMenu );
        menuBar.add( indices );
        menuBar.add( helpMenu );
        // sets menu bar
        setJMenuBar( menuBar );
        setBounds( new java.awt.Rectangle( 0, 0, 802, 515 ) );
        setSize( new java.awt.Dimension( 802, 515 ) );
        setResizable( true );

        addWindowListener( new java.awt.event.WindowAdapter()
        {
            public void windowClosing( java.awt.event.WindowEvent evt )
            {
                exitForm();
            }
        } );

        treePane.getViewport().add( tree );
        tree.setBounds( new java.awt.Rectangle( 6, 184, 82, 80 ) );
        tree.setShowsRootHandles( true );
        tree.setToolTipText( "DB DIT" );
        tree.setScrollsOnExpand( true );
        tree.getSelectionModel().addTreeSelectionListener( new TreeSelectionListener()
        {
            public void valueChanged( TreeSelectionEvent e )
            {
                TreePath path = e.getNewLeadSelectionPath();

                if ( path == null )
                {
                    return;
                }

                Object last = path.getLastPathComponent();
                try
                {
                    if ( last instanceof EntryNode )
                    {
                        displayEntry( ( ( EntryNode ) last ).getEntryId(), ( ( EntryNode ) last ).getLdapEntry() );
                    }
                }
                catch ( Exception ex )
                {
                    ex.printStackTrace();
                }
            }
        } );

        entryPane.getViewport().add( entryTbl );
        entryTbl.setBounds( new java.awt.Rectangle( 321, 103, 32, 32 ) );

        idxPane.getViewport().add( idxTbl );
        idxTbl.setBounds( new java.awt.Rectangle( 429, 134, 32, 32 ) );

        treePane.setSize( new java.awt.Dimension( 285, 435 ) );
        treePane.setPreferredSize( new java.awt.Dimension( 285, 403 ) );
        searchMenu.setText( "Search" );
        searchMenu.setBackground( new java.awt.Color( 205, 205, 205 ) );
        searchMenu.add( run );
        searchMenu.add( debug );
        searchMenu.add( annotate );

        ActionListener searchHandler = new ActionListener()
        {
            public void actionPerformed( ActionEvent an_event )
            {
                if ( log.isDebugEnabled() )
                    log.debug( "action command = " + an_event.getActionCommand() );

                try
                {
                    doFilterDialog( an_event.getActionCommand() );
                }
                catch ( NamingException e )
                {
                    e.printStackTrace();
                }
            }
        };

        annotate.setText( FilterDialog.ANNOTATE_MODE );
        annotate.setActionCommand( FilterDialog.ANNOTATE_MODE );
        annotate.setBackground( new java.awt.Color( 205, 205, 205 ) );
        annotate.addActionListener( searchHandler );

        run.setText( FilterDialog.RUN_MODE );
        run.setActionCommand( FilterDialog.RUN_MODE );
        run.setBackground( new java.awt.Color( 205, 205, 205 ) );
        run.addActionListener( searchHandler );

        debug.setText( FilterDialog.DEBUG_MODE );
        debug.setActionCommand( FilterDialog.DEBUG_MODE );
        debug.setBackground( new java.awt.Color( 205, 205, 205 ) );
        debug.addActionListener( searchHandler );

        indices.setText( "Indices" );
        indices.setBackground( new java.awt.Color( 205, 205, 205 ) );
    }


    private void centerOnScreen( Window window )
    {
        Dimension frameSize = window.getSize();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        frameSize.height = ( ( frameSize.height > screenSize.height ) ? screenSize.height : frameSize.height );
        frameSize.width = ( ( frameSize.width > screenSize.width ) ? screenSize.width : frameSize.width );
        window.setLocation( ( screenSize.width - frameSize.width ) / 2, ( screenSize.height - frameSize.height ) / 2 );
    }


    /**
     * Displays a entry addition dialog.
     */
    public void doAddDialog()
    {
        try
        {
            TreePath path = tree.getSelectionModel().getSelectionPath();
            String parentDn = partition.getSuffix().toString();

            if ( null != path )
            {
                Object last = path.getLastPathComponent();

                if ( last instanceof EntryNode )
                {
                    parentDn = ( ( EntryNode ) last ).getEntryDn();
                }
            }

            if ( null == parentDn )
            {
                JOptionPane.showMessageDialog( this, "Must select a parent entry to add a child to!" );
                return;
            }

            AddEntryDialog dialog = new AddEntryDialog( this, false );
            dialog.setParentDn( parentDn );

            centerOnScreen( dialog );
            dialog.setEnabled( true );
            dialog.setVisible( true );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }


    /**
     * Gets the DN of the DIT node selected in the tree view.
     * 
     * @return the DN of the selected tree node or the root Dn of the tree if 
     * nothing has been selected yet.
     */
    public String getSelectedDn() throws NamingException
    {
        TreePath path = tree.getSelectionModel().getSelectionPath();

        if ( null == path )
        {
            return partition.getSuffix().toString();
        }

        Object last = path.getLastPathComponent();
        String base = null;

        if ( last instanceof EntryNode )
        {
            try
            {
                base = ( ( EntryNode ) last ).getEntryDn();
            }
            catch ( NamingException e )
            {
                e.printStackTrace();
            }
        }
        else
        {
            base = partition.getSuffix().toString();
        }

        return base;
    }


    public void doImport()
    {
        FileReader in = null;
        JFileChooser chooser = new JFileChooser();
        int choice = chooser.showOpenDialog( this );
        File selected = chooser.getSelectedFile();

        if ( JFileChooser.APPROVE_OPTION != choice )
        {
            return;
        }

        try
        {
            in = new FileReader( selected );
            Iterator list = new LdifReader( in );

            while ( list.hasNext() )
            {
                Entry entry = ( Entry ) list.next();
                String updn = entry.getDn();
                Attributes attrs = entry.getAttributes();
                
                LdapDN ndn = new LdapDN( StringTools.deepTrimToLower( updn ) );

                if ( null == partition.getEntryId( ndn.toString() ) )
                {
                    partition.add(ndn, attrs );
                    load();
                }
            }
        }
        catch ( NamingException e )
        {
            // @todo display popup with error here!
            e.printStackTrace();
            return;
        }
        catch ( FileNotFoundException e )
        {
            // @todo display popup with error here!
            e.printStackTrace();
            return;
        }
        catch ( Exception e )
        {
            // @todo display popup with error here!
            e.printStackTrace();
            return;
        }
    }


    /**
     * Exit the Application
     */
    private void exitForm()
    {
        setEnabled( false );
        setVisible( false );
        dispose();

        if ( doCleanUp && partition != null )
        {
            try
            {
                partition.sync();
                partition.destroy();
            }
            catch ( NamingException e )
            {
                e.printStackTrace();
            }

            System.exit( 0 );
        }
    }


    public void doRunDebugAnnotate( FilterDialog dialog, String mode )
    {
        try
        {
            if ( mode == FilterDialog.RUN_MODE )
            {
                doRun( dialog.getFilter(), dialog.getScope(), dialog.getBase(), dialog.getLimit() );
            }
            else if ( mode == FilterDialog.DEBUG_MODE )
            {
                doDebug( dialog.getFilter(), dialog.getScope(), dialog.getBase(), dialog.getLimit() );
            }
            else if ( mode == FilterDialog.ANNOTATE_MODE )
            {
                if ( doAnnotate( dialog.getFilter() ) )
                {
                    // continue
                }
                else
                {
                    // We failed don't loose users filter buf
                    // allow user to make edits.
                    return;
                }
            }
            else
            {
                throw new RuntimeException( "Unrecognized mode." );
            }
        }
        catch ( Exception e )
        {
            // @todo show error popup here!
            e.printStackTrace();
        }
    }


    public void doFilterDialog( final String mode ) throws NamingException
    {
        final FilterDialog dialog = new FilterDialog( mode, this, true );

        if ( tree.getSelectionModel().getSelectionPath() != null )
        {
            dialog.setBase( getSelectedDn() );
        }
        else
        {
            dialog.setBase( partition.getSuffix().toString() );
        }

        dialog.addActionListener( new ActionListener()
        {
            public void actionPerformed( ActionEvent an_event )
            {
                String cmd = an_event.getActionCommand();

                if ( cmd.equals( FilterDialog.SEARCH_CMD ) )
                {
                    doRunDebugAnnotate( dialog, mode );
                }
                else if ( cmd.equals( FilterDialog.CANCEL_CMD ) )
                {
                    // Do nothing! Just exit dialog.
                }
                else
                {
                    throw new RuntimeException( "Unrecognized FilterDialog command: " + cmd );
                }

                dialog.setVisible( false );
                dialog.dispose();
            }
        } );

        //Center the frame on screen
        dialog.setSize( 456, 256 );
        centerOnScreen( dialog );
        dialog.setEnabled( true );
        dialog.setVisible( true );
    }


    public boolean doRun( String filter, String scope, String base, String limit ) throws Exception
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "Search attempt using filter '" + filter + "' " + "with scope '" + scope
                + "' and a return limit of '" + limit + "'" );
        }

        FilterParser parser = new FilterParserImpl();
        ExprNode root = null;

        try
        {
            root = parser.parse( filter );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            JTextArea text = new JTextArea();
            String msg = e.getMessage();

            if ( msg.length() > 1024 )
            {
                msg = msg.substring( 0, 1024 ) + "\n. . . truncated . . .";
            }

            text.setText( msg );
            text.setEnabled( false );
            JOptionPane.showMessageDialog( null, text, "Syntax Error", JOptionPane.ERROR_MESSAGE );
            return false;
        }

        SearchControls ctls = new SearchControls();

        if ( scope == FilterDialog.BASE_SCOPE )
        {
            ctls.setSearchScope( SearchControls.OBJECT_SCOPE );
        }
        else if ( scope == FilterDialog.SINGLE_SCOPE )
        {
            ctls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        }
        else if ( scope == FilterDialog.SUBTREE_SCOPE )
        {
            ctls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        }
        else
        {
            throw new RuntimeException( "Unexpected scope parameter: " + scope );
        }

        int limitMax = Integer.MAX_VALUE;
        if ( !limit.equals( FilterDialog.UNLIMITED ) )
        {
            limitMax = Integer.parseInt( limit );
        }

        Hashtable env = new Hashtable();

        env.put( DerefAliasesEnum.JNDI_PROP, DerefAliasesEnum.DEREFALWAYS_NAME );

        NamingEnumeration cursor = eng.search( new LdapDN( base ), env, root, ctls );
        String[] cols = new String[2];
        cols[0] = "id";
        cols[1] = "dn";
        DefaultTableModel tableModel = new DefaultTableModel( cols, 0 );
        Object[] row = new Object[2];
        int count = 0;
        while ( cursor.hasMore() && count < limitMax )
        {
            IndexRecord rec = ( IndexRecord ) cursor.next();
            row[0] = rec.getEntryId();
            row[1] = partition.getEntryDn( ( BigInteger ) row[0] );
            tableModel.addRow( row );
            count++;
        }

        SearchResultDialog results = new SearchResultDialog( this, false );
        StringBuffer buf = new StringBuffer();
        buf.append( "base: " );
        buf.append( base );
        buf.append( "\n" );
        buf.append( "scope: " );
        buf.append( scope );
        buf.append( "\n" );
        buf.append( "limit: " );
        buf.append( limit );
        buf.append( "\n" );
        buf.append( "total: " );
        buf.append( count );
        buf.append( "\n" );
        buf.append( "filter:\n" );
        buf.append( filter );
        buf.append( "\n" );
        results.setFilter( buf.toString() );

        TreeNode astRoot = new ASTNode( null, root );
        TreeModel treeModel = new DefaultTreeModel( astRoot, true );
        results.setTreeModel( treeModel );
        results.setTableModel( tableModel );
        centerOnScreen( results );
        results.setVisible( true );
        return true;
    }


    public void doDebug( String filter, String scope, String base, String limit )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "Search attempt using filter '" + filter + "' " + "with scope '" + scope
                + "' and a return limit of '" + limit + "'" );
        }
    }


    public void selectTreeNode( BigInteger id )
    {
        Stack stack = new Stack();
        Object[] comps = null;
        TreeNode parent = ( EntryNode ) nodes.get( id );

        while ( parent != null && ( parent != parent.getParent() ) )
        {
            stack.push( parent );
            parent = parent.getParent();
        }

        if ( stack.size() == 0 )
        {
            comps = new Object[1];
            comps[0] = root;
        }
        else
        {
            comps = new Object[stack.size()];
        }

        for ( int ii = 0; stack.size() > 0 && ii < comps.length; ii++ )
        {
            comps[ii] = stack.pop();
        }

        TreePath path = new TreePath( comps );
        tree.scrollPathToVisible( path );
        tree.getSelectionModel().setSelectionPath( path );
        tree.validate();
    }


    public boolean doAnnotate( String filter ) throws Exception
    {
        FilterParser parser = new FilterParserImpl();
        ExprNode root = null;

        try
        {
            root = parser.parse( filter );
        }
        catch ( Exception e )
        {
            JTextArea text = new JTextArea();
            String msg = e.getMessage();

            if ( msg.length() > 1024 )
            {
                msg = msg.substring( 0, 1024 ) + "\n. . . truncated . . .";
            }

            text.setText( msg );
            text.setEnabled( false );
            JOptionPane.showMessageDialog( null, text, "Syntax Error", JOptionPane.ERROR_MESSAGE );
            return false;
        }

        AnnotatedFilterTreeDialog treeDialog = new AnnotatedFilterTreeDialog( PartitionFrame.this, false );
        treeDialog.setFilter( filter );

        eng.getOptimizer().annotate( root );
        TreeNode astRoot = new ASTNode( null, root );
        TreeModel model = new DefaultTreeModel( astRoot, true );
        treeDialog.setModel( model );
        treeDialog.setVisible( true );
        return true;
    }


    /**
     * Shows a dialog to display and scan indices.
     * 
     * @param idxAttr the name of the index or its attribute
     * @throws Exception if the indices cannot be accessed
     */
    public void showIndexDialog( String idxAttr ) throws Exception
    {
        Index index = null;
        boolean isSystem = partition.hasSystemIndexOn( idxAttr );

        if ( isSystem )
        {
            index = partition.getSystemIndex( idxAttr );
        }
        else
        {
            index = partition.getUserIndex( idxAttr );
        }

        if ( index != null )
        {
            IndexDialog dialog = new IndexDialog( this, false, index );
            centerOnScreen( dialog );
            dialog.setEnabled( true );
            dialog.setVisible( true );
        }
    }


    public void buildIndicesMenu( BTreePartition partition )
    {
        JMenuItem item = null;

        ActionListener listener = new ActionListener()
        {
            public void actionPerformed( ActionEvent event )
            {
                try
                {
                    showIndexDialog( event.getActionCommand() );
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                }
            }
        };

        Iterator list = partition.getSystemIndices();
        while ( list.hasNext() )
        {
            String idx = ( String ) list.next();
            Index index = null;
            try
            {
                index = partition.getSystemIndex( idx );
            }
            catch ( IndexNotFoundException e )
            {
                e.printStackTrace();
            }
            
            item = new JMenuItem();
            item.setBackground( new java.awt.Color( 205, 205, 205 ) );
            indices.add( item );
            item.setText( index.getAttribute().getName() );
            item.setActionCommand( index.getAttribute().getName() );
            item.addActionListener( listener );
        }

        indices.add( new JSeparator() );
        list = partition.getUserIndices();
        while ( list.hasNext() )
        {
            String idx = ( String ) list.next();
            Index index = null;
            try
            {
                index = partition.getUserIndex( idx );
            }
            catch ( IndexNotFoundException e )
            {
                e.printStackTrace();
            }
            
            item = new JMenuItem();
            item.setBackground( new java.awt.Color( 205, 205, 205 ) );
            indices.add( item );
            item.setText( index.getAttribute().getName() );
            item.setActionCommand( idx );
            item.addActionListener( listener );
        }
    }


    void displayEntry( BigInteger id, Attributes entry ) throws Exception
    {
        String dn = partition.getEntryUpdn( id );
        AttributesTableModel model = new AttributesTableModel( entry, id, dn, false );
        entryTbl.setModel( model );

        model = new AttributesTableModel( partition.getIndices( id ), id, dn, false );
        idxTbl.setModel( model );

        validate();
    }


    private void load() throws NamingException
    {
        // boolean doFiltered = false;
        nodes = new HashMap();

        Attributes suffix = partition.getSuffixEntry();
        BigInteger id = partition.getEntryId( partition.getSuffix().toString() );
        root = new EntryNode( id, null, partition, suffix, nodes );

        /*
         int option = JOptionPane.showConfirmDialog( null,
         "Would you like to filter leaf nodes on load?", "Use Filter?",
         JOptionPane.OK_CANCEL_OPTION );
         doFiltered = option == JOptionPane.OK_OPTION;

         if(doFiltered) {
         SearchEngine engine = new SearchEngine();
         final FilterDialog dialog =
         new FilterDialog(FilterDialog.LOAD_MODE, this, true);
         dialog.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
         dialog.setVisible(false);
         dialog.dispose();
         }
         });

         dialog.setBase(database.getSuffix().toString());
         dialog.setScope(FilterDialog.SUBTREE_SCOPE);

         //Center the frame on screen
         dialog.setSize(456, 256);
         this.centerOnScreen( dialog );
         dialog.setEnabled(true);
         dialog.setVisible(true);

         FilterParser parser = new FilterParserImpl();
         parser.enableLogging(logger);
         ExprNode exprNode = parser.parse(dialog.getFilter());

         int scope = -1;
         String scopeStr = dialog.getScope();
         if(scopeStr == FilterDialog.BASE_SCOPE) {
         scope = Backend.BASE_SCOPE;
         } else if(scopeStr == FilterDialog.SINGLE_SCOPE) {
         scope = Backend.SINGLE_SCOPE;
         } else if(scopeStr == FilterDialog.SUBTREE_SCOPE) {
         scope = Backend.SUBTREE_SCOPE;
         } else {
         throw new RuntimeException("Unrecognized scope");
         }

         exprNode =
         engine.addScopeNode(exprNode, dialog.getBase(), scope);
         root = new EntryNode(null, database,
         database.getSuffixEntry(), nodes, exprNode, engine);
         } else {
         root = new EntryNode(null, database,
         database.getSuffixEntry(), nodes);
         }
         */

        DefaultTreeModel model = new DefaultTreeModel( root );
        tree.setModel( model );

        if ( isVisible() )
        {
            tree.validate();
        }
    }
}
