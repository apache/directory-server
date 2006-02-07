package org.apache.ldap.server.protocol.gui;


import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.apache.ldap.common.message.AbandonableRequest;


public class OutstandingRequestsModel implements TableModel
{
    final String[] columns = new String[] { "messageId", "type" };
    final Class[] columnClasses = new Class[] { Integer.class, String.class };
    final AbandonableRequest[] requests;

    
    OutstandingRequestsModel( AbandonableRequest[] requests )
    {
        this.requests = requests;
    }

    AbandonableRequest getAbandonableRequest( int row )
    {
        return requests[row];
    }
    
    public int getRowCount()
    {
        return requests.length;
    }

    public int getColumnCount()
    {
        return columns.length;
    }

    public String getColumnName( int columnIndex )
    {
        return columns[columnIndex];
    }

    public Class getColumnClass( int columnIndex )
    {
        return columnClasses[columnIndex];
    }

    public boolean isCellEditable( int rowIndex, int columnIndex )
    {
        return false;
    }

    public Object getValueAt( int rowIndex, int columnIndex )
    {
        AbandonableRequest req = requests[rowIndex];
        
        switch( columnIndex )
        {
            case( 0 ):
                return new Integer( req.getMessageId() );
            case( 1 ):
                return req.getType().toString();
            default:
                throw new IndexOutOfBoundsException( "column index max is " + ( columns.length - 1 ) );
        }
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
        throw new UnsupportedOperationException();
    }

    public void addTableModelListener(TableModelListener l)
    {
    }

    public void removeTableModelListener(TableModelListener l)
    {
    }
}
