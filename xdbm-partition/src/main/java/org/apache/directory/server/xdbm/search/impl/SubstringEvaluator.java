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
import java.util.regex.Pattern;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.filter.SubstringNode;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.normalizers.NoOpNormalizer;


/**
 * Evaluates substring filter assertions on an entry.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SubstringEvaluator<ID extends Comparable<ID>> implements Evaluator<SubstringNode, Entry, ID>
{
    /** Database used while evaluating candidates */
    private final Store<Entry, ID> db;

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

    /** The index to use if any */
    private final Index<String, Entry, ID> idx;


    /**
     * Creates a new SubstringEvaluator for substring expressions.
     *
     * @param node the substring expression node
     * @param db the database this evaluator uses
     * @param schemaManager the schema manager
     * @throws Exception if there are failures accessing resources and the db
     */
    @SuppressWarnings("unchecked")
    public SubstringEvaluator( SubstringNode node, Store<Entry, ID> db, SchemaManager schemaManager )
        throws Exception
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

        if ( db.hasIndexOn( attributeType ) )
        {
            idx = ( Index<String, Entry, ID> ) db.getIndex( attributeType );
        }
        else
        {
            idx = null;
        }
    }


    @SuppressWarnings("unchecked")
    public boolean evaluate( IndexEntry<?, Entry, ID> indexEntry ) throws Exception
    {

        if ( idx == null )
        {
            return evaluateWithoutIndex( ( IndexEntry<String, Entry, ID> ) indexEntry );
        }
        else
        {
            return evaluateWithIndex( indexEntry );
        }
    }


    public boolean evaluateId( ID id ) throws Exception
    {

        if ( idx == null )
        {
            return evaluateWithoutIndex( id );
        }
        else
        {
            return evaluateWithIndex( id );
        }
    }


    public boolean evaluateEntry( Entry entry ) throws Exception
    {

        if ( idx == null )
        {
            //noinspection unchecked
            return evaluateWithoutIndex( entry );
        }
        else
        {
            return evaluateWithIndex( );
        }
    }


    public Pattern getPattern()
    {
        return regex;
    }


    public SubstringNode getExpression()
    {
        return node;
    }


    private boolean evaluateWithIndex( IndexEntry<?, Entry, ID> indexEntry ) throws Exception
    {
        /*
         * Note that this is using the reverse half of the index giving a
         * considerable performance improvement on this kind of operation.
         * Otherwise we would have to scan the entire index if there were
         * no reverse lookups.
         */
        Cursor<IndexEntry<String, Entry, ID>> entries = idx.reverseCursor( indexEntry.getId() );

        // cycle through the attribute values testing for a match
        while ( entries.next() )
        {
            IndexEntry<String, Entry, ID> rec = entries.get();

            // once match is found cleanup and return true
            if ( regex.matcher( ( String ) rec.getValue() ).matches() )
            {
                entries.close();
                return true;
            }
        }

        // we fell through so a match was not found - assertion was false.
        return false;
    }


    private boolean evaluateWithIndex( ) throws Exception
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_721 ) );
    }


    private boolean evaluateWithIndex( ID id ) throws Exception
    {
        /*
         * Note that this is using the reverse half of the index giving a
         * considerable performance improvement on this kind of operation.
         * Otherwise we would have to scan the entire index if there were
         * no reverse lookups.
         */
        Cursor<IndexEntry<String, Entry, ID>> entries = idx.reverseCursor( id );

        // cycle through the attribute values testing for a match
        while ( entries.next() )
        {
            IndexEntry<String, Entry, ID> rec = entries.get();

            // once match is found cleanup and return true
            if ( regex.matcher( ( String ) rec.getValue() ).matches() )
            {
                entries.close();
                return true;
            }
        }

        // we fell through so a match was not found - assertion was false.
        return false;
    }


    // TODO - determine if comaparator and index entry should have the Value
    // wrapper or the raw normalized value
    private boolean evaluateWithoutIndex( ID id ) throws Exception
    {
        return evaluateWithoutIndex( db.lookup( id ) );
    }


    // TODO - determine if comaparator and index entry should have the Value
    // wrapper or the raw normalized value
    private boolean evaluateWithoutIndex( Entry entry ) throws Exception
    {
        // get the attribute
        EntryAttribute attr = entry.get( attributeType );

        // if the attribute exists and the pattern matches return true
        if ( attr != null )
        {
            /*
             * Cycle through the attribute values testing normalized version
             * obtained from using the substring matching rule's normalizer.
             * The test uses the comparator obtained from the appropriate
             * substring matching rule.
             */
            for ( Value<?> value : attr )
            {
                value.normalize( normalizer );
                String strValue = ( String ) value.getNormalizedValue();

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
                    for ( Value<?> value : attr )
                    {
                        value.normalize( normalizer );
                        String strValue = ( String ) value.getNormalizedValue();

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


    // TODO - determine if comaparator and index entry should have the Value
    // wrapper or the raw normalized value
    private boolean evaluateWithoutIndex( IndexEntry<String, Entry, ID> indexEntry ) throws Exception
    {
        Entry entry = indexEntry.getObject();

        // resuscitate the entry if it has not been and set entry in IndexEntry
        if ( null == entry )
        {
            entry = db.lookup( indexEntry.getId() );
            indexEntry.setObject( entry );
        }

        /*
         * Don't make a call here to evaluateWithoutIndex( Entry ) for
         * code reuse since we do want to set the value on the indexEntry on
         * matches.
         */

        // get the attribute
        EntryAttribute attr = entry.get( attributeType );

        // if the attribute exists and the pattern matches return true
        if ( attr != null )
        {
            /*
             * Cycle through the attribute values testing normalized version
             * obtained from using the substring matching rule's normalizer.
             * The test uses the comparator obtained from the appropriate
             * substring matching rule.
             */
            if ( attr.isHR() )
            {
                for ( Value<?> value : attr )
                {
                    value.normalize( normalizer );
                    String strValue = ( String ) value.getNormalizedValue();
    
                    // Once match is found cleanup and return true
                    if ( regex.matcher( strValue ).matches() )
                    {
                        // before returning we set the normalized value
                        indexEntry.setValue( strValue );
                        return true;
                    }
                }
            }
            else
            {
                // Slightly more complex. We won't be able to use a regex to check
                // the value.
                for ( Value<?> value : attr )
                {
                    value.normalize( normalizer );
                    byte[] byteValue = (byte[])value.getNormalizedValue();
    
                    // Once match is found cleanup and return true
                    // @TODO : implement this check.
                    /*
                    if ( check( byteValue ) )
                    {
                        // before returning we set the normalized value
                        indexEntry.setValue( byteValue );
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
                    for ( Value<?> value : attr )
                    {
                        value.normalize( normalizer );
                        String strValue = ( String ) value.getNormalizedValue();

                        // Once match is found cleanup and return true
                        if ( regex.matcher( strValue ).matches() )
                        {
                            // before returning we set the normalized value
                            indexEntry.setValue( strValue );
                            return true;
                        }
                    }
                }
            }
        }

        // we fell through so a match was not found - assertion was false.
        return false;
    }
}