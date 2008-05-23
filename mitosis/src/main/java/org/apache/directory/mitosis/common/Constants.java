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
package org.apache.directory.mitosis.common;


import javax.naming.NamingException;

import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.filtering.EntryFilter;
import org.apache.directory.server.core.interceptor.context.SearchingOperationContext;
import org.apache.directory.shared.ldap.entry.EntryAttribute;


/**
 * Defines constant values used by Mitosis.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Constants
{
    /**
     * The name of the attribute that represents the {@link UUID} of an
     * LDAP entry.
     */
    public static final String ENTRY_UUID = "entryUUID";
    
    /**
     * The name of the attribute that represents the {@link CSN} of an LDAP
     * entry.
     */
    public static final String ENTRY_CSN = "entryCSN";
    
    /**
     * The name of the attribute that determines if an entry is actually
     * deleted or not (even if it exists in a DIT.)
     */
    public static final String ENTRY_DELETED = "entryDeleted";
    
    /**
     * A {@link SearchResultFilter} that filters out the entries whose
     * {@link #ENTRY_DELETED} attribute is <tt>TRUE</tt>.
     */
    public static final EntryFilter DELETED_ENTRIES_FILTER = new EntryFilter()
    {
        public boolean accept( SearchingOperationContext operation, ClonedServerEntry entry )
            throws NamingException
        {
            EntryAttribute deleted = entry.get( ENTRY_DELETED );
            Object value = deleted == null ? null : deleted.get();
            return ( value == null || !"TRUE".equalsIgnoreCase( value.toString() ) );
        }
    };


    private Constants()
    {
    }
}
