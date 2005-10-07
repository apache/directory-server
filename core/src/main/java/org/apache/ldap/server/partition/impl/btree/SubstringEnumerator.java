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

/*
 * $Id$
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 * Created on Oct 13, 2003
 */
package org.apache.ldap.server.partition.impl.btree;


import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.filter.SubstringNode;
import org.apache.ldap.common.schema.AttributeType;
import org.apache.ldap.common.schema.Normalizer;
import org.apache.ldap.server.schema.AttributeTypeRegistry;
import org.apache.regexp.RE;
import org.apache.regexp.RESyntaxException;


/**
 * Enumerator that creates a NamingEnumeration over the set of candidates that 
 * satisfy a substring filter expression.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SubstringEnumerator implements Enumerator
{
    /** Database used */
    private final BTreeDirectoryPartition db;
    /** Evaluator used is an Avalon dependent object */
    private final SubstringEvaluator evaluator;
    /** the attribute type registry */
    private final AttributeTypeRegistry attributeTypeRegistry;


    /**
     * Creates a SubstringEnumerator for a database.
     *
     * @param db the database
     * @param evaluator a substring evaluator
     */
    public SubstringEnumerator( BTreeDirectoryPartition db,
                                AttributeTypeRegistry attributeTypeRegistry,
                                SubstringEvaluator evaluator )
    {
        this.db = db;
        this.evaluator = evaluator;
        this.attributeTypeRegistry = attributeTypeRegistry;
    }


    // ------------------------------------------------------------------------
    // SubstringEnumerator Methods
    // ------------------------------------------------------------------------
    
    
    /**
     * @see Enumerator#enumerate(
     * org.apache.ldap.common.filter.ExprNode)
     */
    public NamingEnumeration enumerate( final ExprNode node )
        throws NamingException
    {
        RE regex = null;
        Index idx = null;
        final SubstringNode snode = ( SubstringNode ) node;
        AttributeType type = attributeTypeRegistry.lookup( snode.getAttribute() );
        Normalizer normalizer = type.getSubstr().getNormalizer();

        if ( db.hasUserIndexOn( snode.getAttribute() ) )
        {
            /*
             * Build out regex in this block so we do not do it twice in the
             * evaluator if there is no index on the attribute of the substr ava
             */
            try 
            {
                regex = snode.getRegex( normalizer );
            } 
            catch ( RESyntaxException e ) 
            {
                NamingException ne = new NamingException( "SubstringNode '" 
                    + node + "' had incorrect syntax" );
                ne.setRootCause( e );
                throw ne;
            }

            /*
             * Get the user index and return an index enumeration using the the
             * compiled regular expression.  Try to constrain even further if
             * an initial term is available in the substring expression.
             */
            idx = db.getUserIndex( snode.getAttribute() );
            if ( null == snode.getInitial() )
            {
                return idx.listIndices( regex );
            } 
            else 
            {
                return idx.listIndices( regex, snode.getInitial() );
            }
        }
        
        /*
         * From this point on we are dealing with an enumeration over entries
         * based on an attribute that is not indexed.  We have no choice but
         * to perform a full table scan but need to leverage an index for the
         * underlying enumeration.  We know that all entries are listed under 
         * the ndn index and so this will enumerate over all entries as the 
         * underlying enumeration.  An evaluator in an assertion is used to 
         * constrain the result set.
         */
        NamingEnumeration underlying = db.getNdnIndex().listIndices();
        IndexAssertion assertion = new IndexAssertion()
        {
            public boolean assertCandidate( final IndexRecord record ) throws NamingException
            {
                return evaluator.evaluate( node, record );
            }
        };

        return new IndexAssertionEnumeration( underlying, assertion );
    }
}