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
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.core.ServerUtils;
import org.apache.directory.server.schema.bootstrap.Schema;
import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;


/**
 * Handles events where entries of objectClass metaSchema are modified.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MetaSchemaHandler implements SchemaChangeHandler
{
    private final PartitionSchemaLoader loader;
    private final Registries globalRegistries;
    private final AttributeType disabledAT;
    

    public MetaSchemaHandler( Registries globalRegistries, PartitionSchemaLoader loader ) 
        throws NamingException
    {
        this.globalRegistries = globalRegistries;
        this.disabledAT = globalRegistries.getAttributeTypeRegistry()
            .lookup( MetaSchemaConstants.M_DISABLED_AT );
        this.loader = loader;
    }

    
    public void modify( LdapDN name, int modOp, Attributes mods, Attributes entry, Attributes targetEntry )
        throws NamingException
    {
        Attribute disabledInMods = ServerUtils.getAttribute( disabledAT, mods );
        if ( disabledInMods != null )
        {
            disable( name, modOp, disabledInMods, ServerUtils.getAttribute( disabledAT, entry ) );
        }
    }


    private void disable( LdapDN name, int modOp, Attribute disabledInMods, Attribute disabledInEntry )
        throws NamingException
    {
        switch ( modOp )
        {
            /*
             * If the user is adding a new m-disabled attribute to an enabled schema, 
             * we check that the value is "TRUE" and disable that schema if so.
             */
            case ( DirContext.ADD_ATTRIBUTE  ):
                if ( disabledInEntry == null )
                {
                    if ( "TRUE".equalsIgnoreCase( ( String ) disabledInMods.get() ) )
                    {
                        disableSchema( getSchemaName( name ) );
                    }
                }
                break;

            /*
             * If the user is removing the m-disabled attribute we check if the schema is currently 
             * disabled.  If so we enable the schema.
             */
            case ( DirContext.REMOVE_ATTRIBUTE  ):
                if ( "TRUE".equalsIgnoreCase( ( String ) disabledInEntry.get() ) )
                {
                    enableSchema( getSchemaName( name ) );
                }
                break;

            /*
             * If the user is replacing the m-disabled attribute we check if the schema is 
             * currently disabled and enable it if the new state has it as enabled.  If the
             * schema is not disabled we disable it if the mods set m-disabled to true.
             */
            case ( DirContext.REPLACE_ATTRIBUTE  ):
                boolean isCurrentlyDisabled = "TRUE".equalsIgnoreCase( ( String ) disabledInEntry.get() );
                boolean isNewStateDisabled = "TRUE".equalsIgnoreCase( ( String ) disabledInMods.get() );

                if ( isCurrentlyDisabled && !isNewStateDisabled )
                {
                    enableSchema( getSchemaName( name ) );
                    break;
                }

                if ( !isCurrentlyDisabled && isNewStateDisabled )
                {
                    disableSchema( getSchemaName( name ) );
                    break;
                }
            default:
                throw new IllegalArgumentException( "Unknown modify operation type: " + modOp );
        }
    }

    
    private final String getSchemaName( LdapDN schema ) throws NamingException
    {
        return ( String ) schema.getRdn().getValue();
    }
    

    private void disableSchema( String schemaName )
    {
        throw new NotImplementedException();
    }


    /**
     * TODO - for now we're just going to add the schema to the global 
     * registries ... we may need to add it to more than that though later.
     */
    private void enableSchema( String schemaName ) throws NamingException
    {
        if ( globalRegistries.getLoadedSchemas().containsKey( schemaName ) )
        {
            // TODO log warning: schemaName + " was already loaded"
            return;
        }
        
        Schema schema = loader.getSchema( schemaName );
        loader.load( schema, globalRegistries );
    }


    public void modify( LdapDN name, ModificationItem[] mods, Attributes entry, Attributes targetEntry ) 
        throws NamingException
    {
        OidRegistry registry = globalRegistries.getOidRegistry();
        Attribute disabledInEntry = ServerUtils.getAttribute( disabledAT, entry );
        
        for ( int ii = 0; ii < mods.length; ii++ )
        {
            String id = registry.getOid( mods[ii].getAttribute().getID() );
            if ( id.equals( disabledAT.getOid() ) )
            {
                disable( name, mods[ii].getModificationOp(), 
                    mods[ii].getAttribute(), disabledInEntry );
            }
        }
    }


    public void add( LdapDN name, Attributes entry ) throws NamingException
    {
        throw new NotImplementedException();
    }


    public void delete( LdapDN name, Attributes entry ) throws NamingException
    {
        throw new NotImplementedException();
    }


    public void rename( LdapDN name, Attributes entry, String newRdn ) throws NamingException
    {
        throw new NotImplementedException();
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, String newRn, boolean deleteOldRn, Attributes entry ) 
        throws NamingException
    {
        throw new NotImplementedException();
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, Attributes entry ) throws NamingException
    {
        throw new NotImplementedException();
    }
}
