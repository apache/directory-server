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
package org.apache.directory.shared.ldap.schema.comparators;


import java.text.ParseException;
import java.util.Comparator;

import org.apache.directory.shared.ldap.schema.parsers.AbstractSchemaDescription;
import org.apache.directory.shared.ldap.schema.parsers.ComparatorDescriptionSchemaParser;


/**
 * A comparator for Comparators. We compare the OIDs
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ComparatorComparator implements Comparator<String>
{
    /** A static instance of this comparator */
    public static final Comparator<String> INSTANCE = new ComparatorComparator();

    /** A parser for the comparators */
    private static final ComparatorDescriptionSchemaParser comparatorParser =
        new ComparatorDescriptionSchemaParser();
    
    /**
     * {@inheritDoc}
     */
    public int compare( String s1, String s2 )
    {
        // -------------------------------------------------------------------
        // Handle some basis cases
        // -------------------------------------------------------------------
        if ( s1 == null )
        {
            return ( s2 == null ) ? 0 : -1;
        }
        
        if ( s2 == null )
        {
            return -1;
        }

        // Let's try to avoid a parse.
        if ( s1.equals( s2 ) )
        {
            return 0;
        }
        
        // Parse the comparators now
        synchronized ( comparatorParser )
        {
            AbstractSchemaDescription c1 = null;
            AbstractSchemaDescription c2 = null;
            
            try
            {
                c1 = comparatorParser.parse( s1 );
            }
            catch ( ParseException pe )
            {
                return -1;
            }
            
            try
            {
                c2 = comparatorParser.parse( s2 );
            }
            catch ( ParseException pe )
            {
                return -1;
            }
            
            // The OID is the only criterium for a comparison
            return ( c1.getNumericOid().compareTo( c2.getNumericOid() ) );
        }
    }
}
