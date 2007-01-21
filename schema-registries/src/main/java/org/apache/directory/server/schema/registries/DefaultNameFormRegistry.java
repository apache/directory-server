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
    private final static Logger log = LoggerFactory.getLogger( DefaultNameFormRegistry.class );
    /** maps an OID to an NameForm */
    private final Map<String,NameForm> byOid;
    /** maps an OID to a schema name*/
    private final Map<String,String> oidToSchema;
    /** the registry used to resolve names to OIDs */
    private final OidRegistry oidRegistry;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates an empty BootstrapNameFormRegistry.
     */
    public DefaultNameFormRegistry(OidRegistry oidRegistry)
    {
        this.byOid = new HashMap<String,NameForm>();
        this.oidToSchema = new HashMap<String,String>();
        this.oidRegistry = oidRegistry;
    }


    // ------------------------------------------------------------------------
    // Service Methods
    // ------------------------------------------------------------------------

    public void register( String schema, NameForm nameForm ) throws NamingException
    {
        if ( byOid.containsKey( nameForm.getOid() ) )
        {
            NamingException e = new NamingException( "nameForm w/ OID " + nameForm.getOid()
                + " has already been registered!" );
            throw e;
        }

        oidToSchema.put( nameForm.getOid(), schema );
        oidRegistry.register( nameForm.getName(), nameForm.getOid() );
        byOid.put( nameForm.getOid(), nameForm );
        if ( log.isDebugEnabled() )
        {
            log.debug( "registered nameForm: " + nameForm );
        }
    }


    public NameForm lookup( String id ) throws NamingException
    {
        id = oidRegistry.getOid( id );

        if ( !byOid.containsKey( id ) )
        {
            NamingException e = new NamingException( "nameForm w/ OID " + id + " not registered!" );
            throw e;
        }

        NameForm nameForm = ( NameForm ) byOid.get( id );
        if ( log.isDebugEnabled() )
        {
            log.debug( "lookup with id '"+ id + "' of nameForm: " + nameForm );
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
        if ( oidToSchema.containsKey( id ) )
        {
            return ( String ) oidToSchema.get( id );
        }

        throw new NamingException( "OID " + id + " not found in oid to " + "schema name map!" );
    }


    public Iterator list()
    {
        return byOid.values().iterator();
    }
}
