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
package org.apache.directory.server.core.event;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.normalization.NormalizingVisitor;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.core.partition.PartitionNexusProxy;
import org.apache.directory.server.schema.ConcreteNameComponentNormalizer;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.shared.ldap.filter.AndNode;
import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.LeafNode;
import org.apache.directory.shared.ldap.filter.NotNode;
import org.apache.directory.shared.ldap.filter.ScopeNode;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.NameComponentNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Binding;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.event.EventContext;
import javax.naming.event.NamespaceChangeListener;
import javax.naming.event.NamingEvent;
import javax.naming.event.NamingListener;
import javax.naming.event.ObjectChangeListener;


/**
 * An interceptor based serivice for notifying NamingListeners of EventContext
 * and EventDirContext changes.
 *
 * @org.apache.xbean.XBean
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class EventInterceptor extends BaseInterceptor
{
    private static Logger log = LoggerFactory.getLogger( EventInterceptor.class );
    
    private PartitionNexus nexus;
    private Map<NamingListener, Object> sources = new HashMap<NamingListener, Object>();
    private Evaluator evaluator;
    private AttributeTypeRegistry attributeRegistry;
    private NormalizingVisitor visitor;

    
    public void init( DirectoryService directoryService ) throws NamingException
    {
        super.init( directoryService );

        OidRegistry oidRegistry = directoryService.getRegistries().getOidRegistry();
        attributeRegistry = directoryService.getRegistries().getAttributeTypeRegistry();
        evaluator = new ExpressionEvaluator( oidRegistry, attributeRegistry );
        nexus = directoryService.getPartitionNexus();
        NameComponentNormalizer ncn = new ConcreteNameComponentNormalizer( attributeRegistry, oidRegistry );
        visitor = new NormalizingVisitor( ncn, directoryService.getRegistries().getOidRegistry() );
    }


    /**
     * Registers a NamingListener with this service for notification of change.
     *
     * @param ctx the context used to register on (the source)
     * @param name the name of the base/target
     * @param filter the filter to use for evaluating event triggering
     * @param searchControls the search controls to use when evaluating triggering
     * @param namingListener the naming listener to register
     * @throws NamingException if there are failures adding the naming listener
     */
    public void addNamingListener( EventContext ctx, Name name, ExprNode filter, SearchControls searchControls,
        NamingListener namingListener ) throws NamingException
    {
        LdapDN normalizedBaseDn = new LdapDN( name );
        normalizedBaseDn.normalize( attributeRegistry.getNormalizerMapping() );
        
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
            if ( child.getChildren().size() == 1 && ! ( child instanceof NotNode ) )
            {
                filter = child.getFirstChild();
            }
        }
        
        
        ScopeNode scope = new ScopeNode( AliasDerefMode.NEVER_DEREF_ALIASES, normalizedBaseDn.toNormName(),
            searchControls.getSearchScope() );
        
        if ( filter != null )
        {
            BranchNode and = new AndNode();
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
            List<Object> list = new ArrayList<Object>();
            list.add( obj );
            list.add( rec );
            sources.put( namingListener, list );
        }
        else if ( obj instanceof List )
        {
            //noinspection unchecked
            List<Object> list = (List<Object>) obj;
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
            List<EventSourceRecord> list = ( List<EventSourceRecord> ) obj;

            for ( int ii = 0; ii < list.size(); ii++ )
            {
                EventSourceRecord rec =  list.get( ii );
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


    public void add( NextInterceptor next, AddOperationContext opContext ) throws NamingException
    {
    	next.add( opContext );
        //super.add( next, opContext );
        
    	LdapDN name = opContext.getDn();
        Attributes entry = opContext.getEntry();
        
        Set<EventSourceRecord> selecting = getSelectingSources( name, entry );
        
        if ( selecting.isEmpty() )
        {
            return;
        }

        Iterator<EventSourceRecord> list = selecting.iterator();
        
        while ( list.hasNext() )
        {
            EventSourceRecord rec = list.next();
            NamingListener listener = rec.getNamingListener();

            if ( listener instanceof NamespaceChangeListener )
            {
                NamespaceChangeListener nclistener = ( NamespaceChangeListener ) listener;
                Binding binding = new Binding( name.getUpName(), entry, false );
                nclistener.objectAdded( new NamingEvent( rec.getEventContext(), NamingEvent.OBJECT_ADDED, binding,
                    null, entry ) );
            }
        }
    }


    public void delete( NextInterceptor next, DeleteOperationContext opContext ) throws NamingException
    {
    	LdapDN name = opContext.getDn();
        Attributes entry = nexus.lookup( new LookupOperationContext( name ) );

        next.delete( opContext );
        //super.delete( next, opContext );
        
        Set<EventSourceRecord> selecting = getSelectingSources( name, entry );
        
        if ( selecting.isEmpty() )
        {
            return;
        }

        Iterator<EventSourceRecord> list = selecting.iterator();

        while ( list.hasNext() )
        {
            EventSourceRecord rec = ( EventSourceRecord ) list.next();
            NamingListener listener = rec.getNamingListener();

            if ( listener instanceof NamespaceChangeListener )
            {
                NamespaceChangeListener nclistener = ( NamespaceChangeListener ) listener;
                Binding binding = new Binding( name.getUpName(), entry, false );
                nclistener.objectRemoved( new NamingEvent( rec.getEventContext(), NamingEvent.OBJECT_REMOVED, null,
                    binding, entry ) );
            }
        }
    }


    private void notifyOnModify( LdapDN name, List<ModificationItemImpl> mods, Attributes oriEntry ) throws NamingException
    {
        Attributes entry = nexus.lookup( new LookupOperationContext( name ) );
        Set<EventSourceRecord> selecting = getSelectingSources( name, entry );
        
        if ( selecting.isEmpty() )
        {
            return;
        }

        Iterator<EventSourceRecord> list = selecting.iterator();
        
        while ( list.hasNext() )
        {
            EventSourceRecord rec =list.next();
            NamingListener listener = rec.getNamingListener();

            if ( listener instanceof ObjectChangeListener )
            {
                ObjectChangeListener oclistener = ( ObjectChangeListener ) listener;
                Binding before = new Binding( name.getUpName(), oriEntry, false );
                Binding after = new Binding( name.getUpName(), entry, false );
                oclistener.objectChanged( new NamingEvent( rec.getEventContext(), NamingEvent.OBJECT_CHANGED, after,
                    before, mods ) );
            }
        }
    }


    public void modify( NextInterceptor next, ModifyOperationContext opContext ) throws NamingException
    {
        Invocation invocation = InvocationStack.getInstance().peek();
        PartitionNexusProxy proxy = invocation.getProxy();
        Attributes oriEntry = proxy.lookup( new LookupOperationContext( opContext.getDn() ), PartitionNexusProxy.LOOKUP_BYPASS );
        
        next.modify( opContext );

        notifyOnModify( opContext.getDn(), opContext.getModItems(), oriEntry );
    }


    private void notifyOnNameChange( LdapDN oldName, LdapDN newName ) throws NamingException
    {
        Attributes entry = nexus.lookup( new LookupOperationContext( newName ) );
        Set<EventSourceRecord> selecting = getSelectingSources( oldName, entry );
        
        if ( selecting.isEmpty() )
        {
            return;
        }

        Iterator<EventSourceRecord> list = selecting.iterator();
        
        while ( list.hasNext() )
        {
            EventSourceRecord rec = list.next();
            NamingListener listener = rec.getNamingListener();

            if ( listener instanceof NamespaceChangeListener )
            {
                NamespaceChangeListener nclistener = ( NamespaceChangeListener ) listener;
                Binding oldBinding = new Binding( oldName.getUpName(), entry, false );
                Binding newBinding = new Binding( newName.getUpName(), entry, false );
                nclistener.objectRenamed( new NamingEvent( rec.getEventContext(), NamingEvent.OBJECT_RENAMED,
                    newBinding, oldBinding, entry ) );
            }
        }
    }


    public void rename( NextInterceptor next, RenameOperationContext opContext ) throws NamingException
    {
    	next.rename( opContext );
        //super.rename( next, opContext );
        
    	LdapDN newName = ( LdapDN ) opContext.getDn().clone();
        newName.remove( newName.size() - 1 );
        newName.add( opContext.getNewRdn() );
        newName.normalize( attributeRegistry.getNormalizerMapping() );
        notifyOnNameChange( opContext.getDn(), newName );
    }


    public void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext opContext )
        throws NamingException
    {
    	next.moveAndRename( opContext );
        //super.moveAndRename( next, opContext );

        LdapDN newName = ( LdapDN ) opContext.getParent().clone();
        newName.add( opContext.getNewRdn() );
        notifyOnNameChange( opContext.getDn(), newName );
    }


    public void move( NextInterceptor next, MoveOperationContext opContext ) throws NamingException
    {
    	next.move( opContext );
        //super.move( next, opContext );

        LdapDN oriChildName = opContext.getDn();
        
        LdapDN newName = ( LdapDN ) opContext.getParent().clone();
        newName.add( oriChildName.get( oriChildName.size() - 1 ) );
        notifyOnNameChange( oriChildName, newName );
    }


    Set<EventSourceRecord> getSelectingSources( LdapDN name, Attributes entry ) throws NamingException
    {
        if ( sources.isEmpty() )
        {
            return Collections.EMPTY_SET;
        }

        Set<EventSourceRecord> selecting = new HashSet<EventSourceRecord>();
        Iterator<Object> list = sources.values().iterator();
        
        while ( list.hasNext() )
        {
            Object obj = list.next();
        
            if ( obj instanceof EventSourceRecord )
            {
                EventSourceRecord rec = ( EventSourceRecord ) obj;
            
                if ( evaluator.evaluate( rec.getFilter(), name.toNormName(), entry ) )
                {
                    selecting.add( rec );
                }
            }
            else if ( obj instanceof List )
            {
                List<EventSourceRecord> records = ( List<EventSourceRecord> ) obj;
                
                for ( EventSourceRecord rec:records )
                {
                    if ( evaluator.evaluate( rec.getFilter(), name.toNormName(), entry ) )
                    {
                        selecting.add( rec );
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
