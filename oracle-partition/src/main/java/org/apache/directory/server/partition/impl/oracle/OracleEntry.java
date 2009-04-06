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

import java.sql.Array;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import oracle.jdbc.OracleConnection;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;

import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.UsageEnum;

/**
 * This class is used like a one to one mapping for the 
 * LDAP_ENTRY pl/sql object. It is encoded and decoded
 * directly from the jdbc driver calling the readSQL and
 * writeSQL methods of the SQLData interface.
 */
public class OracleEntry implements SQLData
{
    private String sqlType= "LDAP_ENTRY"; 
    private OracleConnection connection;
    
    private String reversedDn;
    private String upDn;
    private ArrayList<OracleAttribute> attrs= new ArrayList<OracleAttribute>(); 
    
    /**
     * A connection that can be used to set and get LOBs 
     * values for this entry
     * @param connection
     * @throws Exception
     */
    public void setConnection(OracleConnection connection)
    throws Exception
    {
        this.connection= connection;
    }

    /**
     * @see SQLData
     */
    public String getSQLTypeName() throws SQLException
    {
        return sqlType;
    }
  
    /**
     * @see SQLData
     */
    public void readSQL( SQLInput stream, String sqlType ) throws SQLException
    {
        this.sqlType= sqlType;
        
        reversedDn= stream.readString();
        upDn= stream.readString();
        Array a= stream.readArray();
        
        Object[] oas= ( Object[] ) a.getArray();
        
        for (Object o: oas) 
            attrs.add( ( OracleAttribute ) o );
    }

    /**
     * @see SQLData
     */
    public void writeSQL( SQLOutput stream ) throws SQLException
    {
        stream.writeString( reversedDn );
        stream.writeString( upDn );
        
        ArrayDescriptor desc = ArrayDescriptor.createDescriptor("LDAP_ATTRIBUTE_TABLE",connection);
        ARRAY attrsArray = new ARRAY(desc, connection, attrs.toArray());

        stream.writeArray( attrsArray );
    }

    /**
     * @return the reversed and normalized entry DN
     */
    public String getReversedDn()
    {
        return reversedDn;
    }

    /**
     * @return the user provided DN
     */
    public String getUpDn()
    {
        return upDn;
    }

    /**
     * Set the reversed and normalized entry dn
     * @param reversedDn
     */
    public void setReversedDn( String reversedDn )
    {
        this.reversedDn = reversedDn;
    }

    /**
     * @return a list of entry attributes
     */
    public List<OracleAttribute> getAttrs()
    {
        return attrs;
    }
    
    /**
     * Converts this oracle entry to a ServerEntry
     * @param partition the partition of this entry
     * @return a ServerEntry version of this entry
     * @throws Exception
     */
    public ServerEntry toServerEntry(OraclePartition partition)
    throws Exception
    {
        DefaultServerEntry se= new DefaultServerEntry(partition.getRegistries());
        se.setDn( new LdapDN(upDn) );
        AttributeTypeRegistry atr= partition.getRegistries().getAttributeTypeRegistry();
        
        for (OracleAttribute attribute: attrs)
        {
           attribute.setConnection( partition.getConnectionWrapper().getConnection() ); // used for clob and blob value handling
           
           Object value= attribute.getValue();
           
           if (value instanceof byte[])
             se.add( atr.lookup(attribute.getName()), (byte[])attribute.getValue() );
           else
             se.add( atr.lookup(attribute.getName()), (String)attribute.getValue() );
        }
        
        return se;
    }

    /**
     * Convert a ServerEntry into an OracleEntry 
     * @param entry the ServerEntry to convert
     * @param partition the partition of this entry
     * @return an OracleEntry version of the ServerEntry
     * @throws Exception
     */
    public static OracleEntry fromServerEntry( ServerEntry entry, OraclePartition partition)
    throws Exception
    {
        OracleEntry e= new OracleEntry();
        
        e.setConnection( partition.getConnectionWrapper().getConnection() );
        e.reversedDn= OraclePartition.toReversedDn( entry.getDn() );
        e.upDn= entry.getDn().getUpName();
        
        
        for (AttributeType at: entry.getAttributeTypes())
        {
            String type= (at.getUsage().equals(UsageEnum.USER_APPLICATIONS) ? "u" : null);
            EntryAttribute ea= entry.get( at );
            Iterator<Value<?>> i= ea.getAll();
            
            while (i.hasNext())
            {
                 Value<?> v= i.next();
                 
                 if (v.get() instanceof byte[])
                   e.attrs.add( new OracleAttribute(at.getOid(), (byte[])v.get(), type, e.connection) );
                 else
                   e.attrs.add( new OracleAttribute(at.getOid(), (String)v.get(), type, e.connection) );
            }
        }    
        
        return e;
    }

}
