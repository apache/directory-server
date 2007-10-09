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


package org.apache.directory.server.core.sp;


import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import javax.naming.ldap.LdapContext;

import org.apache.directory.shared.ldap.constants.SchemaConstants;

/**
 * A Factory type class which holds a registry of supported {@link StoredProcEngineConfig}s. A container reference
 * as the base for Stored Procedure storage on the DIT is also handled by this class.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$ $Date$
 */
public class StoredProcExecutionManager
{
    
    private final String storedProcContainer;

    private final List<StoredProcEngineConfig> storedProcEngineConfigs;


    /**
     * Creates a {@link StoredProcExecutionManager} instance.
     * 
     * @param storedProcContainer The base of the DIT subtree used for storing stored procedure units.
     * @param storedProcEngineConfigs A list of {@link StoredProcEngineConfig}s to register different {@link StoredProcEngine}s with this manager.
     */
    public StoredProcExecutionManager( final String storedProcContainer, final List<StoredProcEngineConfig> storedProcEngineConfigs )
    {
        this.storedProcContainer = storedProcContainer;
        this.storedProcEngineConfigs = storedProcEngineConfigs;
    }
    
    /**
     * Finds and returns a stored procedure unit entry whose identifier name
     * is extracted from fullSPName.
     * 
     * @param rootDSE A handle on the root DSE to be used for searching the SP Unit over.
     * @param fullSPName Full name of the Stored Procedure including the unit name.
     * @return The entry associated with the SP Unit.
     * @throws NamingException If the unit cannot be located or any other error occurs.
     */
    public Attributes findStoredProcUnit( LdapContext rootDSE, String fullSPName ) throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setReturningAttributes( SchemaConstants.ALL_USER_ATTRIBUTES_ARRAY );
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        String spUnitName = StoredProcUtils.extractStoredProcUnitName( fullSPName );
        String filter = "(storedProcUnitName=" + spUnitName + ")";
        NamingEnumeration<SearchResult> results = rootDSE.search( storedProcContainer, filter, controls );
        Attributes spUnitEntry = results.nextElement().getAttributes();
        return spUnitEntry;
    }


    /**
     * Initializes and returns a {@link StoredProcEngine} instance which can operate on spUnitEntry
     * considering its specific stored procedure language.
     * 
     * @param spUnitEntry The entry which a {@link StoredProcEngine} type will be mathched with respect to the language identifier.
     * @return A {@link StoredProcEngine} associated with spUnitEntry.
     * @throws NamingException If no {@link StoredProcEngine} that can be associated with the language identifier in spUnitEntry can be found.
     */
    public StoredProcEngine getStoredProcEngineInstance( Attributes spUnitEntry ) throws NamingException
    {

        String spLangId = ( String ) spUnitEntry.get( "STORED_PROC_LANG_ID" ).get();

        for ( StoredProcEngineConfig engineConfig : storedProcEngineConfigs )
        {
            if ( engineConfig.getStoredProcLangId().equalsIgnoreCase( spLangId ) )
            {
                Class<? extends StoredProcEngine> engineType = engineConfig.getStoredProcEngineType();
                StoredProcEngine engine;
                try
                {
                    engine = engineType.newInstance();
                }
                catch ( InstantiationException e )
                {
                    NamingException ne = new NamingException();
                    ne.setRootCause( e );
                    throw ne;
                }
                catch ( IllegalAccessException e )
                {
                    NamingException ne = new NamingException();
                    ne.setRootCause( e );
                    throw ne;
                }
                engine.setSPUnitEntry( spUnitEntry );
                return engine;
            }

        }

        throw new NamingException( "Stored Procedure Language, " + spLangId + " is not supported." );

    }

}
