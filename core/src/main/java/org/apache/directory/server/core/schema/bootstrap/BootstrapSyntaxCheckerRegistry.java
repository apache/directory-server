/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.core.schema.bootstrap;


import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.directory.server.core.schema.SyntaxCheckerRegistry;
import org.apache.directory.shared.ldap.schema.SyntaxChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The POJO implementation for the SyntaxCheckerRegistry service.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BootstrapSyntaxCheckerRegistry implements SyntaxCheckerRegistry
{
    /** static class logger */
    private final static Logger log = LoggerFactory.getLogger( BootstrapSyntaxCheckerRegistry.class );
    /** a map by OID of SyntaxCheckers */
    private final Map byOid;
    /** maps an OID to a schema name*/
    private final Map oidToSchema;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates an instance of a BootstrapSyntaxRegistry.
     */
    public BootstrapSyntaxCheckerRegistry()
    {
        this.byOid = new HashMap();
        this.oidToSchema = new HashMap();
    }


    // ------------------------------------------------------------------------
    // Service Methods
    // ------------------------------------------------------------------------

    
    public void register( String schema, String oid, SyntaxChecker syntaxChecker ) throws NamingException
    {
        if ( byOid.containsKey( oid ) )
        {
            NamingException e = new NamingException( "SyntaxChecker with OID " + oid + " already registered!" );
            throw e;
        }

        byOid.put( oid, syntaxChecker );
        oidToSchema.put( oid, schema );
        if ( log.isDebugEnabled() )
        {
            log.debug( "registered syntaxChecher for OID " + oid );
        }
    }


    public SyntaxChecker lookup( String oid ) throws NamingException
    {
        if ( !byOid.containsKey( oid ) )
        {
            NamingException e = new NamingException( "SyntaxChecker for OID " + oid + " not found!" );
            throw e;
        }

        SyntaxChecker syntaxChecker = ( SyntaxChecker ) byOid.get( oid );
        if ( log.isDebugEnabled() )
        {
            log.debug( "looked up syntaxChecher with OID " + oid );
        }
        return syntaxChecker;
    }


    public boolean hasSyntaxChecker( String oid )
    {
        return byOid.containsKey( oid );
    }


    public String getSchemaName( String oid ) throws NamingException
    {
        if ( Character.isDigit( oid.charAt( 0 ) ) )
        {
            throw new NamingException( "Looks like the arg is not a numeric OID" );
        }

        if ( oidToSchema.containsKey( oid ) )
        {
            return ( String ) oidToSchema.get( oid );
        }

        throw new NamingException( "OID " + oid + " not found in oid to " + "schema name map!" );
    }
}
