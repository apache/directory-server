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
package org.apache.directory.mitosis.operation.support;


import javax.naming.NameNotFoundException;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.NamespaceTools;
import org.apache.directory.shared.ldap.util.StringTools;
import org.apache.directory.mitosis.common.CSN;
import org.apache.directory.mitosis.common.CSNFactory;


public class EntryUtil
{
    @SuppressWarnings("unchecked")
    public static boolean isEntryUpdatable( CSNFactory csnFactory, CoreSession coreSession, LdapDN name, CSN newCSN ) 
        throws Exception
    {
        PartitionNexus nexus = coreSession.getDirectoryService().getPartitionNexus();
        LookupOperationContext lookupContext = new LookupOperationContext( coreSession, name ); 
        ServerEntry entry = nexus.lookup( lookupContext );

        if ( entry == null )
        {
            return true;
        }

        EntryAttribute entryCSNAttr = entry.get( ApacheSchemaConstants.ENTRY_CSN_AT );

        if ( entryCSNAttr == null )
        {
            return true;
        }
        else
        {
            CSN oldCSN = null;

            try
            {
                Object val = entryCSNAttr.get();
                
                if ( val instanceof byte[] )
                {
                    oldCSN = csnFactory.newInstance( StringTools.utf8ToString( (byte[])val ) );
                }
                else
                {
                    oldCSN = csnFactory.newInstance( (String)val );
                }
            }
            catch ( IllegalArgumentException e )
            {
                return true;
            }

            return oldCSN.compareTo( newCSN ) < 0;
        }
    }


    public static void createGlueEntries( CoreSession coreSession, LdapDN name, boolean includeLeaf )
        throws Exception
    {
        assert name.size() > 0;

        for ( int i = name.size() - 1; i > 0; i-- )
        {
            createGlueEntry( coreSession, ( LdapDN ) name.getSuffix( i ) );
        }

        if ( includeLeaf )
        {
            createGlueEntry( coreSession, name );
        }
    }


    private static void createGlueEntry( CoreSession coreSession, LdapDN name ) 
        throws Exception
    {
        DirectoryService ds = coreSession.getDirectoryService();
        PartitionNexus nexus = ds.getPartitionNexus();
        
        try
        {
            if ( nexus.hasEntry( new EntryOperationContext( coreSession, name ) ) )
            {
                return;
            }
        }
        catch ( NameNotFoundException e )
        {
            // Skip if there's no backend associated with the name.
            return;
        }

        // Create a glue entry.
        ServerEntry entry = ds.newEntry( name );
        
        //// Add RDN attribute. 
        String rdn = name.get( name.size() - 1 );
        String rdnAttribute = NamespaceTools.getRdnAttribute( rdn );
        String rdnValue = NamespaceTools.getRdnValue( rdn );
        entry.put( rdnAttribute, rdnValue );
        
        //// Add objectClass attribute. 
        entry.put( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC, SchemaConstants.EXTENSIBLE_OBJECT_OC );

        // And add it to the nexus.
        nexus.add( new AddOperationContext( coreSession, entry ) );
    }


    private EntryUtil()
    {
    }
}
