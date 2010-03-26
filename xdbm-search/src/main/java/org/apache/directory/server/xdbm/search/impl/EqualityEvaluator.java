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
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.LdapComparator;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.comparators.ByteArrayComparator;
import org.apache.directory.shared.ldap.schema.comparators.StringComparator;
import org.apache.directory.shared.ldap.schema.normalizers.NoOpNormalizer;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * An Evaluator which determines if candidates are matched by GreaterEqNode
 * assertions.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class EqualityEvaluator<T, ID> implements Evaluator<EqualityNode<T>, ServerEntry, ID>
{
    private final EqualityNode<T> node;
    private final Store<ServerEntry, ID> db;
    private final SchemaManager schemaManager;
    private final AttributeType type;
    private final Normalizer normalizer;

    /** The comparator to use */
    private final LdapComparator<?> comparator;

    /** The default byte[] comparator if no comparator has been defined */
    private static final Comparator<byte[]> BINARY_COMPARATOR = new ByteArrayComparator( null );

    /** The default String comparator if no comparator has been defined */
    private static final Comparator<String> STRING_COMPARATOR = new StringComparator( null );

    private final Index<T, ServerEntry, ID> idx;


    @SuppressWarnings("unchecked")
    public EqualityEvaluator( EqualityNode<T> node, Store<ServerEntry, ID> db, SchemaManager schemaManager )
        throws Exception
    {
        this.db = db;
        this.node = node;
        this.schemaManager = schemaManager;

        if ( db.hasIndexOn( node.getAttribute() ) )
        {
            idx = ( Index<T, ServerEntry, ID> ) db.getIndex( node.getAttribute() );
            type = null;
            normalizer = null;
            comparator = null;
        }
        else
        {
            idx = null;
            type = schemaManager.lookupAttributeTypeRegistry( node.getAttribute() );

            MatchingRule mr = type.getEquality();

            if ( mr == null )
            {
                normalizer = new NoOpNormalizer( type.getOid() );
                comparator = null;
            }
            else
            {
                normalizer = mr.getNormalizer();
                comparator = mr.getLdapComparator();
            }
        }
    }


    public EqualityNode<T> getExpression()
    {
        return node;
    }


    public boolean evaluate( IndexEntry<?, ServerEntry, ID> indexEntry ) throws Exception
    {
        if ( idx != null )
        {
            return idx.forward( node.getValue().get(), indexEntry.getId() );
        }

        ServerEntry entry = indexEntry.getObject();

        // resuscitate the entry if it has not been and set entry in IndexEntry
        if ( null == entry )
        {
            entry = db.lookup( indexEntry.getId() );
            indexEntry.setObject( entry );
        }

        return evaluateEntry( entry );
    }


    public boolean evaluateEntry( ServerEntry entry ) throws Exception
    {
        // get the attribute
        EntryAttribute attr = entry.get( type );

        // if the attribute does not exist just return false
        if ( attr != null && evaluate( attr ) )
        {
            return true;
        }

        // If we do not have the attribute, loop through the sub classes of
        // the attributeType.  Perhaps the entry has an attribute value of a
        // subtype (descendant) that will produce a match
        if ( schemaManager.getAttributeTypeRegistry().hasDescendants( node.getAttribute() ) )
        {
            // TODO check to see if descendant handling is necessary for the
            // index so we can match properly even when for example a name
            // attribute is used instead of more specific commonName
            Iterator<AttributeType> descendants = schemaManager.getAttributeTypeRegistry().descendants(
                node.getAttribute() );

            while ( descendants.hasNext() )
            {
                AttributeType descendant = descendants.next();

                attr = entry.get( descendant );

                if ( attr != null && evaluate( attr ) )
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
            value.normalize( normalizer );

            //noinspection unchecked
            if ( value.isBinary() )
            {
                // Deal with a binary value
                byte[] serverValue = ( ( Value<byte[]> ) value ).getNormalizedValue();
                byte[] nodeValue = ( ( Value<byte[]> ) node.getValue() ).getNormalizedValue();

                if ( comparator != null )
                {
                    if ( ( ( ( LdapComparator<byte[]> ) comparator ).compare( serverValue, nodeValue ) == 0 ) )
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
            else
            {
                // Deal with a String value
                String serverValue = ( ( Value<String> ) value ).getNormalizedValue();
                String nodeValue = null;

                if ( node.getValue().isBinary() )
                {
                    nodeValue = StringTools.utf8ToString( ( ( Value<byte[]> ) node.getValue() ).getNormalizedValue() );
                }
                else
                {
                    nodeValue = ( ( Value<String> ) node.getValue() ).getNormalizedValue();
                }

                if ( comparator != null )
                {
                    if ( ( ( LdapComparator<String> ) comparator ).compare( serverValue, nodeValue ) == 0 )
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
        }

        return false;
    }
}