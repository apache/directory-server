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
package org.apache.directory.server.core.api.event;


import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidSearchFilterException;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.SubstringNode;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.MatchingRule;
import org.apache.directory.api.ldap.model.schema.Normalizer;
import org.apache.directory.server.i18n.I18n;


/**
 * Evaluates substring filter assertions on an entry.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SubstringEvaluator implements Evaluator
{
    /**
     * Creates a new SubstringEvaluator for substring expressions.
     */
    public SubstringEvaluator()
    {
    }


    /**
     * {@inheritDoc}
     */
    public boolean evaluate( ExprNode node, Dn dn, Entry entry ) throws LdapException
    {
        Pattern regex = null;
        SubstringNode snode = ( SubstringNode ) node;
        AttributeType attributeType = snode.getAttributeType();
        MatchingRule matchingRule = attributeType.getSubstring();

        if ( matchingRule == null )
        {
            matchingRule = attributeType.getEquality();
        }

        Normalizer normalizer = matchingRule.getNormalizer();

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
            LdapInvalidSearchFilterException ne = new LdapInvalidSearchFilterException( I18n.err( I18n.ERR_248, node ) );
            ne.initCause( pse );
            throw ne;
        }

        /*
         * Cycle through the attribute values testing normalized version 
         * obtained from using the substring matching rule's normalizer.
         * The test uses the comparator obtained from the appropriate 
         * substring matching rule.
         */

        for ( Value value : attr )
        {
            String normValue = normalizer.normalize( value.getString() );

            // Once match is found cleanup and return true

            if ( regex.matcher( normValue ).matches() )
            {
                return true;
            }
        }

        // we fell through so a match was not found - assertion was false.
        return false;
    }
}
