/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.ldap.server.referral;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.commons.lang.NotImplementedException;

import org.apache.ldap.common.codec.util.LdapURL;
import org.apache.ldap.common.codec.util.LdapURLEncodingException;
import org.apache.ldap.common.exception.LdapNamingException;
import org.apache.ldap.common.exception.LdapReferralException;
import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.filter.LeafNode;
import org.apache.ldap.common.filter.SimpleNode;
import org.apache.ldap.common.message.ResultCodeEnum;
import org.apache.ldap.common.name.DnParser;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.server.DirectoryServiceConfiguration;
import org.apache.ldap.server.configuration.DirectoryPartitionConfiguration;
import org.apache.ldap.server.configuration.InterceptorConfiguration;
import org.apache.ldap.server.enumeration.ReferralHandlingEnumeration;
import org.apache.ldap.server.enumeration.SearchResultFilter;
import org.apache.ldap.server.enumeration.SearchResultFilteringEnumeration;
import org.apache.ldap.server.interceptor.BaseInterceptor;
import org.apache.ldap.server.interceptor.NextInterceptor;
import org.apache.ldap.server.invocation.Invocation;
import org.apache.ldap.server.invocation.InvocationStack;
import org.apache.ldap.server.jndi.ServerLdapContext;
import org.apache.ldap.server.partition.DirectoryPartition;
import org.apache.ldap.server.partition.DirectoryPartitionNexus;
import org.apache.ldap.server.partition.DirectoryPartitionNexusProxy;
import org.apache.ldap.server.schema.AttributeTypeRegistry;
import org.apache.ldap.server.schema.ConcreteNameComponentNormalizer;

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
    private static final Logger log = LoggerFactory.getLogger( ReferralService.class );
    private static final String IGNORE = "ignore";
    private static final String THROW = "throw";
    private static final String FOLLOW = "follow";
    private static final String REFERRAL_OC = "referral";
    private static final String OBJCLASS_ATTR = "objectClass";
    private static final Collection SEARCH_BYPASS;
    private static final String REF_ATTR = "ref";

    private ReferralLut lut = new ReferralLut();
    private DirectoryPartitionNexus nexus;
    private DnParser parser;
    private Hashtable env;

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
        c.add( "oldAuthorizationService" );
        c.add( "schemaService" );
        c.add( "subentryService" );
        c.add( "operationalAttributeService" );
        c.add( "referralService" );
        c.add( "eventService" );
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
            if ( ! ( attr.get( ii ) instanceof String ) )
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
        AttributeTypeRegistry atr = dsConfig.getGlobalRegistries().getAttributeTypeRegistry();
        parser = new DnParser( new ConcreteNameComponentNormalizer( atr ) );
        env = dsConfig.getEnvironment();
        
        Iterator suffixes = nexus.listSuffixes( true );
        while ( suffixes.hasNext() )
        {
            Name suffix = new LdapName( ( String ) suffixes.next() );
            addReferrals( nexus.search( suffix, env, getReferralFilter(), getControls() ), suffix );
        }
    }


    public void doReferralException( Name farthest, Name targetUpdn, Attribute refs ) throws NamingException
    {
        // handle referral here
        List list = new ArrayList( refs.size() );
        for ( int ii = 0; ii < refs.size(); ii++ )
        {
            String val = ( String ) refs.get( ii );
            
            // need to add non-ldap URLs as-is
            if ( ! val.startsWith( "ldap" ) )
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
                log.error( "Bad URL ("+ val +") for ref in " + farthest + ".  Reference will be ignored." ); 
            }
            
            Name urlDn = parser.parse( ldapUrl.getDn().toString() );
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
            Name extra = new LdapName(); 
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
            buf.append( urlDn );
            list.add( buf.toString() );
        }
        LdapReferralException lre = new LdapReferralException( list );
        throw lre;
    }
    
    
    public void add( NextInterceptor next, String upName, Name normName, Attributes entry ) throws NamingException
    {
        Invocation invocation = InvocationStack.getInstance().peek();
        ServerLdapContext caller = ( ServerLdapContext ) invocation.getCaller();
        String refval = ( String ) caller.getEnvironment().get( Context.REFERRAL );

        // handle a normal add without following referrals
        if ( refval == null || refval.equals( IGNORE ) )
        {
            next.add( upName, normName, entry );
            if ( isReferral( entry ) ) 
            {
                lut.referralAdded( normName );
            }
            return;
        }

        if ( refval.equals( THROW ) )
        {
            Name farthest = lut.getFarthestReferralAncestor( normName );
            if ( farthest == null ) 
            {
                next.add( upName, normName, entry );
                if ( isReferral( entry ) ) 
                {
                    lut.referralAdded( normName );
                }
                return;
            }
            
            Attributes referral = invocation.getProxy().lookup( farthest, DirectoryPartitionNexusProxy.LOOKUP_BYPASS );
            Attribute refs = referral.get( REF_ATTR );
            doReferralException( farthest, new LdapName( upName ), refs );
        }
        else if ( refval.equals( FOLLOW ) )
        {
            throw new NotImplementedException( FOLLOW + " referral handling mode not implemented" );
        }
        else
        {
            throw new LdapNamingException( "Undefined value for " + Context.REFERRAL  + " key: " 
                + refval, ResultCodeEnum.OTHER );
        }
    }

    
    public boolean compare( NextInterceptor next, Name normName, String oid, Object value ) throws NamingException
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
            Name farthest = lut.getFarthestReferralAncestor( normName );
            if ( farthest == null ) 
            {
                return next.compare( normName, oid, value );
            }
            
            Attributes referral = invocation.getProxy().lookup( farthest, DirectoryPartitionNexusProxy.LOOKUP_BYPASS );
            Attribute refs = referral.get( REF_ATTR );
            doReferralException( farthest, normName, refs );
            
            // we really can't get here since doReferralException will throw an exception
            return false;
        }
        else if ( refval.equals( FOLLOW ) )
        {
            throw new NotImplementedException( FOLLOW + " referral handling mode not implemented" );
        }
        else
        {
            throw new LdapNamingException( "Undefined value for " + Context.REFERRAL  + " key: " 
                + refval, ResultCodeEnum.OTHER );
        }
    }
    
    
    public void delete( NextInterceptor next, Name normName ) throws NamingException 
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
            Name farthest = lut.getFarthestReferralAncestor( normName );
            if ( farthest == null ) 
            {
                next.delete( normName );
                if ( lut.isReferral( normName ) ) 
                {
                    lut.referralDeleted( normName );
                }
                return;
            }
            
            Attributes referral = invocation.getProxy().lookup( farthest, DirectoryPartitionNexusProxy.LOOKUP_BYPASS );
            Attribute refs = referral.get( REF_ATTR );
            doReferralException( farthest, normName, refs );
        }
        else if ( refval.equals( FOLLOW ) )
        {
            throw new NotImplementedException( FOLLOW + " referral handling mode not implemented" );
        }
        else
        {
            throw new LdapNamingException( "Undefined value for " + Context.REFERRAL  + " key: " 
                + refval, ResultCodeEnum.OTHER );
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

    
    public void move( NextInterceptor next, Name oldName, Name newParent ) throws NamingException
    {
        Invocation invocation = InvocationStack.getInstance().peek();
        ServerLdapContext caller = ( ServerLdapContext ) invocation.getCaller();
        String refval = ( String ) caller.getEnvironment().get( Context.REFERRAL );
        Name newName = ( Name ) newParent.clone();
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
            Name farthestSrc = lut.getFarthestReferralAncestor( oldName );
            Name farthestDst = lut.getFarthestReferralAncestor( newName ); // note will not return newName so safe
            if ( farthestSrc == null && farthestDst == null && ! lut.isReferral(  newName ) ) 
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
                Attributes referral = invocation.getProxy().lookup( farthestSrc, DirectoryPartitionNexusProxy.LOOKUP_BYPASS );
                Attribute refs = referral.get( REF_ATTR );
                doReferralException( farthestSrc, oldName, refs );
            }
            else if ( farthestDst != null )
            {
                throw new LdapNamingException( farthestDst + " ancestor is a referral for modifyDn on " + newName 
                    + " so it affects multiple DSAs", ResultCodeEnum.AFFECTSMULTIPLEDSAS );
            }
            else if ( lut.isReferral( newName ) )
            {
                throw new LdapNamingException( newName 
                    + " exists and is a referral for modifyDn destination so it affects multiple DSAs", 
                    ResultCodeEnum.AFFECTSMULTIPLEDSAS );
            }
            
            throw new IllegalStateException( "If you get this exception the server's logic was flawed in handling a " +
                    "modifyDn operation while processing referrals.  Report this as a bug!" );
        }
        else if ( refval.equals( FOLLOW ) )
        {
            throw new NotImplementedException( FOLLOW + " referral handling mode not implemented" );
        }
        else
        {
            throw new LdapNamingException( "Undefined value for " + Context.REFERRAL  + " key: " 
                + refval, ResultCodeEnum.OTHER );
        }
    }
    
    
    public void move( NextInterceptor next, Name oldName, Name newParent, String newRdn, boolean deleteOldRdn ) 
        throws NamingException
    {
        Invocation invocation = InvocationStack.getInstance().peek();
        ServerLdapContext caller = ( ServerLdapContext ) invocation.getCaller();
        String refval = ( String ) caller.getEnvironment().get( Context.REFERRAL );
        Name newName = ( Name ) newParent.clone();
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
            Name farthestSrc = lut.getFarthestReferralAncestor( oldName );
            Name farthestDst = lut.getFarthestReferralAncestor( newName ); // safe to use - does not return newName
            if ( farthestSrc == null && farthestDst == null && ! lut.isReferral( newName ) ) 
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
                Attributes referral = invocation.getProxy().lookup( farthestSrc, DirectoryPartitionNexusProxy.LOOKUP_BYPASS );
                Attribute refs = referral.get( REF_ATTR );
                doReferralException( farthestSrc, oldName, refs );
            }
            else if ( farthestDst != null )
            {
                throw new LdapNamingException( farthestDst + " ancestor is a referral for modifyDn on " + newName 
                    + " so it affects multiple DSAs", ResultCodeEnum.AFFECTSMULTIPLEDSAS );
            }
            else if ( lut.isReferral( newName ) )
            {
                throw new LdapNamingException( newName 
                    + " exists and is a referral for modifyDn destination so it affects multiple DSAs", 
                    ResultCodeEnum.AFFECTSMULTIPLEDSAS );
            }
            
            throw new IllegalStateException( "If you get this exception the server's logic was flawed in handling a " +
                    "modifyDn operation while processing referrals.  Report this as a bug!" );
        }
        else if ( refval.equals( FOLLOW ) )
        {
            throw new NotImplementedException( FOLLOW + " referral handling mode not implemented" );
        }
        else
        {
            throw new LdapNamingException( "Undefined value for " + Context.REFERRAL  + " key: " 
                + refval, ResultCodeEnum.OTHER );
        }
    }


    public void modifyRn( NextInterceptor next, Name oldName, String newRdn, boolean deleteOldRdn ) 
        throws NamingException
    {
        Invocation invocation = InvocationStack.getInstance().peek();
        ServerLdapContext caller = ( ServerLdapContext ) invocation.getCaller();
        String refval = ( String ) caller.getEnvironment().get( Context.REFERRAL );
        Name newName = ( Name ) oldName.clone();
        newName.remove( oldName.size() - 1 );
        newName.add( parser.parse( newRdn ).toString() );

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
            Name farthestSrc = lut.getFarthestReferralAncestor( oldName );
            Name farthestDst = lut.getFarthestReferralAncestor( newName );
            if ( farthestSrc == null && farthestDst == null && ! lut.isReferral( newName ) ) 
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
                Attributes referral = invocation.getProxy().lookup( farthestSrc, DirectoryPartitionNexusProxy.LOOKUP_BYPASS );
                Attribute refs = referral.get( REF_ATTR );
                doReferralException( farthestSrc, oldName, refs );
            }
            else if ( farthestDst != null )
            {
                throw new LdapNamingException( farthestDst + " ancestor is a referral for modifyDn on " + newName 
                    + " so it affects multiple DSAs", ResultCodeEnum.AFFECTSMULTIPLEDSAS );
            }
            else if ( lut.isReferral( newName ) )
            {
                throw new LdapNamingException( newName 
                    + " exists and is a referral for modifyDn destination so it affects multiple DSAs", 
                    ResultCodeEnum.AFFECTSMULTIPLEDSAS );
            }
            
            throw new IllegalStateException( "If you get this exception the server's logic was flawed in handling a " +
                    "modifyDn operation while processing referrals.  Report this as a bug!" );
        }
        else if ( refval.equals( FOLLOW ) )
        {
            throw new NotImplementedException( FOLLOW + " referral handling mode not implemented" );
        }
        else
        {
            throw new LdapNamingException( "Undefined value for " + Context.REFERRAL  + " key: " 
                + refval, ResultCodeEnum.OTHER );
        }
    }
    

    private void checkModify( Name name, int modOp, Attributes mods ) throws NamingException
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
                case( DirContext.ADD_ATTRIBUTE ):
                    if ( modsOcHasReferral && !isTargetReferral )
                    {
                        lut.referralAdded( name );
                    }
                    break;
                /* 
                 * if REMOVE op where refferal is removed from objectClass of a
                 * referral entry then we remove the referral from lut
                 */
                case( DirContext.REMOVE_ATTRIBUTE ):
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
                case( DirContext.REPLACE_ATTRIBUTE ):
                    if ( isTargetReferral && ! modsOcHasReferral )
                    {
                        lut.referralDeleted( name );
                    }
                    else if ( ! isTargetReferral && modsOcHasReferral )
                    {
                        lut.referralAdded( name );
                    }
                    break;
                default:
                    throw new IllegalStateException( "undefined modification operation" );
            }
        }
    }
    
    
    public void modify( NextInterceptor next, Name name, int modOp, Attributes mods ) throws NamingException
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
            Name farthest = lut.getFarthestReferralAncestor( name );
            if ( farthest == null ) 
            {
                next.modify( name, modOp, mods );
                checkModify( name, modOp, mods );
                return;
            }
            
            Attributes referral = invocation.getProxy().lookup( farthest, DirectoryPartitionNexusProxy.LOOKUP_BYPASS );
            Attribute refs = referral.get( REF_ATTR );
            doReferralException( farthest, name, refs );
        }
        else if ( refval.equals( FOLLOW ) )
        {
            throw new NotImplementedException( FOLLOW + " referral handling mode not implemented" );
        }
        else
        {
            throw new LdapNamingException( "Undefined value for " + Context.REFERRAL  + " key: " 
                + refval, ResultCodeEnum.OTHER );
        }
    }
    
    
    private void checkModify( Name name, ModificationItem[] mods ) throws NamingException
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
                    case( DirContext.ADD_ATTRIBUTE ):
                        if ( modsOcHasReferral && !isTargetReferral )
                        {
                            lut.referralAdded( name );
                        }
                        break;
                    /* 
                     * if REMOVE op where refferal is removed from objectClass of a
                     * referral entry then we remove the referral from lut
                     */
                    case( DirContext.REMOVE_ATTRIBUTE ):
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
                    case( DirContext.REPLACE_ATTRIBUTE ):
                        if ( isTargetReferral && ! modsOcHasReferral )
                        {
                            lut.referralDeleted( name );
                        }
                        else if ( ! isTargetReferral && modsOcHasReferral )
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
    
    
    public void modify( NextInterceptor next, Name name, ModificationItem[] mods ) throws NamingException
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
            Name farthest = lut.getFarthestReferralAncestor( name );
            if ( farthest == null ) 
            {
                next.modify( name, mods );
                checkModify( name, mods );
                return;
            }
            
            Attributes referral = invocation.getProxy().lookup( farthest, DirectoryPartitionNexusProxy.LOOKUP_BYPASS );
            Attribute refs = referral.get( REF_ATTR );
            doReferralException( farthest, name, refs );
        }
        else if ( refval.equals( FOLLOW ) )
        {
            throw new NotImplementedException( FOLLOW + " referral handling mode not implemented" );
        }
        else
        {
            throw new LdapNamingException( "Undefined value for " + Context.REFERRAL  + " key: " 
                + refval, ResultCodeEnum.OTHER );
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
    
    
    public void addContextPartition( NextInterceptor next, DirectoryPartitionConfiguration cfg ) throws NamingException
    {
        next.addContextPartition( cfg );
        
        // add referrals immediately after adding the new partition
        DirectoryPartition partition = cfg.getContextPartition();
        Name suffix = partition.getSuffix( true );
        Invocation invocation = InvocationStack.getInstance().peek();
        NamingEnumeration list = invocation.getProxy().search( suffix, env, getReferralFilter(), getControls(), SEARCH_BYPASS );
        addReferrals( list, suffix );
    }
    
    
    public void removeContextPartition( NextInterceptor next, Name suffix ) throws NamingException
    {
        // remove referrals immediately before removing the partition
        Invocation invocation = InvocationStack.getInstance().peek();
        NamingEnumeration list = invocation.getProxy().search( suffix, env, getReferralFilter(), getControls(), SEARCH_BYPASS );
        deleteReferrals( list, suffix );
        
        next.removeContextPartition( suffix );
    }
    
    
    private void addReferrals( NamingEnumeration referrals, Name base ) throws NamingException
    {
        while ( referrals.hasMore() )
        {
            SearchResult r = ( SearchResult ) referrals.next();
            Name referral = null;
            if ( r.isRelative() )
            {
                referral = ( Name ) base.clone();
                referral.addAll( parser.parse( r.getName() ) );
            }
            else
            {
                referral = parser.parse( r.getName() );
            }
        }
    }
    
    
    private void deleteReferrals( NamingEnumeration referrals, Name base ) throws NamingException
    {
        while ( referrals.hasMore() )
        {
            SearchResult r = ( SearchResult ) referrals.next();
            Name referral = null;
            if ( r.isRelative() )
            {
                referral = ( Name ) base.clone();
                referral.addAll( parser.parse( r.getName() ) );
            }
            else
            {
                referral = parser.parse( r.getName() );
            }
        }
    }
    
    
    public NamingEnumeration search( NextInterceptor next, Name base, Map env, ExprNode filter, 
        SearchControls controls ) throws NamingException
    {
        Invocation invocation = InvocationStack.getInstance().peek();
        ServerLdapContext caller = ( ServerLdapContext ) invocation.getCaller();
        String refval = ( String ) caller.getEnvironment().get( Context.REFERRAL );

        // handle a normal modify without following referrals
        if ( refval == null || refval.equals( IGNORE ) )
        {
            return next.search( base, env, filter, controls );
        }

        if ( refval.equals( THROW ) )
        {
            if ( lut.isReferral( base ) )
            {
                Attributes referral = invocation.getProxy().lookup( base, DirectoryPartitionNexusProxy.LOOKUP_BYPASS );
                Attribute refs = referral.get( REF_ATTR );
                doReferralExceptionOnSearchBase( base, refs, controls.getSearchScope() );
            }
            
            Name farthest = lut.getFarthestReferralAncestor( base );
            if ( farthest == null ) 
            {
                SearchResultFilteringEnumeration srfe = ( SearchResultFilteringEnumeration ) 
                    next.search( base, env, filter, controls );
                return new ReferralHandlingEnumeration( srfe, lut, parser, nexus, controls.getSearchScope(), true );
            }
            
            Attributes referral = invocation.getProxy().lookup( farthest, DirectoryPartitionNexusProxy.LOOKUP_BYPASS );
            Attribute refs = referral.get( REF_ATTR );
            doReferralExceptionOnSearchBase( farthest, base, refs, controls.getSearchScope() );
            throw new IllegalStateException( "Should never get here: shutting up compiler" );
        }
        else if ( refval.equals( FOLLOW ) )
        {
            throw new NotImplementedException( FOLLOW + " referral handling mode not implemented" );
        }
        else
        {
            throw new LdapNamingException( "Undefined value for " + Context.REFERRAL  + " key: " 
                + refval, ResultCodeEnum.OTHER );
        }
    }

    
    class ReferralFilter implements SearchResultFilter//, SearchResultEnumerationAppender 
    {
        public boolean accept( Invocation invocation, SearchResult result, SearchControls controls ) throws NamingException
        {
            return false;
        }
    }
    

    public void doReferralExceptionOnSearchBase( Name base, Attribute refs, int scope ) throws NamingException
    {
        // handle referral here
        List list = new ArrayList( refs.size() );
        for ( int ii = 0; ii < refs.size(); ii++ )
        {
            String val = ( String ) refs.get( ii );
            
            // need to add non-ldap URLs as-is
            if ( ! val.startsWith( "ldap" ) )
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
                log.error( "Bad URL ("+ val +") for ref in " + base + ".  Reference will be ignored." ); 
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
                case( SearchControls.SUBTREE_SCOPE ):
                    buf.append( "sub" );
                    break;
                case( SearchControls.ONELEVEL_SCOPE ):
                    buf.append( "one" );
                    break;
                case( SearchControls.OBJECT_SCOPE ):
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


    public void doReferralExceptionOnSearchBase( Name farthest, Name targetUpdn, Attribute refs, int scope ) throws NamingException
    {
        // handle referral here
        List list = new ArrayList( refs.size() );
        for ( int ii = 0; ii < refs.size(); ii++ )
        {
            String val = ( String ) refs.get( ii );
            
            // need to add non-ldap URLs as-is
            if ( ! val.startsWith( "ldap" ) )
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
                log.error( "Bad URL ("+ val +") for ref in " + farthest + ".  Reference will be ignored." ); 
            }
            
            Name urlDn = parser.parse( ldapUrl.getDn().toString() );
            int diff = targetUpdn.size() - farthest.size();
            Name extra = new LdapName(); 
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
            buf.append( urlDn );
            buf.append( "??" );
            
            switch ( scope )
            {
                case( SearchControls.SUBTREE_SCOPE ):
                    buf.append( "sub" );
                    break;
                case( SearchControls.ONELEVEL_SCOPE ):
                    buf.append( "one" );
                    break;
                case( SearchControls.OBJECT_SCOPE ):
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
}
