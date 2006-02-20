package org.apache.directory.server.core.partition.impl.btree.gui;


import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Pattern;

import javax.naming.NamingEnumeration;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import org.apache.directory.server.core.partition.impl.btree.Index;
import org.apache.directory.server.core.partition.impl.btree.IndexRecord;
import org.apache.directory.shared.ldap.util.ExceptionUtils;
import org.apache.directory.shared.ldap.util.StringTools;
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
    private static final Logger log = LoggerFactory.getLogger( IndexDialog.class );

    private static final long serialVersionUID = 3689917253680445238L;

    public static final String DEFAULT_CURSOR = "Default";
    public static final String EQUALITY_CURSOR = "Equality";
    public static final String GREATER_CURSOR = "Greater";
    public static final String LESS_CURSOR = "Less";
    public static final String REGEX_CURSOR = "Regex";

    private Panel mainPnl = new Panel();
    private JTabbedPane tabbedPane = new JTabbedPane();
    private JPanel listPnl = new JPanel();
    private JPanel cursorPnl = new JPanel();
    private JPanel resultsPnl = new JPanel();
    private JScrollPane jScrollPane2 = new JScrollPane();
    private JTable resultsTbl = new JTable();
    private JPanel buttonPnl = new JPanel();
    private JButton doneBut = new JButton();
    private JLabel jLabel1 = new JLabel();
    private JTextField keyText = new JTextField();
    private JLabel jLabel2 = new JLabel();
    private JButton scanBut = new JButton();

    private Index index = null;


    /** Creates new form JDialog */
    public IndexDialog(Frame parent, boolean modal, Index index)
    {
        super( parent, modal );
        this.index = index;
        initGUI();
    }


    /**
     * This method is called from within the constructor to initialize the
     * form.
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
        setTitle( "Index On Attribute '" + index.getAttribute().getName() + "'" );
        setBounds( new java.awt.Rectangle( 0, 0, 512, 471 ) );
        getContentPane().add( mainPnl, java.awt.BorderLayout.CENTER );
        mainPnl.setLayout( new java.awt.BorderLayout() );
        mainPnl.add( tabbedPane, java.awt.BorderLayout.CENTER );
        tabbedPane.add( listPnl, "Listing" );
        listPnl.setLayout( new java.awt.GridBagLayout() );

        RadioButtonListener radioListener = new RadioButtonListener();
        JRadioButton radioDefault = new JRadioButton( DEFAULT_CURSOR );
        radioDefault.setActionCommand( DEFAULT_CURSOR );
        radioDefault.setSelected( true );
        radioDefault.addActionListener( radioListener );

        JRadioButton radioEquality = new JRadioButton( EQUALITY_CURSOR );
        radioEquality.setActionCommand( EQUALITY_CURSOR );
        radioEquality.addActionListener( radioListener );

        JRadioButton radioGreater = new JRadioButton( GREATER_CURSOR );
        radioGreater.setActionCommand( GREATER_CURSOR );
        radioGreater.addActionListener( radioListener );

        JRadioButton radioLess = new JRadioButton( LESS_CURSOR );
        radioLess.setActionCommand( LESS_CURSOR );
        radioLess.addActionListener( radioListener );

        JRadioButton radioRegex = new JRadioButton( REGEX_CURSOR );
        radioRegex.setActionCommand( REGEX_CURSOR );
        radioRegex.addActionListener( radioListener );

        ButtonGroup group = new ButtonGroup();
        group.add( radioDefault );
        group.add( radioEquality );
        group.add( radioGreater );
        group.add( radioLess );
        group.add( radioRegex );

        JPanel radioPanel = new JPanel();
        radioPanel.setLayout( new BoxLayout( radioPanel, BoxLayout.X_AXIS ) );
        radioPanel.add( radioDefault );
        radioPanel.add( radioEquality );
        radioPanel.add( radioGreater );
        radioPanel.add( radioLess );
        radioPanel.add( radioRegex );

        listPnl.add( cursorPnl, new java.awt.GridBagConstraints( 0, 0, 1, 1, 1.0, 0.15,
            java.awt.GridBagConstraints.NORTH, java.awt.GridBagConstraints.BOTH, new java.awt.Insets( 15, 0, 30, 0 ),
            0, 0 ) );
        listPnl.add( resultsPnl, new java.awt.GridBagConstraints( 0, 1, 1, 1, 1.0, 0.8,
            java.awt.GridBagConstraints.CENTER, java.awt.GridBagConstraints.BOTH, new java.awt.Insets( 0, 0, 0, 0 ), 0,
            0 ) );
        listPnl.add( buttonPnl, new java.awt.GridBagConstraints( 0, 2, 1, 1, 1.0, 0.05,
            java.awt.GridBagConstraints.CENTER, java.awt.GridBagConstraints.BOTH, new java.awt.Insets( 0, 0, 0, 0 ), 0,
            0 ) );
        cursorPnl.setLayout( new java.awt.GridBagLayout() );
        cursorPnl.setBorder( javax.swing.BorderFactory.createTitledBorder( javax.swing.BorderFactory.createLineBorder(
            new java.awt.Color( 153, 153, 153 ), 1 ), "Display Cursor Constraints",
            javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP, new java.awt.Font(
                "SansSerif", 0, 14 ), new java.awt.Color( 60, 60, 60 ) ) );
        cursorPnl.add( jLabel1, new java.awt.GridBagConstraints( 0, 1, 1, 1, 0.0, 0.0,
            java.awt.GridBagConstraints.WEST, java.awt.GridBagConstraints.NONE, new java.awt.Insets( 0, 15, 0, 10 ), 0,
            0 ) );
        cursorPnl.add( keyText, new java.awt.GridBagConstraints( 1, 1, 1, 1, 0.4, 0.0,
            java.awt.GridBagConstraints.WEST, java.awt.GridBagConstraints.BOTH, new java.awt.Insets( 5, 5, 5, 236 ), 0,
            0 ) );
        cursorPnl.add( jLabel2, new java.awt.GridBagConstraints( 0, 0, 1, 1, 0.0, 0.0,
            java.awt.GridBagConstraints.WEST, java.awt.GridBagConstraints.NONE, new java.awt.Insets( 0, 15, 0, 10 ), 0,
            0 ) );
        cursorPnl.add( radioPanel,
            new java.awt.GridBagConstraints( 1, 0, 1, 1, 0.4, 0.0, java.awt.GridBagConstraints.WEST,
                java.awt.GridBagConstraints.NONE, new java.awt.Insets( 5, 5, 5, 0 ), 0, 0 ) );
        resultsPnl.setLayout( new java.awt.BorderLayout() );
        resultsPnl.setBorder( javax.swing.BorderFactory.createTitledBorder( javax.swing.BorderFactory.createLineBorder(
            new java.awt.Color( 153, 153, 153 ), 1 ), "Scan Results", javax.swing.border.TitledBorder.LEADING,
            javax.swing.border.TitledBorder.TOP, new java.awt.Font( "SansSerif", 0, 14 ), new java.awt.Color( 60, 60,
                60 ) ) );
        resultsPnl.add( jScrollPane2, java.awt.BorderLayout.CENTER );
        jScrollPane2.getViewport().add( resultsTbl );
        buttonPnl.setLayout( new java.awt.FlowLayout( java.awt.FlowLayout.CENTER, 15, 5 ) );
        buttonPnl.add( doneBut );
        buttonPnl.add( scanBut );
        doneBut.setText( "Done" );
        doneBut.addActionListener( new ActionListener()
        {
            public void actionPerformed( ActionEvent e )
            {
                closeDialog();
            }
        } );

        jLabel1.setText( "Key Constraint:" );
        keyText.setText( "" );
        keyText.setMinimumSize( new java.awt.Dimension( 130, 20 ) );
        keyText.setPreferredSize( new java.awt.Dimension( 130, 20 ) );
        keyText.setMaximumSize( new java.awt.Dimension( 130, 20 ) );
        keyText.setFont( new java.awt.Font( "SansSerif", java.awt.Font.PLAIN, 14 ) );
        keyText.setSize( new java.awt.Dimension( 130, 20 ) );
        jLabel2.setText( "Cursor Type:" );

        scanBut.setText( "Scan" );
        scanBut.addActionListener( new ActionListener()
        {
            public void actionPerformed( ActionEvent e )
            {
                doScan( keyText.getText(), selectedCursorType );
            }
        } );

        doScan( null, DEFAULT_CURSOR );
    }

    private String selectedCursorType = DEFAULT_CURSOR;

    class RadioButtonListener implements ActionListener
    {
        public void actionPerformed( ActionEvent e )
        {
            if ( e.getActionCommand() == DEFAULT_CURSOR )
            {
                selectedCursorType = DEFAULT_CURSOR;
            }
            else if ( e.getActionCommand() == EQUALITY_CURSOR )
            {
                selectedCursorType = EQUALITY_CURSOR;
            }
            else if ( e.getActionCommand() == GREATER_CURSOR )
            {
                selectedCursorType = GREATER_CURSOR;
            }
            else if ( e.getActionCommand() == LESS_CURSOR )
            {
                selectedCursorType = LESS_CURSOR;
            }
            else if ( e.getActionCommand() == REGEX_CURSOR )
            {
                selectedCursorType = REGEX_CURSOR;
            }
        }
    }


    private void closeDialog()
    {
        setVisible( false );
        dispose();
    }


    public boolean doScan( String key, String scanType )
    {
        if ( key == null || key.trim().equals( "" ) )
        {
            key = null;
        }

        if ( key == null && scanType != DEFAULT_CURSOR )
        {
            JOptionPane.showMessageDialog( null, "Cannot use a " + scanType + " scan type with a null key constraint.",
                "Missing Key Constraint", JOptionPane.ERROR_MESSAGE );
            return false;
        }

        try
        {
            NamingEnumeration list = null;

            if ( scanType == EQUALITY_CURSOR )
            {
                list = index.listIndices( key );
            }
            else if ( scanType == GREATER_CURSOR )
            {
                list = index.listIndices( key, true );
            }
            else if ( scanType == LESS_CURSOR )
            {
                list = index.listIndices( key, false );
            }
            else if ( scanType == REGEX_CURSOR )
            {
                Pattern regex = StringTools.getRegex( key );
                int starIndex = key.indexOf( '*' );

                if ( starIndex > 0 )
                {
                    String prefix = key.substring( 0, starIndex );

                    if ( log.isDebugEnabled() )
                        log.debug( "Regex prefix = " + prefix );

                    list = index.listIndices( regex, prefix );
                }
                else
                {
                    list = index.listIndices( regex );
                }
            }
            else
            {
                list = index.listIndices();
            }

            Object[] cols = new Object[2];
            Object[] row = null;
            cols[0] = "Keys ( Attribute Value )";
            cols[1] = "Values ( Entry Id )";
            DefaultTableModel model = new DefaultTableModel( cols, 0 );
            int count = 0;

            while ( list.hasMore() )
            {
                IndexRecord rec = ( IndexRecord ) list.next();
                row = new Object[2];
                row[0] = rec.getIndexKey();
                row[1] = rec.getEntryId();
                model.addRow( row );
                count++;
            }

            resultsTbl.setModel( model );
            resultsPnl.setBorder( BorderFactory.createTitledBorder( BorderFactory.createLineBorder( new Color( 153,
                153, 153 ), 1 ), "Scan Results: " + count, TitledBorder.LEADING, TitledBorder.TOP, new Font(
                "SansSerif", 0, 14 ), new Color( 60, 60, 60 ) ) );

            if ( isVisible() )
            {
                validate();
            }
        }
        catch ( Exception e )
        {
            String msg = ExceptionUtils.getStackTrace( e );

            if ( msg.length() > 1024 )
            {
                msg = msg.substring( 0, 1024 ) + "\n. . . TRUNCATED . . .";
            }

            msg = "Error while scanning index " + "on attribute " + index.getAttribute() + " using a " + scanType
                + " cursor type with a key constraint of '" + key + "':\n" + msg;

            JTextArea area = new JTextArea();
            area.setText( msg );
            JOptionPane.showMessageDialog( null, area, "Index Scan Error", JOptionPane.ERROR_MESSAGE );
            return false;
        }

        return true;
    }
}