/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.eve.db;


import java.math.BigInteger;
import java.util.Comparator;

import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.filter.LeafNode;
import org.apache.ldap.common.filter.ScopeNode;
import org.apache.ldap.common.filter.SimpleNode;
import org.apache.ldap.common.schema.Normalizer;
import org.apache.ldap.common.filter.PresenceNode;
import org.apache.ldap.common.NotImplementedException;

import org.apache.eve.schema.NormalizerRegistry;
import org.apache.eve.schema.ComparatorRegistry;


/**
 * Evaluates LeafNode assertions on candidates using a database.
 * 
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class LeafEvaluator implements Evaluator
{
    /** Database used to evaluate leaf with */
    private Database db;
    /** Normalizer registry for up value normalization */
    private NormalizerRegistry normalizerRegistry;
    /** Comparator registry for comparing normalized values */
    private ComparatorRegistry comparatorRegistry;
    /** Substring node evaluator we depend on */
    private SubstringEvaluator substringEvaluator;
    /** ScopeNode evaluator we depend on */
    private ScopeEvaluator scopeEvaluator;


    /**
     * Creates a leaf expression node evaluator.
     *
     * @param db
     * @param scopeEvaluator
     * @param normalizerRegistry
     * @param comparatorRegistry
     * @param substringEvaluator
     */
    public LeafEvaluator( Database db,
                          ScopeEvaluator scopeEvaluator,
                          NormalizerRegistry normalizerRegistry,
                          ComparatorRegistry comparatorRegistry,
                          SubstringEvaluator substringEvaluator )
    {
        this.db = db;
        this.scopeEvaluator = scopeEvaluator;
        this.normalizerRegistry = normalizerRegistry;
        this.comparatorRegistry = comparatorRegistry;
        this.substringEvaluator = substringEvaluator;
    }


    public ScopeEvaluator getScopeEvaluator()
    {
        return scopeEvaluator;
    }


    public NormalizerRegistry getNormalizerRegistry()
    {
        return normalizerRegistry;
    }


    public ComparatorRegistry getComparatorRegistry()
    {
        return comparatorRegistry;
    }


    public SubstringEvaluator getSubstringEvaluator()
    {
        return substringEvaluator;
    }


    /**
     * @see org.apache.eve.db.Evaluator#evaluate(ExprNode, IndexRecord)
     */
    public boolean evaluate( ExprNode node, IndexRecord record ) throws NamingException
    {
        if ( node instanceof ScopeNode )
        {
            return scopeEvaluator.evaluate( node, record );
        }
        
        switch( ( ( LeafNode ) node ).getAssertionType() ) 
        {
        case( LeafNode.APPROXIMATE ):
            return evalEquality( ( SimpleNode ) node, record );
        case( LeafNode.EQUALITY ):
            return evalEquality( ( SimpleNode ) node, record );
        case( LeafNode.EXTENSIBLE ):
            throw new NotImplementedException();
        case( LeafNode.GREATEREQ ):
            return evalGreater( ( SimpleNode ) node, record, true );
        case( LeafNode.LESSEQ ):
            return evalGreater( ( SimpleNode ) node, record, false );
        case( LeafNode.PRESENCE ):
            String attrId = ( ( PresenceNode ) node ).getAttribute();
            return evalPresence( attrId, record ); 
        case( LeafNode.SUBSTRING ):
            return substringEvaluator.evaluate( node, record );
        default:
            throw new NamingException( "Unrecognized leaf node type: "
                + ( ( LeafNode ) node ).getAssertionType() );
        }
    }
    
    
    /**
     * Evaluates a simple greater than or less than attribute value assertion on
     * a perspective candidate.
     * 
     * @param node the greater than or less than node to evaluate
     * @param record the IndexRecord of the perspective candidate
     * @param isGreater true if it is a greater than or equal to comparison,
     *      false if it is a less than or equal to comparison.
     * @return the ava evaluation on the perspective candidate
     * @throws NamingException if there is a database access failure
     */
    private boolean evalGreater( SimpleNode node, IndexRecord record,
        boolean isGreater ) throws NamingException
    {
        String attrId = node.getAttribute();
        BigInteger id = record.getEntryId();
        
        if ( db.hasUserIndexOn( attrId ) )
        {
            Index idx = db.getUserIndex( attrId );
            
            if ( isGreater )
            {
                return idx.hasValue( node.getValue(), id, true ); 
            }
            
            return idx.hasValue( node.getValue(), id, false );
        }
        
        // resusitate entry if need be
        if ( null == record.getAttributes() )
        {
            record.setAttributes( db.lookup( id ) );
        }
        
        // get the attribute associated with the node 
        Attribute attr = record.getAttributes().get( attrId );

        // If we do not have the attribute just return false
        if ( null == attr )
        {
            return false;
        }
        
        /*
         * We need to iterate through all values and for each value we normalize
         * and use the comparator to determine if a match exists.
         */
        Normalizer normalizer = normalizerRegistry.getEquality( attrId );
        Comparator comparator = comparatorRegistry.getEquality( attrId );
        Object filterValue = normalizer.normalize( node.getValue() );
        NamingEnumeration list = attr.getAll();
        
        /*
         * Cheaper to not check isGreater in one loop - better to separate
         * out into two loops which you choose to execute based on isGreater
         */
        if ( isGreater )
        {
            while ( list.hasMore() )
            {
                Object value = normalizer.normalize( list.next() );
            
                // Found a value that is greater than or equal to the ava value
                if ( 0 >= comparator.compare( value, filterValue ) )
                {
                    return true;
                }
            }
        }
        else 
        {    
            while ( list.hasMore() )
            {
                Object value = normalizer.normalize( list.next() );
            
                // Found a value that is less than or equal to the ava value
                if ( 0 <= comparator.compare( value, filterValue ) )
                {
                    return true;
                }
            }
        }
        
        // no match so return false
        return false;
    }

    
    /**
     * Evaluates a simple presence attribute value assertion on a perspective
     * candidate.
     * 
     * @param attrId the name of the attribute tested for presence 
     * @param rec the IndexRecord of the perspective candidate
     * @return the ava evaluation on the perspective candidate
     * @throws NamingException if there is a database access failure
     */
    private boolean evalPresence( String attrId, IndexRecord rec ) 
        throws NamingException
    {
        if ( db.hasUserIndexOn( attrId ) )
        {
            Index idx = db.getExistanceIndex();
            return idx.hasValue( attrId, rec.getEntryId() );
        }
        
        // resusitate entry if need be
        if ( null == rec.getAttributes() )
        {
            rec.setAttributes( db.lookup( rec.getEntryId() ) );
        }
        
        // get the attribute associated with the node 
        Attributes attrs = rec.getAttributes();
        return null != attrs.get( attrId );
    }
    

    /**
     * Evaluates a simple equality attribute value assertion on a perspective
     * candidate.
     * 
     * @param node the equality node to evaluate 
     * @param rec the IndexRecord of the perspective candidate
     * @return the ava evaluation on the perspective candidate
     * @throws NamingException if there is a database access failure
     */
    private boolean evalEquality( SimpleNode node, IndexRecord rec )
        throws NamingException
    {
        if ( db.hasUserIndexOn( node.getAttribute() ) )
        {
            Index idx = db.getUserIndex( node.getAttribute() );
            return idx.hasValue( node.getValue(), rec.getEntryId() );
        }

        Normalizer normalizer = normalizerRegistry.getEquality( node.getAttribute() );
        Comparator comparator = comparatorRegistry.getEquality( node.getAttribute() );

        /*
         * Get the attribute and if it is not set in rec then resusitate it
         * from the master table and set it in rec for use later if at all.
         * Before iterating through all values for a match check to see if the
         * AVA value is contained or the normalized form of the AVA value is 
         * contained.
         */
        
        // resusitate entry if need be
        if ( null == rec.getAttributes() )
        {
            rec.setAttributes( db.lookup( rec.getEntryId() ) );
        }
        
        // get the attribute associated with the node 
        Attribute attr = rec.getAttributes().get( node.getAttribute() );
        
        // If we do not have the attribute just return false
        if ( null == attr )
        {
            return false;
        }
        
        // check if AVA value exists in attribute
        if ( attr.contains( node.getValue() ) )
        {
            return true;
        }

        // get the normalized AVA filter value
        Object filterValue = normalizer.normalize( node.getValue() );

        // check if the normalized value is present
        if ( attr.contains( filterValue ) )
        {
            return true;
        }
        
        /*
         * We need to now iterate through all values because we could not get
         * a lookup to work.  For each value we normalize and use the comparator
         * to determine if a match exists.
         */
        NamingEnumeration list = attr.getAll();
        while ( list.hasMore() )
        {
            Object value = normalizer.normalize( list.next() );
            
            if ( 0 == comparator.compare( value, filterValue ) )
            {
                return true;
            }
        }
        
        // no match so return false
        return false;
    }
}
