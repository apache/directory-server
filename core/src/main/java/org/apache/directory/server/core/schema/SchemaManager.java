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
import javax.naming.directory.ModificationItem;

import org.apache.directory.server.constants.SystemSchemaConstants;
import org.apache.directory.server.core.ServerUtils;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.NotImplementedException;
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
    private final MetaSchemaModifyHandler metaSchemaModifyHandler;
    private final Registries globalRegistries;
    private final AttributeType objectClassAT;
    

    public SchemaManager( Registries globalRegistries, PartitionSchemaLoader loader ) throws NamingException
    {
        this.loader = loader;
        this.globalRegistries = globalRegistries;
        this.objectClassAT = this.globalRegistries.getAttributeTypeRegistry()
            .lookup( SystemSchemaConstants.OBJECT_CLASS_AT );
        this.metaSchemaModifyHandler = new MetaSchemaModifyHandler( this.globalRegistries, this.loader );
    }
    
    
    public Registries getGlobalRegistries()
    {
        throw new NotImplementedException();
    }
    
    
    public Registries getRegistries( LdapDN dn ) throws NamingException
    {
        throw new NotImplementedException();
    }



    public void modify( LdapDN name, int modOp, Attributes mods, Attributes entry ) throws NamingException
    {
        Attribute oc = ServerUtils.getAttribute( objectClassAT, entry );
        
        // We are changing a metaSchema entry
        if ( AttributeUtils.containsValue( oc, "metaSchema", objectClassAT ) )
        {
            metaSchemaModifyHandler.handleMetaSchemaModification( name, modOp, mods, entry );
            return;
        }

        throw new NotImplementedException( "only changes to metaSchema objects are managed at this time" );
    }


    public void modify( LdapDN name, ModificationItem[] mods, Attributes entry ) throws NamingException
    {
        Attribute oc = ServerUtils.getAttribute( objectClassAT, entry );
        
        // We are changing a metaSchema entry
        if ( AttributeUtils.containsValue( oc, "metaSchema", objectClassAT ) )
        {
            metaSchemaModifyHandler.handleMetaSchemaModification( name, mods, entry );
            return;
        }

        throw new NotImplementedException( "only changes to metaSchema objects are managed at this time" );
    }
}
