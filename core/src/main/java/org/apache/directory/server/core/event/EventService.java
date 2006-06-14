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
package org.apache.directory.server.core.event;


import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.configuration.InterceptorConfiguration;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.normalization.NormalizingVisitor;
import org.apache.directory.server.core.partition.DirectoryPartitionNexus;
import org.apache.directory.server.core.partition.DirectoryPartitionNexusProxy;
import org.apache.directory.server.core.schema.AttributeTypeRegistry;
import org.apache.directory.server.core.schema.ConcreteNameComponentNormalizer;
import org.apache.directory.server.core.schema.OidRegistry;
import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.LeafNode;
import org.apache.directory.shared.ldap.filter.ScopeNode;
import org.apache.directory.shared.ldap.message.DerefAliasesEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.NameComponentNormalizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.Binding;
import javax.naming.NamingEnumeration;
import javax.naming.event.*;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.Attribute;
import java.util.*;


/**
 * An interceptor based serivice for notifying NamingListeners of EventContext
 * and EventDirContext changes.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class EventService extends BaseInterceptor
{
    private static Logger log = LoggerFactory.getLogger( EventService.class );
    private DirectoryPartitionNexus nexus;
    private Map sources = new HashMap();
    private Evaluator evaluator = null;
    private AttributeTypeRegistry attributeRegistry;
    private NormalizingVisitor visitor;

    
    public void init( DirectoryServiceConfiguration factoryCfg, InterceptorConfiguration cfg ) throws NamingException
    {
        super.init( factoryCfg, cfg );

        OidRegistry oidRegistry = factoryCfg.getGlobalRegistries().getOidRegistry();
        attributeRegistry = factoryCfg.getGlobalRegistries().getAttributeTypeRegistry();
        evaluator = new ExpressionEvaluator( oidRegistry, attributeRegistry );
        nexus = factoryCfg.getPartitionNexus();
        NameComponentNormalizer ncn = new ConcreteNameComponentNormalizer( attributeRegistry, oidRegistry );
        visitor = new NormalizingVisitor( ncn, factoryCfg.getGlobalRegistries().getOidRegistry() );
    }


    /**
     * Registers a NamingListener with this service for notification of change.
     *
     * @param ctx the context used to register on (the source)
     * @param name the name of the base/target
     * @param filter the filter to use for evaluating event triggering
     * @param searchControls the search controls to use when evaluating triggering
     * @param namingListener the naming listener to register
     */
    public void addNamingListener( EventContext ctx, Name name, ExprNode filter, SearchControls searchControls,
        NamingListener namingListener ) throws NamingException
    {
        LdapDN normalizedBaseDn = new LdapDN( name );
        normalizedBaseDn.normalize();
        
        // -------------------------------------------------------------------
        // must normalize the filter here: need to handle special cases
        // -------------------------------------------------------------------

        if ( filter.isLeaf() )
        {
            LeafNode ln = ( LeafNode ) filter;
            if ( !attributeRegistry.hasAttributeType( ln.getAttribute() ) )
            {
                StringBuffer buf = new StringBuffer();
                buf.append( "undefined filter based on undefined attributeType '" );
                buf.append( ln.getAttribute() );
                buf.append( "' not evaluted at all.  Only using scope node." );
                log.warn( buf.toString() );
                filter = null;
            }
            else
            {
                filter.accept( visitor );
            }
        }
        else 
        {
            filter.accept( visitor );
    
            // Check that after pruning/normalization we have a valid branch node at the top
            BranchNode child = ( BranchNode ) filter;

            // If the remaining filter branch node has no children set filter to null
            if ( child.getChildren().size() == 0 )
            {
                log.warn( "Undefined branchnode filter without child nodes not evaluted at all. " +
                        "Only using scope node." );
                filter = null;
            }

            // Now for AND & OR nodes with a single child left replace them with their child
            if ( child.getChildren().size() == 1 && child.getOperator() != BranchNode.NOT )
            {
                filter = child.getChild();
            }
        }
        
        
        ScopeNode scope = new ScopeNode( DerefAliasesEnum.NEVERDEREFALIASES, normalizedBaseDn.toNormName(), 
            searchControls.getSearchScope() );
        
        if ( filter != null )
        {
            BranchNode and = new BranchNode( BranchNode.AND );
            and.addNode( scope );
            and.addNode( filter );
            filter = and;
        }
        else
        {
            filter = scope;
        }
        
        EventSourceRecord rec = new EventSourceRecord( name, filter, ctx, searchControls, namingListener );
        Object obj = sources.get( namingListener );

        if ( obj == null )
        {
            sources.put( namingListener, rec );
        }
        else if ( obj instanceof EventSourceRecord )
        {
            List list = new ArrayList();
            list.add( obj );
            list.add( rec );
            sources.put( namingListener, list );
        }
        else if ( obj instanceof List )
        {
            List list = ( List ) obj;
            list.add( rec );
        }
    }


    public void removeNamingListener( EventContext ctx, NamingListener namingListener )
    {
        Object obj = sources.get( namingListener );

        if ( obj == null )
        {
            return;
        }

        if ( obj instanceof EventSourceRecord )
        {
            sources.remove( namingListener );
        }
        else if ( obj instanceof List )
        {
            List list = ( List ) obj;

            for ( int ii = 0; ii < list.size(); ii++ )
            {
                EventSourceRecord rec = ( EventSourceRecord ) list.get( ii );
                if ( rec.getEventContext() == ctx )
                {
                    list.remove( ii );
                }
            }

            if ( list.isEmpty() )
            {
                sources.remove( namingListener );
            }
        }

    }


    public void add( NextInterceptor next, LdapDN normName, Attributes entry ) throws NamingException
    {
        super.add( next, normName, entry );
        Set selecting = getSelectingSources( normName, entry );
        if ( selecting.isEmpty() )
        {
            return;
        }

        Iterator list = selecting.iterator();
        while ( list.hasNext() )
        {
            EventSourceRecord rec = ( EventSourceRecord ) list.next();
            NamingListener listener = rec.getNamingListener();

            if ( listener instanceof NamespaceChangeListener )
            {
                NamespaceChangeListener nclistener = ( NamespaceChangeListener ) listener;
                Binding binding = new Binding( normName.toUpName(), entry, false );
                nclistener.objectAdded( new NamingEvent( rec.getEventContext(), NamingEvent.OBJECT_ADDED, binding,
                    null, entry ) );
            }
        }
    }


    public void delete( NextInterceptor next, LdapDN name ) throws NamingException
    {
        Attributes entry = nexus.lookup( name );
        super.delete( next, name );
        Set selecting = getSelectingSources( name, entry );
        if ( selecting.isEmpty() )
        {
            return;
        }

        Iterator list = selecting.iterator();
        while ( list.hasNext() )
        {
            EventSourceRecord rec = ( EventSourceRecord ) list.next();
            NamingListener listener = rec.getNamingListener();

            if ( listener instanceof NamespaceChangeListener )
            {
                NamespaceChangeListener nclistener = ( NamespaceChangeListener ) listener;
                Binding binding = new Binding( name.toUpName(), entry, false );
                nclistener.objectRemoved( new NamingEvent( rec.getEventContext(), NamingEvent.OBJECT_REMOVED, null,
                    binding, entry ) );
            }
        }
    }


    private void notifyOnModify( LdapDN name, ModificationItem[] mods, Attributes oriEntry ) throws NamingException
    {
        Attributes entry = nexus.lookup( name );
        Set selecting = getSelectingSources( name, entry );
        if ( selecting.isEmpty() )
        {
            return;
        }

        Iterator list = selecting.iterator();
        while ( list.hasNext() )
        {
            EventSourceRecord rec = ( EventSourceRecord ) list.next();
            NamingListener listener = rec.getNamingListener();

            if ( listener instanceof ObjectChangeListener )
            {
                ObjectChangeListener oclistener = ( ObjectChangeListener ) listener;
                Binding before = new Binding( name.toUpName(), oriEntry, false );
                Binding after = new Binding( name.toUpName(), entry, false );
                oclistener.objectChanged( new NamingEvent( rec.getEventContext(), NamingEvent.OBJECT_CHANGED, after,
                    before, mods ) );
            }
        }
    }


    public void modify( NextInterceptor next, LdapDN name, int modOp, Attributes mods ) throws NamingException
    {
        Invocation invocation = InvocationStack.getInstance().peek();
        DirectoryPartitionNexusProxy proxy = invocation.getProxy();
        Attributes oriEntry = proxy.lookup( name, DirectoryPartitionNexusProxy.LOOKUP_BYPASS );
        super.modify( next, name, modOp, mods );

        // package modifications in ModItem format for event delivery
        ModificationItem[] modItems = new ModificationItem[mods.size()];
        NamingEnumeration list = mods.getAll();
        for ( int ii = 0; ii < modItems.length; ii++ )
        {
            modItems[ii] = new ModificationItem( modOp, ( Attribute ) list.next() );
        }
        notifyOnModify( name, modItems, oriEntry );
    }


    public void modify( NextInterceptor next, LdapDN name, ModificationItem[] mods ) throws NamingException
    {
        Invocation invocation = InvocationStack.getInstance().peek();
        DirectoryPartitionNexusProxy proxy = invocation.getProxy();
        Attributes oriEntry = proxy.lookup( name, DirectoryPartitionNexusProxy.LOOKUP_BYPASS );
        super.modify( next, name, mods );
        notifyOnModify( name, mods, oriEntry );
    }


    private void notifyOnNameChange( LdapDN oldName, LdapDN newName ) throws NamingException
    {
        Attributes entry = nexus.lookup( newName );
        Set selecting = getSelectingSources( oldName, entry );
        if ( selecting.isEmpty() )
        {
            return;
        }

        Iterator list = selecting.iterator();
        while ( list.hasNext() )
        {
            EventSourceRecord rec = ( EventSourceRecord ) list.next();
            NamingListener listener = rec.getNamingListener();

            if ( listener instanceof NamespaceChangeListener )
            {
                NamespaceChangeListener nclistener = ( NamespaceChangeListener ) listener;
                Binding oldBinding = new Binding( oldName.toUpName(), entry, false );
                Binding newBinding = new Binding( newName.toUpName(), entry, false );
                nclistener.objectRenamed( new NamingEvent( rec.getEventContext(), NamingEvent.OBJECT_RENAMED,
                    newBinding, oldBinding, entry ) );
            }
        }
    }


    public void modifyRn( NextInterceptor next, LdapDN name, String newRn, boolean deleteOldRn ) throws NamingException
    {
        super.modifyRn( next, name, newRn, deleteOldRn );
        LdapDN newName = ( LdapDN ) name.clone();
        newName.remove( newName.size() - 1 );
        newName.add( newRn );
        newName.normalize();
        notifyOnNameChange( name, newName );
    }


    public void move( NextInterceptor next, LdapDN oriChildName, LdapDN newParentName, String newRn, boolean deleteOldRn )
        throws NamingException
    {
        super.move( next, oriChildName, newParentName, newRn, deleteOldRn );
        LdapDN newName = ( LdapDN ) newParentName.clone();
        newName.add( newRn );
        notifyOnNameChange( oriChildName, newName );
    }


    public void move( NextInterceptor next, LdapDN oriChildName, LdapDN newParentName ) throws NamingException
    {
        super.move( next, oriChildName, newParentName );
        LdapDN newName = ( LdapDN ) newParentName.clone();
        newName.add( oriChildName.get( oriChildName.size() - 1 ) );
        notifyOnNameChange( oriChildName, newName );
    }


    Set getSelectingSources( LdapDN name, Attributes entry ) throws NamingException
    {
        if ( sources.isEmpty() )
        {
            return Collections.EMPTY_SET;
        }

        Set selecting = new HashSet();
        Iterator list = sources.values().iterator();
        while ( list.hasNext() )
        {
            Object obj = list.next();
            if ( obj instanceof EventSourceRecord )
            {
                EventSourceRecord rec = ( EventSourceRecord ) obj;
                if ( evaluator.evaluate( rec.getFilter(), name.toNormName(), entry ) )
                {
                    selecting.add( obj );
                }
            }
            else if ( obj instanceof List )
            {
                List records = ( List ) obj;
                for ( int ii = 0; ii < records.size(); ii++ )
                {
                    EventSourceRecord rec = ( EventSourceRecord ) records.get( ii );
                    if ( evaluator.evaluate( rec.getFilter(), name.toNormName(), entry ) )
                    {
                        selecting.add( obj );
                    }
                }
            }
            else
            {
                throw new IllegalStateException( "Unexpected class type of " + obj.getClass() );
            }
        }

        return selecting;
    }

    class EventSourceRecord
    {
        private Name base;
        private SearchControls controls;
        private ExprNode filter;
        private EventContext context;
        private NamingListener listener;


        public EventSourceRecord(Name base, ExprNode filter, EventContext context, SearchControls controls,
            NamingListener listener)
        {
            this.filter = filter;
            this.context = context;
            this.base = base;
            this.controls = controls;
            this.listener = listener;
        }


        public NamingListener getNamingListener()
        {
            return listener;
        }


        public ExprNode getFilter()
        {
            return filter;
        }


        public EventContext getEventContext()
        {
            return context;
        }


        public Name getBase()
        {
            return base;
        }


        public SearchControls getSearchControls()
        {
            return controls;
        }
    }
}
