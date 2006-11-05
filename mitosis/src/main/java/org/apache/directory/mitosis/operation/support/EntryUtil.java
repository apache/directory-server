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
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.NamespaceTools;
import org.apache.directory.mitosis.common.CSN;
import org.apache.directory.mitosis.common.Constants;
import org.apache.directory.mitosis.common.SimpleCSN;


public class EntryUtil
{
    @SuppressWarnings("unchecked")
    public static boolean isEntryUpdatable( PartitionNexus nexus, LdapDN name, CSN newCSN ) throws NamingException
    {
        Attributes entry = nexus.lookup( name );

        if ( entry == null )
        {
            return true;
        }

        Attribute entryCSNAttr = entry.get( Constants.ENTRY_CSN );

        if ( entryCSNAttr == null )
        {
            return true;
        }
        else
        {
            CSN oldCSN = null;

            try
            {
                oldCSN = new SimpleCSN( String.valueOf( entryCSNAttr.get() ) );
            }
            catch ( IllegalArgumentException e )
            {
                return true;
            }

            return oldCSN.compareTo( newCSN ) < 0;
        }
    }


    public static void createGlueEntries( PartitionNexus nexus, LdapDN name, boolean includeLeaf )
        throws NamingException
    {
        assert name.size() > 0;

        for ( int i = name.size() - 1; i > 0; i-- )
        {
            createGlueEntry( nexus, ( LdapDN ) name.getSuffix( i ) );
        }

        if ( includeLeaf )
        {
            createGlueEntry( nexus, name );
        }
    }


    private static void createGlueEntry( PartitionNexus nexus, LdapDN name ) throws NamingException
    {
        try
        {
            if ( nexus.hasEntry( name ) )
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
        Attributes entry = new BasicAttributes( true );
        
        //// Add RDN attribute. 
        String rdn = name.get( name.size() - 1 );
        String rdnAttribute = NamespaceTools.getRdnAttribute( rdn );
        String rdnValue = NamespaceTools.getRdnValue( rdn );
        entry.put( rdnAttribute, rdnValue );
        
        //// Add objectClass attribute. 
        Attribute objectClassAttr = new BasicAttribute( "objectClass" );
        objectClassAttr.add( "top" );
        objectClassAttr.add( "extensibleObject" );
        entry.put( objectClassAttr );

        // And add it to the nexus.
        nexus.add( name, entry );
    }


    private EntryUtil()
    {
    }
}
