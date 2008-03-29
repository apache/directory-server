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


import org.apache.directory.shared.ldap.filter.GreaterEqNode;
import org.apache.directory.shared.ldap.util.AttributeUtils;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.schema.registries.Registries;

import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;
import javax.naming.NamingEnumeration;
import java.util.Iterator;
import java.util.Comparator;


/**
 * An Evaluator which determines if candidates are matched by GreaterEqNode
 * assertions.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class GreaterEqEvaluator implements Evaluator<GreaterEqNode, Attributes>
{
    private final GreaterEqNode node;
    private final Store<Attributes> db;
    private final Registries registries;
    private final AttributeType type;
    private final Normalizer normalizer;
    private final Comparator comparator;
    private final Index<Number,Attributes> idx;


    public GreaterEqEvaluator( GreaterEqNode node, Store<Attributes> db, Registries registries )
        throws Exception
    {
        this.db = db;
        this.node = node;
        this.registries = registries;
        this.type = registries.getAttributeTypeRegistry().lookup( node.getAttribute() );

        if ( db.hasUserIndexOn( node.getAttribute() ) )
        {
            //noinspection unchecked
            idx = db.getUserIndex( node.getAttribute() );
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
            throw new IllegalStateException(
                "Could not find matchingRule to use for GreaterEqNode evaluation: " + node );
        }

        normalizer = mr.getNormalizer();
        comparator = mr.getComparator();
    }


    public GreaterEqNode getExpression()
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


    public Comparator getComparator()
    {
        return comparator;
    }


    public boolean evaluate( IndexEntry<?,Attributes> indexEntry ) throws Exception
    {
        if ( idx != null )
        {
            return idx.hasGreaterOrEqual( ( Number ) indexEntry.getValue(), indexEntry.getId() );
        }

        Attributes entry = indexEntry.getObject();

        // resuscitate the entry if it has not been and set entry in IndexEntry
        if ( null == entry )
        {
            entry = db.lookup( indexEntry.getId() );
            indexEntry.setObject( entry );
        }

        // get the attribute
        Attribute attr = AttributeUtils.getAttribute( entry, type );

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

                attr = AttributeUtils.getAttribute( entry, descendant );

                if ( attr != null && evaluate( attr ) )
                {
                    return true;
                }
            }
        }

        // we fell through so a match was not found - assertion was false.
        return false;
    }


    private boolean evaluate( Attribute attribute ) throws Exception
    {
        /*
         * Cycle through the attribute values testing normalized version
         * obtained from using the ordering or equality matching rule's
         * normalizer.  The test uses the comparator obtained from the
         * appropriate matching rule to perform the check.
         */
        NamingEnumeration values = attribute.getAll();

        while ( values.hasMore() )
        {
            Object value = normalizer.normalize( values.next() );

            // Once match is found cleanup and return true
            //noinspection unchecked
            if ( comparator.compare( value, node.getValue() ) >= 0 )
            {
                values.close();
                return true;
            }
        }

        return false;
    }
}
