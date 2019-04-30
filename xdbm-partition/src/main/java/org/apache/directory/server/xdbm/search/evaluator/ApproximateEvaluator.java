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
package org.apache.directory.server.xdbm.search.evaluator;


import java.util.Iterator;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.filter.ApproximateNode;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.MatchingRule;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.IndexNotFoundException;
import org.apache.directory.server.xdbm.Store;


/**
 * An Evaluator which determines if candidates are matched by ApproximateNode
 * assertions.  Same as equality for now.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ApproximateEvaluator<T> extends LeafEvaluator<T>
{
    /**
     * Creates a new ApproximateEvaluator
     * 
     * @param node The ApproximateNode
     * @param db The Store
     * @param schemaManager The SchemaManager
     * @throws LdapException If the creation failed
     */
    public ApproximateEvaluator( ApproximateNode<T> node, Store db, SchemaManager schemaManager )
        throws LdapException
    {
        super( node, db, schemaManager );

        if ( db.hasIndexOn( attributeType ) )
        {
            try
            {
                idx = ( Index<T, String> ) db.getIndex( attributeType );
            }
            catch ( IndexNotFoundException infe )
            {
                throw new LdapOtherException( infe.getMessage(), infe );
            }
            normalizer = null;
            ldapComparator = null;
        }
        else
        {
            idx = null;

            MatchingRule mr = attributeType.getEquality();

            if ( mr == null )
            {
                throw new IllegalStateException( I18n.err( I18n.ERR_709, node ) );
            }

            normalizer = mr.getNormalizer();
            ldapComparator = mr.getLdapComparator();
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ApproximateNode<T> getExpression()
    {
        return ( ApproximateNode<T> ) node;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean evaluate( Entry entry ) throws LdapException
    {
        // get the attribute
        Attribute attr = entry.get( attributeType );

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

                if ( attr != null && evaluate( attr ) )
                {
                    return true;
                }
            }
        }

        // we fell through so a match was not found - assertion was false.
        return false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean evaluate( PartitionTxn partitionTxn, IndexEntry<?, String> indexEntry ) throws LdapException
    {
        Entry entry = indexEntry.getEntry();

        // resuscitate the entry if it has not been and set entry in IndexEntry
        if ( null == entry )
        {
            entry = db.fetch( partitionTxn, indexEntry.getId() );

            if ( null == entry )
            {
                // The entry is not anymore present : get out
                return false;
            }

            indexEntry.setEntry( entry );
        }

        return evaluate( entry );
    }


    // TODO - determine if comparator and index entry should have the Value
    // wrapper or the raw normalized value
    private boolean evaluate( Attribute attribute )
    {
        /*
         * Cycle through the attribute values testing normalized version
         * obtained from using the ordering or equality matching rule's
         * normalizer.  The test uses the comparator obtained from the
         * appropriate matching rule to perform the check.
         */

        for ( Value value : attribute )
        {
            if ( value.compareTo( node.getValue() )  == 0 )
            {
                return true;
            }
        }

        return false;
    }


    /**
     * @see Object#toString()
     */
    @Override
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "ApproximateEvaluator : " ).append( super.toString() ).append( "\n" );

        return sb.toString();
    }


    /**
     * @see Object#toString()
     */
    @Override
    public String toString()
    {
        return toString( "" );
    }
}