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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import javax.naming.NamingException;
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

import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.partition.impl.btree.AbstractBTreePartition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.search.PartitionSearchResult;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.filter.FilterParser;
import org.apache.directory.shared.ldap.model.ldif.LdifEntry;
import org.apache.directory.shared.ldap.model.ldif.LdifReader;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The frame for the database.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PartitionFrame extends JFrame
{
    private static final Logger LOG = LoggerFactory.getLogger( PartitionFrame.class );

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
    private AbstractBTreePartition partition;
    private boolean doCleanUp;
    private Map<String, EntryNode> nodes;
    private EntryNode root;

    /** A handle on the global schemaManager */
    private SchemaManager schemaManager;


    /**
     * Creates new form JFrame
     * 
     * @param db the partition to view
     * @throws NamingException if there are problems accessing the partition
     */
    public PartitionFrame( AbstractBTreePartition db, SchemaManager schemaManager ) throws Exception
    {
        partition = db;
        this.schemaManager = schemaManager;

        initialize();
        buildIndicesMenu( partition );
        pack();
        load();
    }


    /**
     * This method is called from within the constructor to initialize the form
     *
     * @throws NamingException on partition access errors
     */
    private void initialize() throws Exception
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
        setTitle( "Partition: " + this.partition.getSuffixDn().getName() );
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
                try
                {
                    exitForm();
                }
                catch ( Exception e1 )
                {
                    e1.printStackTrace();
                }
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
                try
                {
                    exitForm();
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                }
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
                LOG.debug( "action command = {}", an_event.getActionCommand() );

                try
                {
                    doFilterDialog( an_event.getActionCommand() );
                }
                catch ( Exception e )
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
            String parentDn = partition.getSuffixDn().getName();

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

            AddEntryDialog dialog = new AddEntryDialog( this, false, schemaManager );
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
     * Gets the Dn of the DIT node selected in the tree view.
     * 
     * @return the Dn of the selected tree node or the root Dn of the tree if
     * nothing has been selected yet.
     * @throws NamingException on partition access errors
     */
    public String getSelectedDn() throws Exception
    {
        TreePath path = tree.getSelectionModel().getSelectionPath();

        if ( null == path )
        {
            return partition.getSuffixDn().getName();
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
            base = partition.getSuffixDn().getName();
        }

        return base;
    }


    public void doImport()
    {
        FileReader in;
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

            for ( LdifEntry entry : new LdifReader( in ) )
            {
                String updn = entry.getDn().getName();

                Dn ndn = new Dn( Strings.deepTrimToLower( updn ) );

                Entry attrs = new DefaultEntry( schemaManager, entry.getEntry() );

                if ( null == partition.getEntryId( ndn ) )
                {
                    partition.add( new AddOperationContext( null, attrs ) );
                    load();
                }
            }
        }
        catch ( NamingException e )
        {
            // @todo display popup with error here!
            e.printStackTrace();
        }
        catch ( FileNotFoundException e )
        {
            // @todo display popup with error here!
            e.printStackTrace();
        }
        catch ( Exception e )
        {
            // @todo display popup with error here!
            e.printStackTrace();
        }
    }


    /**
     * Exit the Application
     */
    private void exitForm() throws Exception
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
            if ( mode.equals( FilterDialog.RUN_MODE ) )
            {
                doRun( dialog.getFilter(), dialog.getScope(), dialog.getBase(), dialog.getLimit() );
            }
            else if ( mode.equals( FilterDialog.DEBUG_MODE ) )
            {
                doDebug( dialog.getFilter(), dialog.getScope(), dialog.getBase(), dialog.getLimit() );
            }
            else if ( mode.equals( FilterDialog.ANNOTATE_MODE ) )
            {
                if ( !doAnnotate( dialog.getFilter() ) )
                {
                    // We failed don't loose users filter buf
                    // allow user to make edits.
                    return;
                }

                LOG.debug( "call to annotate" );
            }
            else
            {
                throw new RuntimeException( I18n.err( I18n.ERR_730 ) );
            }
        }
        catch ( Exception e )
        {
            // @todo show error popup here!
            e.printStackTrace();
        }
    }


    public void doFilterDialog( final String mode ) throws Exception
    {
        final FilterDialog dialog = new FilterDialog( mode, this, true );

        if ( tree.getSelectionModel().getSelectionPath() != null )
        {
            dialog.setBase( getSelectedDn() );
        }
        else
        {
            dialog.setBase( partition.getSuffixDn().getName() );
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
                else if ( !cmd.equals( FilterDialog.CANCEL_CMD ) )
                {
                    throw new RuntimeException( I18n.err( I18n.ERR_731, cmd ) );
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
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "Search attempt using filter '" + filter + "' " + "with scope '" + scope
                + "' and a return limit of '" + limit + "'" );
        }

        ExprNode root;

        try
        {
            root = FilterParser.parse( schemaManager, filter );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            JTextArea text = new JTextArea();
            String msg = e.getLocalizedMessage();

            if ( msg.length() > 1024 )
            {
                msg = msg.substring( 0, 1024 ) + "\n. . . truncated . . .";
            }

            text.setText( msg );
            text.setEnabled( false );
            JOptionPane.showMessageDialog( null, text, "Syntax Error", JOptionPane.ERROR_MESSAGE );
            return false;
        }

        SearchScope searchScope = null;

        if ( scope.equals( FilterDialog.BASE_SCOPE ) )
        {
            searchScope = SearchScope.OBJECT;
        }
        else if ( scope.equals( FilterDialog.SINGLE_SCOPE ) )
        {
            searchScope = SearchScope.ONELEVEL;
        }
        else if ( scope.equals( FilterDialog.SUBTREE_SCOPE ) )
        {
            searchScope = SearchScope.SUBTREE;
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

        SearchOperationContext searchContext = new SearchOperationContext( null );
        searchContext.setAliasDerefMode( AliasDerefMode.DEREF_ALWAYS );
        searchContext.setDn( new Dn( base ) );
        searchContext.setFilter( root );
        searchContext.setScope( searchScope );

        PartitionSearchResult searchResult = partition.getSearchEngine().computeResult( schemaManager, searchContext );

        Cursor cursor = searchResult.getResultSet();

        String[] cols = new String[2];
        cols[0] = "id";
        cols[1] = "dn";
        DefaultTableModel tableModel = new DefaultTableModel( cols, 0 );
        Object[] row = new Object[2];
        int count = 0;
        while ( cursor.next() && count < limitMax )
        {
            IndexEntry rec = ( IndexEntry ) cursor.get();
            row[0] = rec.getId();
            row[1] = partition.getEntryDn( ( String ) row[0] ).getNormName();
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
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "debug attempt using base '" + base + "' filter '" + filter + "' " + "with scope '" + scope
                + "' and a return limit of '" + limit + "'" );
        }

        LOG.warn( "NOT IMPLMENTED YET" );
    }


    public void selectTreeNode( Long id )
    {
        Stack<TreeNode> stack = new Stack<TreeNode>();
        Object[] comps;
        TreeNode parent = nodes.get( id );

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
        ExprNode root;

        try
        {
            root = FilterParser.parse( schemaManager, filter );
        }
        catch ( Exception e )
        {
            JTextArea text = new JTextArea();
            String msg = e.getLocalizedMessage();

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

        partition.getSearchEngine().getOptimizer().annotate( root );
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
        AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( idxAttr );

        Index index;
        boolean isSystem = partition.hasSystemIndexOn( attributeType );

        if ( isSystem )
        {
            index = partition.getSystemIndex( attributeType );
        }
        else
        {
            index = partition.getUserIndex( attributeType );
        }

        if ( index != null )
        {
            IndexDialog dialog = new IndexDialog( this, false, index );
            centerOnScreen( dialog );
            dialog.setEnabled( true );
            dialog.setVisible( true );
        }
    }


    public void buildIndicesMenu( AbstractBTreePartition partition )
    {
        JMenuItem item;

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
            item = new JMenuItem();
            item.setBackground( new java.awt.Color( 205, 205, 205 ) );
            indices.add( item );
            item.setText( idx );
            item.setActionCommand( idx );
            item.addActionListener( listener );
        }

        indices.add( new JSeparator() );
        list = partition.getUserIndices();
        while ( list.hasNext() )
        {
            String idx = ( String ) list.next();
            item = new JMenuItem();
            item.setBackground( new java.awt.Color( 205, 205, 205 ) );
            indices.add( item );
            item.setText( idx );
            item.setActionCommand( idx );
            item.addActionListener( listener );
        }
    }


    void displayEntry( String id, Entry entry ) throws Exception
    {
        String dn = partition.getEntryDn( id ).getName();
        AttributesTableModel model = new AttributesTableModel( entry, id, dn, false );
        entryTbl.setModel( model );

        // TODO use utility method to getIndices below
        //        model = new AttributesTableModel( partition.getIndices( id ), id, dn, false );
        //        idxTbl.setModel( model );
        //
        //        validate();
    }


    private void load() throws Exception
    {
        // boolean doFiltered = false;
        nodes = new HashMap<String, EntryNode>();

        Entry suffix = partition.lookup( partition.getEntryId( partition.getSuffixDn() ) );
        String id = partition.getEntryId( partition.getSuffixDn() );
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

         dialog.setBase(database.getSuffixDn().toString());
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


    public void setDoCleanUp( boolean doCleanUp )
    {
        this.doCleanUp = doCleanUp;
    }
}
