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
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.regexp.RE;
import org.apache.regexp.RESyntaxException;

import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.schema.Normalizer;
import org.apache.ldap.common.filter.SubstringNode;

import org.apache.eve.schema.NormalizerRegistry;


/**
 * Evaluates substring filter assertions on an entry.
 * 
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SubstringEvaluatorImpl implements SubstringEvaluator
{
    /** Database used while evaluating candidates */
    private Database db;
    /** Normalizer registry for up value normalization */
    private NormalizerRegistry normalizerRegistry;


    public SubstringEvaluatorImpl( Database db, NormalizerRegistry normReg )
    {
        this.db = db;
        this.normalizerRegistry = normReg;
    }


    /**
     * @see org.apache.eve.db.Evaluator#evaluate(ExprNode, IndexRecord)
     */
    public boolean evaluate( ExprNode node, IndexRecord record )
        throws NamingException
    {
        RE regex = null; 
        SubstringNode snode = ( SubstringNode ) node;

        if ( db.hasUserIndexOn( snode.getAttribute() ) )
        {
            Index idx = db.getUserIndex( snode.getAttribute() );
        
            /*
             * Note that this is using the reverse half of the index giving a 
             * considerable performance improvement on this kind of operation.
             * Otherwise we would have to scan the entire index if there were
             * no reverse lookups.
             */
        
            NamingEnumeration list = idx.listReverseIndices( record.getEntryId() );

            // compile the regular expression to search for a matching attribute
            try 
            {
                regex = snode.getRegex();
            } 
            catch ( RESyntaxException e ) 
            {
                NamingException ne = new NamingException( "SubstringNode '" 
                    + node + "' had " + "incorrect syntax" );
                ne.setRootCause( e );
                throw ne;
            }

            // cycle through the attribute values testing for a match
            while ( list.hasMore() ) 
            {
                IndexRecord rec = ( IndexRecord ) list.next();
            
                // once match is found cleanup and return true
                if ( regex.match( ( String ) rec.getIndexKey() ) ) 
                {
                    list.close();
                    return true;
                }
            }

            // we fell through so a match was not found - assertion was false.
            return false;
        }

        // --------------------------------------------------------------------
        // Index not defined beyond this point
        // --------------------------------------------------------------------
        
        Attribute attr = null;
        Normalizer normalizer = normalizerRegistry.getSubstring( snode.getAttribute() );

        // resusitate the entry if it has not been and set entry in IndexRecord
        if ( null == record.getAttributes() )
        {
            Attributes attrs = db.lookup( record.getEntryId() );
            record.setAttributes( attrs );
        }
        
        // get the attribute
        attr = record.getAttributes().get( snode.getAttribute() );
        
        // if the attribute does not exist just return false
        if ( null == attr )
        {
            return false;
        }

        // compile the regular expression to search for a matching attribute
        try 
        {
            regex = snode.getRegex();
        } 
        catch ( RESyntaxException e ) 
        {
            NamingException ne = new NamingException( "SubstringNode '" 
                + node + "' had " + "incorrect syntax" );
            ne.setRootCause( e );
            throw ne;
        }
        
        /*
         * Cycle through the attribute values testing normalized version 
         * obtained from using the substring matching rule's normalizer.
         * The test uses the comparator obtained from the appropriate 
         * substring matching rule.
         */ 
        NamingEnumeration list = attr.getAll(); 
        while ( list.hasMore() ) 
        {
            String value = ( String ) 
                normalizer.normalize( list.next() );
            
            // Once match is found cleanup and return true
            if ( regex.match( value ) ) 
            {
                list.close();
                return true;
            }
        }

        // we fell through so a match was not found - assertion was false.
        return false;
    }
}
