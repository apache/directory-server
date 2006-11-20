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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.ServerUtils;
import org.apache.directory.server.core.configuration.PartitionConfiguration;
import org.apache.directory.server.core.configuration.InterceptorConfiguration;
import org.apache.directory.server.core.enumeration.ReferralHandlingEnumeration;
import org.apache.directory.server.core.enumeration.SearchResultFilter;
import org.apache.directory.server.core.enumeration.SearchResultFilteringEnumeration;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.jndi.ServerLdapContext;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.core.partition.PartitionNexusProxy;
import org.apache.directory.server.core.schema.AttributeTypeRegistry;
import org.apache.directory.server.core.schema.OidRegistry;

import org.apache.directory.shared.ldap.codec.util.LdapURL;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.codec.util.LdapURLEncodingException;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.exception.LdapReferralException;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.LeafNode;
import org.apache.directory.shared.ldap.filter.SimpleNode;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An service which is responsible referral handling behavoirs.  It manages 
 * referral handling behavoir when the {@link Context.REFERRAL} is implicitly
 * or explicitly set to "ignore", when set to "throw" and when set to "follow". 
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ReferralService extends BaseInterceptor
{
    public static final String NAME = "referralService";
    private static final Logger log = LoggerFactory.getLogger( ReferralService.class );
    private static final String IGNORE = "ignore";
    private static final String THROW_FINDING_BASE = "throw-finding-base";
    private static final String THROW = "throw";
    private static final String FOLLOW = "follow";
    private static final String REFERRAL_OC = "referral";
    private static final String OBJCLASS_ATTR = "objectClass";
    private static final Collection SEARCH_BYPASS;
    private static final String REF_ATTR = "ref";

    private ReferralLut lut = new ReferralLut();
    private PartitionNexus nexus;
    private Hashtable env;
    private AttributeTypeRegistry attrRegistry;
    private OidRegistry oidRegistry;

    
    static
    {
        /*
         * These are the services that we will bypass while searching for referrals in
         * partitions of the system during startup and during add/remove partition ops
         */
        Collection c = new HashSet();
        c.add( "normalizationService" );
        c.add( "authenticationService" );
        c.add( "authorizationService" );
        c.add( "defaultAuthorizationService" );
        c.add( "schemaService" );
        c.add( "subentryService" );
        c.add( "operationalAttributeService" );
        c.add( "referralService" );
        c.add( "eventService" );
        c.add( "triggerService" );
        SEARCH_BYPASS = Collections.unmodifiableCollection( c );
    }


    static boolean hasValue( Attribute attr, String value ) throws NamingException
    {
        if ( attr == null )
        {
            return false;
        }
        for ( int ii = 0; ii < attr.size(); ii++ )
        {
            if ( !( attr.get( ii ) instanceof String ) )
            {
                continue;
            }
            if ( value.equalsIgnoreCase( ( String ) attr.get( ii ) ) )
            {
                return true;
            }
        }
        return false;
    }


    static boolean isReferral( Attributes entry ) throws NamingException
    {
        Attribute oc = entry.get( OBJCLASS_ATTR );
        if ( oc == null )
        {
            log.warn( "could not find objectClass attribute in entry: " + entry );
            return false;
        }
        for ( int ii = 0; ii < oc.size(); ii++ )
        {
            if ( REFERRAL_OC.equalsIgnoreCase( ( String ) oc.get( ii ) ) )
            {
                return true;
            }
        }
        return false;
    }


    public void init( DirectoryServiceConfiguration dsConfig, InterceptorConfiguration cfg ) throws NamingException
    {
        nexus = dsConfig.getPartitionNexus();
        attrRegistry = dsConfig.getGlobalRegistries().getAttributeTypeRegistry();
        oidRegistry = dsConfig.getGlobalRegistries().getOidRegistry();
        env = dsConfig.getEnvironment();

        Iterator suffixes = nexus.listSuffixes();
        while ( suffixes.hasNext() )
        {
            LdapDN suffix = new LdapDN( ( String ) suffixes.next() );
            addReferrals( nexus.search( suffix, env, getReferralFilter(), getControls() ), suffix );
        }
    }


    public void doReferralException( LdapDN farthest, LdapDN targetUpdn, Attribute refs ) throws NamingException
    {
        // handle referral here
        List list = new ArrayList( refs.size() );
        for ( int ii = 0; ii < refs.size(); ii++ )
        {
            String val = ( String ) refs.get( ii );

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
                log.error( "Bad URL (" + val + ") for ref in " + farthest + ".  Reference will be ignored." );
            }

            LdapDN urlDn = new LdapDN( ldapUrl.getDn().toNormName() );
            urlDn.normalize( attrRegistry.getNormalizerMapping() );
            if ( urlDn.equals( farthest ) )
            {
                // according to the protocol there is no need for the dn since it is the same as this request
                StringBuffer buf = new StringBuffer();
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
            StringBuffer buf = new StringBuffer();
            buf.append( ldapUrl.getScheme() );
            buf.append( ldapUrl.getHost() );
            if ( ldapUrl.getPort() > 0 )
            {
                buf.append( ":" );
                buf.append( ldapUrl.getPort() );
            }
            buf.append( "/" );
            buf.append( urlDn.getUpName() );
            list.add( buf.toString() );
        }
        LdapReferralException lre = new LdapReferralException( list );
        throw lre;
    }


    public void add(NextInterceptor next, LdapDN normName, Attributes entry) throws NamingException
    {
        Invocation invocation = InvocationStack.getInstance().peek();
        ServerLdapContext caller = ( ServerLdapContext ) invocation.getCaller();
        String refval = ( String ) caller.getEnvironment().get( Context.REFERRAL );

        // handle a normal add without following referrals
        if ( refval == null || refval.equals( IGNORE ) )
        {
            next.add(normName, entry );
            if ( isReferral( entry ) )
            {
                lut.referralAdded( normName );
            }
            return;
        }

        if ( refval.equals( THROW ) )
        {
            LdapDN farthest = lut.getFarthestReferralAncestor( normName );
            if ( farthest == null )
            {
                next.add(normName, entry );
                if ( isReferral( entry ) )
                {
                    lut.referralAdded( normName );
                }
                return;
            }

            Attributes referral = invocation.getProxy().lookup( farthest, PartitionNexusProxy.LOOKUP_BYPASS );
            AttributeType refsType = attrRegistry.lookup( oidRegistry.getOid( REF_ATTR ) );
            Attribute refs = ServerUtils.getAttribute( refsType, referral );
            doReferralException( farthest, new LdapDN( normName.getUpName() ), refs );
        }
        else if ( refval.equals( FOLLOW ) )
        {
            throw new NotImplementedException( FOLLOW + " referral handling mode not implemented" );
        }
        else
        {
            throw new LdapNamingException( "Undefined value for " + Context.REFERRAL + " key: " + refval,
                ResultCodeEnum.OTHER );
        }
    }


    public boolean compare( NextInterceptor next, LdapDN normName, String oid, Object value ) throws NamingException
    {
        Invocation invocation = InvocationStack.getInstance().peek();
        ServerLdapContext caller = ( ServerLdapContext ) invocation.getCaller();
        String refval = ( String ) caller.getEnvironment().get( Context.REFERRAL );

        // handle a normal add without following referrals
        if ( refval == null || refval.equals( IGNORE ) )
        {
            return next.compare( normName, oid, value );
        }

        if ( refval.equals( THROW ) )
        {
            LdapDN farthest = lut.getFarthestReferralAncestor( normName );
            if ( farthest == null )
            {
                return next.compare( normName, oid, value );
            }

            Attributes referral = invocation.getProxy().lookup( farthest, PartitionNexusProxy.LOOKUP_BYPASS );
            Attribute refs = referral.get( REF_ATTR );
            doReferralException( farthest, new LdapDN( normName.getUpName() ), refs );

            // we really can't get here since doReferralException will throw an exception
            return false;
        }
        else if ( refval.equals( FOLLOW ) )
        {
            throw new NotImplementedException( FOLLOW + " referral handling mode not implemented" );
        }
        else
        {
            throw new LdapNamingException( "Undefined value for " + Context.REFERRAL + " key: " + refval,
                ResultCodeEnum.OTHER );
        }
    }


    public void delete( NextInterceptor next, LdapDN normName ) throws NamingException
    {
        Invocation invocation = InvocationStack.getInstance().peek();
        ServerLdapContext caller = ( ServerLdapContext ) invocation.getCaller();
        String refval = ( String ) caller.getEnvironment().get( Context.REFERRAL );

        // handle a normal delete without following referrals
        if ( refval == null || refval.equals( IGNORE ) )
        {
            next.delete( normName );
            if ( lut.isReferral( normName ) )
            {
                lut.referralDeleted( normName );
            }
            return;
        }

        if ( refval.equals( THROW ) )
        {
            LdapDN farthest = lut.getFarthestReferralAncestor( normName );
            if ( farthest == null )
            {
                next.delete( normName );
                if ( lut.isReferral( normName ) )
                {
                    lut.referralDeleted( normName );
                }
                return;
            }

            Attributes referral = invocation.getProxy().lookup( farthest, PartitionNexusProxy.LOOKUP_BYPASS );
            Attribute refs = referral.get( REF_ATTR );
            doReferralException( farthest, new LdapDN( normName.getUpName() ), refs );
        }
        else if ( refval.equals( FOLLOW ) )
        {
            throw new NotImplementedException( FOLLOW + " referral handling mode not implemented" );
        }
        else
        {
            throw new LdapNamingException( "Undefined value for " + Context.REFERRAL + " key: " + refval,
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

    public void move( NextInterceptor next, LdapDN oldName, LdapDN newParent ) throws NamingException
    {
        Invocation invocation = InvocationStack.getInstance().peek();
        ServerLdapContext caller = ( ServerLdapContext ) invocation.getCaller();
        String refval = ( String ) caller.getEnvironment().get( Context.REFERRAL );
        LdapDN newName = ( LdapDN ) newParent.clone();
        newName.add( oldName.get( oldName.size() - 1 ) );

        // handle a normal modify without following referrals
        if ( refval == null || refval.equals( IGNORE ) )
        {
            next.move( oldName, newParent );
            if ( lut.isReferral( oldName ) )
            {
                lut.referralChanged( oldName, newName );
            }
            return;
        }

        if ( refval.equals( THROW ) )
        {
            LdapDN farthestSrc = lut.getFarthestReferralAncestor( oldName );
            LdapDN farthestDst = lut.getFarthestReferralAncestor( newName ); // note will not return newName so safe
            if ( farthestSrc == null && farthestDst == null && !lut.isReferral( newName ) )
            {
                next.move( oldName, newParent );
                if ( lut.isReferral( oldName ) )
                {
                    lut.referralChanged( oldName, newName );
                }
                return;
            }
            else if ( farthestSrc != null )
            {
                Attributes referral = invocation.getProxy().lookup( farthestSrc,
                    PartitionNexusProxy.LOOKUP_BYPASS );
                Attribute refs = referral.get( REF_ATTR );
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
        }
        else if ( refval.equals( FOLLOW ) )
        {
            throw new NotImplementedException( FOLLOW + " referral handling mode not implemented" );
        }
        else
        {
            throw new LdapNamingException( "Undefined value for " + Context.REFERRAL + " key: " + refval,
                ResultCodeEnum.OTHER );
        }
    }


    public void move( NextInterceptor next, LdapDN oldName, LdapDN newParent, String newRdn, boolean deleteOldRdn )
        throws NamingException
    {
        Invocation invocation = InvocationStack.getInstance().peek();
        ServerLdapContext caller = ( ServerLdapContext ) invocation.getCaller();
        String refval = ( String ) caller.getEnvironment().get( Context.REFERRAL );
        LdapDN newName = ( LdapDN ) newParent.clone();
        newName.add( newRdn );

        // handle a normal modify without following referrals
        if ( refval == null || refval.equals( IGNORE ) )
        {
            next.move( oldName, newParent, newRdn, deleteOldRdn );
            if ( lut.isReferral( oldName ) )
            {
                lut.referralChanged( oldName, newName );
            }
            return;
        }

        if ( refval.equals( THROW ) )
        {
            LdapDN farthestSrc = lut.getFarthestReferralAncestor( oldName );
            LdapDN farthestDst = lut.getFarthestReferralAncestor( newName ); // safe to use - does not return newName
            if ( farthestSrc == null && farthestDst == null && !lut.isReferral( newName ) )
            {
                next.move( oldName, newParent, newRdn, deleteOldRdn );
                if ( lut.isReferral( oldName ) )
                {
                    lut.referralChanged( oldName, newName );
                }
                return;
            }
            else if ( farthestSrc != null )
            {
                Attributes referral = invocation.getProxy().lookup( farthestSrc,
                    PartitionNexusProxy.LOOKUP_BYPASS );
                Attribute refs = referral.get( REF_ATTR );
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
        }
        else if ( refval.equals( FOLLOW ) )
        {
            throw new NotImplementedException( FOLLOW + " referral handling mode not implemented" );
        }
        else
        {
            throw new LdapNamingException( "Undefined value for " + Context.REFERRAL + " key: " + refval,
                ResultCodeEnum.OTHER );
        }
    }


    public void modifyRn( NextInterceptor next, LdapDN oldName, String newRdn, boolean deleteOldRdn )
        throws NamingException
    {
        Invocation invocation = InvocationStack.getInstance().peek();
        ServerLdapContext caller = ( ServerLdapContext ) invocation.getCaller();
        String refval = ( String ) caller.getEnvironment().get( Context.REFERRAL );
        LdapDN newName = ( LdapDN ) oldName.clone();
        newName.remove( oldName.size() - 1 );

        LdapDN newRdnName = new LdapDN( newRdn );
        newRdnName.normalize( attrRegistry.getNormalizerMapping() );
        newName.add( newRdnName.toNormName() );

        // handle a normal modify without following referrals
        if ( refval == null || refval.equals( IGNORE ) )
        {
            next.modifyRn( oldName, newRdn, deleteOldRdn );
            if ( lut.isReferral( oldName ) )
            {
                lut.referralChanged( oldName, newName );
            }
            return;
        }

        if ( refval.equals( THROW ) )
        {
            LdapDN farthestSrc = lut.getFarthestReferralAncestor( oldName );
            LdapDN farthestDst = lut.getFarthestReferralAncestor( newName );
            if ( farthestSrc == null && farthestDst == null && !lut.isReferral( newName ) )
            {
                next.modifyRn( oldName, newRdn, deleteOldRdn );
                if ( lut.isReferral( oldName ) )
                {
                    lut.referralChanged( oldName, newName );
                }
                return;
            }
            if ( farthestSrc != null )
            {
                Attributes referral = invocation.getProxy().lookup( farthestSrc,
                    PartitionNexusProxy.LOOKUP_BYPASS );
                Attribute refs = referral.get( REF_ATTR );
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
        }
        else if ( refval.equals( FOLLOW ) )
        {
            throw new NotImplementedException( FOLLOW + " referral handling mode not implemented" );
        }
        else
        {
            throw new LdapNamingException( "Undefined value for " + Context.REFERRAL + " key: " + refval,
                ResultCodeEnum.OTHER );
        }
    }


    private void checkModify( LdapDN name, int modOp, Attributes mods ) throws NamingException
    {
        // -------------------------------------------------------------------
        // Check and update lut if we change the objectClass 
        // -------------------------------------------------------------------

        boolean isTargetReferral = lut.isReferral( name );
        boolean isOcChange = mods.get( OBJCLASS_ATTR ) != null;
        boolean modsOcHasReferral = hasValue( mods.get( OBJCLASS_ATTR ), REFERRAL_OC );
        if ( isOcChange )
        {
            switch ( modOp )
            {
                /* 
                 * if ADD op where refferal is added to objectClass of a
                 * non-referral entry then we add a new referral to lut
                 */
                case ( DirContext.ADD_ATTRIBUTE  ):
                    if ( modsOcHasReferral && !isTargetReferral )
                    {
                        lut.referralAdded( name );
                    }
                    break;
                /* 
                 * if REMOVE op where refferal is removed from objectClass of a
                 * referral entry then we remove the referral from lut
                 */
                case ( DirContext.REMOVE_ATTRIBUTE  ):
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
                case ( DirContext.REPLACE_ATTRIBUTE  ):
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
        }
    }


    public void modify( NextInterceptor next, LdapDN name, int modOp, Attributes mods ) throws NamingException
    {
        Invocation invocation = InvocationStack.getInstance().peek();
        ServerLdapContext caller = ( ServerLdapContext ) invocation.getCaller();
        String refval = ( String ) caller.getEnvironment().get( Context.REFERRAL );

        // handle a normal modify without following referrals
        if ( refval == null || refval.equals( IGNORE ) )
        {
            next.modify( name, modOp, mods );
            checkModify( name, modOp, mods );
            return;
        }

        if ( refval.equals( THROW ) )
        {
            LdapDN farthest = lut.getFarthestReferralAncestor( name );
            if ( farthest == null )
            {
                next.modify( name, modOp, mods );
                checkModify( name, modOp, mods );
                return;
            }

            Attributes referral = invocation.getProxy().lookup( farthest, PartitionNexusProxy.LOOKUP_BYPASS );
            Attribute refs = referral.get( REF_ATTR );
            doReferralException( farthest, new LdapDN( name.getUpName() ), refs );
        }
        else if ( refval.equals( FOLLOW ) )
        {
            throw new NotImplementedException( FOLLOW + " referral handling mode not implemented" );
        }
        else
        {
            throw new LdapNamingException( "Undefined value for " + Context.REFERRAL + " key: " + refval,
                ResultCodeEnum.OTHER );
        }
    }


    private void checkModify( LdapDN name, ModificationItem[] mods ) throws NamingException
    {
        boolean isTargetReferral = lut.isReferral( name );

        // -------------------------------------------------------------------
        // Check and update lut if we change the objectClass 
        // -------------------------------------------------------------------

        for ( int ii = 0; ii < mods.length; ii++ )
        {
            if ( mods[ii].getAttribute().getID().equalsIgnoreCase( OBJCLASS_ATTR ) )
            {
                boolean modsOcHasReferral = hasValue( mods[ii].getAttribute(), REFERRAL_OC );

                switch ( mods[ii].getModificationOp() )
                {
                    /* 
                     * if ADD op where refferal is added to objectClass of a
                     * non-referral entry then we add a new referral to lut
                     */
                    case ( DirContext.ADD_ATTRIBUTE  ):
                        if ( modsOcHasReferral && !isTargetReferral )
                        {
                            lut.referralAdded( name );
                        }
                        break;
                    /* 
                     * if REMOVE op where refferal is removed from objectClass of a
                     * referral entry then we remove the referral from lut
                     */
                    case ( DirContext.REMOVE_ATTRIBUTE  ):
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
                    case ( DirContext.REPLACE_ATTRIBUTE  ):
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


    public void modify( NextInterceptor next, LdapDN name, ModificationItem[] mods ) throws NamingException
    {
        Invocation invocation = InvocationStack.getInstance().peek();
        ServerLdapContext caller = ( ServerLdapContext ) invocation.getCaller();
        String refval = ( String ) caller.getEnvironment().get( Context.REFERRAL );

        // handle a normal modify without following referrals
        if ( refval == null || refval.equals( IGNORE ) )
        {
            next.modify( name, mods );
            checkModify( name, mods );
            return;
        }

        if ( refval.equals( THROW ) )
        {
            LdapDN farthest = lut.getFarthestReferralAncestor( name );
            if ( farthest == null )
            {
                next.modify( name, mods );
                checkModify( name, mods );
                return;
            }

            Attributes referral = invocation.getProxy().lookup( farthest, PartitionNexusProxy.LOOKUP_BYPASS );
            Attribute refs = referral.get( REF_ATTR );
            doReferralException( farthest, new LdapDN( name.getUpName() ), refs );
        }
        else if ( refval.equals( FOLLOW ) )
        {
            throw new NotImplementedException( FOLLOW + " referral handling mode not implemented" );
        }
        else
        {
            throw new LdapNamingException( "Undefined value for " + Context.REFERRAL + " key: " + refval,
                ResultCodeEnum.OTHER );
        }
    }


    static ExprNode getReferralFilter()
    {
        return new SimpleNode( OBJCLASS_ATTR, REFERRAL_OC, LeafNode.EQUALITY );
    }


    static SearchControls getControls()
    {
        SearchControls controls = new SearchControls();
        controls.setReturningObjFlag( false );
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        return controls;
    }


    public void addContextPartition( NextInterceptor next, PartitionConfiguration cfg ) throws NamingException
    {
        next.addContextPartition( cfg );

        // add referrals immediately after adding the new partition
        Partition partition = cfg.getContextPartition();
        LdapDN suffix = partition.getSuffix();
        Invocation invocation = InvocationStack.getInstance().peek();
        NamingEnumeration list = invocation.getProxy().search( suffix, env, getReferralFilter(), getControls(),
            SEARCH_BYPASS );
        addReferrals( list, suffix );
    }


    public void removeContextPartition( NextInterceptor next, LdapDN suffix ) throws NamingException
    {
        // remove referrals immediately before removing the partition
        Invocation invocation = InvocationStack.getInstance().peek();
        NamingEnumeration list = invocation.getProxy().search( suffix, env, getReferralFilter(), getControls(),
            SEARCH_BYPASS );
        deleteReferrals( list, suffix );

        next.removeContextPartition( suffix );
    }


    private void addReferrals( NamingEnumeration referrals, LdapDN base ) throws NamingException
    {
        while ( referrals.hasMore() )
        {
            SearchResult r = ( SearchResult ) referrals.next();
            LdapDN referral = null;
            LdapDN result = new LdapDN( r.getName() );
            //result = LdapDN.normalize( result, registry.getNormalizerMapping() );
            result.normalize( attrRegistry.getNormalizerMapping() );
            
            if ( r.isRelative() )
            {
                referral = ( LdapDN ) base.clone();
                referral.addAll( result );
            }
            else
            {
                referral = result;
            }
        }
    }


    private void deleteReferrals( NamingEnumeration referrals, LdapDN base ) throws NamingException
    {
        while ( referrals.hasMore() )
        {
            SearchResult r = ( SearchResult ) referrals.next();
            LdapDN referral = null;
            LdapDN result = new LdapDN( r.getName() );
            result.normalize( attrRegistry.getNormalizerMapping() );

            if ( r.isRelative() )
            {
                referral = ( LdapDN ) base.clone();
                referral.addAll( result );
            }
            else
            {
                referral = result;
            }
        }
    }


    public NamingEnumeration search( NextInterceptor next, LdapDN base, Map env, ExprNode filter, SearchControls controls )
        throws NamingException
    {
        Invocation invocation = InvocationStack.getInstance().peek();
        ServerLdapContext caller = ( ServerLdapContext ) invocation.getCaller();
        String refval = ( String ) caller.getEnvironment().get( Context.REFERRAL );

        // handle a normal modify without following referrals
        if ( refval == null || refval.equals( IGNORE ) )
        {
            return next.search( base, env, filter, controls );
        }

        /**
         * THROW_FINDING_BASE is a special setting which allows for finding base to 
         * throw exceptions but not when searching.  While search all results are 
         * returned as if they are regular entries.
         */
        if ( refval.equals( THROW_FINDING_BASE ) )
        {
            if ( lut.isReferral( base ) )
            {
                Attributes referral = invocation.getProxy().lookup( base, PartitionNexusProxy.LOOKUP_BYPASS );
                Attribute refs = referral.get( REF_ATTR );
                doReferralExceptionOnSearchBase( base, refs, controls.getSearchScope() );
            }

            LdapDN farthest = lut.getFarthestReferralAncestor( base );
            if ( farthest == null )
            {
                return next.search( base, env, filter, controls );
            }

            Attributes referral = invocation.getProxy().lookup( farthest, PartitionNexusProxy.LOOKUP_BYPASS );
            Attribute refs = referral.get( REF_ATTR );
            doReferralExceptionOnSearchBase( farthest, new LdapDN( base.getUpName() ), refs, controls.getSearchScope() );
            throw new IllegalStateException( "Should never get here: shutting up compiler" );
        }
        if ( refval.equals( THROW ) )
        {
            if ( lut.isReferral( base ) )
            {
                Attributes referral = invocation.getProxy().lookup( base, PartitionNexusProxy.LOOKUP_BYPASS );
                Attribute refs = referral.get( REF_ATTR );
                doReferralExceptionOnSearchBase( base, refs, controls.getSearchScope() );
            }

            LdapDN farthest = lut.getFarthestReferralAncestor( base );
            if ( farthest == null )
            {
                SearchResultFilteringEnumeration srfe = ( SearchResultFilteringEnumeration ) next.search( base, env,
                    filter, controls );
                return new ReferralHandlingEnumeration( srfe, lut, attrRegistry, nexus, controls.getSearchScope(), true );
            }

            Attributes referral = invocation.getProxy().lookup( farthest, PartitionNexusProxy.LOOKUP_BYPASS );
            Attribute refs = referral.get( REF_ATTR );
            doReferralExceptionOnSearchBase( farthest, new LdapDN( base.getUpName() ), refs, controls.getSearchScope() );
            throw new IllegalStateException( "Should never get here: shutting up compiler" );
        }
        else if ( refval.equals( FOLLOW ) )
        {
            throw new NotImplementedException( FOLLOW + " referral handling mode not implemented" );
        }
        else
        {
            throw new LdapNamingException( "Undefined value for " + Context.REFERRAL + " key: " + refval,
                ResultCodeEnum.OTHER );
        }
    }

    class ReferralFilter implements SearchResultFilter//, SearchResultEnumerationAppender 
    {
        public boolean accept( Invocation invocation, SearchResult result, SearchControls controls )
            throws NamingException
        {
            return false;
        }
    }


    public void doReferralExceptionOnSearchBase( LdapDN base, Attribute refs, int scope ) throws NamingException
    {
        // handle referral here
        List list = new ArrayList( refs.size() );
        for ( int ii = 0; ii < refs.size(); ii++ )
        {
            String val = ( String ) refs.get( ii );

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
                log.error( "Bad URL (" + val + ") for ref in " + base + ".  Reference will be ignored." );
            }

            StringBuffer buf = new StringBuffer();
            buf.append( ldapUrl.getScheme() );
            buf.append( ldapUrl.getHost() );
            if ( ldapUrl.getPort() > 0 )
            {
                buf.append( ":" );
                buf.append( ldapUrl.getPort() );
            }
            buf.append( "/" );
            buf.append( ldapUrl.getDn() );
            buf.append( "??" );

            switch ( scope )
            {
                case ( SearchControls.SUBTREE_SCOPE  ):
                    buf.append( "sub" );
                    break;
                case ( SearchControls.ONELEVEL_SCOPE  ):
                    buf.append( "one" );
                    break;
                case ( SearchControls.OBJECT_SCOPE  ):
                    buf.append( "base" );
                    break;
                default:
                    throw new IllegalStateException( "Unknown recognized search scope: " + scope );
            }

            list.add( buf.toString() );
        }
        LdapReferralException lre = new LdapReferralException( list );
        throw lre;
    }


    public void doReferralExceptionOnSearchBase( LdapDN farthest, LdapDN targetUpdn, Attribute refs, int scope )
        throws NamingException
    {
        // handle referral here
        List list = new ArrayList( refs.size() );
        for ( int ii = 0; ii < refs.size(); ii++ )
        {
            String val = ( String ) refs.get( ii );

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
                log.error( "Bad URL (" + val + ") for ref in " + farthest + ".  Reference will be ignored." );
            }

            LdapDN urlDn = new LdapDN( ldapUrl.getDn().toNormName() );
            urlDn.normalize( attrRegistry.getNormalizerMapping() );
            int diff = targetUpdn.size() - farthest.size();
            LdapDN extra = new LdapDN();
            for ( int jj = 0; jj < diff; jj++ )
            {
                extra.add( targetUpdn.get( farthest.size() + jj ) );
            }

            urlDn.addAll( extra );
            StringBuffer buf = new StringBuffer();
            buf.append( ldapUrl.getScheme() );
            buf.append( ldapUrl.getHost() );
            if ( ldapUrl.getPort() > 0 )
            {
                buf.append( ":" );
                buf.append( ldapUrl.getPort() );
            }
            buf.append( "/" );
            buf.append( urlDn.getUpName() );
            buf.append( "??" );

            switch ( scope )
            {
                case ( SearchControls.SUBTREE_SCOPE  ):
                    buf.append( "sub" );
                    break;
                case ( SearchControls.ONELEVEL_SCOPE  ):
                    buf.append( "one" );
                    break;
                case ( SearchControls.OBJECT_SCOPE  ):
                    buf.append( "base" );
                    break;
                default:
                    throw new IllegalStateException( "Unknown recognized search scope: " + scope );
            }
            list.add( buf.toString() );
        }
        LdapReferralException lre = new LdapReferralException( list );
        throw lre;
    }


    public boolean isReferral( String name ) throws NamingException
    {
        if ( lut.isReferral( name ) )
        {
            return true;
        }

        LdapDN dn = new LdapDN( name );
        dn.normalize( attrRegistry.getNormalizerMapping() );

        if ( lut.isReferral( dn ) )
        {
            return true;
        }

        return false;
    }
}
