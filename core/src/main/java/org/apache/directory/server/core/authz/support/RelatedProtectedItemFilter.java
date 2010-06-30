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
package org.apache.directory.server.core.authz.support;


import java.util.Collection;
import java.util.Iterator;

import org.apache.directory.server.core.event.Evaluator;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.subtree.RefinementEvaluator;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.aci.MicroOperation;
import org.apache.directory.shared.ldap.aci.ProtectedItem;
import org.apache.directory.shared.ldap.aci.ProtectedItem.MaxValueCountItem;
import org.apache.directory.shared.ldap.aci.ProtectedItem.RestrictedByItem;
import org.apache.directory.shared.ldap.aci.protectedItem.AllAttributeValuesItem;
import org.apache.directory.shared.ldap.aci.protectedItem.AttributeTypeItem;
import org.apache.directory.shared.ldap.aci.protectedItem.AttributeValueItem;
import org.apache.directory.shared.ldap.aci.protectedItem.SelfValueItem;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.registries.OidRegistry;


/**
 * An {@link ACITupleFilter} that discards all tuples whose {@link ProtectedItem}s
 * are not related with the operation. (18.8.3.2, X.501)
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class RelatedProtectedItemFilter implements ACITupleFilter
{
    private final RefinementEvaluator refinementEvaluator;
    private final Evaluator entryEvaluator;
    private final SchemaManager schemaManager;


    public RelatedProtectedItemFilter( RefinementEvaluator refinementEvaluator, Evaluator entryEvaluator, 
        OidRegistry oidRegistry, SchemaManager schemaManager )
    {
        this.refinementEvaluator = refinementEvaluator;
        this.entryEvaluator = entryEvaluator;
        this.schemaManager = schemaManager;
    }


    public Collection<ACITuple> filter( 
            SchemaManager schemaManager, 
            Collection<ACITuple> tuples, 
            OperationScope scope, 
            OperationContext opContext,
            Collection<DN> userGroupNames, 
            DN userName, 
            Entry userEntry,
            AuthenticationLevel authenticationLevel, 
            DN entryName, 
            String attrId,
            Value<?> attrValue, 
            Entry entry, 
            Collection<MicroOperation> microOperations,
            Entry entryView )
        throws LdapException
    {
        if ( tuples.size() == 0 )
        {
            return tuples;
        }

        for ( Iterator<ACITuple> i = tuples.iterator(); i.hasNext(); )
        {
            ACITuple tuple = i.next();
            
            if ( !isRelated( tuple, scope, userName, entryName, attrId, attrValue, entry ) )
            {
                i.remove();
            }
        }

        return tuples;
    }


    private boolean isRelated( ACITuple tuple, OperationScope scope, DN userName, DN entryName, String attrId,
                               Value<?> attrValue, Entry entry ) throws LdapException, InternalError
    {
        String oid = null;
        
        if ( attrId != null )
        {
            oid = schemaManager.getAttributeTypeRegistry().getOidByName( attrId );
        }
        
        for ( ProtectedItem item : tuple.getProtectedItems() )
        {
            if ( item == ProtectedItem.ENTRY )
            {
                if ( scope == OperationScope.ENTRY )
                {
                    return true;
                }
            }
            else if ( item == ProtectedItem.ALL_USER_ATTRIBUTE_TYPES )
            {
                if ( scope != OperationScope.ATTRIBUTE_TYPE && scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE )
                {
                    continue;
                }

                return true;
            }
            else if ( item == ProtectedItem.ALL_USER_ATTRIBUTE_TYPES_AND_VALUES )
            {
                if ( scope != OperationScope.ATTRIBUTE_TYPE && scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE )
                {
                    continue;
                }

                return true;
            }
            else if ( item instanceof AllAttributeValuesItem )
            {
                if ( scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE )
                {
                    continue;
                }

                AllAttributeValuesItem aav = ( AllAttributeValuesItem ) item;

                for ( Iterator<AttributeType> iterator = aav.iterator(); iterator.hasNext(); )
                {
                    AttributeType attributeType = iterator.next();
                    
                    if ( oid.equals( attributeType.getOid() ) )
                    {
                        return true;
                    }
                }
            }
            else if ( item instanceof AttributeTypeItem )
            {
                if ( scope != OperationScope.ATTRIBUTE_TYPE )
                {
                    continue;
                }

                AttributeTypeItem at = ( AttributeTypeItem ) item;
                
                for ( Iterator<AttributeType> iterator = at.iterator(); iterator.hasNext(); )
                {
                    AttributeType attributeType = iterator.next();
                    
                    if ( oid.equals( attributeType.getOid() ) )
                    {
                        return true;
                    }
                }
            }
            else if ( item instanceof AttributeValueItem )
            {
                if ( scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE )
                {
                    continue;
                }

                AttributeValueItem av = ( AttributeValueItem ) item;
                
                for ( Iterator<EntryAttribute> j = av.iterator(); j.hasNext(); )
                {
                    EntryAttribute attr = j.next();
                    
                    AttributeType attributeType =  attr.getAttributeType();
                    String attrOid = null;
                    
                    if ( attributeType != null )
                    {
                        attrOid = attr.getAttributeType().getOid();
                    }
                    else
                    {
                        attributeType = schemaManager.getAttributeTypeRegistry().lookup( attr.getId() );
                        attrOid = attributeType.getOid();
                        attr.setAttributeType( attributeType );
                    }
                    
                    if ( oid.equals( attrOid ) && attr.contains( attrValue ) )
                    {
                        return true;
                    }
                }
            }
            else if ( item instanceof ProtectedItem.Classes )
            {
                ProtectedItem.Classes c = ( ProtectedItem.Classes ) item;
                if ( refinementEvaluator.evaluate( c.getClasses(), entry.get( SchemaConstants.OBJECT_CLASS_AT ) ) )
                {
                    return true;
                }
            }
            else if ( item instanceof ProtectedItem.MaxImmSub )
            {
                return true;
            }
            else if ( item instanceof ProtectedItem.MaxValueCount )
            {
                if ( scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE )
                {
                    continue;
                }

                ProtectedItem.MaxValueCount mvc = ( ProtectedItem.MaxValueCount ) item;
                for ( Iterator<MaxValueCountItem> j = mvc.iterator(); j.hasNext(); )
                {
                    MaxValueCountItem mvcItem = j.next();
                    
                    if ( oid.equals( schemaManager.getAttributeTypeRegistry().getOidByName( mvcItem.getAttributeType() ) ) )
                    {
                        return true;
                    }
                }
            }
            else if ( item instanceof ProtectedItem.RangeOfValues )
            {
                ProtectedItem.RangeOfValues rov = ( ProtectedItem.RangeOfValues ) item;
                
                if ( entryEvaluator.evaluate( rov.getFilter(), entryName, entry ) )
                {
                    return true;
                }
            }
            else if ( item instanceof ProtectedItem.RestrictedBy )
            {
                if ( scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE )
                {
                    continue;
                }

                ProtectedItem.RestrictedBy rb = ( ProtectedItem.RestrictedBy ) item;
                for ( Iterator<RestrictedByItem> j = rb.iterator(); j.hasNext(); )
                {
                    RestrictedByItem rbItem = j.next();
                    if ( oid.equals( schemaManager.getAttributeTypeRegistry().getOidByName( rbItem.getAttributeType() ) ) )
                    {
                        return true;
                    }
                }
            }
            else if ( item instanceof SelfValueItem )
            {
                if ( scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE && scope != OperationScope.ATTRIBUTE_TYPE )
                {
                    continue;
                }

                SelfValueItem sv = ( SelfValueItem ) item;
                
                for ( Iterator<AttributeType> iterator = sv.iterator(); iterator.hasNext(); )
                {
                    AttributeType attributeType = iterator.next();
                    
                    if ( oid.equals( attributeType.getOid() ) )
                    {
                        EntryAttribute attr = entry.get( oid );
                        
                        if ( ( attr != null ) && 
                             ( ( attr.contains( userName.getNormName() ) || 
                               ( attr.contains( userName.getName() ) ) ) ) )
                        {
                            return true;
                        }
                    }
                }
            }
            else
            {
                throw new InternalError( I18n.err( I18n.ERR_232, item.getClass().getName() ) );
            }
        }

        return false;
    }
}
