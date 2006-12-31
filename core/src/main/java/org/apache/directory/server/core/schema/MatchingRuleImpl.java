/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.server.core.schema;

import java.util.Comparator;

import javax.naming.NamingException;

import org.apache.directory.server.schema.registries.ComparatorRegistry;
import org.apache.directory.server.schema.registries.NormalizerRegistry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.server.schema.registries.SyntaxRegistry;
import org.apache.directory.shared.ldap.schema.AbstractMatchingRule;
import org.apache.directory.shared.ldap.schema.MutableSchemaObject;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.Syntax;

class MatchingRuleImpl extends AbstractMatchingRule implements MutableSchemaObject
{
    private static final long serialVersionUID = 1L;
    private final SyntaxRegistry syntaxRegistry;
    private final ComparatorRegistry comparatorRegistry;
    private final NormalizerRegistry normalizerRegistry;
    private final String syntaxOid;
    
    /**
     * Creates a MatchingRule using the minimal set of required information.
     *
     * @param oid the object identifier for this matching rule
     */
    protected MatchingRuleImpl( String oid, String syntaxOid, Registries registries )
    {
        super( oid );
        this.syntaxOid = syntaxOid;
        syntaxRegistry = registries.getSyntaxRegistry();
        normalizerRegistry = registries.getNormalizerRegistry();
        comparatorRegistry = registries.getComparatorRegistry();
    }


    public Syntax getSyntax() throws NamingException
    {
        return syntaxRegistry.lookup( syntaxOid );
    }


    public Comparator getComparator() throws NamingException
    {
        return comparatorRegistry.lookup( oid );
    }


    public Normalizer getNormalizer() throws NamingException
    {
        return normalizerRegistry.lookup( oid );
    }
    

    public void setDescription( String description )
    {
        super.setDescription( description );
    }
    
    
    public void setNames( String[] names )
    {
        super.setNames( names );
    }
    
    
    public void setObsolete( boolean obsolete )
    {
        super.setObsolete( obsolete );
    }
}