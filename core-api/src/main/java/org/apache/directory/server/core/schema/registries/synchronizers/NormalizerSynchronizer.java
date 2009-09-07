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
package org.apache.directory.server.core.schema.registries.synchronizers;


import javax.naming.NamingException;

import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.registries.MatchingRuleRegistry;
import org.apache.directory.shared.ldap.schema.registries.NormalizerRegistry;
import org.apache.directory.shared.ldap.schema.registries.Registries;


/**
 *
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class NormalizerSynchronizer extends AbstractRegistrySynchronizer
{
    private final NormalizerRegistry normalizerRegistry;
    private final MatchingRuleRegistry matchingRuleRegistry;
    

    public NormalizerSynchronizer( Registries registries ) throws Exception
    {
        super( registries );
        this.normalizerRegistry = registries.getNormalizerRegistry();
        this.matchingRuleRegistry = registries.getMatchingRuleRegistry();
    }
    
    
    protected boolean modify( LdapDN name, ServerEntry entry, ServerEntry targetEntry, boolean cascade ) throws Exception
    {
        String oid = getOid( entry );
        Normalizer normalizer = factory.getNormalizer( targetEntry, registries );
        
        if ( isSchemaLoaded( name ) )
        {
            normalizerRegistry.unregister( oid );
            normalizerRegistry.register( normalizer );
            
            return SCHEMA_MODIFIED;
        }
        
        return SCHEMA_UNCHANGED;
    }


    public void add( LdapDN name, ServerEntry entry ) throws Exception
    {
        LdapDN parentDn = ( LdapDN ) name.clone();
        parentDn.remove( parentDn.size() - 1 );
        checkNewParent( parentDn );
        checkOidIsUniqueForNormalizer( entry );
        
        Normalizer normalizer = factory.getNormalizer( entry, registries );
        
        if ( isSchemaLoaded( name ) )
        {
            normalizerRegistry.register( normalizer );
        }
    }


    public void delete( LdapDN name, ServerEntry entry, boolean cascade ) throws Exception
    {
        delete( getOid( entry ), cascade );
    }


    public void delete( String oid, boolean cascade ) throws NamingException
    {
        if ( matchingRuleRegistry.contains( oid ) )
        {
            throw new LdapOperationNotSupportedException( "The normalizer with OID " + oid 
                + " cannot be deleted until all " 
                + "matchingRules using that normalizer have also been deleted.", 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }
        
        if ( normalizerRegistry.contains( oid ) )
        {
            normalizerRegistry.unregister( oid );
        }
    }
    

    public void rename( LdapDN name, ServerEntry entry, Rdn newRdn, boolean cascade ) throws Exception
    {
        String oldOid = getOid( entry );

        if ( matchingRuleRegistry.contains( oldOid ) )
        {
            throw new LdapOperationNotSupportedException( "The normalizer with OID " + oldOid 
                + " cannot have it's OID changed until all " 
                + "matchingRules using that normalizer have been deleted.", 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        String oid = ( String ) newRdn.getValue();
        checkOidIsUniqueForNormalizer( oid );
        
        if ( isSchemaLoaded( name ) )
        {
            Normalizer normalizer = factory.getNormalizer( entry, registries );
            normalizerRegistry.unregister( oldOid );
            normalizerRegistry.register( normalizer );
        }
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, Rdn newRdn, boolean deleteOldRn,
        ServerEntry entry, boolean cascade ) throws Exception
    {
        checkNewParent( newParentName );
        String oldOid = getOid( entry );

        if ( matchingRuleRegistry.contains( oldOid ) )
        {
            throw new LdapOperationNotSupportedException( "The normalizer with OID " + oldOid 
                + " cannot have it's OID changed until all " 
                + "matchingRules using that normalizer have been deleted.", 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        String oid = ( String ) newRdn.getValue();
        checkOidIsUniqueForNormalizer( oid );
        Normalizer normalizer = factory.getNormalizer( entry, registries );

        if ( isSchemaLoaded( oriChildName ) )
        {
            normalizerRegistry.unregister( oldOid );
        }

        if ( isSchemaLoaded( newParentName ) )
        {
            normalizerRegistry.register( normalizer );
        }
    }


    public void replace( LdapDN oriChildName, LdapDN newParentName, ServerEntry entry, boolean cascade ) 
        throws Exception
    {
        checkNewParent( newParentName );
        String oid = getOid( entry );

        if ( matchingRuleRegistry.contains( oid ) )
        {
            throw new LdapOperationNotSupportedException( "The normalizer with OID " + oid 
                + " cannot be moved to another schema until all " 
                + "matchingRules using that normalizer have been deleted.", 
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        Normalizer normalizer = factory.getNormalizer( entry, registries );
        
        if ( isSchemaLoaded( oriChildName ) )
        {
            normalizerRegistry.unregister( oid );
        }
        
        if ( isSchemaLoaded( newParentName ) )
        {
            normalizerRegistry.register( normalizer );
        }
    }

    
    private void checkOidIsUniqueForNormalizer( String oid ) throws NamingException
    {
        if ( registries.getNormalizerRegistry().contains( oid ) )
        {
            throw new LdapNamingException( "Oid " + oid + " for new schema normalizer is not unique.", 
                ResultCodeEnum.OTHER );
        }
    }


    private void checkOidIsUniqueForNormalizer( ServerEntry entry ) throws Exception
    {
        String oid = getOid( entry );
        
        if ( registries.getNormalizerRegistry().contains( oid ) )
        {
            throw new LdapNamingException( "Oid " + oid + " for new schema normalizer is not unique.", 
                ResultCodeEnum.OTHER );
        }
    }


    private void checkNewParent( LdapDN newParent ) throws NamingException
    {
        if ( newParent.size() != 3 )
        {
            throw new LdapInvalidNameException( 
                "The parent dn of a normalizer should be at most 3 name components in length.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
        
        Rdn rdn = newParent.getRdn();

        if ( ! registries.getAttributeTypeRegistry().getOidByName( rdn.getNormType() ).equals( SchemaConstants.OU_AT_OID ) )
        {
            throw new LdapInvalidNameException( "The parent entry of a normalizer should be an organizationalUnit.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
        
        if ( ! ( ( String ) rdn.getValue() ).equalsIgnoreCase( SchemaConstants.NORMALIZERS_AT ) )
        {
            throw new LdapInvalidNameException( 
                "The parent entry of a normalizer should have a relative name of ou=normalizers.", 
                ResultCodeEnum.NAMING_VIOLATION );
        }
    }
}
