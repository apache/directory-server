package org.apache.directory.server.ldap.gui;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JButton;
import javax.swing.BoxLayout;
import javax.swing.JTextField;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.directory.server.ldap.SessionRegistry;
import org.apache.directory.server.ldap.support.extended.GracefulShutdownHandler;
import org.apache.directory.shared.ldap.message.extended.GracefulDisconnect;
import org.apache.directory.shared.ldap.message.extended.NoticeOfDisconnect;
import org.apache.mina.common.CloseFuture;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SessionsFrame extends JFrame
{
    private static final Logger log = LoggerFactory.getLogger( SessionsFrame.class );
    private static final long serialVersionUID = -863445561454536133L;
    private static final String REFRESH_COMMAND = "Refresh";

    boolean isServiceBound = true;
    private IoSession requestor;
    private IoHandler ldapProvider;
    private JPanel jContentPane = null;
    private JPanel mainPanel = null;
    private JScrollPane sessionsPane = null;
    private JTable sessionsTable = null;
    private JPanel filterPanel = null;
    private JButton filterButton = null;
    private JTextField filterText = null;
    private JMenuBar menuBar = null;
    private JMenu menuFile = null;
    private JMenuItem exitItem = null;
    private JMenu menuSession = null;
    private JMenuItem closeItem = null;
    private JMenu menuSendNoD = null;
    private JMenuItem unavailableItem = null;
    private JMenuItem protocolErrorItem = null;
    private JMenuItem strongAuthRequiredItem = null;
    private JPanel southPanel = null;
    private JMenuItem showRequests = null;
    //    private JPopupMenu popupMenu = null;
    //    private JMenuItem jMenuItem = null;
    //    private JMenu jMenu = null;
    //    private JMenuItem jMenuItem1 = null;
    //    private JMenuItem jMenuItem2 = null;
    //    private JMenuItem jMenuItem3 = null;
    //    private JMenuItem jMenuItem4 = null;
    private JButton refreshButton = null;

    private IoSession selected;
    private JMenuItem unbindItem = null;
    private JMenuItem bindItem = null;

 
    /**
     * This is the default constructor
     */
    public SessionsFrame()
    {
        super();
        initialize();
    }


    /**
     * This method initializes this
     */
    private void initialize()
    {
        this.setSize( 789, 436 );
        this.setJMenuBar( getMainMenuBar() );
        this.setContentPane( getJContentPane() );
        this.setTitle( "Sessions" );
        this.addWindowListener( new java.awt.event.WindowAdapter()
        {
            public void windowClosing( java.awt.event.WindowEvent e )
            {
                SessionsFrame.this.setVisible( false );
                SessionsFrame.this.dispose();
            }
        } );
    }


    /**
     * This method initializes jContentPane
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJContentPane()
    {
        if ( jContentPane == null )
        {
            jContentPane = new JPanel();
            jContentPane.setLayout( new BorderLayout() );
            jContentPane.add( getMainPanel(), java.awt.BorderLayout.CENTER );
        }
        return jContentPane;
    }


    /**
     * This method initializes jPanel	
     * 	
     * @return javax.swing.JPanel	
     */
    private JPanel getMainPanel()
    {
        if ( mainPanel == null )
        {
            mainPanel = new JPanel();
            mainPanel.setLayout( new BorderLayout() );
            mainPanel.add( getFilterPanel(), java.awt.BorderLayout.NORTH );
            mainPanel.add( getSessionsPane(), java.awt.BorderLayout.CENTER );
            mainPanel.add( getSouthPanel(), java.awt.BorderLayout.SOUTH );
        }
        return mainPanel;
    }


    /**
     * This method initializes jScrollPane	
     * 	
     * @return javax.swing.JScrollPane	
     */
    private JScrollPane getSessionsPane()
    {
        if ( sessionsPane == null )
        {
            sessionsPane = new JScrollPane();
            sessionsPane.setName( "jScrollPane" );
            sessionsPane.setViewportView( getSessionsTable() );
        }
        return sessionsPane;
    }


    /**
     * This method initializes jTable	
     * 	
     * @return javax.swing.JTable	
     */
    private JTable getSessionsTable()
    {
        if ( sessionsTable == null )
        {
            sessionsTable = new JTable();
            sessionsTable.setSelectionMode( javax.swing.ListSelectionModel.SINGLE_SELECTION );
            //            sessionsTable.addMouseListener( new java.awt.event.MouseAdapter()
            //            {
            //                public void mouseReleased(java.awt.event.MouseEvent e)
            //                {
            //                    if ( e.getButton() == MouseEvent.BUTTON3 )
            //                    {
            //                        if ( popupMenu == null )
            //                        {
            //                            popupMenu = SessionsFrame.this.getSessionsPopupMenu();
            //                        }
            //                        popupMenu.setVisible( false );
            //                    }
            //                }
            //
            //                public void mousePressed(java.awt.event.MouseEvent e)
            //                {
            //                    if ( e.getButton() == MouseEvent.BUTTON3 )
            //                    {
            //                        if ( popupMenu == null )
            //                        {
            //                            popupMenu = SessionsFrame.this.getSessionsPopupMenu();
            //                        }
            //                        Point location = e.getComponent().getLocationOnScreen();
            //                        popupMenu.setLocation( location.x + e.getPoint().x, location.y + e.getPoint().y );
            //                        popupMenu.setVisible( true );
            //                    }
            //                }
            //            } );
            sessionsTable.setModel( new SessionsModel( SessionRegistry.getSingleton().getSessions() ) );
            sessionsTable.getSelectionModel().addListSelectionListener( new ListSelectionListener()
            {
                public void valueChanged( ListSelectionEvent e )
                {
                    int row = sessionsTable.getSelectedRow();
                    if ( row == -1 )
                    {
                        selected = null;
                    }
                    else
                    {
                        selected = ( ( SessionsModel ) sessionsTable.getModel() ).getIoSession( row );
                        closeItem.setEnabled( true );
                        menuSendNoD.setEnabled( true );
                        showRequests.setEnabled( true );
                    }
                }
            } );
        }
        return sessionsTable;
    }


    /**
     * This method initializes jPanel	
     * 	
     * @return javax.swing.JPanel	
     */
    private JPanel getFilterPanel()
    {
        if ( filterPanel == null )
        {
            filterPanel = new JPanel();
            filterPanel.setLayout( new BoxLayout( getFilterPanel(), BoxLayout.X_AXIS ) );
            filterPanel.setBorder( javax.swing.BorderFactory
                .createEtchedBorder( javax.swing.border.EtchedBorder.RAISED ) );
            filterPanel.add( getFilterButton(), null );
            filterPanel.add( getFilterText(), null );
        }
        return filterPanel;
    }


    /**
     * This method initializes jButton	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getFilterButton()
    {
        if ( filterButton == null )
        {
            filterButton = new JButton();
            filterButton.setText( "Filter" );
        }
        return filterButton;
    }


    /**
     * This method initializes jTextField	
     * 	
     * @return javax.swing.JTextField	
     */
    private JTextField getFilterText()
    {
        if ( filterText == null )
        {
            filterText = new JTextField();
        }
        return filterText;
    }


    /**
     * This method initializes jJMenuBar	
     * 	
     * @return javax.swing.JMenuBar	
     */
    private JMenuBar getMainMenuBar()
    {
        if ( menuBar == null )
        {
            menuBar = new JMenuBar();
            menuBar.add( getMenuFile() );
            menuBar.add( getMenuSession() );
        }
        return menuBar;
    }


    /**
     * This method initializes jMenu	
     * 	
     * @return javax.swing.JMenu	
     */
    private JMenu getMenuFile()
    {
        if ( menuFile == null )
        {
            menuFile = new JMenu();
            menuFile.setText( "File" );
            menuFile.add( getExitItem() );
        }
        return menuFile;
    }


    /**
     * This method initializes jMenuItem	
     * 	
     * @return javax.swing.JMenuItem	
     */
    private JMenuItem getExitItem()
    {
        if ( exitItem == null )
        {
            exitItem = new JMenuItem();
            exitItem.setText( "exit" );
            exitItem.addActionListener( new java.awt.event.ActionListener()
            {
                public void actionPerformed( java.awt.event.ActionEvent e )
                {
                    SessionsFrame.this.setVisible( false );
                    SessionsFrame.this.dispose();
                }
            } );
        }
        return exitItem;
    }


    /**
     * This method initializes jMenu	
     * 	
     * @return javax.swing.JMenu	
     */
    private JMenu getMenuSession()
    {
        if ( menuSession == null )
        {
            menuSession = new JMenu();
            menuSession.setText( "Session" );
            menuSession.add( getCloseItem() );
            closeItem.setEnabled( false );
            menuSession.add( getMenuSendNoD() );
            menuSendNoD.setEnabled( false );
            menuSession.add( getShowRequests() );
            menuSession.add( getUnbindItem() );
            menuSession.add( getBindItem() );
            showRequests.setEnabled( false );
        }
        return menuSession;
    }


    /**
     * This method initializes jMenuItem	
     * 	
     * @return javax.swing.JMenuItem	
     */
    private JMenuItem getCloseItem()
    {
        if ( closeItem == null )
        {
            closeItem = new JMenuItem();
            closeItem.setText( "close" );
            closeItem.addActionListener( new java.awt.event.ActionListener()
            {
                public void actionPerformed( java.awt.event.ActionEvent e )
                {
                    SessionRegistry.getSingleton().terminateSession( selected );
                    try
                    {
                        Thread.sleep( 250 );
                    }
                    catch ( InterruptedException e1 )
                    {
                        log.error( "", e1 );
                    }
                    refresh();
                }
            } );
        }
        return closeItem;
    }


    /**
     * This method initializes jMenu	
     * 	
     * @return javax.swing.JMenu	
     */
    private JMenu getMenuSendNoD()
    {
        if ( menuSendNoD == null )
        {
            menuSendNoD = new JMenu();
            menuSendNoD.setText( "Send NoD" );
            menuSendNoD.add( getUnavailableItem() );
            menuSendNoD.add( getProtocolErrorItem() );
            menuSendNoD.add( getStrongAuthRequiredItem() );
        }
        return menuSendNoD;
    }


    /**
     * This method initializes jMenuItem	
     * 	
     * @return javax.swing.JMenuItem	
     */
    private JMenuItem getUnavailableItem()
    {
        if ( unavailableItem == null )
        {
            unavailableItem = new JMenuItem();
            unavailableItem.setText( "unavailable" );
            unavailableItem.addActionListener( new java.awt.event.ActionListener()
            {
                public void actionPerformed( java.awt.event.ActionEvent e )
                {
                    selected.write( NoticeOfDisconnect.UNAVAILABLE );
                    try
                    {
                        Thread.sleep( 250 );
                    }
                    catch ( InterruptedException e1 )
                    {
                        log.error( "", e1 );
                    }
                    refresh();
                }
            } );
        }
        return unavailableItem;
    }


    /**
     * This method initializes jMenuItem	
     * 	
     * @return javax.swing.JMenuItem	
     */
    private JMenuItem getProtocolErrorItem()
    {
        if ( protocolErrorItem == null )
        {
            protocolErrorItem = new JMenuItem();
            protocolErrorItem.setText( "protocolError" );
            protocolErrorItem.addActionListener( new java.awt.event.ActionListener()
            {
                public void actionPerformed( java.awt.event.ActionEvent e )
                {
                    selected.write( NoticeOfDisconnect.PROTOCOLERROR );
                    try
                    {
                        Thread.sleep( 250 );
                    }
                    catch ( InterruptedException e1 )
                    {
                        log.error( "", e1 );
                    }
                    refresh();
                }
            } );
        }
        return protocolErrorItem;
    }


    /**
     * This method initializes jMenuItem	
     * 	
     * @return javax.swing.JMenuItem	
     */
    private JMenuItem getStrongAuthRequiredItem()
    {
        if ( strongAuthRequiredItem == null )
        {
            strongAuthRequiredItem = new JMenuItem();
            strongAuthRequiredItem.setText( "strongAuthRequired" );
            strongAuthRequiredItem.addActionListener( new java.awt.event.ActionListener()
            {
                public void actionPerformed( java.awt.event.ActionEvent e )
                {
                    WriteFuture future = selected.write( NoticeOfDisconnect.STRONGAUTHREQUIRED );
                    try
                    {
                        future.join( 1000 );
                        CloseFuture cfuture = selected.close();
                        cfuture.join( 1000 );
                    }
                    catch ( Exception e1 )
                    {
                        log.error( "", e1 );
                    }
                    refresh();
                }
            } );
        }
        return strongAuthRequiredItem;
    }


    //    /**
    //     * This method initializes jPopupMenu	
    //     * 	
    //     * @return javax.swing.JPopupMenu	
    //     */
    //    private JPopupMenu getSessionsPopupMenu()
    //    {
    //        if ( popupMenu == null )
    //        {
    //            popupMenu = new JPopupMenu();
    //            popupMenu.add(getJMenuItem());
    //            popupMenu.add(getJMenu());
    //            popupMenu.add(getJMenuItem4());
    //        }
    //        return popupMenu;
    //    }

    /**
     * This method initializes jPanel	
     * 	
     * @return javax.swing.JPanel	
     */
    private JPanel getSouthPanel()
    {
        if ( southPanel == null )
        {
            southPanel = new JPanel();
            southPanel
                .setBorder( javax.swing.BorderFactory.createEtchedBorder( javax.swing.border.EtchedBorder.RAISED ) );
            southPanel.add( getRefreshButton(), null );
        }
        return southPanel;
    }


    /**
     * This method initializes jMenuItem	
     * 	
     * @return javax.swing.JMenuItem	
     */
    private JMenuItem getShowRequests()
    {
        if ( showRequests == null )
        {
            showRequests = new JMenuItem();
            showRequests.setText( "show requests" );
            showRequests.addActionListener( new java.awt.event.ActionListener()
            {
                public void actionPerformed( java.awt.event.ActionEvent e )
                {
                    OutstandingRequestsDialog dialog = new OutstandingRequestsDialog( SessionsFrame.this, selected );
                    dialog.addWindowListener( new WindowAdapter()
                    {
                        public void windowClosed( WindowEvent e )
                        {
                            e.getWindow().dispose();
                        }
                    } );
                    dialog.setVisible( true );
                }
            } );
        }
        return showRequests;
    }


    //    /**
    //     * This method initializes jMenuItem	
    //     * 	
    //     * @return javax.swing.JMenuItem	
    //     */
    //    private JMenuItem getJMenuItem()
    //    {
    //        if ( jMenuItem == null )
    //        {
    //            jMenuItem = new JMenuItem();
    //            jMenuItem.setText("close");
    //        }
    //        return jMenuItem;
    //    }
    //
    //
    //    /**
    //     * This method initializes jMenu	
    //     * 	
    //     * @return javax.swing.JMenu	
    //     */
    //    private JMenu getJMenu()
    //    {
    //        if ( jMenu == null )
    //        {
    //            jMenu = new JMenu();
    //            jMenu.setText("Send NoD");
    //            jMenu.add(getJMenuItem1());
    //            jMenu.add(getJMenuItem2());
    //            jMenu.add(getJMenuItem3());
    //        }
    //        return jMenu;
    //    }
    //
    //
    //    /**
    //     * This method initializes jMenuItem1	
    //     * 	
    //     * @return javax.swing.JMenuItem	
    //     */
    //    private JMenuItem getJMenuItem1()
    //    {
    //        if ( jMenuItem1 == null )
    //        {
    //            jMenuItem1 = new JMenuItem();
    //            jMenuItem1.setText("unavailable");
    //        }
    //        return jMenuItem1;
    //    }
    //
    //
    //    /**
    //     * This method initializes jMenuItem2	
    //     * 	
    //     * @return javax.swing.JMenuItem	
    //     */
    //    private JMenuItem getJMenuItem2()
    //    {
    //        if ( jMenuItem2 == null )
    //        {
    //            jMenuItem2 = new JMenuItem();
    //            jMenuItem2.setText("protocolError");
    //        }
    //        return jMenuItem2;
    //    }
    //
    //
    //    /**
    //     * This method initializes jMenuItem3	
    //     * 	
    //     * @return javax.swing.JMenuItem	
    //     */
    //    private JMenuItem getJMenuItem3()
    //    {
    //        if ( jMenuItem3 == null )
    //        {
    //            jMenuItem3 = new JMenuItem();
    //            jMenuItem3.setText("strongAuthRequired");
    //        }
    //        return jMenuItem3;
    //    }
    //
    //
    //    /**
    //     * This method initializes jMenuItem4	
    //     * 	
    //     * @return javax.swing.JMenuItem	
    //     */
    //    private JMenuItem getJMenuItem4()
    //    {
    //        if ( jMenuItem4 == null )
    //        {
    //            jMenuItem4 = new JMenuItem();
    //            jMenuItem4.setText("show requests");
    //        }
    //        return jMenuItem4;
    //    }

    /**
     * This method initializes jButton2	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getRefreshButton()
    {
        if ( refreshButton == null )
        {
            refreshButton = new JButton();
            refreshButton.setText( REFRESH_COMMAND );
            refreshButton.addActionListener( new java.awt.event.ActionListener()
            {
                public void actionPerformed( java.awt.event.ActionEvent e )
                {
                    if ( e.getActionCommand() == REFRESH_COMMAND )
                    {
                        refresh();
                    }
                }
            } );
        }
        return refreshButton;
    }


    private void refresh()
    {
        log.info( "Refreshing Sessions UI" );
        sessionsTable.setModel( new SessionsModel( SessionRegistry.getSingleton().getSessions() ) );
        closeItem.setEnabled( false );
        menuSendNoD.setEnabled( false );
        showRequests.setEnabled( false );
        unbindItem.setEnabled( isServiceBound );
        bindItem.setEnabled( !isServiceBound );
    }

    public void setRequestor( IoSession requestor )
    {
        this.requestor = requestor;
    }

    /**
     * This method initializes jMenuItem	
     * 	
     * @return javax.swing.JMenuItem	
     */
    private JMenuItem getUnbindItem()
    {
        if ( unbindItem == null )
        {
            unbindItem = new JMenuItem();
            unbindItem.setText( "Unbind Service" );
            unbindItem.setEnabled( isServiceBound );
            unbindItem.addActionListener( new java.awt.event.ActionListener()
            {
                public void actionPerformed( java.awt.event.ActionEvent e )
                {
                    int input = JOptionPane.showConfirmDialog( SessionsFrame.this,
                        "Selecting no will send a notice of disconnect ONLY.  "
                            + "\nSelecting yes will send both.  Cancel will abort unbind.",
                        "Send graceful disconnect before disconnect notice?", JOptionPane.YES_NO_CANCEL_OPTION );
                    IoAcceptor acceptor = ( IoAcceptor ) requestor.getService();
                    List sessions = new ArrayList( acceptor.getManagedSessions( requestor.getServiceAddress() ) );
                    //                    ServerLdapContext ctx;
                    //                    try
                    //                    {
                    //                        ctx = ( ServerLdapContext ) SessionRegistry.getSingleton()
                    //                            .getLdapContext( requestor, null, false );
                    //                    }
                    //                    catch ( NamingException ne )
                    //                    {
                    //                        JOptionPane.showInternalMessageDialog( SessionsFrame.this, 
                    //                            ne.getMessage(), "Encountered an Error", JOptionPane.ERROR_MESSAGE );
                    //                        log.warn( "Could not access requestor's context.", ne );
                    //                        return;
                    //                    }
                    //                    DirectoryService service = ctx.getService();
                    //                    StartupConfiguration cfg = service.getConfiguration().getStartupConfiguration();
                    // might add an exit vm feature using the default from the 
                    // configuration property

                    if ( input == JOptionPane.CANCEL_OPTION )
                    {
                        return;
                    }
                    else if ( input == JOptionPane.NO_OPTION )
                    {
                        GracefulShutdownHandler.sendNoticeOfDisconnect( sessions, requestor );
                        acceptor.unbind( requestor.getServiceAddress() );
                        isServiceBound = false;
                        unbindItem.setEnabled( isServiceBound );
                        bindItem.setEnabled( !isServiceBound );
                        JOptionPane.showMessageDialog( SessionsFrame.this, "Ldap service for "
                            + requestor.getLocalAddress() + " has been successfully unbound.", "Success!",
                            JOptionPane.INFORMATION_MESSAGE );
                        refresh();
                        return;
                    }
                    else
                    {
                        ShutdownDialog dialog = new ShutdownDialog();
                        setCenteredPosition( SessionsFrame.this, dialog );
                        dialog.setModal( true );
                        dialog.setVisible( true );

                        if ( dialog.isSendCanceled() )
                        {
                            log.debug( "GracefulShutdown was canceled." );
                            JOptionPane.showMessageDialog( SessionsFrame.this, "Shutdown has been canceled.",
                                "Graceful Shutdown Aborted", JOptionPane.OK_OPTION );
                            return;
                        }

                        log.debug( "GracefulShutdown parameters captured." );
                        int timeOffline = dialog.getTimeOffline();
                        int delay = dialog.getDelay();
                        GracefulDisconnect graceful = new GracefulDisconnect( timeOffline, delay );
                        GracefulShutdownHandler.sendGracefulDisconnect( sessions, graceful, requestor );
                        acceptor.unbind( requestor.getServiceAddress() );
                        isServiceBound = false;
                        unbindItem.setEnabled( isServiceBound );
                        bindItem.setEnabled( !isServiceBound );

                        // do progress dialog with bypass button to wait for delay time
                        if ( delay > 0 )
                        {
                            ShutdownProgress progress = new ShutdownProgress();
                            setCenteredPosition( SessionsFrame.this, progress );
                            progress.setModal( true );
                            progress.setTime( delay * 1000 );
                            Thread t = new Thread( progress );
                            t.start();
                            progress.setVisible( true );
                        }

                        // now send the notice of disconnect
                        GracefulShutdownHandler.sendNoticeOfDisconnect( sessions, requestor );
                        JOptionPane.showMessageDialog( SessionsFrame.this, "Ldap service for "
                            + requestor.getLocalAddress() + " has been successfully unbound.", "Success!",
                            JOptionPane.OK_OPTION );
                        refresh();
                    }
                }
            } );
        }
        return unbindItem;
    }


    private void setCenteredPosition( JFrame frame, Component comp )
    {
        Point pt = new Point();
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenSize = tk.getScreenSize();
        pt.x = ( screenSize.width - frame.getWidth() ) / 2;
        pt.y = ( screenSize.height - frame.getHeight() ) / 2;

        pt.x += ( frame.getWidth() - comp.getWidth() ) / 2;
        pt.y += ( frame.getHeight() - comp.getHeight() ) / 2;
        comp.setLocation( pt );
    }


    /**
     * This method initializes jMenuItem	
     * 	
     * @return javax.swing.JMenuItem	
     */
    private JMenuItem getBindItem()
    {
        if ( bindItem == null )
        {
            bindItem = new JMenuItem();
            bindItem.setText( "Bind Service" );
            unbindItem.setEnabled( !isServiceBound );
            bindItem.addActionListener( new java.awt.event.ActionListener()
            {
                public void actionPerformed( java.awt.event.ActionEvent e )
                {
                    try
                    {
                        ( ( IoAcceptor ) requestor.getService() ).bind( requestor.getServiceAddress(), getLdapProvider() );
                        JOptionPane.showMessageDialog( SessionsFrame.this, "Ldap service " + requestor.getServiceAddress()
                            + " has been successfully bound.\n" + " Clients may now connect to the server once again.",
                            "Success!", JOptionPane.INFORMATION_MESSAGE );
                        isServiceBound = true;
                        unbindItem.setEnabled( isServiceBound );
                        bindItem.setEnabled( !isServiceBound );
                    }
                    catch ( IOException e1 )
                    {
                        log.error( "failed to rebind ldap service", e1 );
                        JOptionPane.showMessageDialog( SessionsFrame.this, e1.getMessage(), "Error encountered!",
                            JOptionPane.ERROR_MESSAGE );
                    }
                }
            } );
        }
        return bindItem;
    }


    public void setLdapProvider( IoHandler ldapProvider )
    {
        this.ldapProvider = ldapProvider;
    }


    public IoHandler getLdapProvider()
    {
        return ldapProvider;
    }
} //  @jve:decl-index=0:visual-constraint="10,10"
