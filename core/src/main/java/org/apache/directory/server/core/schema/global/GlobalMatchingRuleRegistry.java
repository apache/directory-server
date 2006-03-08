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
package org.apache.directory.server.core.schema.global;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.directory.server.core.schema.MatchingRuleRegistry;
import org.apache.directory.server.core.schema.OidRegistry;
import org.apache.directory.server.core.schema.bootstrap.BootstrapMatchingRuleRegistry;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.util.JoinIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A plain old java object implementation of an MatchingRuleRegistry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class GlobalMatchingRuleRegistry implements MatchingRuleRegistry
{
    /** static class logger */
    private final static Logger log = LoggerFactory.getLogger( GlobalMatchingRuleRegistry.class );
    /** maps an OID to an MatchingRule */
    private final Map byOid;
    /** maps an OID to a schema name*/
    private final Map oidToSchema;
    /** the registry used to resolve names to OIDs */
    private final OidRegistry oidRegistry;
    /** the underlying bootstrap registry to delegate on misses to */
    private BootstrapMatchingRuleRegistry bootstrap;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates an empty BootstrapMatchingRuleRegistry.
     */
    public GlobalMatchingRuleRegistry(BootstrapMatchingRuleRegistry bootstrap, OidRegistry oidRegistry)
    {
        this.byOid = new HashMap();
        this.oidToSchema = new HashMap();
        this.oidRegistry = oidRegistry;
        this.bootstrap = bootstrap;
        if ( this.bootstrap == null )
        {
            throw new NullPointerException( "the bootstrap registry cannot be null" );
        }
    }


    // ------------------------------------------------------------------------
    // Service Methods
    // ------------------------------------------------------------------------

    
    public void register( String schema, MatchingRule matchingRule ) throws NamingException
    {
        if ( byOid.containsKey( matchingRule.getOid() ) || bootstrap.hasMatchingRule( matchingRule.getOid() ) )
        {
            NamingException e = new NamingException( "matchingRule w/ OID " + matchingRule.getOid()
                + " has already been registered!" );
            throw e;
        }

        oidRegistry.register( matchingRule.getName(), matchingRule.getOid() );
        byOid.put( matchingRule.getOid(), matchingRule );
        oidToSchema.put( matchingRule.getOid(), schema );
        if ( log.isDebugEnabled() )
        {
            log.debug( "registered matchingRule: " + matchingRule );
        }
    }


    public MatchingRule lookup( String id ) throws NamingException
    {
        id = oidRegistry.getOid( id );

        if ( byOid.containsKey( id ) )
        {
            MatchingRule matchingRule = ( MatchingRule ) byOid.get( id );
            if ( log.isDebugEnabled() )
            {
                log.debug( "looked up matchingRuleUse: " + matchingRule );
            }
            return matchingRule;
        }

        if ( bootstrap.hasMatchingRule( id ) )
        {
            MatchingRule matchingRule = bootstrap.lookup( id );
            if ( log.isDebugEnabled() )
            {
                log.debug( "looked up matchingRuleUse: " + matchingRule );
            }
            return matchingRule;
        }

        NamingException e = new NamingException( "dITContentRule w/ OID " + id + " not registered!" );
        throw e;
    }


    public boolean hasMatchingRule( String id )
    {
        if ( oidRegistry.hasOid( id ) )
        {
            try
            {
                return byOid.containsKey( oidRegistry.getOid( id ) ) || bootstrap.hasMatchingRule( id );
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

        if ( bootstrap.hasMatchingRule( id ) )
        {
            return bootstrap.getSchemaName( id );
        }

        throw new NamingException( "OID " + id + " not found in oid to " + "schema name map!" );
    }


    public Iterator list()
    {
        return new JoinIterator( new Iterator[]
            { byOid.values().iterator(), bootstrap.list() } );
    }
}
