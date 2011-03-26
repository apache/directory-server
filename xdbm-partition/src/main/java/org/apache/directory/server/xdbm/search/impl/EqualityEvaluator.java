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
package org.apache.directory.server.xdbm.search.impl;


import java.util.Comparator;
import java.util.Iterator;

import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.EntryAttribute;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.filter.EqualityNode;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.MatchingRule;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.model.schema.comparators.ByteArrayComparator;
import org.apache.directory.shared.ldap.model.schema.comparators.StringComparator;
import org.apache.directory.shared.ldap.model.schema.normalizers.NoOpNormalizer;
import org.apache.directory.shared.util.Strings;


/**
 * An Evaluator which determines if candidates are matched by GreaterEqNode
 * assertions.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EqualityEvaluator<T, ID extends Comparable<ID>> extends LeafEvaluator<T, ID>
{
    /** The default byte[] comparator if no comparator has been defined */
    private static final Comparator<byte[]> BINARY_COMPARATOR = new ByteArrayComparator( null );

    /** The default String comparator if no comparator has been defined */
    private static final Comparator<String> STRING_COMPARATOR = new StringComparator( null );


    @SuppressWarnings("unchecked")
    public EqualityEvaluator( EqualityNode<T> node, Store<Entry, ID> db, SchemaManager schemaManager )
        throws Exception
    {
        super( node, db, schemaManager );

        if ( db.hasIndexOn( attributeType ) )
        {
            idx = ( Index<T, Entry, ID> ) db.getIndex( attributeType );
            normalizer = null;
            ldapComparator = null;
        }
        else
        {
            idx = null;

            MatchingRule mr = attributeType.getEquality();

            if ( mr == null )
            {
                normalizer = new NoOpNormalizer( attributeType.getOid() );
                ldapComparator = null;
            }
            else
            {
                normalizer = mr.getNormalizer();
                ldapComparator = mr.getLdapComparator();
            }
        }
    }


    public EqualityNode<T> getExpression()
    {
        return (EqualityNode<T>)node;
    }


    public boolean evaluate( IndexEntry<?, Entry, ID> indexEntry ) throws Exception
    {
        if ( idx != null )
        {
            return idx.forward( node.getValue().get(), indexEntry.getId() );
        }

        Entry entry = indexEntry.getObject();

        // resuscitate the entry if it has not been and set entry in IndexEntry
        if ( null == entry )
        {
            entry = db.lookup( indexEntry.getId() );
            indexEntry.setObject( entry );
        }

        return evaluateEntry( entry );
    }


    public boolean evaluateEntry( Entry entry ) throws Exception
    {
        // get the attribute
        EntryAttribute attr = entry.get( attributeType );

        // if the attribute does not exist just return false
        if ( ( attr != null ) && evaluate( attr ) )
        {
            return true;
        }

        // If we do not have the attribute, loop through the sub classes of
        // the attributeType.  Perhaps the entry has an attribute value of a
        // subtype (descendant) that will produce a match
        if ( schemaManager.getAttributeTypeRegistry().hasDescendants( attributeType ) )
        {
            // TODO check to see if descendant handling is necessary for the
            // index so we can match properly even when for example a name
            // attribute is used instead of more specific commonName
            Iterator<AttributeType> descendants = schemaManager.getAttributeTypeRegistry().descendants( attributeType );

            while ( descendants.hasNext() )
            {
                AttributeType descendant = descendants.next();

                attr = entry.get( descendant );

                if ( ( attr != null ) && evaluate( attr ) )
                {
                    return true;
                }
            }
        }

        // we fell through so a match was not found - assertion was false.
        return false;
    }


    public boolean evaluateId( ID id ) throws Exception
    {
        if ( idx != null )
        {
            return idx.reverse( id );
        }

        return evaluateEntry( db.lookup( id ) );
    }


    // TODO - determine if comparator and index entry should have the Value
    // wrapper or the raw normalized value
    private boolean evaluate( EntryAttribute attribute ) throws Exception
    {
        /*
         * Cycle through the attribute values testing normalized version
         * obtained from using the ordering or equality matching rule's
         * normalizer.  The test uses the comparator obtained from the
         * appropriate matching rule to perform the check.
         */
        for ( Value<?> value : attribute )
        {
            //noinspection unchecked
            if ( value.isHR() )
            {
                // Deal with a String value
                String serverValue = ( ( Value<String> ) value ).getNormValue();
                String nodeValue = null;

                if ( node.getValue().isHR() )
                {
                    nodeValue = ( ( Value<String> ) node.getValue() ).getNormValue();
                }
                else
                {
                    nodeValue = Strings.utf8ToString(((Value<byte[]>) node.getValue()).getNormValue());
                }

                if ( ldapComparator != null )
                {
                    if ( ldapComparator.compare( serverValue, nodeValue ) == 0 )
                    {
                        return true;
                    }
                }
                else
                {
                    if ( STRING_COMPARATOR.compare( serverValue, nodeValue ) == 0 )
                    {
                        return true;
                    }
                }
            }
            else
            {
                // Deal with a binary value
                byte[] serverValue = ( (Value<byte[]>) value ).getNormValue();
                byte[] nodeValue = ( ( Value<byte[]> ) node.getValue() ).getNormValue();

                if ( ldapComparator != null )
                {
                    if ( ldapComparator.compare( (Object)serverValue, (Object)nodeValue ) == 0 )
                    {
                        return true;
                    }
                }
                else
                {
                    if ( BINARY_COMPARATOR.compare( serverValue, nodeValue ) == 0 )
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}