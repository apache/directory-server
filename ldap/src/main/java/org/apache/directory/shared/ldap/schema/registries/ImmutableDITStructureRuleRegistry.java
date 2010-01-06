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
import org.apache.directory.shared.ldap.schema.DITStructureRule;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.SchemaObjectType;


/**
 * An immutable wrapper of the DITStructureRule registry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 828111 $
 */
public class ImmutableDITStructureRuleRegistry implements DITStructureRuleRegistry
{
    /** The wrapped DITStructureRule registry */
    DITStructureRuleRegistry immutableDITStructureRuleRegistry;


    /**
     * Creates a new instance of ImmutableDITStructureRuleRegistry.
     *
     * @param ditStructureRuleRegistry The wrapped DITStructureRule registry
     */
    public ImmutableDITStructureRuleRegistry( DITStructureRuleRegistry ditStructureRuleRegistry )
    {
        immutableDITStructureRuleRegistry = ditStructureRuleRegistry;
    }


    /**
     * {@inheritDoc}
     */
    public boolean contains( int ruleId )
    {
        return immutableDITStructureRuleRegistry.contains( ruleId );
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<DITStructureRule> iterator()
    {
        return immutableDITStructureRuleRegistry.iterator();
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<Integer> ruleIdIterator()
    {
        return immutableDITStructureRuleRegistry.ruleIdIterator();
    }


    /**
     * {@inheritDoc}
     */
    public String getSchemaName( int ruleId ) throws NamingException
    {
        return immutableDITStructureRuleRegistry.getSchemaName( ruleId );
    }


    /**
     * {@inheritDoc}
     */
    public void register( DITStructureRule ditStructureRule ) throws NamingException
    {
    }


    /**
     * {@inheritDoc}
     */
    public DITStructureRule lookup( int ruleId ) throws NamingException
    {
        return immutableDITStructureRuleRegistry.lookup( ruleId );
    }


    /**
     * {@inheritDoc}
     */
    public void unregister( int ruleId ) throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Cannot modify the DITStructureRuleRegistry copy",
            ResultCodeEnum.NO_SUCH_OPERATION );
    }


    /**
     * {@inheritDoc}
     */
    public void unregisterSchemaElements( String schemaName ) throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Cannot modify the DITStructureRuleRegistry copy",
            ResultCodeEnum.NO_SUCH_OPERATION );
    }


    /**
     * {@inheritDoc}
     */
    public void renameSchema( String originalSchemaName, String newSchemaName ) throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Cannot modify the DITStructureRuleRegistry copy",
            ResultCodeEnum.NO_SUCH_OPERATION );
    }


    /**
     * {@inheritDoc}
     */
    public ImmutableDITStructureRuleRegistry copy()
    {
        return ( ImmutableDITStructureRuleRegistry ) immutableDITStructureRuleRegistry.copy();
    }


    /**
     * {@inheritDoc}
     */
    public int size()
    {
        return immutableDITStructureRuleRegistry.size();
    }


    /**
     * {@inheritDoc}
     */
    public boolean contains( String oid )
    {
        return immutableDITStructureRuleRegistry.contains( oid );
    }


    /**
     * {@inheritDoc}
     */
    public String getOidByName( String name ) throws NamingException
    {
        return immutableDITStructureRuleRegistry.getOidByName( name );
    }


    /**
     * {@inheritDoc}
     */
    public String getSchemaName( String oid ) throws NamingException
    {
        return immutableDITStructureRuleRegistry.getSchemaName( oid );
    }


    /**
     * {@inheritDoc}
     */
    public SchemaObjectType getType()
    {
        return immutableDITStructureRuleRegistry.getType();
    }


    /**
     * {@inheritDoc}
     */
    public DITStructureRule lookup( String oid ) throws NamingException
    {
        return immutableDITStructureRuleRegistry.lookup( oid );
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<String> oidsIterator()
    {
        return immutableDITStructureRuleRegistry.oidsIterator();
    }


    /**
     * {@inheritDoc}
     */
    public DITStructureRule unregister( String numericOid ) throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Cannot modify the DITStructureRuleRegistry copy",
            ResultCodeEnum.NO_SUCH_OPERATION );
    }


    /**
     * {@inheritDoc}
     */
    public SchemaObject get( String oid )
    {
        return immutableDITStructureRuleRegistry.get( oid );
    }


    /**
     * {@inheritDoc}
     */
    public void clear() throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Cannot modify the DITStructureRuleRegistry copy",
            ResultCodeEnum.NO_SUCH_OPERATION );
    }


    /**
     * {@inheritDoc}
     */
    public DITStructureRule unregister( DITStructureRule schemaObject ) throws NamingException
    {
        throw new LdapOperationNotSupportedException( "Cannot modify the DITStructureRuleRegistry copy",
            ResultCodeEnum.NO_SUCH_OPERATION );
    }
}
