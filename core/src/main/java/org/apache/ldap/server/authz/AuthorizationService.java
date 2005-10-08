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
package org.apache.ldap.server.authz;


import org.apache.ldap.server.DirectoryServiceConfiguration;
import org.apache.ldap.server.interceptor.BaseInterceptor;
import org.apache.ldap.server.interceptor.NextInterceptor;
import org.apache.ldap.server.interceptor.InterceptorChain;
import org.apache.ldap.server.jndi.ServerContext;
import org.apache.ldap.server.configuration.InterceptorConfiguration;
import org.apache.ldap.server.partition.DirectoryPartitionNexus;
import org.apache.ldap.server.authz.support.ACDFEngine;
import org.apache.ldap.server.invocation.InvocationStack;
import org.apache.ldap.server.authn.LdapPrincipal;
import org.apache.ldap.server.schema.ConcreteNameComponentNormalizer;
import org.apache.ldap.server.subtree.SubentryService;
import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.aci.MicroOperation;
import org.apache.ldap.common.aci.ACIItemParser;
import org.apache.ldap.common.aci.ACIItem;
import org.apache.ldap.common.exception.LdapNamingException;
import org.apache.ldap.common.message.ResultCodeEnum;
import org.apache.ldap.common.util.NamespaceTools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.*;
import java.util.*;
import java.text.ParseException;


