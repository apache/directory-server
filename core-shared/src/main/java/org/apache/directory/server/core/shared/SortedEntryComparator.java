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
package org.apache.directory.server.core.shared;


import java.io.Serializable;
import java.util.Comparator;
import java.util.TreeSet;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.LdapComparator;
import org.apache.directory.api.ldap.model.schema.MatchingRule;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.comparators.ParsedDnComparator;

/**
 * A comparator to sort the entries as per <a href="http://tools.ietf.org/html/rfc2891">RFC 2891</a>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
class SortedEntryComparator implements Comparator<Entry>, Serializable
{

    /** the attribute's type */
    private transient AttributeType type;

    /** comparator used for comparing the values of the given attribute type */
    private transient LdapComparator comparator;

    /** flag to indicate if the attribute type is multivalued */
    private boolean multivalued;

    /** flag for indicating the order of sorting */
    private boolean reverse;

    /** flag to indicate if the attribute is human readable or binary */
    private boolean hr;


    /**
     * 
     * Creates a new instance of SortedEntryComparator.
     *
     * @param at the attribute's type
     * @param mrOid the OID or name of the matchingrule
     * @param reverse flag to indicate the sort order
     */
    public SortedEntryComparator( AttributeType at, String mrule, boolean reverse, SchemaManager schemaManager ) throws LdapException
    {
        this.type = at;
        this.reverse = reverse;

        if ( !at.isSingleValued() )
        {
            multivalued = true;
        }
        
        // Special case : entryDn
        if ( SchemaConstants.ENTRY_DN_AT_OID.equals( at.getOid() ) )
        {
            // We will use the Entry's DN comparator.
            comparator = new ParsedDnComparator( SchemaConstants.ENTRY_DN_AT_OID );
            comparator.setSchemaManager( schemaManager );
            hr = true;
        }
        else
        { 
            hr = at.getSyntax().isHumanReadable();
    
            if ( mrule != null )
            {
                comparator = schemaManager.lookupComparatorRegistry( mrule );
            }
            else
            {
                MatchingRule mr = at.getOrdering();
                
                if ( mr == null )
                {
                    mr = at.getEquality();
                }
                
                comparator = schemaManager.lookupComparatorRegistry( mr.getOid() );
            }
            
            comparator.setSchemaManager( schemaManager );
        }
    }


    @Override
    public int compare( Entry entry1, Entry entry2 )
    {
        Attribute at1 = entry1.get( type );

        Attribute at2 = entry2.get( type );

        // as per section 2.2 of the spec null values are considered larger
        if ( at1 == null )
        {
            return reverse ? -1 : 1;
        }
        else if ( at2 == null )
        {
            return reverse ? 1 : -1;
        }

        Object o1;
        Object o2;

        if ( multivalued )
        {
            TreeSet ts = new TreeSet( comparator );

            o1 = sortAndGetFirst( at1, ts );

            ts.clear();
            o2 = sortAndGetFirst( at2, ts );
        }
        else
        {
            Value v1 = at1.get();
            Value v2 = at2.get();

            if ( hr )
            {
                o1 = v1.getValue();
                o2 = v2.getValue();
            }
            else
            {
                o1 = v1.getBytes();
                o2 = v2.getBytes();
            }
        }

        int c;

        try
        {
            if ( reverse )
            {
                c = comparator.compare( comparator.getNormalizer().normalize( ( String ) o2 ), o1 );
            }
            else
            {
                c = comparator.compare( comparator.getNormalizer().normalize( ( String ) o1 ), o2 );
            }
    
            if ( c == 0 )
            {
                return 1;
            }

            return c;
        }
        catch ( LdapException le )
        {
            return 0;
        }
    }


    /**
     * sorts the values of an attribute and picks the least value
     * 
     * @param at the attribute
     * @param ts the TreeSet for sorting 
     * @return the least value among the values of the attribute
     */
    private Object sortAndGetFirst( Attribute at, TreeSet ts )
    {
        for ( Value v : at )
        {
            if ( hr )
            {
                ts.add( v.getNormalized() );
            }
            else
            {
                ts.add( v.getBytes() );
            }
        }

        return ts.first();
    }
}