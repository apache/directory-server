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
package org.apache.directory.shared.ldap.schema.comparators;


import javax.naming.Name;
import javax.naming.NamingException;

import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.LdapComparator;
import org.apache.directory.shared.ldap.schema.SchemaManager;


/**
 * 
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DnComparator extends LdapComparator<Object>
{
    /** A reference to the schema manager */ 
    private transient SchemaManager schemaManager;
    
    public DnComparator( String oid )
    {
        super( oid );
    }
    
    
    /**
     * {@inheritDoc}
     */
    public int compare( Object obj0, Object obj1 ) 
    {
        LdapDN dn0 = null;
        LdapDN dn1 = null;
        
        try 
        {
            dn0 = getDn( obj0 );
            dn1 = getDn( obj1 );
        }
        catch ( NamingException e )
        {
            // -- what do we do here ?
            return -1;
        }
        
        return dn0.compareTo( dn1 );
    }


    public LdapDN getDn( Object obj ) throws NamingException
    {
        LdapDN dn = null;
        
        if ( obj instanceof LdapDN )
        {
            dn = (LdapDN)obj;
            
            dn = ( dn.isNormalized() ? dn : LdapDN.normalize( dn, schemaManager.getNormalizerMapping() ) );
        }
        else if ( obj instanceof Name )
        {
            dn = new LdapDN( ( Name ) obj );
            dn.normalize( schemaManager.getNormalizerMapping() );
        }
        else if ( obj instanceof String )
        {
            dn = new LdapDN( ( String ) obj );
            dn.normalize( schemaManager.getNormalizerMapping() );
        }
        else
        {
            throw new IllegalStateException( "I do not know how to handle dn comparisons with objects of class: " 
                + (obj == null ? null : obj.getClass() ) );
        }
        
        return dn;
    }


    /**
     * {@inheritDoc}
     */
    public void setSchemaManager( SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;
    }
}
