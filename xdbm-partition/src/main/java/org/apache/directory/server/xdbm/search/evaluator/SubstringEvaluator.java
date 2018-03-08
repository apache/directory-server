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
import java.util.regex.Pattern;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.filter.SubstringNode;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.MatchingRule;
import org.apache.directory.api.ldap.model.schema.Normalizer;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.normalizers.NoOpNormalizer;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.search.Evaluator;


/**
 * Evaluates substring filter assertions on an entry.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SubstringEvaluator implements Evaluator<SubstringNode>
{
    /** Database used while evaluating candidates */
    private final Store db;

    /** Reference to the SchemaManager */
    private final SchemaManager schemaManager;

    /** The Substring expression */
    private final SubstringNode node;

    /** The regular expression generated for the SubstringNode pattern */
    private final Pattern regex;

    /** The AttributeType we will use for the evaluation */
    private final AttributeType attributeType;

    /** The associated normalizer */
    private final Normalizer normalizer;


    /**
     * Creates a new SubstringEvaluator for substring expressions.
     *
     * @param node the substring expression node
     * @param db the database this evaluator uses
     * @param schemaManager the schema manager
     * @throws Exception if there are failures accessing resources and the db
     */
    public SubstringEvaluator( SubstringNode node, Store db, SchemaManager schemaManager ) throws LdapException
    {
        this.db = db;
        this.node = node;
        this.schemaManager = schemaManager;
        this.attributeType = node.getAttributeType();

        MatchingRule rule = attributeType.getSubstring();

        if ( rule == null )
        {
            rule = attributeType.getEquality();
        }

        if ( rule != null )
        {
            normalizer = rule.getNormalizer();
        }
        else
        {
            normalizer = new NoOpNormalizer( attributeType.getSyntaxOid() );
        }

        // compile the regular expression to search for a matching attribute
        // if the attributeType is humanReadable
        if ( attributeType.getSyntax().isHumanReadable() )
        {
            regex = node.getRegex( normalizer );
        }
        else
        {
            regex = null;
        }
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean evaluate( PartitionTxn partitionTxn, IndexEntry<?, String> indexEntryQM ) throws LdapException
    {
        IndexEntry<String, String> indexEntry = ( IndexEntry<String, String> ) indexEntryQM;

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

        /*
         * Don't make a call here to evaluateWithoutIndex( Entry ) for
         * code reuse since we do want to set the value on the indexEntry on
         * matches.
         */

        // get the attribute
        Attribute attr = entry.get( attributeType );

        // if the attribute exists and the pattern matches return true
        if ( attr != null )
        {
            /*
             * Cycle through the attribute values testing normalized version
             * obtained from using the substring matching rule's normalizer.
             * The test uses the comparator obtained from the appropriate
             * substring matching rule.
             */
            if ( attr.isHumanReadable() )
            {
                for ( Value value : attr )
                {
                    String strValue = value.getValue();
                    String normalizedValue = attr.getAttributeType().getEquality().getNormalizer().normalize( strValue );

                    // Once match is found cleanup and return true
                    if ( regex.matcher( normalizedValue ).matches() )
                    {
                        // before returning we set the normalized value
                        indexEntry.setKey( strValue );
                        return true;
                    }
                }
            }
            else
            {
                // Slightly more complex. We won't be able to use a regex to check
                // the value.
                for ( Value value : attr )
                {
                    byte[] byteValue = value.getBytes();

                    // Once match is found cleanup and return true
                    // @TODO : implement this check.
                    /*
                    if ( check( byteValue ) )
                    {
                        // before returning we set the normalized value
                        indexEntry.setKey( byteValue );
                        return true;
                    }
                    */
                }
            }

            // Fall through as we didn't find any matching value for this attribute.
            // We will have to check in the potential descendant, if any.
        }

        // If we do not have the attribute, loop through the descendant
        // May be the node Attribute has descendant ?
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

                if ( null != attr )
                {

                    /*
                     * Cycle through the attribute values testing normalized version
                     * obtained from using the substring matching rule's normalizer.
                     * The test uses the comparator obtained from the appropriate
                     * substring matching rule.
                     */
                    for ( Value value : attr )
                    {
                        String strValue = value.getValue();
                        String normalizedValue = attr.getAttributeType().getEquality().getNormalizer().normalize( strValue );

                        // Once match is found cleanup and return true
                        if ( regex.matcher( normalizedValue ).matches() )
                        {
                            // before returning we set the normalized value
                            indexEntry.setKey( strValue );
                            return true;
                        }
                    }
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
    public boolean evaluate( Entry entry ) throws LdapException
    {
        // get the attribute
        Attribute attr = entry.get( attributeType );

        // if the attribute exists and the pattern matches return true
        if ( attr != null )
        {
            /*
             * Cycle through the attribute values testing normalized version
             * obtained from using the substring matching rule's normalizer.
             * The test uses the comparator obtained from the appropriate
             * substring matching rule.
             */
            for ( Value value : attr )
            {
                String strValue = value.getValue();

                // Once match is found cleanup and return true
                if ( regex.matcher( strValue ).matches() )
                {
                    return true;
                }
            }

            // Fall through as we didn't find any matching value for this attribute.
            // We will have to check in the potential descendant, if any.
        }

        // If we do not have the attribute, loop through the descendant
        // May be the node Attribute has descendant ?
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

                if ( null != attr )
                {

                    /*
                     * Cycle through the attribute values testing normalized version
                     * obtained from using the substring matching rule's normalizer.
                     * The test uses the comparator obtained from the appropriate
                     * substring matching rule.
                     */
                    for ( Value value : attr )
                    {
                        String strValue = value.getValue();

                        // Once match is found cleanup and return true
                        if ( regex.matcher( strValue ).matches() )
                        {
                            return true;
                        }
                    }
                }
            }
        }

        // we fell through so a match was not found - assertion was false.
        return false;
    }


    public Pattern getPattern()
    {
        return regex;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public SubstringNode getExpression()
    {
        return node;
    }


    /**
     * @see Object#toString()
     */
    @Override
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "SubstringEvaluator : " ).append( node ).append( "\n" );

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