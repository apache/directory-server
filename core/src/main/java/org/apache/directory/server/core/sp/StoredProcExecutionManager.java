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


public class StoredProcExecutionManager
{

    private final String spContainer;

    private final List<StoredProcEngineConfig> spEngineConfigs;


    public StoredProcExecutionManager( final String spContainer, final List<StoredProcEngineConfig> spEngineConfigs )
    {
        this.spContainer = spContainer;
        this.spEngineConfigs = spEngineConfigs;
    }
    
    public Attributes findStoredProcUnit( LdapContext rootCtx, String fullSPName ) throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setReturningAttributes( new String[] { "*" } );
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        String spUnitName = StoredProcUtils.extractStoredProcUnitName( fullSPName );
        String filter = "(storedProcUnitName=" + spUnitName + ")";
        NamingEnumeration<SearchResult> results = rootCtx.search( spContainer, filter, controls );
        Attributes spUnitEntry = results.nextElement().getAttributes();
        return spUnitEntry;
    }


    public StoredProcEngine getStoredProcEngineInstance( Attributes spUnitEntry ) throws NamingException
    {

        String spLangId = ( String ) spUnitEntry.get( "storedProcLangId" ).get();

        for ( StoredProcEngineConfig engineConfig : spEngineConfigs )
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
