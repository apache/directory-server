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
package org.apache.directory.server.core.referral;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DefaultCoreSession;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.ReferralHandlingMode;
import org.apache.directory.server.core.authn.AuthenticationInterceptor;
import org.apache.directory.server.core.authn.LdapPrincipal;
import org.apache.directory.server.core.authz.AciAuthorizationInterceptor;
import org.apache.directory.server.core.authz.DefaultAuthorizationInterceptor;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ServerAttribute;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.entry.ServerStringValue;
import org.apache.directory.server.core.event.EventInterceptor;
import org.apache.directory.server.core.filtering.EntryFilter;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.AddContextPartitionOperationContext;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RemoveContextPartitionOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchingOperationContext;
import org.apache.directory.server.core.normalization.NormalizationInterceptor;
import org.apache.directory.server.core.operational.OperationalAttributeInterceptor;
import org.apache.directory.server.core.partition.ByPassConstants;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.core.schema.SchemaInterceptor;
import org.apache.directory.server.core.subtree.SubentryInterceptor;
import org.apache.directory.server.core.trigger.TriggerInterceptor;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.codec.util.LdapURL;
import org.apache.directory.shared.ldap.codec.util.LdapURLEncodingException;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.entry.client.ClientStringValue;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.exception.LdapReferralException;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;


