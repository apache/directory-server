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
import org.apache.directory.shared.ldap.schema.MatchingRuleUse;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.SchemaObjectType;


/**
 * An immutable wrapper of the MatchingRuleUse registry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 828111 $
 */
public class ImmutableMatchingRuleUseRegistry implements MatchingRuleUseRegistry
{
    /** The wrapped MatchingRuleUse registry */
    MatchingRuleUseRegistry immutableMatchingRuleUseRegistry;


    /**
     * Creates a new instance of ImmutableMatchingRuleUseRegistry.
     *
     * @param matchingRuleUseRegistry The wrapped MatchingRuleUse registry
     */
    public ImmutableMatchingRuleUseRegistry( MatchingRuleUseRegistry matchingRuleUseRegistry )
    {
        immutableMatchingRuleUseRegistry = matchingRuleUseRegistry;
    }


    /**
     * {@inheritDoc}
     */
    public ImmutableMatchingRuleUseRegistry copy()
    {
        return ( ImmutableMatchingRuleUseRegistry ) immutableMatchingRuleUseRegistry.copy();
    }


    /**
     * {@inheritDoc}
     */
    public int size()
    {
        return immutableMatchingRuleUseRegistry.size();
    }


    /**
     * {@inheritDoc}
     */
    public boolean contains( String oid )
    {
        return immutableMatchingRuleUseRegistry.contains( oid );
    }


    /**
     * {@inheritDoc}
     */
    public String getOidByName( String name ) throws NamingException
    {
        return immutableMatchingRuleUseRegistry.getOidByName( name );
    }


    /**
     * {@inheritDoc}
     */
    public String getSchemaName( String oid ) throws NamingException
    {
        return immutableMatchingRuleUseRegistry.getSchemaName( oid );
    }


    /**
     * {@inheritDoc}
     */
    public SchemaObjectType getType()
    {
        return immutableMatchingRuleUseRegistry.getType();
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<MatchingRuleUse> iterator()
    {
        return immutableMatchingRuleUseRegistry.iterator();
    }


    /**
     * {@inheritDoc}
     */
    public MatchingRuleUse lookup( String oid ) throws NamingException
    {
        return immutableMatchingRuleUseRegistry.lookup( oid );
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<String> oidsIterator()
    {
        return immutableMatchingRuleUseRegistry.oidsIterator();
    }


    /**
     * {@inheritDoc}
     */
    public void register( MatchingRuleUse schemaObject ) throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Cannot modify the MatchingRuleUseRegistry copy",
            ResultCodeEnum.NO_SUCH_OPERATION );
    }


    /**
     * {@inheritDoc}
     */
    public void renameSchema( String originalSchemaName, String newSchemaName ) throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Cannot modify the MatchingRuleUseRegistry copy",
            ResultCodeEnum.NO_SUCH_OPERATION );
    }


    /**
     * {@inheritDoc}
     */
    public MatchingRuleUse unregister( String numericOid ) throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Cannot modify the MatchingRuleUseRegistry copy",
            ResultCodeEnum.NO_SUCH_OPERATION );
    }


    /**
     * {@inheritDoc}
     */
    public void unregisterSchemaElements( String schemaName ) throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Cannot modify the MatchingRuleUseRegistry copy",
            ResultCodeEnum.NO_SUCH_OPERATION );
    }


    /**
     * {@inheritDoc}
     */
    public SchemaObject get( String oid )
    {
        return immutableMatchingRuleUseRegistry.get( oid );
    }


    /**
     * {@inheritDoc}
     */
    public void clear() throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Cannot modify the MatchingRuleUseRegistry copy",
            ResultCodeEnum.NO_SUCH_OPERATION );
    }


    /**
     * {@inheritDoc}
     */
    public MatchingRuleUse unregister( MatchingRuleUse schemaObject ) throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Cannot modify the MatchingRuleUseRegistry copy",
            ResultCodeEnum.NO_SUCH_OPERATION );
    }
}
