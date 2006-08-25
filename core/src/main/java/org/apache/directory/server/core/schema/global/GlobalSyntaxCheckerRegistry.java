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
package org.apache.directory.server.core.schema.global;


import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.directory.server.core.schema.SyntaxCheckerRegistry;
import org.apache.directory.server.core.schema.bootstrap.BootstrapSyntaxCheckerRegistry;
import org.apache.directory.shared.ldap.schema.SyntaxChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A simple POJO implementation of the SyntaxCheckerRegistry service interface.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class GlobalSyntaxCheckerRegistry implements SyntaxCheckerRegistry
{
    /** static class logger */
    private final static Logger log = LoggerFactory.getLogger( GlobalSyntaxCheckerRegistry.class );
    /** the syntaxCheckers in this registry */
    private final Map syntaxCheckers;
    /** maps an OID to a schema name*/
    private final Map oidToSchema;
    /** the underlying bootstrap registry to delegate on misses to */
    private BootstrapSyntaxCheckerRegistry bootstrap;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    
    /**
     * Creates a default SyntaxCheckerRegistry by initializing the map and the
     * montior.
     */
    public GlobalSyntaxCheckerRegistry(BootstrapSyntaxCheckerRegistry bootstrap)
    {
        this.oidToSchema = new HashMap();
        this.syntaxCheckers = new HashMap();
        this.bootstrap = bootstrap;
        if ( this.bootstrap == null )
        {
            throw new NullPointerException( "the bootstrap registry cannot be null" );
        }
    }

    
    // ------------------------------------------------------------------------
    // Service Methods
    // ------------------------------------------------------------------------

    
    /*
     * TODO: why supply the oid here if SyntaxCheckers contain the oid value of their 
     * syntax as a property?
     */
    public void register( String schema, String oid, SyntaxChecker syntaxChecker ) throws NamingException
    {
        if ( syntaxCheckers.containsKey( oid ) || bootstrap.hasSyntaxChecker( oid ) )
        {
            NamingException e = new NamingException( "SyntaxChecker with OID " + oid + " already registered!" );
            throw e;
        }

        oidToSchema.put( oid, schema );
        syntaxCheckers.put( oid, syntaxChecker );
        if ( log.isDebugEnabled() )
        {
            log.debug( "registered syntax checker for syntax: " + oid );
        }
    }


    public SyntaxChecker lookup( String oid ) throws NamingException
    {
        SyntaxChecker c;
        NamingException e;

        if ( syntaxCheckers.containsKey( oid ) )
        {
            c = ( SyntaxChecker ) syntaxCheckers.get( oid );
            if ( log.isDebugEnabled() )
            {
                log.debug( "lookup succeeded for syntax checker of syntax: " + oid );
            }
            return c;
        }

        if ( bootstrap.hasSyntaxChecker( oid ) )
        {
            c = bootstrap.lookup( oid );
            if ( log.isDebugEnabled() )
            {
                log.debug( "lookup succeeded for syntax checker of syntax: " + oid );
            }
            return c;
        }

        e = new NamingException( "SyntaxChecker not found for OID: " + oid );
        throw e;
    }


    public boolean hasSyntaxChecker( String oid )
    {
        return syntaxCheckers.containsKey( oid ) || bootstrap.hasSyntaxChecker( oid );
    }


    public String getSchemaName( String oid ) throws NamingException
    {
        if ( !Character.isDigit( oid.charAt( 0 ) ) )
        {
            throw new NamingException( "OID " + oid + " is not a numeric OID" );
        }

        if ( oidToSchema.containsKey( oid ) )
        {
            return ( String ) oidToSchema.get( oid );
        }

        if ( bootstrap.hasSyntaxChecker( oid ) )
        {
            return bootstrap.getSchemaName( oid );
        }

        throw new NamingException( "OID " + oid + " not found in oid to " + "schema name map!" );
    }
}
