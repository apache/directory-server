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
package org.apache.directory.shared.ldap.schema.registries;


import java.util.Iterator;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.SchemaObjectType;


/**
 * An immutable wrapper of the MatchingRule registry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 828111 $
 */
public class ImmutableMatchingRuleRegistry implements MatchingRuleRegistry
{
    /** The wrapped MatchingRule registry */
    MatchingRuleRegistry immutableMatchingRuleRegistry;


    /**
     * Creates a new instance of ImmutableMatchingRuleRegistry.
     *
     * @param matchingRuleRegistry The wrapped MatchingRule registry
     */
    public ImmutableMatchingRuleRegistry( MatchingRuleRegistry matchingRuleRegistry )
    {
        immutableMatchingRuleRegistry = matchingRuleRegistry;
    }


    /**
     * {@inheritDoc}
     */
    public ImmutableMatchingRuleRegistry copy()
    {
        return ( ImmutableMatchingRuleRegistry ) immutableMatchingRuleRegistry.copy();
    }


    /**
     * {@inheritDoc}
     */
    public int size()
    {
        return immutableMatchingRuleRegistry.size();
    }


    /**
     * {@inheritDoc}
     */
    public boolean contains( String oid )
    {
        return immutableMatchingRuleRegistry.contains( oid );
    }


    /**
     * {@inheritDoc}
     */
    public String getOidByName( String name ) throws NamingException
    {
        return immutableMatchingRuleRegistry.getOidByName( name );
    }


    /**
     * {@inheritDoc}
     */
    public String getSchemaName( String oid ) throws NamingException
    {
        return immutableMatchingRuleRegistry.getSchemaName( oid );
    }


    /**
     * {@inheritDoc}
     */
    public SchemaObjectType getType()
    {
        return immutableMatchingRuleRegistry.getType();
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<MatchingRule> iterator()
    {
        return immutableMatchingRuleRegistry.iterator();
    }


    /**
     * {@inheritDoc}
     */
    public MatchingRule lookup( String oid ) throws NamingException
    {
        return immutableMatchingRuleRegistry.lookup( oid );
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<String> oidsIterator()
    {
        return immutableMatchingRuleRegistry.oidsIterator();
    }


    /**
     * {@inheritDoc}
     */
    public void register( MatchingRule schemaObject ) throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Cannot modify the MatchingRuleRegistry copy",
            ResultCodeEnum.NO_SUCH_OPERATION );
    }


    /**
     * {@inheritDoc}
     */
    public void renameSchema( String originalSchemaName, String newSchemaName ) throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Cannot modify the MatchingRuleRegistry copy",
            ResultCodeEnum.NO_SUCH_OPERATION );
    }


    /**
     * {@inheritDoc}
     */
    public MatchingRule unregister( String numericOid ) throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Cannot modify the MatchingRuleRegistry copy",
            ResultCodeEnum.NO_SUCH_OPERATION );
    }


    /**
     * {@inheritDoc}
     */
    public void unregisterSchemaElements( String schemaName ) throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Cannot modify the MatchingRuleRegistry copy",
            ResultCodeEnum.NO_SUCH_OPERATION );
    }


    /**
     * {@inheritDoc}
     */
    public SchemaObject get( String oid )
    {
        return immutableMatchingRuleRegistry.get( oid );
    }


    /**
     * {@inheritDoc}
     */
    public void clear() throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Cannot modify the MatchingRuleRegistry copy",
            ResultCodeEnum.NO_SUCH_OPERATION );
    }


    /**
     * {@inheritDoc}
     */
    public MatchingRule unregister( MatchingRule schemaObject ) throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Cannot modify the MatchingRuleRegistry copy",
            ResultCodeEnum.NO_SUCH_OPERATION );
    }
}
