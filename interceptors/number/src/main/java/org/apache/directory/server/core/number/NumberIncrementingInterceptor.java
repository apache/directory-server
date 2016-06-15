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
package org.apache.directory.server.core.number;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.MatchingRule;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An interceptor to increment any attribute with integer matching rule
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NumberIncrementingInterceptor extends BaseInterceptor
{
    /** A {@link Logger} for this class */
    private static final Logger LOG = LoggerFactory.getLogger( NumberIncrementingInterceptor.class );

    /** the DN of the holder entry */
    private Dn numberHolder;
    
    /** a map of integer attribute and it's present value */
    private Map<String, AtomicInteger> incMap = new HashMap<>();
    
    
    @Override
    public void init( DirectoryService directoryService ) throws LdapException
    {
        super.init( directoryService );
        
        numberHolder = new Dn( schemaManager, "ou=autoIncDataHolder,ou=system" );
        
        Partition systemPartition = directoryService.getSystemPartition();
        
        LookupOperationContext lookupContext = new LookupOperationContext( directoryService.getAdminSession(), numberHolder, SchemaConstants.ALL_ATTRIBUTES_ARRAY ); 
        Entry entry = systemPartition.lookup( lookupContext );
        
        if ( entry == null )
        {
            //FIXME make sure this entry addition gets replicated
            entry = new DefaultEntry( schemaManager );
            entry.setDn( numberHolder );
            entry.add( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.ORGANIZATIONAL_UNIT_OC );
            entry.add( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.EXTENSIBLE_OBJECT_OC );
            entry.add( SchemaConstants.OU_AT, numberHolder.getRdn().getValue() );
            entry.add( SchemaConstants.ENTRY_UUID_AT, UUID.randomUUID().toString() );
            entry.add( SchemaConstants.ENTRY_CSN_AT, directoryService.getCSN().toString() );
            
            AddOperationContext addContext = new AddOperationContext( directoryService.getAdminSession() );
            addContext.setDn( numberHolder );
            addContext.setEntry( new ClonedServerEntry( entry ) );
            
            LOG.debug( "Adding container entry to hold numeric attribute values" );
            systemPartition.add( addContext );
        }
        else
        {
            for ( Attribute at : entry )
            {
                MatchingRule mr = at.getAttributeType().getEquality();
                
                if ( ( mr != null ) && SchemaConstants.INTEGER_MATCH_MR_OID.equals( mr.getOid() ) )
                {
                    int t = Integer.parseInt( at.getString() );
                    incMap.put( at.getId(), new AtomicInteger( t ) );
                }
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void add( AddOperationContext addContext ) throws LdapException
    {
        LOG.debug( ">>> Entering into the number incrementing interceptor, addRequest" );

        if ( addContext.isReplEvent() )
        {
            // Nope, go on.
            next( addContext );
            return;
        }

        Entry entry = addContext.getEntry();

        List<Attribute> lst = new ArrayList<>();
        
        for ( String oid : incMap.keySet() )
        {
            Attribute at = entry.get( oid );
            if ( at != null )
            {
                lst.add( at );
            }
        }
        
        if ( lst.isEmpty() )
        {
            next( addContext );
            return;
        }

        for ( Attribute at : lst )
        {
            int stored = incMap.get( at.getId() ).get();
            at.clear();
            at.add( String.valueOf( stored + 1 ) );
        }
        
        // Ok, we are golden.
        next( addContext );

        ModifyOperationContext bindModCtx = new ModifyOperationContext( directoryService.getAdminSession() );
        bindModCtx.setDn( numberHolder );
        bindModCtx.setPushToEvtInterceptor( true );

        List<Modification> mods = new ArrayList<>();

        for ( Attribute at : lst )
        {
            AtomicInteger ai = incMap.get( at.getId() );
            ai.set( ai.get() + 1 );
            
            Modification mod = new DefaultModification();
            mod.setOperation( ModificationOperation.REPLACE_ATTRIBUTE );
            mod.setAttribute( at );
            
            mods.add( mod );
        }
        
        bindModCtx.setModItems( mods );
        
        directoryService.getPartitionNexus().modify( bindModCtx );
        
        LOG.debug( "Successfully updated numeric attribute in {}", numberHolder );
    }
}
