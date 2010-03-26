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


import java.util.Iterator;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.filter.LessEqNode;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.LdapComparator;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.SchemaManager;


/**
 * An Evaluator which determines if candidates are matched by LessEqNode
 * assertions.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class LessEqEvaluator<T, ID> implements Evaluator<LessEqNode<T>, ServerEntry, ID>
{
    private final LessEqNode<T> node;
    private final Store<ServerEntry, ID> db;
    private final SchemaManager schemaManager;
    private final AttributeType type;
    private final Normalizer normalizer;
    private final LdapComparator<? super Object> ldapComparator;
    private final Index<T, ServerEntry, ID> idx;


    @SuppressWarnings("unchecked")
    public LessEqEvaluator( LessEqNode<T> node, Store<ServerEntry, ID> db, SchemaManager schemaManager )
        throws Exception
    {
        this.db = db;
        this.node = node;
        this.schemaManager = schemaManager;
        this.type = schemaManager.lookupAttributeTypeRegistry( node.getAttribute() );

        if ( db.hasIndexOn( node.getAttribute() ) )
        {
            idx = ( Index<T, ServerEntry, ID> ) db.getIndex( node.getAttribute() );
        }
        else
        {
            idx = null;
        }

        /*
         * We prefer matching using the Normalizer and Comparator pair from
         * the ordering matchingRule if one is available.  It may very well
         * not be.  If so then we resort to using the Normalizer and
         * Comparator from the equality matchingRule as a last resort.
         */
        MatchingRule mr = type.getOrdering();

        if ( mr == null )
        {
            mr = type.getEquality();
        }

        if ( mr == null )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_717, node ) );
        }

        normalizer = mr.getNormalizer();
        ldapComparator = mr.getLdapComparator();
    }


    public LessEqNode<T> getExpression()
    {
        return node;
    }


    public AttributeType getAttributeType()
    {
        return type;
    }


    public Normalizer getNormalizer()
    {
        return normalizer;
    }


    public LdapComparator<? super Object> getLdapComparator()
    {
        return ldapComparator;
    }


    public boolean evaluateId( ID id ) throws Exception
    {
        if ( idx != null )
        {
            return idx.reverseLessOrEq( id, node.getValue().get() );
        }

        return evaluateEntry( db.lookup( id ) );
    }


    public boolean evaluate( IndexEntry<?, ServerEntry, ID> indexEntry ) throws Exception
    {
        if ( idx != null )
        {
            return idx.reverseLessOrEq( indexEntry.getId(), node.getValue().get() );
        }

        ServerEntry entry = indexEntry.getObject();

        // resuscitate the entry if it has not been and set entry in IndexEntry
        if ( null == entry )
        {
            entry = db.lookup( indexEntry.getId() );
            indexEntry.setObject( entry );
        }

        if ( null == entry )
        {
            return false;
        }

        // get the attribute
        EntryAttribute attr = entry.get( type );

        // if the attribute does not exist just return false
        //noinspection unchecked
        if ( attr != null && evaluate( ( IndexEntry<Object, ServerEntry, ID> ) indexEntry, attr ) )
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

                //noinspection unchecked
                if ( attr != null && evaluate( ( IndexEntry<Object, ServerEntry, ID> ) indexEntry, attr ) )
                {
                    return true;
                }
            }
        }

        // we fell through so a match was not found - assertion was false.
        return false;
    }


    public boolean evaluateEntry( ServerEntry entry ) throws Exception
    {
        // get the attribute
        EntryAttribute attr = entry.get( type );

        // if the attribute does not exist just return false
        if ( attr != null && evaluate( null, attr ) )
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

                if ( attr != null && evaluate( null, attr ) )
                {
                    return true;
                }
            }
        }

        // we fell through so a match was not found - assertion was false.
        return false;
    }


    // TODO - determine if comaparator and index entry should have the Value
    // wrapper or the raw normalized value
    private boolean evaluate( IndexEntry<Object, ServerEntry, ID> indexEntry, EntryAttribute attribute )
        throws Exception
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
            if ( ldapComparator.compare( value.getNormalizedValue(), node.getValue().getNormalizedValue() ) <= 0 )
            {
                if ( indexEntry != null )
                {
                    indexEntry.setValue( value.getNormalizedValue() );
                }
                return true;
            }
        }

        return false;
    }
}