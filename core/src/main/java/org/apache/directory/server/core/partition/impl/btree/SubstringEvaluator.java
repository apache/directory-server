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
package org.apache.directory.server.core.partition.impl.btree;


import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.SubstringNode;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.util.AttributeUtils;


/**
 * Evaluates substring filter assertions on an entry.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SubstringEvaluator implements Evaluator
{
    /** Database used while evaluating candidates */
    private BTreePartition db;
    
    /** Oid Registry used to translate attributeIds to OIDs */
    private OidRegistry oidRegistry;
    
    /** AttributeType registry needed for normalizing and comparing values */
    private AttributeTypeRegistry attributeTypeRegistry;


    /**
     * Creates a new SubstringEvaluator for substring expressions.
     *
     * @param db the database this evaluator uses
     * @param oidRegistry the OID registry for name to OID mapping
     * @param attributeTypeRegistry the attributeType registry
     */
    public SubstringEvaluator( BTreePartition db, OidRegistry oidRegistry,
        AttributeTypeRegistry attributeTypeRegistry )
    {
        this.db = db;
        this.oidRegistry = oidRegistry;
        this.attributeTypeRegistry = attributeTypeRegistry;
    }


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.Evaluator#evaluate(ExprNode, IndexRecord)
     */
    public boolean evaluate( ExprNode node, IndexRecord record ) throws NamingException
    {
        Pattern regex = null;
        SubstringNode snode = ( SubstringNode ) node;
        String filterAttribute = snode.getAttribute();
        
        String oid = oidRegistry.getOid( filterAttribute );
        AttributeType type = attributeTypeRegistry.lookup( oid );

        MatchingRule rule = type.getSubstr();
        
        if ( rule == null )
        {
            rule = type.getEquality();
        }

        Normalizer normalizer = rule.getNormalizer();

        if ( db.hasUserIndexOn( filterAttribute ) )
        {
            Index idx = db.getUserIndex( filterAttribute );

            /*
             * Note that this is using the reverse half of the index giving a 
             * considerable performance improvement on this kind of operation.
             * Otherwise we would have to scan the entire index if there were
             * no reverse lookups.
             */

            try
            {
                NamingEnumeration entries = idx.listReverseIndices( record.getEntryId() );
            }
            catch ( java.io.IOException e )
            {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            // compile the regular expression to search for a matching attribute
            try
            {
                regex = snode.getRegex( normalizer );
            }
            catch ( PatternSyntaxException pse )
            {
                NamingException ne = new NamingException( "SubstringNode '" + node + "' had " + "incorrect syntax" );
                ne.setRootCause( pse );
                throw ne;
            }

            // cycle through the attribute values testing for a match
            while ( entries.hasMore() )
            {
                IndexRecord rec = ( IndexRecord ) entries.next();

                // once match is found cleanup and return true
                if ( regex.matcher( ( String ) rec.getIndexKey() ).matches() )
                {
                    entries.close();
                    return true;
                }
            }

            // we fell through so a match was not found - assertion was false.
            //return false;
        }

        // --------------------------------------------------------------------
        // Index not defined beyond this point
        // --------------------------------------------------------------------

        Attributes entry = record.getAttributes();
        
        // resuscitate the entry if it has not been and set entry in IndexRecord
        if ( null == entry )
        {
            Attributes attrs = db.lookup( (Long)record.getEntryId() );
            record.setAttributes( attrs );
            entry = record.getAttributes();
        }

        // Of course, if the entry does not contains any attributes
        // (very unlikely !!!), get out of here
        // TODO Can this simply happens ???
        if ( entry == null )
        {
            return false;
        }

        // get the attribute
        Attribute attr = AttributeUtils.getAttribute( entry, type );

        // if the attribute does not exist just return false
        if ( attr != null)
        {
            // compile the regular expression to search for a matching attribute
            try
            {
                regex = snode.getRegex( normalizer );
            }
            catch ( PatternSyntaxException pse )
            {
                NamingException ne = new NamingException( "SubstringNode '" + node + "' had " + "incorrect syntax" );
                ne.setRootCause( pse );
                throw ne;
            }

            /*
             * Cycle through the attribute values testing normalized version 
             * obtained from using the substring matching rule's normalizer.
             * The test uses the comparator obtained from the appropriate 
             * substring matching rule.
             */
            NamingEnumeration values = attr.getAll();
            
            while ( values.hasMore() )
            {
                String value = ( String ) normalizer.normalize( values.next() );
    
                // Once match is found cleanup and return true
                if ( regex.matcher( value ).matches() )
                {
                    values.close();
                    return true;
                }
            }
            
            // Fall through as we didn't find any matching value for this attribute.
            // We will have to check in the potential descendant, if any.
        }
        
        // If we do not have the attribute, loop through the descendant
        // May be the node Attribute has descendant ?
        if ( attributeTypeRegistry.hasDescendants( filterAttribute ) )
        {
            Iterator<AttributeType> descendants = attributeTypeRegistry.descendants( filterAttribute );

            while ( descendants.hasNext() )
            {
                AttributeType descendant = descendants.next();

                attr = AttributeUtils.getAttribute( entry, descendant );

                if ( null == attr )
                {
                    continue;
                }
                else
                {
                    // compile the regular expression to search for a matching attribute
                    try
                    {
                        regex = snode.getRegex( normalizer );
                    }
                    catch ( PatternSyntaxException pse )
                    {
                        NamingException ne = new NamingException( "SubstringNode '" + node + "' had " + "incorrect syntax" );
                        ne.setRootCause( pse );
                        throw ne;
                    }

                    /*
                     * Cycle through the attribute values testing normalized version 
                     * obtained from using the substring matching rule's normalizer.
                     * The test uses the comparator obtained from the appropriate 
                     * substring matching rule.
                     */
                    NamingEnumeration values = attr.getAll();
                    
                    while ( values.hasMore() )
                    {
                        String value = ( String ) normalizer.normalize( values.next() );
            
                        // Once match is found cleanup and return true
                        if ( regex.matcher( value ).matches() )
                        {
                            values.close();
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