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
import java.util.Arrays;
import java.util.List;

import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.StringTools;

/**
 * A Lookup context used for Interceptors. It contains all the informations
 * needed for the lookup operation, and used by all the interceptors
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class LookupOperationContext  extends AbstractOperationContext
{
    /** The list of attributes id to return */
    private List<String> attrsId;
    
    /** The list of attributes OIDs for attributes to be returned */
    private List<String> attrsOid;
    
    /**
     * 
     * Creates a new instance of LookupOperationContext.
     *
     */
    public LookupOperationContext()
    {
    	super();
    }

    /**
     * 
     * Creates a new instance of LookupOperationContext.
     *
     */
    public LookupOperationContext( LdapDN dn )
    {
        super( dn );
    }

    /**
     * 
     * Creates a new instance of LookupOperationContext.
     *
     */
    public LookupOperationContext( String attrsId[] )
    {
    	super();
        this.attrsId = new ArrayList<String>();
        attrsOid = new ArrayList<String>();
        setAttrsId( attrsId );
    }

    /**
     * 
     * Creates a new instance of LookupOperationContext.
     *
     */
    public LookupOperationContext( LdapDN dn, String attrsId[] )
    {
        super( dn );
        this.attrsId = new ArrayList<String>();
        attrsOid = new ArrayList<String>();
        setAttrsId( attrsId );
    }

    /**
     * @return Get the attribute ids as a String array
     */
    public String[] getAttrsIdArray()
    {
        if ( attrsId == null )
        {
            return new String[]{};
        }
        else
        {
            String[] attrs = new String[ attrsId.size()];
            return attrsId.toArray( attrs );
        }
    }

    /**
     * Set the attribute Ids
     *
     * @param attrsId The String array containing all the attribute IDs
     */
    public void setAttrsId( String[] attrsId )
    {
        if ( attrsId == null )
        {
            this.attrsId = new ArrayList<String>();
        }
        else
        {
            this.attrsId = new ArrayList<String>( Arrays.asList( attrsId ) );
        }
    }

    /**
     * @return Get the attribute oids as a String array
     */
    public String[] getAttrsOidArray()
    {
        String[] attrs = new String[ attrsId.size()];
        return attrsOid.toArray( attrs );
    }

    /**
     * Set the attribute oIds
     *
     * @param attrsId The String array containing all the attribute OIDs
     */
    public void setAttrsOid( String[] attrsOid )
    {
        if ( attrsOid == null )
        {
            this.attrsOid = new ArrayList<String>();
        }
        else
        {
            this.attrsOid = new ArrayList<String>( Arrays.asList( attrsOid ) );
        }
    }
    
    /**
     * Add an attribute OID to the current list, creating the list if necessary
     *
     * @param attrOid The oid to add
     */
    public void addAttrsOid( String attrOid )
    {
        if ( attrsOid == null )
        {
            attrsOid = new ArrayList<String>(); 
        }
        
        attrsOid.add( attrOid );
    }

    /**
     * Add an attribute ID to the current list, creating the list if necessary
     *
     * @param attrId the Id to add
     */
    public void addAttrsId( String attrId )
    {
        if ( attrsId == null )
        {
            attrsId = new ArrayList<String>(); 
        }
        
        attrsId.add( attrId );
    }

    /**
     * Add an attribute ID and OID to the current lists, creating the lists if necessary
     *
     * @param attrId the Id to add
     * @param attrOid The oid to add
     */
    public void addAttrs( String attrId, String attrOid )
    {
        if ( attrsId == null )
        {
            attrsId = new ArrayList<String>(); 
        }
        
        if ( attrsOid == null )
        {
            attrsOid = new ArrayList<String>(); 
        }
        
        attrsId.add( attrId );
        attrsOid.add( attrOid );
    }

    /**
     * @return The attribute IDs list
     */
    public List<String> getAttrsId()
    {
        return attrsId;
    }

    /**
     * @return The attribute OIDs list
     */
    public List<String> getAttrsOid()
    {
        return attrsOid;
    }
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "LookupContext for DN '" + getDn().getUpName() + "'" + ( ( attrsId != null ) ? ", attributes : <" + StringTools.listToString( attrsId ) + ">" : "" );
    }
}
