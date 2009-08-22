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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;

import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.schema.SyntaxChecker;
import org.apache.directory.shared.ldap.schema.parsers.SyntaxCheckerDescription;
import org.apache.directory.shared.ldap.schema.registries.SyntaxCheckerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The POJO implementation for the SyntaxCheckerRegistry service.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultSyntaxCheckerRegistry implements SyntaxCheckerRegistry
{
    /** static class logger */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultSyntaxCheckerRegistry.class );
    
    /** a map by OID of SyntaxCheckers */
    private final Map<String, SyntaxChecker> byOidSyntaxChecker;
    
    /** maps an OID to a syntaxCheckerDescription */
    private final Map<String, SyntaxCheckerDescription> oidToDescription;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------
    /**
     * Creates an instance of a DefaultSyntaxRegistry.
     */
    public DefaultSyntaxCheckerRegistry()
    {
        byOidSyntaxChecker = new ConcurrentHashMap<String, SyntaxChecker>();
        oidToDescription = new ConcurrentHashMap<String, SyntaxCheckerDescription>();
    }


    // ------------------------------------------------------------------------
    // Service Methods
    // ------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public void register( SyntaxCheckerDescription syntaxCheckerDescription, SyntaxChecker syntaxChecker ) throws NamingException
    {
        String oid = syntaxChecker.getOid();
        
        if ( byOidSyntaxChecker.containsKey( oid ) )
        {
            String msg = "SyntaxChecker with OID " + oid + " already registered!";
            LOG.warn( msg );
            throw new NamingException( msg );
        }

        byOidSyntaxChecker.put( oid, syntaxChecker );
        oidToDescription.put( oid, syntaxCheckerDescription );
        
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "registered syntaxChecher for OID {}", oid );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void register( SyntaxChecker syntaxChecker ) throws NamingException
    {
        String oid = syntaxChecker.getOid();
        
        if ( byOidSyntaxChecker.containsKey( oid ) )
        {
            String msg = "SyntaxChecker with OID " + oid + " already registered!";
            LOG.warn( msg );
            throw new NamingException( msg );
        }

        byOidSyntaxChecker.put( oid, syntaxChecker );
        
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "registered syntaxChecher for OID {}", oid );
        }
    }


    /**
     * {@inheritDoc}
     */
    public SyntaxChecker lookup( String oid ) throws NamingException
    {
        SyntaxChecker syntaxChecker = byOidSyntaxChecker.get( oid );

        if ( syntaxChecker == null )
        {
            String msg = "SyntaxChecker for OID " + oid + " not found!";
            LOG.warn( msg );
            throw new NamingException( msg );
        }
        
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "looked up syntaxChecher with OID {}", oid );
        }

        return syntaxChecker;
    }


    /**
     * {@inheritDoc}
     */
    public boolean contains( String oid )
    {
        return byOidSyntaxChecker.containsKey( oid );
    }


    /**
     * {@inheritDoc}
     */
    public String getSchemaName( String oid ) throws NamingException
    {
        if ( ! OID.isOID( oid ) )
        {
            String msg = "Looks like the arg is not a numeric OID";
            LOG.warn( msg );
            throw new NamingException( msg );
        }

        SyntaxCheckerDescription description = oidToDescription.get( oid );
        
        if ( description != null )
        {
            return getSchema( description );
        }

        String msg = "OID " + oid + " not found in oid to schema name map!";
        LOG.warn( msg );
        throw new NamingException( msg );
    }
    
    
    private static String getSchema( SyntaxCheckerDescription desc ) 
    {
        List<String> ext = desc.getExtensions().get( "X-SCHEMA" );
        
        if ( ( ext == null ) || ( ext.size() == 0 ) )
        {
            return "other";
        }
        
        return ext.get( 0 );
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<String> oidsIterator()
    {
        return byOidSyntaxChecker.keySet().iterator();
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<SyntaxChecker> iterator()
    {
        return byOidSyntaxChecker.values().iterator();
    }


    /**
     * {@inheritDoc}
     */
    public void unregister( String numericOid ) throws NamingException
    {
        if ( ! OID.isOID( numericOid ) )
        {
            String msg = "Looks like the arg is not a numeric OID";
            LOG.warn( msg );
            throw new NamingException( msg );
        }

        byOidSyntaxChecker.remove( numericOid );
        oidToDescription.remove( numericOid );
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void unregisterSchemaElements( String schemaName )
    {
        List<String> oids = new ArrayList<String>( byOidSyntaxChecker.keySet() );
        
        for ( String oid : oids )
        {
            SyntaxCheckerDescription description = oidToDescription.get( oid );
            String schemaNameForOid = getSchema( description );
            
            if ( schemaNameForOid.equalsIgnoreCase( schemaName ) )
            {
                byOidSyntaxChecker.remove( oid );
                oidToDescription.remove( oid );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void renameSchema( String originalSchemaName, String newSchemaName )
    {
        List<String> oids = new ArrayList<String>( byOidSyntaxChecker.keySet() );
        
        for ( String oid : oids )
        {
            SyntaxCheckerDescription description = oidToDescription.get( oid );
            String schemaNameForOid = getSchema( description );
            
            if ( schemaNameForOid.equalsIgnoreCase( originalSchemaName ) )
            {
                List<String> values = description.getExtensions().get( "X-SCHEMA" );
                values.clear();
                values.add( newSchemaName );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<SyntaxCheckerDescription> syntaxCheckerDescriptionIterator()
    {
        return oidToDescription.values().iterator();
    }
}
