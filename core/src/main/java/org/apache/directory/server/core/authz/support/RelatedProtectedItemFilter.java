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

import javax.naming.NamingException;
import javax.naming.directory.Attribute;

import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.event.Evaluator;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.subtree.RefinementEvaluator;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.aci.MicroOperation;
import org.apache.directory.shared.ldap.aci.ProtectedItem;
import org.apache.directory.shared.ldap.aci.ProtectedItem.MaxValueCountItem;
import org.apache.directory.shared.ldap.aci.ProtectedItem.RestrictedByItem;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.schema.registries.OidRegistry;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.util.AttributeUtils;


/**
 * An {@link ACITupleFilter} that discards all tuples whose {@link ProtectedItem}s
 * are not related with the operation. (18.8.3.2, X.501)
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class RelatedProtectedItemFilter implements ACITupleFilter
{
    private final RefinementEvaluator refinementEvaluator;
    private final Evaluator entryEvaluator;
    private final AttributeTypeRegistry atRegistry;


    public RelatedProtectedItemFilter( RefinementEvaluator refinementEvaluator, Evaluator entryEvaluator, 
        OidRegistry oidRegistry, AttributeTypeRegistry attrRegistry )
    {
        this.refinementEvaluator = refinementEvaluator;
        this.entryEvaluator = entryEvaluator;
        this.atRegistry = attrRegistry;
    }


    public Collection<ACITuple> filter( 
            Registries registries, 
            Collection<ACITuple> tuples, 
            OperationScope scope, 
            OperationContext opContext,
            Collection<LdapDN> userGroupNames, 
            LdapDN userName, 
            ServerEntry userEntry,
            AuthenticationLevel authenticationLevel, 
            LdapDN entryName, 
            String attrId,
            Value<?> attrValue, 
            ServerEntry entry, 
            Collection<MicroOperation> microOperations,
            ServerEntry entryView )
        throws NamingException
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


    private boolean isRelated( ACITuple tuple, OperationScope scope, LdapDN userName, LdapDN entryName, String attrId,
                               Value<?> attrValue, ServerEntry entry ) throws NamingException, InternalError
    {
        String oid = null;
        
        if ( attrId != null )
        {
            oid = atRegistry.getOid( attrId );
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
            else if ( item instanceof ProtectedItem.AllAttributeValues )
            {
                if ( scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE )
                {
                    continue;
                }

                ProtectedItem.AllAttributeValues aav = ( ProtectedItem.AllAttributeValues ) item;

                for ( Iterator<String> j = aav.iterator(); j.hasNext(); )
                {
                    if ( oid.equals( atRegistry.getOid( j.next() ) ) )
                    {
                        return true;
                    }
                }
            }
            else if ( item instanceof ProtectedItem.AttributeType )
            {
                if ( scope != OperationScope.ATTRIBUTE_TYPE )
                {
                    continue;
                }

                ProtectedItem.AttributeType at = ( ProtectedItem.AttributeType ) item;
                
                for ( Iterator<String> j = at.iterator(); j.hasNext(); )
                {
                    if ( oid.equals( atRegistry.getOid( j.next() ) ) )
                    {
                        return true;
                    }
                }
            }
            else if ( item instanceof ProtectedItem.AttributeValue )
            {
                if ( scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE )
                {
                    continue;
                }

                ProtectedItem.AttributeValue av = ( ProtectedItem.AttributeValue ) item;
                for ( Iterator<Attribute> j = av.iterator(); j.hasNext(); )
                {
                    Attribute attr = j.next();
                    String attrOid = atRegistry.getOid( attr.getID() );
                    AttributeType attrType = atRegistry.lookup( attrOid );
                    
                    if ( oid.equals( attrOid ) && AttributeUtils.containsValue( attr, attrValue, attrType ) )
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
                    
                    if ( oid.equals( atRegistry.getOid( mvcItem.getAttributeType() ) ) )
                    {
                        return true;
                    }
                }
            }
            else if ( item instanceof ProtectedItem.RangeOfValues )
            {
                ProtectedItem.RangeOfValues rov = ( ProtectedItem.RangeOfValues ) item;
                
                if ( entryEvaluator.evaluate( rov.getFilter(), entryName.toString(), entry ) )
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
                    if ( oid.equals( atRegistry.getOid( rbItem.getAttributeType() ) ) )
                    {
                        return true;
                    }
                }
            }
            else if ( item instanceof ProtectedItem.SelfValue )
            {
                if ( scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE && scope != OperationScope.ATTRIBUTE_TYPE )
                {
                    continue;
                }

                ProtectedItem.SelfValue sv = ( ProtectedItem.SelfValue ) item;
                for ( Iterator<String> j = sv.iterator(); j.hasNext(); )
                {
                    String svItem = j.next();
                    
                    if ( oid.equals( atRegistry.getOid( svItem ) ) )
                    {
                        EntryAttribute attr = entry.get( oid );
                        
                        if ( ( attr != null ) && 
                             ( ( attr.contains( userName.toNormName() ) || 
                               ( attr.contains( userName.getUpName() ) ) ) ) )
                        {
                            return true;
                        }
                    }
                }
            }
            else
            {
                throw new InternalError( "Unexpected protectedItem: " + item.getClass().getName() );
            }
        }

        return false;
    }
}