/**
 * An service which is responsible referral handling behavoirs.  It manages 
 * referral handling behavoir when the {@link Context#REFERRAL} is implicitly
 * or explicitly set to "ignore", when set to "throw" and when set to "follow". 
 * 
 * @org.apache.xbean.XBean
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ReferralInterceptor extends BaseInterceptor
{
    private static final Logger LOG = LoggerFactory.getLogger( ReferralInterceptor.class );
    private static final Collection<String> SEARCH_BYPASS;

    private ReferralLut lut = new ReferralLut();
    private PartitionNexus nexus;

    /** The attributeType registry */
    private AttributeTypeRegistry atRegistry;

    /** Thre global registries */
    private Registries registries;

    /** The OID registry */
    private OidRegistry oidRegistry;

    static
    {
        /*
         * These are the services that we will bypass while searching for referrals in
         * partitions of the system during startup and during add/remove partition ops
         */
        Collection<String> c = new HashSet<String>();
        c.add( NormalizationInterceptor.class.getName() );
        c.add( AuthenticationInterceptor.class.getName() );
        c.add( ReferralInterceptor.class.getName() );
        c.add( AciAuthorizationInterceptor.class.getName() );
        c.add( DefaultAuthorizationInterceptor.class.getName() );
        //        c.add( ExceptionInterceptor.class.getName() );
        c.add( OperationalAttributeInterceptor.class.getName() );
        c.add( SchemaInterceptor.class.getName() );
        c.add( SubentryInterceptor.class.getName() );
        //        c.add( CollectiveAttributeInterceptor.class.getName() );
        c.add( EventInterceptor.class.getName() );
        c.add( TriggerInterceptor.class.getName() );
        SEARCH_BYPASS = Collections.unmodifiableCollection( c );
    }


    static boolean isReferral( ServerEntry entry ) throws NamingException
    {
        EntryAttribute oc = entry.get( SchemaConstants.OBJECT_CLASS_AT );

        if ( oc == null )
        {
            LOG.warn( "could not find objectClass attribute in entry: " + entry );
            return false;
        }

        if ( oc.contains( SchemaConstants.REFERRAL_OC ) )
        {
            //We have a referral ObjectClass, let's check that the ref is
            // valid, accordingly to the RFC

            // Get the 'ref' attributeType
            EntryAttribute refAttr = entry.get( SchemaConstants.REF_AT );

            if ( refAttr == null )
            {
                // very unlikely, as we have already checked the entry in SchemaInterceptor
                String message = "An entry with a 'referral' ObjectClass must contains a 'ref' Attribute";
                LOG.error( message );
                throw new NamingException( message );
            }

            for ( Value<?> value : refAttr )
            {
                ServerStringValue ref = ( ServerStringValue ) value;

                String refVal = ref.get();

                try
                {
                    LdapURL ldapUrl = new LdapURL( refVal );

                    // We have a LDAP URL, we have to check that :
                    // - we don't have scope specifier
                    // - we don't have filters
                    // - we don't have attribute description list
                    // - we don't have extensions
                    // - the DN is not empty

                    if ( ldapUrl.getScope() != SearchControls.OBJECT_SCOPE )
                    {
                        // This is the default value if we don't have any scope
                        // Let's assume that it's incorrect if we get something
                        // else in the LdapURL
                        String message = "An LDAPURL should not contains a scope";
                        LOG.error( message );
                        throw new NamingException( message );
                    }

                    if ( !StringTools.isEmpty( ldapUrl.getFilter() ) )
                    {
                        String message = "An LDAPURL should not contains filters";
                        LOG.error( message );
                        throw new NamingException( message );
                    }

                    if ( ( ldapUrl.getAttributes() != null ) && ( ldapUrl.getAttributes().size() != 0 ) )
                    {
                        String message = "An LDAPURL should not contains any description attribute list";
                        LOG.error( message );
                        throw new NamingException( message );
                    }

                    if ( ( ldapUrl.getExtensions() != null ) && ( ldapUrl.getExtensions().size() != 0 ) )
                    {
                        String message = "An LDAPURL should not contains any extension";
                        LOG.error( message );
                        throw new NamingException( message );
                    }

                    if ( ( ldapUrl.getCriticalExtensions() != null ) && ( ldapUrl.getCriticalExtensions().size() != 0 ) )
                    {
                        String message = "An LDAPURL should not contains any critical extension";
                        LOG.error( message );
                        throw new NamingException( message );
                    }

                    LdapDN dn = ldapUrl.getDn();

                    if ( ( dn == null ) || dn.isEmpty() )
                    {
                        String message = "An LDAPURL should contains a non-empty DN";
                        LOG.error( message );
                        throw new NamingException( message );
                    }
                }
                catch ( LdapURLEncodingException luee )
                {
                    // Either the URL is invalid, or it's not a LDAP URL.
                    // we will just ignore this LdapURL.
                }
            }

            return true;
        }

        return false;
    }


    public void init( DirectoryService directoryService ) throws Exception
    {
        nexus = directoryService.getPartitionNexus();
        registries = directoryService.getRegistries();
        atRegistry = registries.getAttributeTypeRegistry();
        oidRegistry = registries.getOidRegistry();

        Iterator<String> suffixes = nexus.listSuffixes( null );

        while ( suffixes.hasNext() )
        {
            LdapDN suffix = new LdapDN( suffixes.next() );
            
            LdapDN adminDn = new LdapDN( ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED );
            adminDn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            CoreSession adminSession = new DefaultCoreSession( 
                new LdapPrincipal( adminDn, AuthenticationLevel.STRONG ), directoryService );

            addReferrals( nexus.search( new SearchOperationContext( adminSession, suffix, AliasDerefMode.DEREF_ALWAYS,
                getReferralFilter(), getControls() ) ), suffix );
        }
    }


    public void doReferralException( LdapDN farthest, LdapDN targetUpdn, EntryAttribute refs ) throws NamingException
    {
        // handle referral here
        List<String> list = new ArrayList<String>( refs.size() );

        for ( Value<?> value : refs )
        {
            String val = ( String ) value.get();

            // need to add non-ldap URLs as-is
            if ( !val.startsWith( "ldap" ) )
            {
                list.add( val );
                continue;
            }

            // parse the ref value and normalize the DN according to schema 
            LdapURL ldapUrl = new LdapURL();
            try
            {
                ldapUrl.parse( val.toCharArray() );
            }
            catch ( LdapURLEncodingException e )
            {
                LOG.error( "Bad URL (" + val + ") for ref in " + farthest + ".  Reference will be ignored." );
            }

            LdapDN urlDn = new LdapDN( ldapUrl.getDn().toNormName() );
            urlDn.normalize( atRegistry.getNormalizerMapping() );

            if ( urlDn.equals( farthest ) )
            {
                // according to the protocol there is no need for the dn since it is the same as this request
                StringBuilder buf = new StringBuilder();
                buf.append( ldapUrl.getScheme() );
                buf.append( ldapUrl.getHost() );

                if ( ldapUrl.getPort() > 0 )
                {
                    buf.append( ":" );
                    buf.append( ldapUrl.getPort() );
                }

                list.add( buf.toString() );
                continue;
            }

            /*
             * If we get here then the DN of the referral was not the same as the 
             * DN of the ref LDAP URL.  We must calculate the remaining (difference)
             * name past the farthest referral DN which the target name extends.
             */
            int diff = targetUpdn.size() - farthest.size();
            LdapDN extra = new LdapDN();

            for ( int jj = 0; jj < diff; jj++ )
            {
                extra.add( targetUpdn.get( farthest.size() + jj ) );
            }

            urlDn.addAll( extra );

            StringBuilder buf = new StringBuilder();
            buf.append( ldapUrl.getScheme() );
            buf.append( ldapUrl.getHost() );

            if ( ldapUrl.getPort() > 0 )
            {
                buf.append( ":" );
                buf.append( ldapUrl.getPort() );
            }

            buf.append( "/" );
            buf.append( LdapURL.urlEncode( urlDn.getUpName(), false ) );
            list.add( buf.toString() );
        }

        throw new LdapReferralException( list );
    }


    public void add( NextInterceptor next, AddOperationContext opContext ) throws Exception
    {
        LdapDN name = opContext.getDn();
        ServerEntry entry = opContext.getEntry();
        ReferralHandlingMode refval = opContext.getSession().getReferralHandlingMode();

        switch( refval )
        {
            case IGNORE:
                next.add( opContext );

                if ( isReferral( entry ) )
                {
                    lut.referralAdded( name );
                }
                break;
            case THROW:
                LdapDN farthest = lut.getFarthestReferralAncestor( name );

                if ( farthest == null )
                {
                    next.add( opContext );

                    if ( isReferral( entry ) )
                    {
                        lut.referralAdded( name );
                    }
                    return;
                }

                ClonedServerEntry referral = opContext.lookup( farthest, ByPassConstants.LOOKUP_BYPASS );

                AttributeType refsType = atRegistry.lookup( oidRegistry.getOid( SchemaConstants.REF_AT ) );
                EntryAttribute refs = referral.get( refsType );
                doReferralException( farthest, new LdapDN( name.getUpName() ), refs );
                break;
            case FOLLOW:
                throw new NotImplementedException( "FOLLOW referral handling mode not implemented" );
            default:
                throw new LdapNamingException( "Undefined value for referral handling mode: " + refval,
                    ResultCodeEnum.OTHER );
        }
    }


    public boolean compare( NextInterceptor next, CompareOperationContext opContext ) throws Exception
    {
        LdapDN name = opContext.getDn();
        ReferralHandlingMode refval = opContext.getSession().getReferralHandlingMode();

        switch( refval )
        {
            case IGNORE:
                return next.compare( opContext );
            case THROW:
                LdapDN farthest = lut.getFarthestReferralAncestor( name );

                if ( farthest == null )
                {
                    return next.compare( opContext );
                }

                ClonedServerEntry referral = opContext.lookup( farthest, ByPassConstants.LOOKUP_BYPASS );
                EntryAttribute refs = referral.get( SchemaConstants.REF_AT );
                doReferralException( farthest, new LdapDN( name.getUpName() ), refs );

                // we really can't get here since doReferralException will throw an exception
                return false;
            case FOLLOW:
                throw new NotImplementedException( "FOLLOW referral handling mode not implemented" );
            default:
                throw new LdapNamingException( "Undefined value for referral handling mode: " + refval,
                    ResultCodeEnum.OTHER );
        }
    }


    public void delete( NextInterceptor next, DeleteOperationContext opContext ) throws Exception
    {
        LdapDN name = opContext.getDn();
        ReferralHandlingMode refval = opContext.getSession().getReferralHandlingMode();

        switch( refval )
        {
            case IGNORE:
                next.delete( opContext );

                if ( lut.isReferral( name ) )
                {
                    lut.referralDeleted( name );
                }

                return;
            case THROW:
                LdapDN farthest = lut.getFarthestReferralAncestor( name );

                if ( farthest == null )
                {
                    next.delete( opContext );

                    if ( lut.isReferral( name ) )
                    {
                        lut.referralDeleted( name );
                    }

                    return;
                }

                ClonedServerEntry referral = opContext.lookup( farthest, ByPassConstants.LOOKUP_BYPASS );
                EntryAttribute refs = referral.get( SchemaConstants.REF_AT );
                doReferralException( farthest, new LdapDN( name.getUpName() ), refs );
                break;
            case FOLLOW:
                throw new NotImplementedException( "FOLLOW referral handling mode not implemented" );
            default:
                throw new LdapNamingException( "Undefined value for referral handling mode: " + refval,
                    ResultCodeEnum.OTHER );
        }
    }


    /* -----------------------------------------------------------------------
     * Special handling instructions for ModifyDn operations:
     * ======================================================
     * 
     * From RFC 3296 here => http://www.ietf.org/rfc/rfc3296.txt
     * 
     * 5.6.2 Modify DN
     *
     * If the newSuperior is a referral object or is subordinate to a
     * referral object, the server SHOULD return affectsMultipleDSAs.  If
     * the newRDN already exists but is a referral object, the server SHOULD
     * return affectsMultipleDSAs instead of entryAlreadyExists.
     * -----------------------------------------------------------------------
     */

    public void move( NextInterceptor next, MoveOperationContext opContext ) throws Exception
    {
        LdapDN oldName = opContext.getDn();
        ReferralHandlingMode refval = opContext.getSession().getReferralHandlingMode();
        LdapDN newName = ( LdapDN ) opContext.getParent().clone();
        newName.add( oldName.get( oldName.size() - 1 ) );

        switch( refval )
        {
            case IGNORE:
                next.move( opContext );

                if ( lut.isReferral( oldName ) )
                {
                    lut.referralChanged( oldName, newName );
                }

                return;
            case THROW:
                LdapDN farthestSrc = lut.getFarthestReferralAncestor( oldName );
                LdapDN farthestDst = lut.getFarthestReferralAncestor( newName ); // note will not return newName so safe

                if ( farthestSrc == null && farthestDst == null && !lut.isReferral( newName ) )
                {
                    next.move( opContext );

                    if ( lut.isReferral( oldName ) )
                    {
                        lut.referralChanged( oldName, newName );
                    }

                    return;
                }
                else if ( farthestSrc != null )
                {
                    ClonedServerEntry referral = opContext.lookup( farthestSrc, ByPassConstants.LOOKUP_BYPASS );
                    EntryAttribute refs = referral.get( SchemaConstants.REF_AT );
                    doReferralException( farthestSrc, new LdapDN( oldName.getUpName() ), refs );
                }
                else if ( farthestDst != null )
                {
                    throw new LdapNamingException( farthestDst + " ancestor is a referral for modifyDn on " + newName
                        + " so it affects multiple DSAs", ResultCodeEnum.AFFECTS_MULTIPLE_DSAS );
                }
                else if ( lut.isReferral( newName ) )
                {
                    throw new LdapNamingException( newName
                        + " exists and is a referral for modifyDn destination so it affects multiple DSAs",
                        ResultCodeEnum.AFFECTS_MULTIPLE_DSAS );
                }

                throw new IllegalStateException( "If you get this exception the server's logic was flawed in handling a "
                    + "modifyDn operation while processing referrals.  Report this as a bug!" );                
            case FOLLOW:
                throw new NotImplementedException( "FOLLOW referral handling mode not implemented" );
            default:
                throw new LdapNamingException( "Undefined value for referral handling mode: " + refval,
                    ResultCodeEnum.OTHER );
        }
    }


    public void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext opContext ) throws Exception
    {
        LdapDN oldName = opContext.getDn();
        ReferralHandlingMode refval = opContext.getSession().getReferralHandlingMode();
        LdapDN newName = ( LdapDN ) opContext.getParent().clone();
        newName.add( opContext.getNewRdn() );

        switch( refval )
        {
            case IGNORE:
                next.moveAndRename( opContext );

                if ( lut.isReferral( oldName ) )
                {
                    lut.referralChanged( oldName, newName );
                }
                return;
            case THROW:
                LdapDN farthestSrc = lut.getFarthestReferralAncestor( oldName );
                LdapDN farthestDst = lut.getFarthestReferralAncestor( newName ); // safe to use - does not return newName

                if ( farthestSrc == null && farthestDst == null && !lut.isReferral( newName ) )
                {
                    next.moveAndRename( opContext );

                    if ( lut.isReferral( oldName ) )
                    {
                        lut.referralChanged( oldName, newName );
                    }
                    return;
                }
                else if ( farthestSrc != null )
                {
                    ClonedServerEntry referral = opContext.lookup( farthestSrc, ByPassConstants.LOOKUP_BYPASS );
                    EntryAttribute refs = referral.get( SchemaConstants.REF_AT );
                    doReferralException( farthestSrc, new LdapDN( oldName.getUpName() ), refs );
                }
                else if ( farthestDst != null )
                {
                    throw new LdapNamingException( farthestDst + " ancestor is a referral for modifyDn on " + newName
                        + " so it affects multiple DSAs", ResultCodeEnum.AFFECTS_MULTIPLE_DSAS );
                }
                else if ( lut.isReferral( newName ) )
                {
                    throw new LdapNamingException( newName
                        + " exists and is a referral for modifyDn destination so it affects multiple DSAs",
                        ResultCodeEnum.AFFECTS_MULTIPLE_DSAS );
                }

                throw new IllegalStateException( "If you get this exception the server's logic was flawed in handling a "
                    + "modifyDn operation while processing referrals.  Report this as a bug!" );
            case FOLLOW:
                throw new NotImplementedException( "FOLLOW referral handling mode not implemented" );
            default:
                throw new LdapNamingException( "Undefined value for referral handling mode: " + refval,
                    ResultCodeEnum.OTHER );
        }
    }


    public void rename( NextInterceptor next, RenameOperationContext opContext ) throws Exception
    {
        LdapDN oldName = opContext.getDn();
        ReferralHandlingMode refval = opContext.getSession().getReferralHandlingMode();
        LdapDN newName = ( LdapDN ) oldName.clone();
        newName.remove( oldName.size() - 1 );
        newName.add( opContext.getNewRdn() );

        switch( refval )
        {
            case IGNORE:
                next.rename( opContext );

                if ( lut.isReferral( oldName ) )
                {
                    lut.referralChanged( oldName, newName );
                }

                return;
            case THROW:
                LdapDN farthestSrc = lut.getFarthestReferralAncestor( oldName );
                LdapDN farthestDst = lut.getFarthestReferralAncestor( newName );

                if ( farthestSrc == null && farthestDst == null && !lut.isReferral( newName ) )
                {
                    next.rename( opContext );

                    if ( lut.isReferral( oldName ) )
                    {
                        lut.referralChanged( oldName, newName );
                    }

                    return;
                }

                if ( farthestSrc != null )
                {
                    ClonedServerEntry referral = opContext.lookup( farthestSrc, ByPassConstants.LOOKUP_BYPASS );
                    EntryAttribute refs = referral.get( SchemaConstants.REF_AT );
                    doReferralException( farthestSrc, new LdapDN( oldName.getUpName() ), refs );

                }
                else if ( farthestDst != null )
                {
                    throw new LdapNamingException( farthestDst + " ancestor is a referral for modifyDn on " + newName
                        + " so it affects multiple DSAs", ResultCodeEnum.AFFECTS_MULTIPLE_DSAS );
                }
                else if ( lut.isReferral( newName ) )
                {
                    throw new LdapNamingException( newName
                        + " exists and is a referral for modifyDn destination so it affects multiple DSAs",
                        ResultCodeEnum.AFFECTS_MULTIPLE_DSAS );
                }

                throw new IllegalStateException( "If you get this exception the server's logic was flawed in handling a "
                    + "modifyDn operation while processing referrals.  Report this as a bug!" );
            case FOLLOW:
                throw new NotImplementedException( "FOLLOW referral handling mode not implemented" );
            default:
                throw new LdapNamingException( "Undefined value for referral handling mode: " + refval,
                    ResultCodeEnum.OTHER );
        }
    }


    private void checkModify( LdapDN name, List<Modification> mods ) throws NamingException
    {
        boolean isTargetReferral = lut.isReferral( name );

        // -------------------------------------------------------------------
        // Check and update lut if we change the objectClass 
        // -------------------------------------------------------------------

        for ( Modification mod : mods )
        {
            if ( mod.getAttribute().getId().equalsIgnoreCase( SchemaConstants.OBJECT_CLASS_AT ) )
            {
                boolean modsOcHasReferral = ( ( ServerAttribute ) mod.getAttribute() )
                    .contains( SchemaConstants.REFERRAL_OC );

                switch ( mod.getOperation() )
                {
                    /* 
                     * if ADD op where refferal is added to objectClass of a
                     * non-referral entry then we add a new referral to lut
                     */
                    case ADD_ATTRIBUTE:
                        if ( modsOcHasReferral && !isTargetReferral )
                        {
                            lut.referralAdded( name );
                        }

                        break;

                    /*
                    * if REMOVE op where refferal is removed from objectClass of a
                    * referral entry then we remove the referral from lut
                    */
                    case REMOVE_ATTRIBUTE:
                        if ( modsOcHasReferral && isTargetReferral )
                        {
                            lut.referralDeleted( name );
                        }

                        break;

                    /*
                    * if REPLACE op on referral has new set of OC values which does
                    * not contain a referral value then we remove the referral from
                    * the lut
                    *
                    * if REPLACE op on non-referral has new set of OC values with
                    * referral value then we add the new referral to the lut
                    */
                    case REPLACE_ATTRIBUTE:
                        if ( isTargetReferral && !modsOcHasReferral )
                        {
                            lut.referralDeleted( name );
                        }
                        else if ( !isTargetReferral && modsOcHasReferral )
                        {
                            lut.referralAdded( name );
                        }

                        break;

                    default:
                        throw new IllegalStateException( "undefined modification operation" );
                }

                break;
            }
        }
    }


    public void modify( NextInterceptor next, ModifyOperationContext opContext ) throws Exception
    {
        ReferralHandlingMode refval = opContext.getSession().getReferralHandlingMode();
        LdapDN name = opContext.getDn();
        List<Modification> mods = opContext.getModItems();

        switch( refval )
        {
            case IGNORE:
                next.modify( opContext );
                checkModify( name, mods );
                return;
            case THROW:
                LdapDN farthest = lut.getFarthestReferralAncestor( name );

                if ( farthest == null )
                {
                    next.modify( opContext );
                    checkModify( name, mods );
                    return;
                }

                ClonedServerEntry referral = opContext.lookup( farthest, ByPassConstants.LOOKUP_BYPASS );
                EntryAttribute refs = referral.get( SchemaConstants.REF_AT );
                doReferralException( farthest, new LdapDN( name.getUpName() ), refs );
                break;
            case FOLLOW:
                throw new NotImplementedException( "FOLLOW referral handling mode not implemented" );
            default:
                throw new LdapNamingException( "Undefined value for referral handling mode: " + refval,
                    ResultCodeEnum.OTHER );
        }
    }


    static ExprNode getReferralFilter()
    {
        return new EqualityNode<String>( SchemaConstants.OBJECT_CLASS_AT, 
            new ClientStringValue( SchemaConstants.REFERRAL_OC ) );
    }


    static SearchControls getControls()
    {
        SearchControls controls = new SearchControls();
        controls.setReturningObjFlag( false );
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        return controls;
    }


    public void addContextPartition( NextInterceptor next, AddContextPartitionOperationContext opContext )
        throws Exception
    {
        next.addContextPartition( opContext );

        // add referrals immediately after adding the new partition
        Partition partition = opContext.getPartition();
        LdapDN suffix = partition.getSuffixDn();
        SearchOperationContext searchContext = new SearchOperationContext( 
            opContext.getSession(), suffix, AliasDerefMode.DEREF_ALWAYS, getReferralFilter(), getControls() );
        searchContext.setByPassed( SEARCH_BYPASS );
        EntryFilteringCursor list = opContext.getSession().getDirectoryService()
            .getOperationManager().search( searchContext );
        addReferrals( list, suffix );
    }


    public void removeContextPartition( NextInterceptor next, RemoveContextPartitionOperationContext opContext )
        throws Exception
    {
        // remove referrals immediately before removing the partition
        SearchOperationContext searchContext = new SearchOperationContext( 
            opContext.getSession(), opContext.getDn(), AliasDerefMode.DEREF_ALWAYS, 
            getReferralFilter(), getControls() );
        searchContext.setByPassed( SEARCH_BYPASS );
        EntryFilteringCursor cursor = opContext.getSession().getDirectoryService()
            .getOperationManager().search( searchContext );

        deleteReferrals( cursor, opContext.getDn() );
        next.removeContextPartition( opContext );
    }


    private void addReferrals( EntryFilteringCursor referrals, LdapDN base ) throws Exception
    {
        while ( referrals.next() )
        {
            ServerEntry r = referrals.get();
            LdapDN result = new LdapDN( r.getDn() );
            result.normalize( atRegistry.getNormalizerMapping() );

            // Now, add the referral to the cache
            lut.referralAdded( result );
        }
    }


    private void deleteReferrals( EntryFilteringCursor referrals, LdapDN base ) throws Exception
    {
        while ( referrals.next() )
        {
            ServerEntry r = referrals.get();
//            LdapDN referral;
            LdapDN result = new LdapDN( r.getDn() );
            result.normalize( atRegistry.getNormalizerMapping() );

            /*if ( r.isRelative() )
            {
                referral = ( LdapDN ) base.clone();
                referral.addAll( result );
            }*/

            // Now, remove the referral from the cache
            lut.referralDeleted( result );
        }
    }


    public EntryFilteringCursor search( NextInterceptor next, SearchOperationContext opContext ) throws Exception
    {
        ReferralHandlingMode refval = opContext.getSession().getReferralHandlingMode();
        LdapDN base = opContext.getDn();
        SearchControls controls = opContext.getSearchControls();
        
        // set inside switch
        ClonedServerEntry referral = null;
        LdapDN farthest = null;
        EntryAttribute refs = null;
        
        switch( refval )
        {
            case IGNORE:
                return next.search( opContext );

            /*
             * THROW_FINDING_BASE is a special setting which allows for finding base to 
             * throw exceptions but not when searching.  While search all results are 
             * returned as if they are regular entries.
             */
            case THROW_FINDING_BASE:
                if ( lut.isReferral( base ) )
                {
                    referral = opContext.lookup( base, ByPassConstants.LOOKUP_BYPASS );
                    refs = referral.get( SchemaConstants.REF_AT );
                    doReferralExceptionOnSearchBase( base, refs, controls.getSearchScope() );
                }

                farthest = lut.getFarthestReferralAncestor( base );

                if ( farthest == null )
                {
                    return next.search( opContext );
                }

                referral = opContext.lookup( farthest, ByPassConstants.LOOKUP_BYPASS );
                refs = referral.get( SchemaConstants.REF_AT );
                doReferralExceptionOnSearchBase( farthest, new LdapDN( base.getUpName() ), refs, controls.getSearchScope() );
                throw new IllegalStateException( "Should never get here: shutting up compiler" );
            case THROW:
                if ( lut.isReferral( base ) )
                {
                    referral = opContext.lookup( base, ByPassConstants.LOOKUP_BYPASS );
                    refs = referral.get( SchemaConstants.REF_AT );
                    doReferralExceptionOnSearchBase( base, refs, controls.getSearchScope() );
                }

                farthest = lut.getFarthestReferralAncestor( base );

                if ( farthest == null )
                {
                    EntryFilteringCursor srfe = next.search( opContext );
                    return new ReferralHandlingCursor( srfe, lut, true );
                }

                referral = opContext.lookup( farthest, ByPassConstants.LOOKUP_BYPASS );
                refs = referral.get( SchemaConstants.REF_AT );
                doReferralExceptionOnSearchBase( farthest, new LdapDN( base.getUpName() ), 
                    refs, controls.getSearchScope() );
                throw new IllegalStateException( "Should never get here: shutting up compiler" );
            case FOLLOW:
                throw new NotImplementedException( "FOLLOW referral handling mode not implemented" );
            default:
                throw new LdapNamingException( "Undefined value for referral handling mode: " + refval,
                    ResultCodeEnum.OTHER );
        }
    }

    
    class ReferralFilter implements EntryFilter 
    {
        public boolean accept( SearchingOperationContext operation, ClonedServerEntry result ) throws Exception
        {
            return false;
        }
    }


    public void doReferralExceptionOnSearchBase( LdapDN base, EntryAttribute refs, int scope ) throws NamingException
    {
        // handle referral here
        List<String> list = new ArrayList<String>( refs.size() );

        for ( Value<?> value : refs )
        {
            String val = ( String ) value.get();

            // need to add non-ldap URLs as-is
            if ( !val.startsWith( "ldap" ) )
            {
                list.add( val );
                continue;
            }

            // parse the ref value and normalize the DN according to schema 
            LdapURL ldapUrl = new LdapURL();
            try
            {
                ldapUrl.parse( val.toCharArray() );
            }
            catch ( LdapURLEncodingException e )
            {
                LOG.error( "Bad URL (" + val + ") for ref in " + base + ".  Reference will be ignored." );
            }

            StringBuilder buf = new StringBuilder();
            buf.append( ldapUrl.getScheme() );
            buf.append( ldapUrl.getHost() );

            if ( ldapUrl.getPort() > 0 )
            {
                buf.append( ":" );
                buf.append( ldapUrl.getPort() );
            }

            buf.append( "/" );
            buf.append( LdapURL.urlEncode( ldapUrl.getDn().getUpName(), false ) );
            buf.append( "??" );

            switch ( scope )
            {
                case ( SearchControls.SUBTREE_SCOPE   ):
                    buf.append( "sub" );
                    break;

                case ( SearchControls.ONELEVEL_SCOPE   ):
                    buf.append( "one" );
                    break;

                case ( SearchControls.OBJECT_SCOPE   ):
                    buf.append( "base" );
                    break;

                default:
                    throw new IllegalStateException( "Unknown recognized search scope: " + scope );
            }

            list.add( buf.toString() );
        }

        throw new LdapReferralException( list );
    }


    public void doReferralExceptionOnSearchBase( LdapDN farthest, LdapDN targetUpdn, EntryAttribute refs, int scope )
        throws NamingException
    {
        // handle referral here
        List<String> list = new ArrayList<String>( refs.size() );

        for ( Value<?> value : refs )
        {
            String val = ( String ) value.get();

            // need to add non-ldap URLs as-is
            if ( !val.startsWith( "ldap" ) )
            {
                list.add( val );
                continue;
            }

            // parse the ref value and normalize the DN according to schema 
            LdapURL ldapUrl = new LdapURL();

            try
            {
                ldapUrl.parse( val.toCharArray() );
            }
            catch ( LdapURLEncodingException e )
            {
                LOG.error( "Bad URL (" + val + ") for ref in " + farthest + ".  Reference will be ignored." );
            }

            LdapDN urlDn = new LdapDN( ldapUrl.getDn().toNormName() );
            urlDn.normalize( atRegistry.getNormalizerMapping() );
            int diff = targetUpdn.size() - farthest.size();
            LdapDN extra = new LdapDN();

            for ( int jj = 0; jj < diff; jj++ )
            {
                extra.add( targetUpdn.get( farthest.size() + jj ) );
            }

            urlDn.addAll( extra );

            StringBuilder buf = new StringBuilder();
            buf.append( ldapUrl.getScheme() );
            buf.append( ldapUrl.getHost() );

            if ( ldapUrl.getPort() > 0 )
            {
                buf.append( ":" );
                buf.append( ldapUrl.getPort() );
            }

            buf.append( "/" );
            buf.append( LdapURL.urlEncode( urlDn.getUpName(), false ) );
            buf.append( "??" );

            switch ( scope )
            {
                case ( SearchControls.SUBTREE_SCOPE   ):
                    buf.append( "sub" );
                    break;

                case ( SearchControls.ONELEVEL_SCOPE   ):
                    buf.append( "one" );
                    break;

                case ( SearchControls.OBJECT_SCOPE   ):
                    buf.append( "base" );
                    break;

                default:
                    throw new IllegalStateException( "Unknown recognized search scope: " + scope );
            }

            list.add( buf.toString() );
        }

        throw new LdapReferralException( list );
    }


    /**
     * Check if the given name is a referral or not.
     * 
     * @param name The DN to check
     * @return <code>true</code> if the DN is a referral
     * @throws NamingException I fthe DN is incorrect
     */
    public boolean isReferral( String name ) throws NamingException
    {
        if ( lut.isReferral( name ) )
        {
            return true;
        }

        LdapDN dn = new LdapDN( name );
        dn.normalize( atRegistry.getNormalizerMapping() );

        return lut.isReferral( dn );
    }


    /**
     * Check if the given name is a referral or not.
     * 
     * @param name The DN to check
     * @return <code>true</code> if the DN is a referral
     * @throws NamingException I fthe DN is incorrect
     */
    public boolean isReferral( LdapDN name ) throws NamingException
    {
        return lut
            .isReferral( name.isNormalized() ? name : LdapDN.normalize( name, atRegistry.getNormalizerMapping() ) );
    }
}
