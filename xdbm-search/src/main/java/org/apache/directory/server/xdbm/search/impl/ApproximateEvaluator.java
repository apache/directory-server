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
import org.apache.directory.shared.ldap.filter.ApproximateNode;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.LdapComparator;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.SchemaManager;


/**
 * An Evaluator which determines if candidates are matched by ApproximateNode
 * assertions.  Same as equality for now.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ApproximateEvaluator<T, ID> implements Evaluator<ApproximateNode<T>, ServerEntry, ID>
{
    private final ApproximateNode<T> node;
    private final Store<ServerEntry, ID> db;
    private final SchemaManager schemaManager;
    private final AttributeType type;
    private final Normalizer normalizer;
    private final LdapComparator<? super Object> ldapComparator;
    private final Index<T, ServerEntry, ID> idx;


    @SuppressWarnings("unchecked")
    public ApproximateEvaluator( ApproximateNode<T> node, Store<ServerEntry, ID> db, SchemaManager schemaManager )
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
            ldapComparator = null;
        }
        else
        {
            idx = null;
            type = schemaManager.lookupAttributeTypeRegistry( node.getAttribute() );

            MatchingRule mr = type.getEquality();

            if ( mr == null )
            {
                throw new IllegalStateException( I18n.err( I18n.ERR_709, node ) );
            }

            normalizer = mr.getNormalizer();
            ldapComparator = mr.getLdapComparator();
        }
    }


    public ApproximateNode<T> getExpression()
    {
        return node;
    }


    public boolean evaluateEntry( ServerEntry entry ) throws Exception
    {
        // get the attribute
        EntryAttribute attr = entry.get( type );

        // if the attribute does not exist just return false
        if ( ( attr != null ) && evaluate( attr ) )
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


    // TODO - determine if comaparator and index entry should have the Value
    // wrapper or the raw normalized value
    private boolean evaluate( EntryAttribute attribute ) throws Exception
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
            if ( ldapComparator.compare( value.getNormalizedValue(), node.getValue().getNormalizedValue() ) == 0 )
            {
                return true;
            }
        }

        return false;
    }
}