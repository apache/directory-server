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


import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.constants.SystemSchemaConstants;
import org.apache.directory.server.core.ServerUtils;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.util.AttributeUtils;


/**
 * Central point of control for schemas enforced by the server.  The 
 * following duties are presently performed by this class:
 * 
 * <ul>
 *   <li>Provide central point of access for all registries: global and SAA specific registries</li>
 *   <li>Manage enabling and disabling schemas</li>
 *   <li>Responding to specific schema object changes</li>
 * </ul>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SchemaManager
{
    private final PartitionSchemaLoader loader;
    private final MetaSchemaHandler metaSchemaHandler;
    private final Registries globalRegistries;
    private final AttributeType objectClassAT;
    private final MetaComparatorHandler metaComparatorHandler;
    private final MetaNormalizerHandler metaNormalizerHandler;
    private final MetaSyntaxCheckerHandler metaSyntaxCheckerHandler;
    private final MetaSyntaxHandler metaSyntaxHandler;
    private final MetaMatchingRuleHandler metaMatchingRuleHandler;
    private final MetaAttributeTypeHandler metaAttributeTypeHandler;
    

    public SchemaManager( Registries globalRegistries, PartitionSchemaLoader loader, SchemaPartitionDao dao ) 
        throws NamingException
    {
        this.loader = loader;
        this.globalRegistries = globalRegistries;
        this.objectClassAT = this.globalRegistries.getAttributeTypeRegistry()
            .lookup( SystemSchemaConstants.OBJECT_CLASS_AT );
        this.metaSchemaHandler = new MetaSchemaHandler( this.globalRegistries, this.loader );
        this.metaComparatorHandler = new MetaComparatorHandler( globalRegistries, loader );
        this.metaNormalizerHandler = new MetaNormalizerHandler( globalRegistries, loader );
        this.metaSyntaxCheckerHandler = new MetaSyntaxCheckerHandler( globalRegistries, loader );
        this.metaSyntaxHandler = new MetaSyntaxHandler( globalRegistries, loader, dao );
        this.metaMatchingRuleHandler = new MetaMatchingRuleHandler( globalRegistries, loader, dao );
        this.metaAttributeTypeHandler = new MetaAttributeTypeHandler( globalRegistries, loader, dao );
    }
    
    
    public Registries getGlobalRegistries()
    {
        return globalRegistries;
    }
    
    
    public Registries getRegistries( LdapDN dn ) throws NamingException
    {
        throw new NotImplementedException();
    }

    
    public void add( LdapDN name, Attributes entry ) throws NamingException
    {
        Attribute oc = ServerUtils.getAttribute( objectClassAT, entry );
        
        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SCHEMA_OC, objectClassAT ) )
        {
            metaSchemaHandler.add( name, entry );
            return;
        }
        
        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_COMPARATOR_OC, objectClassAT ) )
        {
            metaComparatorHandler.add( name, entry );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_NORMALIZER_OC, objectClassAT ) )
        {
            metaNormalizerHandler.add( name, entry );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SYNTAX_CHECKER_OC, objectClassAT ) )
        {
            metaSyntaxCheckerHandler.add( name, entry );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SYNTAX_OC, objectClassAT ) )
        {
            metaSyntaxHandler.add( name, entry );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_MATCHING_RULE_OC, objectClassAT ) )
        {
            metaMatchingRuleHandler.add( name, entry );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_ATTRIBUTE_TYPE_OC, objectClassAT ) )
        {
            metaAttributeTypeHandler.add( name, entry );
            return;
        }

        throw new NotImplementedException( "only changes to metaSchema objects are managed at this time" );
    }
    

    public void delete( LdapDN name, Attributes entry ) throws NamingException
    {
        Attribute oc = ServerUtils.getAttribute( objectClassAT, entry );
        
        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SCHEMA_OC, objectClassAT ) )
        {
            metaSchemaHandler.delete( name, entry );
            return;
        }
        
        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_COMPARATOR_OC, objectClassAT ) )
        {
            metaComparatorHandler.delete( name, entry );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_NORMALIZER_OC, objectClassAT ) )
        {
            metaNormalizerHandler.delete( name, entry );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SYNTAX_CHECKER_OC, objectClassAT ) )
        {
            metaSyntaxCheckerHandler.delete( name, entry );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SYNTAX_OC, objectClassAT ) )
        {
            metaSyntaxHandler.delete( name, entry );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_MATCHING_RULE_OC, objectClassAT ) )
        {
            metaMatchingRuleHandler.delete( name, entry );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_ATTRIBUTE_TYPE_OC, objectClassAT ) )
        {
            metaAttributeTypeHandler.delete( name, entry );
            return;
        }

        throw new NotImplementedException( "only changes to metaSchema objects are managed at this time" );
    }
    

    public void modify( LdapDN name, int modOp, Attributes mods, Attributes entry, Attributes targetEntry ) 
        throws NamingException
    {
        Attribute oc = ServerUtils.getAttribute( objectClassAT, entry );
        
        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SCHEMA_OC, objectClassAT ) )
        {
            metaSchemaHandler.modify( name, modOp, mods, entry, targetEntry );
            return;
        }
        
        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_COMPARATOR_OC, objectClassAT ) )
        {
            metaComparatorHandler.modify( name, modOp, mods, entry, targetEntry );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_NORMALIZER_OC, objectClassAT ) )
        {
            metaNormalizerHandler.modify( name, modOp, mods, entry, targetEntry );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SYNTAX_CHECKER_OC, objectClassAT ) )
        {
            metaSyntaxCheckerHandler.modify( name, modOp, mods, entry, targetEntry );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SYNTAX_OC, objectClassAT ) )
        {
            metaSyntaxHandler.modify( name, modOp, mods, entry, targetEntry );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_MATCHING_RULE_OC, objectClassAT ) )
        {
            metaMatchingRuleHandler.modify( name, modOp, mods, entry, targetEntry );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_ATTRIBUTE_TYPE_OC, objectClassAT ) )
        {
            metaAttributeTypeHandler.modify( name, modOp, mods, entry, targetEntry );
            return;
        }

        throw new NotImplementedException( "only changes to metaSchema objects are managed at this time" );
    }


    public void modify( LdapDN name, ModificationItemImpl[] mods, Attributes entry, Attributes targetEntry ) 
        throws NamingException
    {
        Attribute oc = ServerUtils.getAttribute( objectClassAT, entry );
        
        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SCHEMA_OC, objectClassAT ) )
        {
            metaSchemaHandler.modify( name, mods, entry, targetEntry );
            return;
        }
        
        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_COMPARATOR_OC, objectClassAT ) )
        {
            metaComparatorHandler.modify( name, mods, entry, targetEntry );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_NORMALIZER_OC, objectClassAT ) )
        {
            metaNormalizerHandler.modify( name, mods, entry, targetEntry );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SYNTAX_CHECKER_OC, objectClassAT ) )
        {
            metaSyntaxCheckerHandler.modify( name, mods, entry, targetEntry );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SYNTAX_OC, objectClassAT ) )
        {
            metaSyntaxHandler.modify( name, mods, entry, targetEntry );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_MATCHING_RULE_OC, objectClassAT ) )
        {
            metaMatchingRuleHandler.modify( name, mods, entry, targetEntry );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_ATTRIBUTE_TYPE_OC, objectClassAT ) )
        {
            metaAttributeTypeHandler.modify( name, mods, entry, targetEntry );
            return;
        }

        throw new NotImplementedException( "only changes to metaSchema objects are managed at this time" );
    }


    public void modifyRn( LdapDN name, String newRdn, boolean deleteOldRn, Attributes entry ) throws NamingException
    {
        Attribute oc = ServerUtils.getAttribute( objectClassAT, entry );
        
        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SCHEMA_OC, objectClassAT ) )
        {
            metaSchemaHandler.rename( name, entry, newRdn );
            return;
        }
        
        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_COMPARATOR_OC, objectClassAT ) )
        {
            metaComparatorHandler.rename( name, entry, newRdn );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_NORMALIZER_OC, objectClassAT ) )
        {
            metaNormalizerHandler.rename( name, entry, newRdn );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SYNTAX_CHECKER_OC, objectClassAT ) )
        {
            metaSyntaxCheckerHandler.rename( name, entry, newRdn );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SYNTAX_OC, objectClassAT ) )
        {
            metaSyntaxHandler.rename( name, entry, newRdn );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_MATCHING_RULE_OC, objectClassAT ) )
        {
            metaMatchingRuleHandler.rename( name, entry, newRdn );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_ATTRIBUTE_TYPE_OC, objectClassAT ) )
        {
            metaAttributeTypeHandler.rename( name, entry, newRdn );
            return;
        }

        throw new NotImplementedException( "only changes to metaSchema objects are managed at this time" );
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, Attributes entry ) throws NamingException
    {
        Attribute oc = ServerUtils.getAttribute( objectClassAT, entry );
        
        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SCHEMA_OC, objectClassAT ) )
        {
            metaSchemaHandler.move( oriChildName, newParentName, entry );
            return;
        }
        
        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_COMPARATOR_OC, objectClassAT ) )
        {
            metaComparatorHandler.move( oriChildName, newParentName, entry );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_NORMALIZER_OC, objectClassAT ) )
        {
            metaNormalizerHandler.move( oriChildName, newParentName, entry );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SYNTAX_CHECKER_OC, objectClassAT ) )
        {
            metaSyntaxCheckerHandler.move( oriChildName, newParentName, entry );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SYNTAX_OC, objectClassAT ) )
        {
            metaSyntaxHandler.move( oriChildName, newParentName, entry );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_MATCHING_RULE_OC, objectClassAT ) )
        {
            metaMatchingRuleHandler.move( oriChildName, newParentName, entry );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_ATTRIBUTE_TYPE_OC, objectClassAT ) )
        {
            metaAttributeTypeHandler.move( oriChildName, newParentName, entry );
            return;
        }

        throw new NotImplementedException( "only changes to metaSchema objects are managed at this time" );
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, String newRn, boolean deleteOldRn, Attributes entry )
        throws NamingException
    {
        Attribute oc = ServerUtils.getAttribute( objectClassAT, entry );
        
        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SCHEMA_OC, objectClassAT ) )
        {
            metaSchemaHandler.move( oriChildName, newParentName, newRn, deleteOldRn, entry );
            return;
        }
        
        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_COMPARATOR_OC, objectClassAT ) )
        {
            metaComparatorHandler.move( oriChildName, newParentName, newRn, deleteOldRn, entry );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_NORMALIZER_OC, objectClassAT ) )
        {
            metaNormalizerHandler.move( oriChildName, newParentName, newRn, deleteOldRn, entry );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SYNTAX_CHECKER_OC, objectClassAT ) )
        {
            metaSyntaxCheckerHandler.move( oriChildName, newParentName, newRn, deleteOldRn, entry );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_SYNTAX_OC, objectClassAT ) )
        {
            metaSyntaxHandler.move( oriChildName, newParentName, newRn, deleteOldRn, entry );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_MATCHING_RULE_OC, objectClassAT ) )
        {
            metaMatchingRuleHandler.move( oriChildName, newParentName, newRn, deleteOldRn, entry );
            return;
        }

        if ( AttributeUtils.containsValue( oc, MetaSchemaConstants.META_ATTRIBUTE_TYPE_OC, objectClassAT ) )
        {
            metaAttributeTypeHandler.move( oriChildName, newParentName, newRn, deleteOldRn, entry );
            return;
        }

        throw new NotImplementedException( "only changes to metaSchema objects are managed at this time" );
    }
}
