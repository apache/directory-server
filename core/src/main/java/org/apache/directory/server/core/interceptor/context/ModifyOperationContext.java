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
package org.apache.directory.server.core.interceptor.context;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;

import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.name.LdapDN;

/**
 * A Modify context used for Interceptors. It contains all the informations
 * needed for the modify operation, and used by all the interceptors
 * 
 * This context can use either Attributes or ModificationItem, but not both.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ModifyOperationContext extends AbstractOperationContext
{
    /** The modification items */
    private List<ModificationItemImpl> modItems;
    
    /**
     * 
     * Creates a new instance of ModifyOperationContext.
     *
     */
    public ModifyOperationContext()
    {
    	super();
    }

    /**
     * 
     * Creates a new instance of ModifyOperationContext.
     *
     */
    public ModifyOperationContext( LdapDN dn, List<ModificationItemImpl> modItems )
    {
        super( dn );
        this.modItems = modItems;
    }

    /**
     * Set the modified attributes
     * @param modItems The modified attributes
     */
    public void setModItems( List<ModificationItemImpl> modItems )
    {
        this.modItems = modItems;
    }

    /**
     * @return The modifications
     */
    public List<ModificationItemImpl> getModItems() 
    {
        return modItems;
    }
    
    @SuppressWarnings( value = "unchecked" )
    public static List<ModificationItemImpl> createModItems( Attributes attributes, int modOp ) throws NamingException
    {
        List<ModificationItemImpl> items = new ArrayList<ModificationItemImpl>( attributes.size() );
        NamingEnumeration<Attribute> e = ( NamingEnumeration<Attribute> ) attributes.getAll();
        
        while ( e.hasMore() )
        {
            items.add( new ModificationItemImpl( modOp, e.next() ) );
        }

        return items;
    }
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append("ModifyContext for DN '").append( getDn().getUpName() ).append( "', modifications :\n" );
        
        for ( ModificationItem mod:modItems )
        {
            sb.append( mod ).append( '\n' );
        }
        
        return sb.toString();
    }
}
