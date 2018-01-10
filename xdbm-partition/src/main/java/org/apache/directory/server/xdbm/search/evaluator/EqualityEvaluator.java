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
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.MatchingRule;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.normalizers.NoOpNormalizer;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;


/**
 * An Evaluator which determines if candidates are matched by GreaterEqNode
 * assertions.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EqualityEvaluator<T> extends LeafEvaluator<T>
{
    @SuppressWarnings("unchecked")
    public EqualityEvaluator( EqualityNode<T> node, Store db, SchemaManager schemaManager )
        throws Exception
    {
        super( node, db, schemaManager );

        if ( db.hasIndexOn( attributeType ) )
        {
            idx = ( Index<T, String> ) db.getIndex( attributeType );
        }

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


    /**
     * {@inheritDoc}
     */
    @Override
    public EqualityNode<T> getExpression()
    {
        return ( EqualityNode<T> ) node;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean evaluate( IndexEntry<?, String> indexEntry ) throws LdapException
    {
        Entry entry = indexEntry.getEntry();

        // resuscitate the entry if it has not been and set entry in IndexEntry
        if ( null == entry )
        {
            entry = db.fetch( indexEntry.getId() );

            if ( null == entry )
            {
                // The entry is not anymore present : get out
                return false;
            }

            indexEntry.setEntry( entry );
        }

        return evaluate( entry );
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

                if ( ( attr != null ) && evaluate( attr ) )
                {
                    return true;
                }
            }
        }

        // we fell through so a match was not found - assertion was false.
        return false;
    }


    // TODO - determine if comparator and index entry should have the Value
    // wrapper or the raw normalized value
    private boolean evaluate( Attribute attribute ) throws LdapException
    {
        if ( attribute.contains( node.getValue() ) )
        {
            return true;
        }

        /*
         * Cycle through the attribute values testing normalized version
         * obtained from using the ordering or equality matching rule's
         * normalizer.  The test uses the comparator obtained from the
         * appropriate matching rule to perform the check.
         */
        for ( Value value : attribute )
        {
            //noinspection unchecked
            if ( value.isHumanReadable() )
            {
                // Deal with a String value
                String serverValue = value.getNormalized();
                String nodeValue = node.getValue().getNormalized();

                if ( serverValue.compareTo( nodeValue ) == 0 )
                {
                    return true;
                }
            }
            else
            {
                // Deal with a binary value
                byte[] nodeValue = node.getValue().getBytes();

                if ( value.compareTo( nodeValue ) == 0 )
                {
                    return true;
                }
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

        sb.append( tabs ).append( "EqualityEvaluator : " ).append( super.toString() ).append( '\n' );

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