package org.apache.ldap.server.protocol.gui;


import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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

import org.apache.ldap.common.message.ResultCodeEnum;
import org.apache.ldap.common.message.extended.NoticeOfDisconnect;
import org.apache.ldap.server.protocol.SessionRegistry;
import org.apache.mina.common.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class SessionsFrame extends JFrame
{
    private static final Logger log = LoggerFactory.getLogger( SessionsFrame.class );
    private static final long serialVersionUID = -863445561454536133L;
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
     * 
     * @return void
     */
    private void initialize()
    {
        this.setSize(789, 436);
        this.setJMenuBar( getMainMenuBar() );
        this.setContentPane( getJContentPane() );
        this.setTitle("Sessions");
        this.addWindowListener( new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent e)
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
            jContentPane.add(getMainPanel(), java.awt.BorderLayout.CENTER);
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
            mainPanel.setLayout(new BorderLayout());
            mainPanel.add(getFilterPanel(), java.awt.BorderLayout.NORTH);
            mainPanel.add(getSessionsPane(), java.awt.BorderLayout.CENTER);
            mainPanel.add(getSouthPanel(), java.awt.BorderLayout.SOUTH);
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
            sessionsPane.setName("jScrollPane");
            sessionsPane.setViewportView(getSessionsTable());
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
            sessionsTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
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
            sessionsTable.getSelectionModel().addListSelectionListener( new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e)
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
            });
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
            filterPanel.setLayout(new BoxLayout(getFilterPanel(), BoxLayout.X_AXIS));
            filterPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
            filterPanel.add(getFilterButton(), null);
            filterPanel.add(getFilterText(), null);
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
            filterButton.setText("Filter");
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
            menuBar.add(getMenuFile());
            menuBar.add(getMenuSession());
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
            menuFile.setText("File");
            menuFile.add(getExitItem());
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
            exitItem.setText("exit");
            exitItem.addActionListener( new java.awt.event.ActionListener()
            {
                public void actionPerformed(java.awt.event.ActionEvent e)
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
            menuSession.setText("Session");
            menuSession.add(getCloseItem());
            closeItem.setEnabled( false );
            menuSession.add(getMenuSendNoD());
            menuSendNoD.setEnabled( false );
            menuSession.add(getShowRequests());
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
            closeItem.setText("close");
            closeItem.addActionListener( new java.awt.event.ActionListener()
            {
                public void actionPerformed(java.awt.event.ActionEvent e)
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
            menuSendNoD.setText("Send NoD");
            menuSendNoD.add(getUnavailableItem());
            menuSendNoD.add(getProtocolErrorItem());
            menuSendNoD.add(getStrongAuthRequiredItem());
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
            unavailableItem.setText("unavailable");
            unavailableItem.addActionListener( new java.awt.event.ActionListener()
            {
                public void actionPerformed(java.awt.event.ActionEvent e)
                {
                    selected.write( new NoticeOfDisconnect( ResultCodeEnum.UNAVAILABLE ) ); 
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
            protocolErrorItem.setText("protocolError");
            protocolErrorItem.addActionListener( new java.awt.event.ActionListener()
            {
                public void actionPerformed(java.awt.event.ActionEvent e)
                {
                    selected.write( new NoticeOfDisconnect( ResultCodeEnum.PROTOCOLERROR ) ); 
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
            strongAuthRequiredItem.setText("strongAuthRequired");
            strongAuthRequiredItem.addActionListener( new java.awt.event.ActionListener()
            {
                public void actionPerformed(java.awt.event.ActionEvent e)
                {
                    selected.write( new NoticeOfDisconnect( ResultCodeEnum.STRONGAUTHREQUIRED ) ); 
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
            southPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
            southPanel.add(getRefreshButton(), null);
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
            showRequests.setText("show requests");
            showRequests.addActionListener( new java.awt.event.ActionListener()
            {
                public void actionPerformed(java.awt.event.ActionEvent e)
                {
                    OutstandingRequestsDialog dialog = new OutstandingRequestsDialog( SessionsFrame.this, selected );
                    dialog.addWindowListener( new WindowAdapter() {
                        public void windowClosed(WindowEvent e)
                        {
                            e.getWindow().dispose();
                        }
                    });
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


    private static final String REFRESH_COMMAND = "Refresh";
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
                public void actionPerformed(java.awt.event.ActionEvent e)
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
    }
}  //  @jve:decl-index=0:visual-constraint="10,10"
