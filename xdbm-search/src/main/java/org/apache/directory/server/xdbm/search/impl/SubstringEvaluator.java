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

import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.entry.ServerAttribute;
import org.apache.directory.shared.ldap.filter.SubstringNode;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.normalizers.NoOpNormalizer;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.cursor.Cursor;
import org.apache.directory.shared.ldap.entry.Value;


/**
 * Evaluates substring filter assertions on an entry.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SubstringEvaluator implements Evaluator<SubstringNode, ServerEntry>
{
    /** Database used while evaluating candidates */
    private final Store<ServerEntry> db;
    
    /** Oid Registry used to translate attributeIds to OIDs */
    private final Registries registries;

    /** The Substring expression */
    private final SubstringNode node;

    /** The regular expression generated for the SubstringNode pattern */
    private final Pattern regex;

    private final AttributeType type;

    private final Normalizer normalizer;

    private final Index<String,ServerEntry> idx;


    /**
     * Creates a new SubstringEvaluator for substring expressions.
     *
     * @param node the substring expression node
     * @param db the database this evaluator uses
     * @param registries the set of registries
     * @throws Exception if there are failures accessing resources and the db
     */
    public SubstringEvaluator( SubstringNode node, Store<ServerEntry> db, Registries registries ) throws Exception
    {
        this.db = db;
        this.node = node;
        this.registries = registries;

        String oid = registries.getAttributeTypeRegistry().getOid( node.getAttribute() );
        type = registries.getAttributeTypeRegistry().lookup( oid );

        MatchingRule rule = type.getSubstr();

        if ( rule == null )
        {
            rule = type.getEquality();
        }

        if ( rule != null )
        {
            normalizer = rule.getNormalizer();
        }
        else
        {
            normalizer = new NoOpNormalizer( type.getSyntaxOid() );
        }
        
        // compile the regular expression to search for a matching attribute
        regex = node.getRegex( normalizer );

        if ( db.hasUserIndexOn( node.getAttribute() ) )
        {
            //noinspection unchecked
            idx = ( Index<String,ServerEntry> ) db.getUserIndex( node.getAttribute() );
        }
        else
        {
            idx = null;
        }
    }


    public boolean evaluate( IndexEntry<?,ServerEntry> indexEntry ) throws Exception
    {

        if ( idx == null )
        {
            //noinspection unchecked
            return evaluateWithoutIndex( ( IndexEntry<String,ServerEntry> ) indexEntry );
        }
        else
        {
            return evaluateWithIndex( indexEntry );
        }
    }


    public boolean evaluate( Long id ) throws Exception
    {

        if ( idx == null )
        {
            //noinspection unchecked
            return evaluateWithoutIndex( id );
        }
        else
        {
            return evaluateWithIndex( id );
        }
    }


    public boolean evaluate( ServerEntry entry ) throws Exception
    {

        if ( idx == null )
        {
            //noinspection unchecked
            return evaluateWithoutIndex( entry );
        }
        else
        {
            return evaluateWithIndex( entry );
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


    private boolean evaluateWithIndex( IndexEntry<?,ServerEntry> indexEntry ) throws Exception
    {
        /*
         * Note that this is using the reverse half of the index giving a
         * considerable performance improvement on this kind of operation.
         * Otherwise we would have to scan the entire index if there were
         * no reverse lookups.
         */
        Cursor<IndexEntry<String,ServerEntry>> entries = idx.reverseCursor( indexEntry.getId() );

        // cycle through the attribute values testing for a match
        while ( entries.next() )
        {
            IndexEntry rec = entries.get();

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


    @SuppressWarnings( { "UnusedDeclaration" } )
    private boolean evaluateWithIndex( ServerEntry entry ) throws Exception
    {
        throw new UnsupportedOperationException( "This is too inefficient without getId() on ServerEntry" );
    }


    private boolean evaluateWithIndex( Long id ) throws Exception
    {
        /*
         * Note that this is using the reverse half of the index giving a
         * considerable performance improvement on this kind of operation.
         * Otherwise we would have to scan the entire index if there were
         * no reverse lookups.
         */
        Cursor<IndexEntry<String,ServerEntry>> entries = idx.reverseCursor( id );

        // cycle through the attribute values testing for a match
        while ( entries.next() )
        {
            IndexEntry rec = entries.get();

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
    private boolean evaluateWithoutIndex( Long id ) throws Exception
    {
        return evaluateWithoutIndex ( db.lookup( id ) );
    }


    // TODO - determine if comaparator and index entry should have the Value
    // wrapper or the raw normalized value
    private boolean evaluateWithoutIndex( ServerEntry entry ) throws Exception
    {
        // get the attribute
        ServerAttribute attr = ( ServerAttribute ) entry.get( type );

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
    private boolean evaluateWithoutIndex( IndexEntry<String,ServerEntry> indexEntry ) throws Exception
    {
        ServerEntry entry = indexEntry.getObject();

        // resuscitate the entry if it has not been and set entry in IndexEntry
        if ( null == entry )
        {
            entry = db.lookup( indexEntry.getId() );
            indexEntry.setObject( entry );
        }

        /*
         * Don't make a call here to evaluateWithoutIndex( ServerEntry ) for
         * code reuse since we do want to set the value on the indexEntry on
         * matches.
         */

        // get the attribute
        ServerAttribute attr = ( ServerAttribute ) entry.get( type );

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

            // Fall through as we didn't find any matching value for this attribute.
            // We will have to check in the potential descendant, if any.
        }

        // If we do not have the attribute, loop through the descendant
        // May be the node Attribute has descendant ?
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