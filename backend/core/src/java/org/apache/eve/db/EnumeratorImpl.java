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


import javax.naming.NamingException;
import javax.naming.NamingEnumeration;

import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.filter.LeafNode;
import org.apache.ldap.common.filter.ScopeNode;
import org.apache.ldap.common.filter.BranchNode;
import org.apache.ldap.common.filter.SimpleNode;
import org.apache.ldap.common.filter.PresenceNode;
import org.apache.ldap.common.filter.AssertionNode;
import org.apache.ldap.common.filter.SubstringNode;
import org.apache.ldap.common.NotImplementedException;


/**
 * Enumerates over candidates that satisfy a filter expression.
 * 
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class EnumeratorImpl implements Enumerator
{
    /** The database used by this enumerator */
    private Database db = null;
    /** Enumerator flyweight for evaulating filter scope assertions */
    private ScopeEnumerator scopeEnumerator;
    /** Enumerator flyweight for evaulating filter substring assertions */
    private SubstringEnumerator substringEnumerator;
    /** Enumerator flyweight for evaulating filter conjunction assertions */
    private ConjunctionEnumerator conjunctionEnumerator;
    /** Enumerator flyweight for evaulating filter disjunction assertions */
    private DisjunctionEnumerator disjunctionEnumerator;
    /** Enumerator flyweight for evaulating filter negation assertions */
    private NegationEnumerator negationEnumerator;
    /** Evaluator dependency on a LeafNode evaluator */
    private LeafEvaluator evaluator;


    public EnumeratorImpl( Database db, Evaluator topEvaluator,
                           LeafEvaluator leafEvaluator )
    {
        this.db = db;
        scopeEnumerator = new ScopeEnumerator();
        substringEnumerator = new SubstringEnumerator();
        conjunctionEnumerator = new ConjunctionEnumerator( this, topEvaluator );
        disjunctionEnumerator = new DisjunctionEnumerator( this );
        negationEnumerator = new NegationEnumerator();
    }


    /**
     * Creates an enumeration to enumerate through the set of candidates 
     * satisfying a filter expression.
     * 
     * @param node a filter expression root
     * @return an enumeration over the 
     * @throws NamingException if database access fails
     */
    public NamingEnumeration enumerate( ExprNode node ) throws NamingException
    {
        NamingEnumeration list = null;

        if ( node instanceof ScopeNode )
        {
            list = scopeEnumerator.enumerate( ( ScopeNode ) node );
        }
        else if ( node instanceof AssertionNode )
        {
            throw new IllegalArgumentException( "Cannot produce enumeration " 
                + "on an AssertionNode" );
        }
        else if ( node.isLeaf() ) 
        {
            LeafNode leaf = ( LeafNode ) node;

            switch( leaf.getAssertionType() )
            {
            case( LeafNode.APPROXIMATE ):
                list = enumEquality( ( SimpleNode ) node );
                break;
            case( LeafNode.EQUALITY ):
                list = enumEquality( ( SimpleNode ) node );
                break;
            case( LeafNode.EXTENSIBLE ):
                // N O T   I M P L E M E N T E D   Y E T !
                throw new NotImplementedException();
            case( LeafNode.GREATEREQ ):
                list = enumGreater( ( SimpleNode ) node, true );
                break;
            case( LeafNode.LESSEQ ):
                list = enumGreater( ( SimpleNode ) node, false );
                break;
            case( LeafNode.PRESENCE ):
                list = enumPresence( ( PresenceNode ) node );
                break;
            case( LeafNode.SUBSTRING ):
                list = substringEnumerator.enumerate( ( SubstringNode )
                    leaf );
                break;
            default:
                throw new IllegalArgumentException( "Unknown leaf assertion" );
            }
        } 
        else 
        {
            BranchNode branch = ( BranchNode ) node;
            
            switch( branch.getOperator() )
            {
            case( BranchNode.AND ):
                list = conjunctionEnumerator.enumerate( branch );
                break;
            case( BranchNode.NOT ):
                list = negationEnumerator.enumerate( branch );
                break;
            case( BranchNode.OR ):
                list = disjunctionEnumerator.enumerate( branch );
                break;
            default:
                throw new IllegalArgumentException( 
                    "Unknown branch logical operator" );
            }
        }

        return list;
    }

    
    /**
     * Returns an enumeration over candidates that satisfy a presence attribute 
     * value assertion.
     * 
     * @param node the presence AVA node
     * @return an enumeration over the index records matching the AVA
     * @throws NamingException if there is a failure while accessing the db
     */
    private NamingEnumeration enumPresence( final PresenceNode node ) 
        throws NamingException
    {
        if ( db.hasUserIndexOn( node.getAttribute() ) )
        {
            Index idx = db.getExistanceIndex();
            return idx.listIndices( node.getAttribute() );
        }
        
        return nonIndexedScan( node );
    }
    

    /**
     * Returns an enumeration over candidates that satisfy a simple greater than
     * or less than or equal to attribute value assertion.
     * 
     * @param node the AVA node
     * @param isGreater true if >= false if <= is used
     * @return an enumeration over the index records matching the AVA
     * @throws NamingException if there is a failure while accessing the db
     */
    private NamingEnumeration enumGreater( final SimpleNode node, 
        final boolean isGreater ) throws NamingException
    {
        if ( db.hasUserIndexOn( node.getAttribute() ) )
        {
            Index idx = db.getUserIndex( node.getAttribute() );
            
            if ( isGreater )
            {
                return idx.listIndices( node.getValue(), true );
            }
            else 
            {
                return idx.listIndices( node.getValue(), false );
            }
        }

        return nonIndexedScan( node );
    }
    
    
    /**
     * Returns an enumeration over candidates that satisfy a simple equality 
     * attribute value assertion.
     * 
     * @param node the equality AVA node
     * @return an enumeration over the index records matching the AVA
     * @throws NamingException if there is a failure while accessing the db
     */
    private NamingEnumeration enumEquality( final SimpleNode node )
        throws NamingException
    {
        if ( db.hasUserIndexOn( node.getAttribute() ) )
        {
            Index idx = db.getUserIndex( node.getAttribute() );
            return idx.listIndices( node.getValue() );
        }

        return nonIndexedScan( node );
    }
    
    
    /**
     * Creates a scan over all entries in the database with an assertion to test
     * for the correct evaluation of a filter expression on a LeafNode.
     * 
     * @param node the leaf node to produce a scan over
     * @return the enumeration over all perspective candidates satisfying expr
     * @throws NamingException if db access failures result
     */
    private NamingEnumeration nonIndexedScan( final LeafNode node )
        throws NamingException
    {
        NamingEnumeration underlying = db.getNdnIndex().listIndices();
        IndexAssertion assertion = new IndexAssertion()
        {
            public boolean assertCandidate( IndexRecord record ) 
                throws NamingException
            {
                return evaluator.evaluate( node, record );
            }
        };
        
        return new IndexAssertionEnumeration( underlying, assertion );
    }
}
