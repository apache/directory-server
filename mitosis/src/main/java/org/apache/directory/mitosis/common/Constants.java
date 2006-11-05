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
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.enumeration.SearchResultFilter;
import org.apache.directory.server.core.invocation.Invocation;


/**
 * 
 * TODO Constants.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Constants
{
    public static final String ENTRY_UUID = "entryUUID";
    public static final String ENTRY_CSN = "entryCSN";
    public static final String ENTRY_DELETED = "entryDeleted";
    public static final String OBJECT_CLASS_OID = "2.5.4.0";

    
    public static final SearchResultFilter DELETED_ENTRIES_FILTER = new SearchResultFilter()
    {
        public boolean accept( Invocation invocation, SearchResult result, SearchControls controls )
            throws NamingException
        {
            if ( controls.getReturningAttributes() == null )
            {
                Attributes entry = result.getAttributes();
                Attribute deleted = entry.get( ENTRY_DELETED );
                Object value = deleted == null ? null : deleted.get();
                return ( value == null || !"true".equals( value.toString() ) );
            }

            return true;
        }
    };


    private Constants()
    {
    }
}
