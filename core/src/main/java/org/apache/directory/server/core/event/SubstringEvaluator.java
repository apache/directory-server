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
package org.apache.directory.server.core.event;


import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.directory.server.core.schema.AttributeTypeRegistry;
import org.apache.directory.server.core.schema.OidRegistry;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.SubstringNode;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.Normalizer;


/**
 * Evaluates substring filter assertions on an entry.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SubstringEvaluator implements Evaluator
{
    /** Oid Registry used to translate attributeIds to OIDs */
    private OidRegistry oidRegistry;
    /** AttributeType registry needed for normalizing and comparing values */
    private AttributeTypeRegistry attributeTypeRegistry;


    /**
     * Creates a new SubstringEvaluator for substring expressions.
     *
     * @param oidRegistry the OID registry for name to OID mapping
     * @param attributeTypeRegistry the attributeType registry
     */
    public SubstringEvaluator( OidRegistry oidRegistry,
                               AttributeTypeRegistry attributeTypeRegistry )
    {
        this.oidRegistry = oidRegistry;
        this.attributeTypeRegistry = attributeTypeRegistry;
    }


    /**
     * @see Evaluator#evaluate(ExprNode, String, Attributes)
     */
    public boolean evaluate( ExprNode node, String dn, Attributes entry )
        throws NamingException
    {
        Pattern regex = null; 
        SubstringNode snode = ( SubstringNode ) node;
        String oid = oidRegistry.getOid( snode.getAttribute() );
        AttributeType type = attributeTypeRegistry.lookup( oid );
        Normalizer normalizer = type.getSubstr().getNormalizer();

        // get the attribute
        Attribute attr = entry.get( snode.getAttribute() );
        
        // if the attribute does not exist just return false
        if ( null == attr )
        {
            return false;
        }

        // compile the regular expression to search for a matching attribute
        try 
        {
            regex = snode.getRegex( normalizer );
        } 
        catch ( PatternSyntaxException pse ) 
        {
            NamingException ne = new NamingException( "SubstringNode '" 
                + node + "' had " + "incorrect syntax" );
            ne.setRootCause( pse );
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
            
            if ( regex.matcher( value ).matches() ) 
            {
                list.close();
                return true;
            }
        }

        // we fell through so a match was not found - assertion was false.
        return false;
    }
}
