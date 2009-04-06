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
package org.apache.directory.server.partition.impl.oracle;

import java.sql.SQLException;

import oracle.jdbc.OracleResultSet;

import org.apache.directory.server.core.cursor.AbstractCursor;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.partition.impl.oracle.OraclePartition.OracleCursorWrapper;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.filter.PresenceNode;

/**
 * An adaptor for Oracle based result sets.
 * From the fact we use oracle resultsets that contains
 * PL/SQL objects, it is pretty simple to implement a cursor
 * just wrapping the OracleResultSet  
 */
public final class OracleEntryCursorAdaptor extends AbstractCursor<ServerEntry>
{
    private OraclePartition partition;
    private OracleResultSet resultSet;
    private OracleCursorWrapper cw;
    
    /**
     * Create a cursor for a list operation context
     * @param partition
     * @param ctx
     * @throws Exception
     */
    public OracleEntryCursorAdaptor(OraclePartition partition, ListOperationContext ctx)
    throws Exception
    {
        this.partition= partition;
        cw= partition.prepareCursor("select value(o) from table(partition_facade.list(?)) o", OraclePartition.toReversedDn( ctx.getDn() ));
        resultSet= cw.getResultSet();
    }

    /**
     * Create a cursor for a search operation context
     * @param partition
     * @param ctx
     * @throws Exception
     */
    public OracleEntryCursorAdaptor(OraclePartition partition, SearchOperationContext ctx)
    throws Exception
    {
        this.partition= partition;
        int searchScope= ctx.getSearchControls().getSearchScope();
        String[] returningAttributes= ctx.getSearchControls().getReturningAttributes();
        returningAttributes= (returningAttributes == null ? new String[]{"*"} : returningAttributes);
        long countLimit= ctx.getSearchControls().getCountLimit();
        
        String filter= null;
        
        if (!(ctx.getFilter() instanceof PresenceNode&&
              ( ((PresenceNode)ctx.getFilter()).getAttribute().equals( SchemaConstants.OBJECT_CLASS_AT_OID )
                || ((PresenceNode)ctx.getFilter()).getAttribute().equals( SchemaConstants.OBJECT_CLASS_AT )
              )
             )
           )
            filter= partition.getXStream().toXML(ctx.getFilter()); 
        
        
        cw= partition.prepareCursor("select value(o) from table(partition_facade.search(?,?,?,?,?)) o", OraclePartition.toReversedDn( ctx.getDn() ), searchScope, filter, returningAttributes, countLimit);
        resultSet= cw.getResultSet();
    }

    public void after( ServerEntry element ) throws Exception
    {
        resultSet.beforeFirst();
        
        while (resultSet.next())
           if (resultSet.getObject( 1 ).equals( element ))
             break;
    }

    public void afterLast() throws Exception
    {
        resultSet.afterLast();
    }

    public boolean available()
    {
        try
        {
            return !resultSet.isClosed();
        }
        catch ( SQLException e )
        {
            return false;
        }
    }

    public void before( ServerEntry element ) throws Exception
    {
        resultSet.beforeFirst();
        
        while (resultSet.next())
           if (resultSet.getObject( 1 ).equals( element ))
             break;
        
        resultSet.previous();
    }

    public void beforeFirst() throws Exception
    {
        resultSet.beforeFirst();
    }

    public void close() throws Exception
    {
        resultSet.close();
    }

    public void close( Exception reason ) throws Exception
    {
        resultSet.close();        
        super.close(reason);
    }

    public boolean first() throws Exception
    {
        return resultSet.first();
    }

    public ServerEntry get() throws Exception
    {
        return ((OracleEntry)resultSet.getObject( 1 )).toServerEntry( partition );
    }


    public boolean isElementReused()
    {
        return false;
    }

    public boolean last() throws Exception
    {
        return resultSet.last();
    }

    public boolean next() throws Exception
    {
        return resultSet.next();
    }

    public boolean previous() throws Exception
    {
        return resultSet.previous();
    }

}
