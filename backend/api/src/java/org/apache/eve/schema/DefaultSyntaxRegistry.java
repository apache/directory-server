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
package org.apache.eve.schema;


import org.apache.ldap.common.schema.Syntax;

import java.util.Map;
import java.util.HashMap;

import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;


/**
 * A SyntaxRegistry service available during server startup when other resources
 * like a syntax backing store is unavailable.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultSyntaxRegistry implements SyntaxRegistry
{
    /** a map of entries using an OID for the key and a Syntax for the value */
    private final Map syntaxes;
    /** the OID registry this registry uses to register new syntax OIDs */
    private final OidRegistry registry;
    /** a monitor used to track noteable registry events */
    private SyntaxRegistryMonitor monitor = null;
    
    
    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------
    
    
    /**
     * Creates a DefaultSyntaxRegistry using existing Syntaxes for lookups.
     * 
     * @param syntaxes a map of OIDs to their respective Syntax objects
     */
    public DefaultSyntaxRegistry( Syntax[] syntaxes, OidRegistry registry )
    {
        this ( syntaxes, registry, new SyntaxRegistryMonitorAdapter() );
    }

        
    /**
     * Creates a DefaultSyntaxRegistry using existing Syntaxes for lookups.
     * 
     * @param syntaxes a map of OIDs to their respective Syntax objects
     */
    public DefaultSyntaxRegistry( Syntax[] syntaxes,
                                    OidRegistry registry,
                                    SyntaxRegistryMonitor monitor )
    {
        this.monitor = monitor;
        this.registry = registry;
        this.syntaxes = new HashMap();
        
        for ( int ii = 0; ii < syntaxes.length; ii++ )
        {
            this.syntaxes.put( syntaxes[ii].getOid(), syntaxes[ii] );
            
            registry.register( syntaxes[ii].getOid(),
                    syntaxes[ii].getOid() );
            if ( syntaxes[ii].getName() != null )
            {    
                registry.register( syntaxes[ii].getName(),
                        syntaxes[ii].getOid() );
            }
            
            monitor.registered( syntaxes[ii] );
        }
    }
    

    // ------------------------------------------------------------------------
    // SyntaxRegistry interface methods
    // ------------------------------------------------------------------------
    
    
    /**
     * @see org.apache.eve.schema.SyntaxRegistry#lookup(java.lang.String)
     */
    public Syntax lookup( String oid ) throws NamingException
    {
        if ( syntaxes.containsKey( oid ) )
        {
            Syntax syntax = ( Syntax ) syntaxes.get( oid );
            monitor.lookedUp( syntax );
            return syntax;
        }
        
        NamingException fault = new NamingException( "Unknown syntax OID "
                + oid );
        monitor.lookupFailed( oid, fault );
        throw fault;
    }
    

    /**
     * @see org.apache.eve.schema.SyntaxRegistry#register(Syntax)
     */
    public void register( Syntax syntax ) throws NamingException
    {
        NamingException fault = new OperationNotSupportedException(
                "Syntax registration on read-only bootstrap SyntaxRegistry not "
                + "supported." );
        monitor.registerFailed( syntax, fault );
        throw fault;
    }

    
    /**
     * @see org.apache.eve.schema.SyntaxRegistry#hasSyntax(java.lang.String)
     */
    public boolean hasSyntax( String oid )
    {
        return syntaxes.containsKey( oid );
    }


    // ------------------------------------------------------------------------
    // package friendly monitor methods
    // ------------------------------------------------------------------------
    
    
    /**
     * Gets the monitor for this registry.
     * 
     * @return the monitor
     */
    SyntaxRegistryMonitor getMonitor()
    {
        return monitor;
    }

    
    /**
     * Sets the monitor for this registry.
     * 
     * @param monitor the monitor to set
     */
    void setMonitor( SyntaxRegistryMonitor monitor )
    {
        this.monitor = monitor;
    }
}
