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
package org.apache.directory.server.ldap.gui;


import java.net.InetSocketAddress;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapSession;


public class SessionsModel implements TableModel
{
    final String[] columns = new String[]
        { "client address", "client port", "server address", "server port" };
    final Class<?>[] columnClasses = new Class[]
        { String.class, Integer.class, String.class, Integer.class };
    final LdapSession[] sessions;


    SessionsModel( LdapSession[] sessions )
    {
        this.sessions = sessions;
    }


    LdapSession getLdapSession( int row )
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


    public Class<?> getColumnClass( int columnIndex )
    {
        return columnClasses[columnIndex];
    }


    public boolean isCellEditable( int rowIndex, int columnIndex )
    {
        return false;
    }


    public Object getValueAt( int rowIndex, int columnIndex )
    {
        LdapSession session = sessions[rowIndex];

        switch ( columnIndex )
        {
            case ( 0 ):
                return ( ( InetSocketAddress ) session.getIoSession().getRemoteAddress() ).getHostName();
            case ( 1 ):
                return Integer.valueOf( ( ( InetSocketAddress ) session.getIoSession().getRemoteAddress() ).getPort() );
            case ( 2 ):
                return ( ( InetSocketAddress ) session.getIoSession().getLocalAddress() ).getHostName();
            case ( 3 ):
                return Integer.valueOf( ( ( InetSocketAddress ) session.getIoSession().getLocalAddress() ).getPort() );
            default:
                throw new IndexOutOfBoundsException( I18n.err( I18n.ERR_658, ( columns.length - 1 ) ) );
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
