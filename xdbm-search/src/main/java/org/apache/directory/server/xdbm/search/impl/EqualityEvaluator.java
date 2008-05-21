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


import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.entry.ServerAttribute;

import java.util.Iterator;
import java.util.Comparator;


/**
 * An Evaluator which determines if candidates are matched by GreaterEqNode
 * assertions.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class EqualityEvaluator<T> implements Evaluator<EqualityNode<T>, ServerEntry>
{
    private final EqualityNode<T> node;
    private final Store<ServerEntry> db;
    private final Registries registries;
    private final AttributeType type;
    private final Normalizer normalizer;
    private final Comparator comparator;
    private final Index<T,ServerEntry> idx;


    public EqualityEvaluator( EqualityNode<T> node, Store<ServerEntry> db, Registries registries )
        throws Exception
    {
        this.db = db;
        this.node = node;
        this.registries = registries;

        if ( db.hasUserIndexOn( node.getAttribute() ) )
        {
            //noinspection unchecked
            idx = ( Index<T, ServerEntry> ) db.getUserIndex( node.getAttribute() );
            type = null;
            normalizer = null;
            comparator = null;
        }
        else
        {
            idx = null;
            type = registries.getAttributeTypeRegistry().lookup( node.getAttribute() );

            MatchingRule mr = type.getEquality();

            if ( mr == null )
            {
                throw new IllegalStateException(
                    "Could not find matchingRule to use for EqualityNode evaluation: " + node );
            }

            normalizer = mr.getNormalizer();
            comparator = mr.getComparator();
        }
    }


    public EqualityNode<T> getExpression()
    {
        return node;
    }


    public boolean evaluate( IndexEntry<?,ServerEntry> indexEntry ) throws Exception
    {
        if ( idx != null )
        {
            return idx.forward( ( T ) indexEntry.getValue(), indexEntry.getId() );
        }

        ServerEntry entry = indexEntry.getObject();

        // resuscitate the entry if it has not been and set entry in IndexEntry
        if ( null == entry )
        {
            entry = db.lookup( indexEntry.getId() );
            indexEntry.setObject( entry );
        }

        return evaluate( entry );
    }


    public boolean evaluate( ServerEntry entry ) throws Exception
    {
        // get the attribute
        ServerAttribute attr = ( ServerAttribute ) entry.get( type );

        // if the attribute does not exist just return false
        if ( attr != null && evaluate( attr ) )
        {
            return true;
        }

        // If we do not have the attribute, loop through the sub classes of
        // the attributeType.  Perhaps the entry has an attribute value of a
        // subtype (descendant) that will produce a match
        if ( registries.getAttributeTypeRegistry().hasDescendants( node.getAttribute() ) )
        {
            // TODO check to see if descendant handling is necessary for the
            // index so we can match properly even when for example a name
            // attribute is used instead of more specific commonName
            Iterator<AttributeType> descendants =
                registries.getAttributeTypeRegistry().descendants( node.getAttribute() );

            while ( descendants.hasNext() )
            {
                AttributeType descendant = descendants.next();

                attr = ( ServerAttribute ) entry.get( descendant );

                if ( attr != null && evaluate( attr ) )
                {
                    return true;
                }
            }
        }

        // we fell through so a match was not found - assertion was false.
        return false;
    }


    public boolean evaluate( Long id ) throws Exception
    {
        if ( idx != null )
        {
            return idx.reverse( id );
        }

        return evaluate ( db.lookup( id ) );
    }


    // TODO - determine if comaparator and index entry should have the Value
    // wrapper or the raw normalized value
    private boolean evaluate( ServerAttribute attribute ) throws Exception
    {
        /*
         * Cycle through the attribute values testing normalized version
         * obtained from using the ordering or equality matching rule's
         * normalizer.  The test uses the comparator obtained from the
         * appropriate matching rule to perform the check.
         */
        for ( Value value : attribute )
        {
            value.normalize( normalizer );

            //noinspection unchecked
            if ( comparator.compare( value.getNormalizedValue(), node.getValue().getNormalizedValue() ) == 0 )
            {
                return true;
            }
        }

        return false;
    }
}