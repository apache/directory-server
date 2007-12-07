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
package org.apache.directory.server.schema.registries;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.schema.NameForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A plain old java object implementation of an NameFormRegistry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultNameFormRegistry implements NameFormRegistry
{
    /** static class logger */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultNameFormRegistry.class );
    /** maps an OID to an NameForm */
    private final Map<String,NameForm> byOid;
    /** the registry used to resolve names to OIDs */
    private final OidRegistry oidRegistry;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates an empty DefaultNameFormRegistry.
     *
     * @param oidRegistry used by this registry for OID to name resolution of
     * dependencies and to automatically register and unregister it's aliases and OIDs
     */
    public DefaultNameFormRegistry( OidRegistry oidRegistry )
    {
        this.byOid = new HashMap<String,NameForm>();
        this.oidRegistry = oidRegistry;
    }


    // ------------------------------------------------------------------------
    // Service Methods
    // ------------------------------------------------------------------------

    public void register( NameForm nameForm ) throws NamingException
    {
        if ( byOid.containsKey( nameForm.getOid() ) )
        {
            throw new NamingException( "nameForm w/ OID " + nameForm.getOid()
                + " has already been registered!" );
        }

        oidRegistry.register( nameForm.getName(), nameForm.getOid() );
        byOid.put( nameForm.getOid(), nameForm );
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "registered nameForm: " + nameForm );
        }
    }


    public NameForm lookup( String id ) throws NamingException
    {
        id = oidRegistry.getOid( id );

        if ( !byOid.containsKey( id ) )
        {
            throw new NamingException( "nameForm w/ OID " + id + " not registered!" );
        }

        NameForm nameForm = byOid.get( id );
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "lookup with id '"+ id + "' of nameForm: " + nameForm );
        }
        return nameForm;
    }


    public boolean hasNameForm( String id )
    {
        if ( oidRegistry.hasOid( id ) )
        {
            try
            {
                return byOid.containsKey( oidRegistry.getOid( id ) );
            }
            catch ( NamingException e )
            {
                return false;
            }
        }

        return false;
    }


    public String getSchemaName( String id ) throws NamingException
    {
        id = oidRegistry.getOid( id );
        NameForm nf = byOid.get( id );
        if ( nf != null )
        {
            return nf.getSchema();
        }

        throw new NamingException( "OID " + id + " not found in oid to " + "NameForm map!" );
    }


    public Iterator<NameForm> iterator()
    {
        return byOid.values().iterator();
    }
    
    
    public void unregister( String numericOid ) throws NamingException
    {
        if ( ! Character.isDigit( numericOid.charAt( 0 ) ) )
        {
            throw new NamingException( "Looks like the arg is not a numeric OID" );
        }

        byOid.remove( numericOid );
        oidRegistry.unregister( numericOid );
    }
}
