/*
 *   Copyright 2006 The Apache Software Foundation
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
package org.apache.directory.server.ldap.gui;


import java.net.InetSocketAddress;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.apache.mina.common.IoSession;


public class SessionsModel implements TableModel
{
    final String[] columns = new String[]
        { "client address", "client port", "server address", "server port" };
    final Class[] columnClasses = new Class[]
        { String.class, Integer.class, String.class, Integer.class };
    final IoSession[] sessions;


    SessionsModel(IoSession[] sessions)
    {
        this.sessions = sessions;
    }


    IoSession getIoSession( int row )
    {
        return sessions[row];
    }


    public int getRowCount()
    {
        return sessions.length;
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
        IoSession session = sessions[rowIndex];

        switch ( columnIndex )
        {
            case ( 0 ):
                return ( ( InetSocketAddress ) session.getRemoteAddress() ).getHostName();
            case ( 1 ):
                return new Integer( ( ( InetSocketAddress ) session.getRemoteAddress() ).getPort() );
            case ( 2 ):
                return ( ( InetSocketAddress ) session.getLocalAddress() ).getHostName();
            case ( 3 ):
                return new Integer( ( ( InetSocketAddress ) session.getLocalAddress() ).getPort() );
            default:
                throw new IndexOutOfBoundsException( "column index max is " + ( columns.length - 1 ) );
        }
    }


    public void setValueAt( Object aValue, int rowIndex, int columnIndex )
    {
        throw new UnsupportedOperationException();
    }


    public void addTableModelListener( TableModelListener l )
    {
    }


    public void removeTableModelListener( TableModelListener l )
    {
    }
}