/**
 * An ACI based authorization service.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AuthorizationService extends BaseInterceptor
{
    /** the logger for this class */
    private static final Logger log = LoggerFactory.getLogger( AuthorizationService.class );

    private static final String ENTRYACI_ATTR = "entryACI";
    private static final String SUBENTRYACI_ATTR = "subentryACI";
    private static final String AC_SUBENTRY_ATTR = "accessControlSubentries";

    /** the partition nexus */
    private DirectoryPartitionNexus nexus;
    /** a tupleCache that responds to add, delete, and modify attempts */
    private TupleCache tupleCache;
    /** a groupCache that responds to add, delete, and modify attempts */
    private GroupCache groupCache;
    /** a normalizing ACIItem parser */
    private ACIItemParser aciParser;
    /** use and instance of the ACDF engine */
    private ACDFEngine engine;
    /** interceptor chain */
    private InterceptorChain chain;
    /** whether or not this interceptor is activated */
    private boolean enabled = false;


    /**
     * Initializes this interceptor based service by getting a handle on the nexus, setting up
     * the tupe and group membership caches and the ACIItem parser and the ACDF engine.
     *
     * @param factoryCfg the ContextFactory configuration for the server
     * @param cfg the interceptor configuration
     * @throws NamingException if there are problems during initialization
     */
    public void init( DirectoryServiceConfiguration factoryCfg, InterceptorConfiguration cfg ) throws NamingException
    {
        super.init( factoryCfg, cfg );

        nexus = factoryCfg.getPartitionNexus();
        tupleCache = new TupleCache( factoryCfg );
        groupCache = new GroupCache( factoryCfg );
        aciParser = new ACIItemParser( new ConcreteNameComponentNormalizer(
                factoryCfg.getGlobalRegistries().getAttributeTypeRegistry() ) );
        engine = new ACDFEngine( factoryCfg.getGlobalRegistries().getOidRegistry(),
                factoryCfg.getGlobalRegistries().getAttributeTypeRegistry() );
        chain = factoryCfg.getInterceptorChain();
        enabled = factoryCfg.getStartupConfiguration().isAccessControlEnabled();
    }


    /**
     * Adds perscriptiveACI tuples to a collection of tuples by accessing the
     * tupleCache.  The tuple cache is accessed for each A/C subentry
     * associated with the protected entry.  Note that subentries are handled
     * differently: their parent, the administrative entry is accessed to
     * determine the perscriptiveACIs effecting the AP and hence the subentry
     * which is considered to be in the same context.
     *
     * @param tuples the collection of tuples to add to
     * @param dn the normalized distinguished name of the protected entry
     * @param entry the target entry that access to is being controled
     * @throws NamingException if there are problems accessing attribute values
     */
    private void addPerscriptiveAciTuples( Collection tuples, Name dn, Attributes entry ) throws NamingException
    {
        /*
         * If the protected entry is a subentry, then the entry being evaluated
         * for perscriptiveACIs is in fact the administrative entry.  By
         * substituting the administrative entry for the actual subentry the
         * code below this "if" statement correctly evaluates the effects of
         * perscriptiveACI on the subentry.  Basically subentries are considered
         * to be in the same naming context as their access point so the subentries
         * effecting their parent entry applies to them as well.
         */
        if ( entry.get( "objectClass" ).contains( "subentry" ) )
        {
            Name parentDn = ( Name ) dn.clone();
            parentDn.remove( dn.size() - 1 );
            entry = nexus.lookup( parentDn );
        }

        Attribute subentries = entry.get( AC_SUBENTRY_ATTR );
        if ( subentries == null ) return;
        for ( int ii = 0; ii < subentries.size(); ii++ )
        {
            String subentryDn = ( String ) subentries.get( ii );
            tuples.addAll( tupleCache.getACITuples( subentryDn ) );
        }
    }


    /**
     * Adds the set of entryACI tuples to a collection of tuples.  The entryACI
     * is parsed and tuples are generated on they fly then added to the collection.
     *
     * @param tuples the collection of tuples to add to
     * @param entry the target entry that access to is being regulated
     * @throws NamingException if there are problems accessing attribute values
     */
    private void addEntryAciTuples( Collection tuples, Attributes entry ) throws NamingException
    {
        Attribute entryAci = entry.get( ENTRYACI_ATTR );
        if ( entryAci == null ) return;

        for ( int ii = 0; ii < entryAci.size(); ii++ )
        {
            String aciString = ( String ) entryAci.get( ii );
            ACIItem item;

            try
            {
                item = aciParser.parse( aciString );
            }
            catch ( ParseException e )
            {
                String msg = "failed to parse entryACI: " + aciString ;
                log.error( msg, e );
                throw new LdapNamingException( msg, ResultCodeEnum.OPERATIONSERROR );
            }

            tuples.add( item.toTuples() );
        }
    }


    /**
     * Adds the set of subentryACI tuples to a collection of tuples.  The subentryACI
     * is parsed and tuples are generated on the fly then added to the collection.
     *
     * @param tuples the collection of tuples to add to
     * @param dn the normalized distinguished name of the protected entry
     * @param entry the target entry that access to is being regulated
     * @throws NamingException if there are problems accessing attribute values
     */
    private void addSubentryAciTuples( Collection tuples, Name dn, Attributes entry ) throws NamingException
    {
        // only perform this for subentries
        if ( ! entry.get( "objectClass" ).contains( "subentry" ) ) return;

        // get the parent or administrative entry for this subentry since it
        // will contain the subentryACI attributes that effect subentries
        Name parentDn = ( Name ) dn.clone();
        parentDn.remove( dn.size() - 1 );
        Attributes administrativeEntry = nexus.lookup( parentDn );
        Attribute subentryAci = administrativeEntry.get( SUBENTRYACI_ATTR );

        if ( subentryAci == null ) return;

        for ( int ii = 0; ii < subentryAci.size(); ii++ )
        {
            String aciString = ( String ) subentryAci.get( ii );
            ACIItem item;

            try
            {
                item = aciParser.parse( aciString );
            }
            catch ( ParseException e )
            {
                String msg = "failed to parse subentryACI: " + aciString ;
                log.error( msg, e );
                throw new LdapNamingException( msg, ResultCodeEnum.OPERATIONSERROR );
            }

            tuples.add( item.toTuples() );
        }
    }


    /* -------------------------------------------------------------------------------
     * Within every access controled interceptor method we must retrieve the ACITuple
     * set for all the perscriptiveACIs that apply to the candidate, the target entry
     * operated upon.  This ACITuple set is gotten from the TupleCache by looking up
     * the subentries referenced by the accessControlSubentries operational attribute
     * within the target entry.
     *
     * Then the entry is inspected for an entryACI.  This is not done for the add op
     * since it could introduce a security breech.  So for non-add ops if present a
     * set of ACITuples are generated for all the entryACIs within the entry.  This
     * set is combined with the ACITuples cached for the perscriptiveACI affecting
     * the target entry.
     *
     * The union of ACITuples are fed into the engine along with other parameters
     * to decide where permission is granted or rejected for the specific operation.
     * -------------------------------------------------------------------------------
     */

    public void add( NextInterceptor next, String upName, Name normName, Attributes entry ) throws NamingException
    {
        // Access the principal requesting the operation, and bypass checks if it is the admin
        LdapPrincipal user = ( ( ServerContext ) InvocationStack.getInstance().peek().getCaller() ).getPrincipal();
        if ( user.getName().equalsIgnoreCase( DirectoryPartitionNexus.ADMIN_PRINCIPAL ) || ! enabled )
        {
            next.add( upName, normName, entry );
            tupleCache.subentryAdded( upName, normName, entry );
            groupCache.groupAdded( upName, normName, entry );
            return;
        }

        // perform checks below here
        SubentryService subentryService = ( SubentryService ) chain.get( "subentryService" );
        Attributes subentryAttrs = subentryService.getSubentryAttributes( normName, entry );
        NamingEnumeration attrList = entry.getAll();
        while( attrList.hasMore() )
        {
            subentryAttrs.put( ( Attribute ) attrList.next() );
        }

        // Assemble all the information required to make an access control decision
        Set userGroups = groupCache.getGroups( user.getName() );
        Collection tuples = new HashSet();

        // note that entryACI should not be considered in adds (it's a security breach)
        addPerscriptiveAciTuples( tuples, normName, subentryAttrs );
        addSubentryAciTuples( tuples, normName, subentryAttrs );
        Collection perms = Collections.singleton( MicroOperation.ADD );
        engine.checkPermission( next, userGroups, user.getJndiName(), user.getAuthenticationLevel(), normName, null,
            null, perms, tuples, subentryAttrs );
        NamingEnumeration attributeList = entry.getAll();
        while ( attributeList.hasMore() )
        {
            Attribute attr = ( Attribute ) attributeList.next();
            for ( int ii = 0; ii < attr.size(); ii++ )
            {
                engine.checkPermission( next, userGroups, user.getJndiName(), user.getAuthenticationLevel(), normName,
                        attr.getID(), attr.get( ii ), perms, tuples, entry );
            }
        }

        // if we've gotten this far then access is granted
        next.add( upName, normName, entry );
        tupleCache.subentryAdded( upName, normName, entry );
        groupCache.groupAdded( upName, normName, entry );
    }


    public void delete( NextInterceptor next, Name name ) throws NamingException
    {
        // Access the principal requesting the operation, and bypass checks if it is the admin
        Attributes entry = nexus.lookup( name );
        LdapPrincipal user = ( ( ServerContext ) InvocationStack.getInstance().peek().getCaller() ).getPrincipal();
        if ( user.getName().equalsIgnoreCase( DirectoryPartitionNexus.ADMIN_PRINCIPAL ) || ! enabled )
        {
            next.delete( name );
            tupleCache.subentryDeleted( name, entry );
            groupCache.groupDeleted( name, entry );
            return;
        }

        Set userGroups = groupCache.getGroups( user.getName() );
        Collection tuples = new HashSet();
        addPerscriptiveAciTuples( tuples, name, entry );
        addEntryAciTuples( tuples, entry );
        addSubentryAciTuples( tuples, name, entry );

        engine.checkPermission( next, userGroups, user.getJndiName(), user.getAuthenticationLevel(), name, null,
                null, Collections.singleton( MicroOperation.REMOVE ), tuples, entry );

        next.delete( name );
        tupleCache.subentryDeleted( name, entry );
        groupCache.groupDeleted( name, entry );
    }


    public void modify( NextInterceptor next, Name name, int modOp, Attributes mods ) throws NamingException
    {
        // Access the principal requesting the operation, and bypass checks if it is the admin
        Attributes entry = nexus.lookup( name );
        LdapPrincipal user = ( ( ServerContext ) InvocationStack.getInstance().peek().getCaller() ).getPrincipal();
        if ( user.getName().equalsIgnoreCase( DirectoryPartitionNexus.ADMIN_PRINCIPAL ) || ! enabled )
        {
            next.modify( name, modOp, mods );
            tupleCache.subentryModified( name, modOp, mods, entry );
            groupCache.groupModified( name, modOp, mods, entry );
            return;
        }

        Set userGroups = groupCache.getGroups( user.getName() );
        Collection tuples = new HashSet();
        addPerscriptiveAciTuples( tuples, name, entry );
        addEntryAciTuples( tuples, entry );
        addSubentryAciTuples( tuples, name, entry );

        engine.checkPermission( next, userGroups, user.getJndiName(), user.getAuthenticationLevel(), name, null,
                null, Collections.singleton( MicroOperation.MODIFY ), tuples, entry );

        NamingEnumeration attrList = mods.getAll();
        Collection perms = null;
        switch( modOp )
        {
            case( DirContext.ADD_ATTRIBUTE ):
                perms = Collections.singleton( MicroOperation.ADD );
                break;
            case( DirContext.REMOVE_ATTRIBUTE ):
                perms = Collections.singleton( MicroOperation.REMOVE );
                break;
            case( DirContext.REPLACE_ATTRIBUTE ):
                perms = new HashSet();
                perms.add( MicroOperation.ADD );
                perms.add( MicroOperation.REMOVE );
                break;
        }

        while( attrList.hasMore() )
        {
            Attribute attr = ( Attribute ) attrList.next();
            for ( int ii = 0; ii < attr.size(); ii++ )
            {
                engine.checkPermission( next, userGroups, user.getJndiName(), user.getAuthenticationLevel(),
                        name, attr.getID(), attr.get( ii ), perms, tuples, entry );
            }
        }

        next.modify( name, modOp, mods );
        tupleCache.subentryModified( name, modOp, mods, entry );
        groupCache.groupModified( name, modOp, mods, entry );
    }


    public void modify( NextInterceptor next, Name name, ModificationItem[] mods ) throws NamingException
    {
        // Access the principal requesting the operation, and bypass checks if it is the admin
        Attributes entry = nexus.lookup( name );
        LdapPrincipal user = ( ( ServerContext ) InvocationStack.getInstance().peek().getCaller() ).getPrincipal();
        if ( user.getName().equalsIgnoreCase( DirectoryPartitionNexus.ADMIN_PRINCIPAL ) || ! enabled )
        {
            next.modify( name, mods );
            tupleCache.subentryModified( name, mods, entry );
            groupCache.groupModified( name, mods, entry );
            return;
        }

        Set userGroups = groupCache.getGroups( user.getName() );
        Collection tuples = new HashSet();
        addPerscriptiveAciTuples( tuples, name, entry );
        addEntryAciTuples( tuples, entry );
        addSubentryAciTuples( tuples, name, entry );

        engine.checkPermission( next, userGroups, user.getJndiName(), user.getAuthenticationLevel(), name, null,
                null, Collections.singleton( MicroOperation.MODIFY ), tuples, entry );

        Collection perms = null;
        Collection remove = Collections.singleton( MicroOperation.REMOVE );
        Collection add = Collections.singleton( MicroOperation.ADD );
        Collection replace = new HashSet();
        replace.add( MicroOperation.ADD );
        replace.add( MicroOperation.REMOVE );

        for ( int ii = 0; ii < mods.length; ii++ )
        {
            switch( mods[ii].getModificationOp() )
            {
                case( DirContext.ADD_ATTRIBUTE ):
                    perms = add;
                    break;
                case( DirContext.REMOVE_ATTRIBUTE ):
                    perms = remove;
                    break;
                case( DirContext.REPLACE_ATTRIBUTE ):
                    perms = replace;
                    break;
            }

            Attribute attr = mods[ii].getAttribute();
            for ( int jj = 0; jj < attr.size(); jj++ )
            {
                engine.checkPermission( next, userGroups, user.getJndiName(), user.getAuthenticationLevel(),
                        name, attr.getID(), attr.get( jj ), perms, tuples, entry );
            }
        }

        next.modify( name, mods );
        tupleCache.subentryModified( name, mods, entry );
        groupCache.groupModified( name, mods, entry );
    }


    public boolean hasEntry( NextInterceptor next, Name name ) throws NamingException
    {
//        Attributes entry = nexus.lookup( name );
//        ServerContext ctx = ( ServerContext ) InvocationStack.getInstance().peek().getCaller();
//        LdapPrincipal user = ctx.getPrincipal();
//        Set userGroups = groupCache.getGroups( user.getName() );
//        Collection tuples = new HashSet();
//        addPerscriptiveAciTuples( tuples, entry );
//        addEntryAciTuples( tuples, entry );
//        addSubentryAciTuples( tuples, entry );
//
//        engine.checkPermission( next, userGroups, user.getJndiName(), user.getAuthenticationLevel(), name, null,
//                null, BROWSE_OPS, tuples );

        return next.hasEntry( name );
    }


    public NamingEnumeration list( NextInterceptor next, Name base ) throws NamingException
    {
//        Attributes entry = nexus.lookup( base );
//        ServerContext ctx = ( ServerContext ) InvocationStack.getInstance().peek().getCaller();
//        LdapPrincipal user = ctx.getPrincipal();
//        Set userGroups = groupCache.getGroups( user.getName() );
//        Collection tuples = new HashSet();
//        addPerscriptiveAciTuples( tuples, entry );
//        addEntryAciTuples( tuples, entry );
//        addSubentryAciTuples( tuples, entry );
//
//        engine.checkPermission( next, userGroups, user.getJndiName(), user.getAuthenticationLevel(), base, null,
//                null, SEARCH_OPS, tuples );

        return super.list( next, base );
    }


    public Iterator listSuffixes( NextInterceptor next, boolean normalized ) throws NamingException
    {
        return super.listSuffixes( next, normalized );
    }


    public Attributes lookup( NextInterceptor next, Name dn, String[] attrIds ) throws NamingException
    {
        return super.lookup( next, dn, attrIds );
    }


    public Attributes lookup( NextInterceptor next, Name name ) throws NamingException
    {
        return super.lookup( next, name );
    }


    public void modifyRn( NextInterceptor next, Name name, String newRn, boolean deleteOldRn ) throws NamingException
    {
        // Access the principal requesting the operation, and bypass checks if it is the admin
        Attributes entry = nexus.lookup( name );
        LdapPrincipal user = ( ( ServerContext ) InvocationStack.getInstance().peek().getCaller() ).getPrincipal();
        Name newName = ( Name ) name.clone();
        newName.remove( name.size() - 1 );
        newName.add( newRn );
        if ( user.getName().equalsIgnoreCase( DirectoryPartitionNexus.ADMIN_PRINCIPAL ) || ! enabled )
        {
            next.modifyRn( name, newRn, deleteOldRn );
            tupleCache.subentryRenamed( name, newName );
            groupCache.groupRenamed( name, newName );
            return;
        }

        Set userGroups = groupCache.getGroups( user.getName() );
        Collection tuples = new HashSet();
        addPerscriptiveAciTuples( tuples, name, entry );
        addEntryAciTuples( tuples, entry );
        addSubentryAciTuples( tuples, name, entry );

        engine.checkPermission( next, userGroups, user.getJndiName(), user.getAuthenticationLevel(), name, null,
                null, Collections.singleton( MicroOperation.RENAME ), tuples, entry );

        if ( deleteOldRn )
        {
            String oldRn = name.get( name.size() - 1 );
            if ( NamespaceTools.hasCompositeComponents( oldRn ) )
            {
                String[] comps = NamespaceTools.getCompositeComponents( oldRn );
                for ( int ii = 0; ii < comps.length; ii++ )
                {
                    String id = NamespaceTools.getRdnAttribute( comps[ii] );
                    String value = NamespaceTools.getRdnValue( comps[ii] );
                    engine.checkPermission( next, userGroups, user.getJndiName(),
                            user.getAuthenticationLevel(), name, id,
                            value, Collections.singleton( MicroOperation.REMOVE ),
                            tuples, entry );
                }
            }
            else
            {
                String id = NamespaceTools.getRdnAttribute( oldRn );
                String value = NamespaceTools.getRdnValue( oldRn );
                engine.checkPermission( next, userGroups, user.getJndiName(),
                        user.getAuthenticationLevel(), name, id,
                        value, Collections.singleton( MicroOperation.REMOVE ),
                        tuples, entry );
            }
        }

        next.modifyRn( name, newRn, deleteOldRn );
        tupleCache.subentryRenamed( name, newName );
        groupCache.groupRenamed( name, newName );
    }


    public void move( NextInterceptor next, Name oriChildName, Name newParentName, String newRn, boolean deleteOldRn )
            throws NamingException
    {
        // Access the principal requesting the operation, and bypass checks if it is the admin
        Attributes entry = nexus.lookup( oriChildName );
        LdapPrincipal user = ( ( ServerContext ) InvocationStack.getInstance().peek().getCaller() ).getPrincipal();
        Name newName = ( Name ) newParentName.clone();
        newName.add( newRn );
        if ( user.getName().equalsIgnoreCase( DirectoryPartitionNexus.ADMIN_PRINCIPAL ) || ! enabled )
        {
            next.move( oriChildName, newParentName, newRn, deleteOldRn );
            tupleCache.subentryRenamed( oriChildName, newName );
            groupCache.groupRenamed( oriChildName, newName );
            return;
        }

        Set userGroups = groupCache.getGroups( user.getName() );
        Collection tuples = new HashSet();
        addPerscriptiveAciTuples( tuples, oriChildName, entry );
        addEntryAciTuples( tuples, entry );
        addSubentryAciTuples( tuples, oriChildName, entry );

        Collection perms = new HashSet();
        perms.add( MicroOperation.RENAME );
        perms.add( MicroOperation.EXPORT );
        engine.checkPermission( next, userGroups, user.getJndiName(), user.getAuthenticationLevel(),
                oriChildName, null, null, perms, tuples, entry );

        Collection destTuples = new HashSet();
        addPerscriptiveAciTuples( destTuples, oriChildName, entry );
        addEntryAciTuples( destTuples, entry );
        addSubentryAciTuples( destTuples, oriChildName, entry );
        engine.checkPermission( next, userGroups, user.getJndiName(), user.getAuthenticationLevel(),
                oriChildName, null, null, Collections.singleton( MicroOperation.IMPORT ), tuples, entry );

        if ( deleteOldRn )
        {
            String oldRn = oriChildName.get( oriChildName.size() - 1 );
            if ( NamespaceTools.hasCompositeComponents( oldRn ) )
            {
                String[] comps = NamespaceTools.getCompositeComponents( oldRn );
                for ( int ii = 0; ii < comps.length; ii++ )
                {
                    String id = NamespaceTools.getRdnAttribute( comps[ii] );
                    String value = NamespaceTools.getRdnValue( comps[ii] );
                    engine.checkPermission( next, userGroups, user.getJndiName(),
                            user.getAuthenticationLevel(), oriChildName, id,
                            value, Collections.singleton( MicroOperation.REMOVE ),
                            tuples, entry );
                }
            }
            else
            {
                String id = NamespaceTools.getRdnAttribute( oldRn );
                String value = NamespaceTools.getRdnValue( oldRn );
                engine.checkPermission( next, userGroups, user.getJndiName(),
                        user.getAuthenticationLevel(), oriChildName, id,
                        value, Collections.singleton( MicroOperation.REMOVE ),
                        tuples, entry );
            }
        }

        next.move( oriChildName, newParentName, newRn, deleteOldRn );
        tupleCache.subentryRenamed( oriChildName, newName );
        groupCache.groupRenamed( oriChildName, newName );
    }


    public void move( NextInterceptor next, Name oriChildName, Name newParentName ) throws NamingException
    {
        // Access the principal requesting the operation, and bypass checks if it is the admin
        Attributes entry = nexus.lookup( oriChildName );
        LdapPrincipal user = ( ( ServerContext ) InvocationStack.getInstance().peek().getCaller() ).getPrincipal();
        Name newName = ( Name ) newParentName.clone();
        newName.add( oriChildName.get( oriChildName.size() - 1 ) );
        if ( user.getName().equalsIgnoreCase( DirectoryPartitionNexus.ADMIN_PRINCIPAL ) || ! enabled )
        {
            next.move( oriChildName, newParentName );
            tupleCache.subentryRenamed( oriChildName, newName );
            groupCache.groupRenamed( oriChildName, newName );
            return;
        }

        Set userGroups = groupCache.getGroups( user.getName() );
        Collection tuples = new HashSet();
        addPerscriptiveAciTuples( tuples, oriChildName, entry );
        addEntryAciTuples( tuples, entry );
        addSubentryAciTuples( tuples, oriChildName, entry );

        engine.checkPermission( next, userGroups, user.getJndiName(), user.getAuthenticationLevel(),
                oriChildName, null, null, Collections.singleton( MicroOperation.EXPORT ), tuples, entry );

        Collection destTuples = new HashSet();
        addPerscriptiveAciTuples( destTuples, oriChildName, entry );
        addEntryAciTuples( destTuples, entry );
        addSubentryAciTuples( destTuples, oriChildName, entry );
        engine.checkPermission( next, userGroups, user.getJndiName(), user.getAuthenticationLevel(),
                oriChildName, null, null, Collections.singleton( MicroOperation.IMPORT ), tuples, entry );

        next.move( oriChildName, newParentName );
        tupleCache.subentryRenamed( oriChildName, newName );
        groupCache.groupRenamed( oriChildName, newName );
    }


    public NamingEnumeration search( NextInterceptor next, Name base, Map env, ExprNode filter,
                                     SearchControls searchCtls ) throws NamingException
    {
//        Attributes entry = nexus.lookup( base );
//        ServerContext ctx = ( ServerContext ) InvocationStack.getInstance().peek().getCaller();
//        LdapPrincipal user = ctx.getPrincipal();
//        Set userGroups = groupCache.getGroups( user.getName() );
//        Collection tuples = new HashSet();
//        addPerscriptiveAciTuples( tuples, entry );
//        addEntryAciTuples( tuples, entry );
//        addSubentryAciTuples( tuples, entry );
//
//        engine.checkPermission( next, userGroups, user.getJndiName(), user.getAuthenticationLevel(), base, null,
//                null, SEARCH_OPS, tuples );

        return super.search( next, base, env, filter, searchCtls );
    }


    public boolean compare( NextInterceptor next, Name name, String oid, Object value ) throws NamingException
    {

        // Access the principal requesting the operation, and bypass checks if it is the admin
        Attributes entry = nexus.lookup( name );
        LdapPrincipal user = ( ( ServerContext ) InvocationStack.getInstance().peek().getCaller() ).getPrincipal();
        if ( user.getName().equalsIgnoreCase( DirectoryPartitionNexus.ADMIN_PRINCIPAL ) || ! enabled )
        {
            return next.compare( name, oid, value );
        }

        Set userGroups = groupCache.getGroups( user.getName() );
        Collection tuples = new HashSet();
        addPerscriptiveAciTuples( tuples, name, entry );
        addEntryAciTuples( tuples, entry );
        addSubentryAciTuples( tuples, name, entry );

        engine.checkPermission( next, userGroups, user.getJndiName(), user.getAuthenticationLevel(), name, null,
                null, Collections.singleton( MicroOperation.READ ), tuples, entry );
        engine.checkPermission( next, userGroups, user.getJndiName(), user.getAuthenticationLevel(), name, oid,
                value, Collections.singleton( MicroOperation.COMPARE ), tuples, entry );

        return next.compare( name, oid, value );
    }


    public void cacheNewGroup( String upName, Name normName, Attributes entry ) throws NamingException
    {
        this.groupCache.groupAdded( upName, normName, entry );
    }
}
