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
public class LookupServiceContext implements ServiceContext
{
    /** The entry DN */
    private LdapDN dn;
    
    /** The list of attributes id to return */
    private List<String> attrsId;
    
    /** The list of attributes OIDs for attributes to be returned */
    private List<String> attrsOid;
    
    /** An empty array to avoid returning an Object[] */
    private static final String[] EMPTY_ARRAY = new String[]{};

    /**
     * 
     * Creates a new instance of LookupServiceContext.
     *
     */
    public LookupServiceContext()
    {
    }

    /**
     * 
     * Creates a new instance of LookupServiceContext.
     *
     */
    public LookupServiceContext( LdapDN dn )
    {
        this.dn = dn;
    }

    /**
     * 
     * Creates a new instance of LookupServiceContext.
     *
     */
    public LookupServiceContext( String attrsId[] )
    {
        this.attrsId = new ArrayList<String>();
        attrsOid = new ArrayList<String>();
        setAttrsId( attrsId );
    }

    /**
     * 
     * Creates a new instance of LookupServiceContext.
     *
     */
    public LookupServiceContext( LdapDN dn, String attrsId[] )
    {
        this.dn = dn;
        this.attrsId = new ArrayList<String>();
        attrsOid = new ArrayList<String>();
        setAttrsId( attrsId );
    }

    /**
     * @return The entry's DN
     */
    public LdapDN getDn()
    {
        return dn;
    }
    
    /**
     * Set the principal's DN.
     *
     * @param unbindDn The principal's DN
     */
    public void setDn( LdapDN dn )
    {
        this.dn = dn;
    }

    public String[] getAttrsIdArray()
    {
        String[] attrs = new String[ attrsId.size()];
        return attrsId.toArray( attrs );
    }

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

    public String[] getAttrsOidArray()
    {
        String[] attrs = new String[ attrsId.size()];
        return attrsOid.toArray( attrs );
    }

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
    
    public void addAttrsOid( String attrOid )
    {
        attrsOid.add( attrOid );
    }

    public void addAttrsId( String attrId )
    {
        attrsId.add( attrId );
    }

    public void addAttrs( String attrId, String attrOid )
    {
        attrsId.add( attrId );
        attrsOid.add( attrOid );
    }

    public List<String> getAttrsId()
    {
        return attrsId;
    }

    public List<String> getAttrsOid()
    {
        return attrsOid;
    }
    
    public String toString()
    {
        return "LookupContext for DN '" + dn.getUpName() + "'" + ( ( attrsId != null ) ? ", attributes : <" + StringTools.listToString( attrsId ) + ">" : "" );
    }
}
