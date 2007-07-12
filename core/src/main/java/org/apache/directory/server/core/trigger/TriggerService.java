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

package org.apache.directory.server.core.trigger;


import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.configuration.InterceptorConfiguration;
import org.apache.directory.server.core.configuration.StartupConfiguration;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.InterceptorChain;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.jndi.ServerLdapContext;
import org.apache.directory.server.core.partition.PartitionNexusProxy;
import org.apache.directory.server.core.sp.LdapClassLoader;
import org.apache.directory.server.core.subtree.SubentryService;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.NormalizerMappingResolver;
import org.apache.directory.shared.ldap.trigger.ActionTime;
import org.apache.directory.shared.ldap.trigger.LdapOperation;
import org.apache.directory.shared.ldap.trigger.TriggerSpecification;
import org.apache.directory.shared.ldap.trigger.TriggerSpecificationParser;
import org.apache.directory.shared.ldap.trigger.TriggerSpecification.SPSpec;
import org.apache.directory.shared.ldap.util.DirectoryClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The Trigger Service based on the Trigger Specification.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$
 */
public class TriggerService extends BaseInterceptor
{
    /** the logger for this class */
    private static final Logger log = LoggerFactory.getLogger( TriggerService.class );
    /** the entry trigger attribute string: entryTrigger */
    private static final String ENTRY_TRIGGER_ATTR = "entryTriggerSpecification";

    /** a triggerSpecCache that responds to add, delete, and modify attempts */
    private TriggerSpecCache triggerSpecCache;
    /** a normalizing Trigger Specification parser */
    private TriggerSpecificationParser triggerParser;
    /** the attribute type registry */
    private AttributeTypeRegistry attrRegistry;
    /** */
    private InterceptorChain chain;
    /** whether or not this interceptor is activated */
    private boolean enabled = true;

    /** a Trigger Execution Authorizer */
    private TriggerExecutionAuthorizer triggerExecutionAuthorizer = new SimpleTriggerExecutionAuthorizer();

    /**
     * Adds prescriptiveTrigger TriggerSpecificaitons to a collection of
     * TriggerSpeficaitions by accessing the triggerSpecCache.  The trigger
     * specification cache is accessed for each trigger subentry associated
     * with the entry.
     * Note that subentries are handled differently: their parent, the administrative
     * entry is accessed to determine the perscriptiveTriggers effecting the AP
     * and hence the subentry which is considered to be in the same context.
     *
     * @param triggerSpecs the collection of trigger specifications to add to
     * @param dn the normalized distinguished name of the entry
     * @param entry the target entry that is considered as the trigger source
     * @throws NamingException if there are problems accessing attribute values
     */
    private void addPrescriptiveTriggerSpecs( List<TriggerSpecification> triggerSpecs, PartitionNexusProxy proxy,
        LdapDN dn, Attributes entry ) throws NamingException
    {
        
        /*
         * If the protected entry is a subentry, then the entry being evaluated
         * for perscriptiveTriggerss is in fact the administrative entry.  By
         * substituting the administrative entry for the actual subentry the
         * code below this "if" statement correctly evaluates the effects of
         * perscriptiveTrigger on the subentry.  Basically subentries are considered
         * to be in the same naming context as their access point so the subentries
         * effecting their parent entry applies to them as well.
         */
        if ( entry.get( SchemaConstants.OBJECT_CLASS_AT ).contains( SchemaConstants.SUBENTRY_OC ) )
        {
            LdapDN parentDn = ( LdapDN ) dn.clone();
            parentDn.remove( dn.size() - 1 );
            entry = proxy.lookup( new LookupOperationContext( parentDn ), PartitionNexusProxy.LOOKUP_BYPASS );
        }

        Attribute subentries = entry.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
        if ( subentries == null )
        {
            return;
        }
        for ( int ii = 0; ii < subentries.size(); ii++ )
        {
            String subentryDn = ( String ) subentries.get( ii );
            triggerSpecs.addAll( triggerSpecCache.getSubentryTriggerSpecs( subentryDn ) );
        }
    }

    /**
     * Adds the set of entryTriggers to a collection of trigger specifications.
     * The entryTrigger is parsed and tuples are generated on they fly then
     * added to the collection.
     *
     * @param triggerSpecs the collection of trigger specifications to add to
     * @param entry the target entry that is considered as the trigger source
     * @throws NamingException if there are problems accessing attribute values
     */
    private void addEntryTriggerSpecs( List<TriggerSpecification> triggerSpecs, Attributes entry ) throws NamingException
    {
        Attribute entryTrigger = entry.get( ENTRY_TRIGGER_ATTR );
        if ( entryTrigger == null )
        {
            return;
        }

        for ( int ii = 0; ii < entryTrigger.size(); ii++ )
        {
            String triggerString = ( String ) entryTrigger.get( ii );
            TriggerSpecification item;

            try
            {
                item = triggerParser.parse( triggerString );
            }
            catch ( ParseException e )
            {
                String msg = "failed to parse entryTrigger: " + triggerString;
                log.error( msg, e );
                throw new LdapNamingException( msg, ResultCodeEnum.OPERATIONS_ERROR );
            }

            triggerSpecs.add( item );
        }
    }
    
    /**
     * Return a selection of trigger specifications for a certain type of trigger action time.
     * 
     * @NOTE: This method serves as an extion point for new Action Time types.
     * 
     * @param triggerSpecs
     * @param ldapOperation
     */
    public Map<ActionTime, List<TriggerSpecification>> getActionTimeMappedTriggerSpecsForOperation( List<TriggerSpecification> triggerSpecs, LdapOperation ldapOperation )
    {
        List<TriggerSpecification> afterTriggerSpecs = new ArrayList<TriggerSpecification>();
        Map<ActionTime, List<TriggerSpecification>> triggerSpecMap = new HashMap<ActionTime, List<TriggerSpecification>>();
        
        Iterator<TriggerSpecification> it = triggerSpecs.iterator();
        while ( it.hasNext() )
        {
            TriggerSpecification triggerSpec = it.next();
            if ( triggerSpec.getLdapOperation().equals( ldapOperation ) )
            {
                if ( triggerSpec.getActionTime().equals( ActionTime.AFTER ) )
                {
                    afterTriggerSpecs.add( triggerSpec );
                }
                else
                {
                	
                }    
            }
        }
        
        triggerSpecMap.put( ActionTime.AFTER, afterTriggerSpecs );
        
        return triggerSpecMap;
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Interceptor Overrides
    ////////////////////////////////////////////////////////////////////////////
    
    public void init( DirectoryServiceConfiguration dirServCfg, InterceptorConfiguration intCfg ) throws NamingException
    {
        super.init( dirServCfg, intCfg );
        triggerSpecCache = new TriggerSpecCache( dirServCfg );
        attrRegistry = dirServCfg.getRegistries().getAttributeTypeRegistry();
        triggerParser = new TriggerSpecificationParser
            ( new NormalizerMappingResolver()
                {
                    public Map getNormalizerMapping() throws NamingException
                    {
                        return attrRegistry.getNormalizerMapping();
                    }
                }
            );
        chain = dirServCfg.getInterceptorChain();
        this.enabled = true; // TODO: Get this from the configuration if needed.
    }

    public void add( NextInterceptor next, OperationContext addContext ) throws NamingException
    {
    	LdapDN name = addContext.getDn();
    	Attributes entry = ((AddOperationContext)addContext).getEntry();
    	
        // Bypass trigger handling if the service is disabled.
        if ( !enabled )
        {
            next.add( addContext );
            return;
        }
        
        // Gather supplementary data.
        Invocation invocation = InvocationStack.getInstance().peek();
        PartitionNexusProxy proxy = invocation.getProxy();
        ServerLdapContext callerRootCtx = ( ServerLdapContext ) ( ( ServerLdapContext ) invocation.getCaller() ).getRootContext();
        StoredProcedureParameterInjector injector = new AddStoredProcedureParameterInjector( invocation, name, entry );

        // Gather Trigger Specifications which apply to the entry being added.
        List<TriggerSpecification> triggerSpecs = new ArrayList<TriggerSpecification>();
        addPrescriptiveTriggerSpecs( triggerSpecs, proxy, name, entry );

        /**
         *  NOTE: We do not handle entryTriggerSpecs for ADD operation.
         */
        
        Map<ActionTime, List<TriggerSpecification>> triggerMap = getActionTimeMappedTriggerSpecsForOperation( triggerSpecs, LdapOperation.ADD );
        
        next.add( addContext );
        triggerSpecCache.subentryAdded( name, entry );
        
        // Fire AFTER Triggers.
        List<TriggerSpecification> afterTriggerSpecs = triggerMap.get( ActionTime.AFTER );
        executeTriggers( afterTriggerSpecs, injector, callerRootCtx );
    }

    public void delete( NextInterceptor next, OperationContext deleteContext ) throws NamingException
    {
    	LdapDN name = deleteContext.getDn();
    	
        // Bypass trigger handling if the service is disabled.
        if ( !enabled )
        {
            next.delete( deleteContext );
            return;
        }
        
        // Gather supplementary data.
        Invocation invocation = InvocationStack.getInstance().peek();
        PartitionNexusProxy proxy = invocation.getProxy();
        Attributes deletedEntry = proxy.lookup( new LookupOperationContext( name ), PartitionNexusProxy.LOOKUP_BYPASS );
        ServerLdapContext callerRootCtx = ( ServerLdapContext ) ( ( ServerLdapContext ) invocation.getCaller() ).getRootContext();
        StoredProcedureParameterInjector injector = new DeleteStoredProcedureParameterInjector( invocation, name );

        // Gather Trigger Specifications which apply to the entry being deleted.
        List<TriggerSpecification> triggerSpecs = new ArrayList<TriggerSpecification>();
        addPrescriptiveTriggerSpecs( triggerSpecs, proxy, name, deletedEntry );
        addEntryTriggerSpecs( triggerSpecs, deletedEntry );
        
        Map<ActionTime, List<TriggerSpecification>> triggerMap = getActionTimeMappedTriggerSpecsForOperation( triggerSpecs, LdapOperation.DELETE );
        
        next.delete( deleteContext );
        triggerSpecCache.subentryDeleted( name, deletedEntry );
        
        // Fire AFTER Triggers.
        List<TriggerSpecification> afterTriggerSpecs = triggerMap.get( ActionTime.AFTER );
        executeTriggers( afterTriggerSpecs, injector, callerRootCtx );
    }
    
    public void modify( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        // Bypass trigger handling if the service is disabled.
        if ( !enabled )
        {
            next.modify( opContext );
            return;
        }
        
        LdapDN normName = opContext.getDn();
        
        // Gather supplementary data.
        Invocation invocation = InvocationStack.getInstance().peek();
        PartitionNexusProxy proxy = invocation.getProxy();
        Attributes modifiedEntry = proxy.lookup( new LookupOperationContext( normName ), PartitionNexusProxy.LOOKUP_BYPASS );
        ServerLdapContext callerRootCtx = ( ServerLdapContext ) ( ( ServerLdapContext ) invocation.getCaller() ).getRootContext();
        StoredProcedureParameterInjector injector = new ModifyStoredProcedureParameterInjector( invocation, opContext );

        // Gather Trigger Specifications which apply to the entry being modified.
        List<TriggerSpecification> triggerSpecs = new ArrayList<TriggerSpecification>();
        addPrescriptiveTriggerSpecs( triggerSpecs, proxy, normName, modifiedEntry );
        addEntryTriggerSpecs( triggerSpecs, modifiedEntry );
        
        Map<ActionTime, List<TriggerSpecification>> triggerMap = getActionTimeMappedTriggerSpecsForOperation( triggerSpecs, LdapOperation.MODIFY );
        
        next.modify( opContext );
        triggerSpecCache.subentryModified( opContext, modifiedEntry );
        
        // Fire AFTER Triggers.
        List<TriggerSpecification> afterTriggerSpecs = triggerMap.get( ActionTime.AFTER );
        executeTriggers( afterTriggerSpecs, injector, callerRootCtx );
    }
    

    public void rename( NextInterceptor next, OperationContext renameContext ) throws NamingException
    {
        LdapDN name = renameContext.getDn();
        String newRdn = ((RenameOperationContext)renameContext).getNewRdn();
        boolean deleteOldRn = ((RenameOperationContext)renameContext).getDelOldDn();
        
        // Bypass trigger handling if the service is disabled.
        if ( !enabled )
        {
            next.rename( renameContext );
            return;
        }
        
        // Gather supplementary data.        
        Invocation invocation = InvocationStack.getInstance().peek();
        PartitionNexusProxy proxy = invocation.getProxy();
        Attributes renamedEntry = proxy.lookup( new LookupOperationContext( name ), PartitionNexusProxy.LOOKUP_BYPASS );
        ServerLdapContext callerRootCtx = ( ServerLdapContext ) ( ( ServerLdapContext ) invocation.getCaller() ).getRootContext();
        
        LdapDN oldRDN = new LdapDN( name.getRdn().getUpName() );
        LdapDN newRDN = new LdapDN( newRdn );
        LdapDN oldSuperiorDN = ( LdapDN ) name.clone();
        oldSuperiorDN.remove( oldSuperiorDN.size() - 1 );
        LdapDN newSuperiorDN = ( LdapDN ) oldSuperiorDN.clone();
        LdapDN oldDN = ( LdapDN ) name.clone();
        LdapDN newDN = ( LdapDN ) name.clone();
        newDN.add( newRdn );
        
        StoredProcedureParameterInjector injector = new ModifyDNStoredProcedureParameterInjector(
            invocation, deleteOldRn, oldRDN, newRDN, oldSuperiorDN, newSuperiorDN, oldDN, newDN );
        
        // Gather Trigger Specifications which apply to the entry being renamed.
        List<TriggerSpecification> triggerSpecs = new ArrayList<TriggerSpecification>();
        addPrescriptiveTriggerSpecs( triggerSpecs, proxy, name, renamedEntry );
        addEntryTriggerSpecs( triggerSpecs, renamedEntry );
        
        Map<ActionTime, List<TriggerSpecification>> triggerMap = getActionTimeMappedTriggerSpecsForOperation( triggerSpecs, LdapOperation.MODIFYDN_RENAME );
        
        next.rename( renameContext );
        triggerSpecCache.subentryRenamed( name, newDN );
        
        // Fire AFTER Triggers.
        List<TriggerSpecification> afterTriggerSpecs = triggerMap.get( ActionTime.AFTER );
        executeTriggers( afterTriggerSpecs, injector, callerRootCtx );
    }
    
    public void moveAndRename( NextInterceptor next, OperationContext moveAndRenameContext ) throws NamingException
    {
        LdapDN oriChildName = moveAndRenameContext.getDn();
        LdapDN parent = ((MoveAndRenameOperationContext)moveAndRenameContext).getParent();
        String newRn = ((MoveAndRenameOperationContext)moveAndRenameContext).getNewRdn();
        boolean deleteOldRn = ((MoveAndRenameOperationContext)moveAndRenameContext).getDelOldDn();

        // Bypass trigger handling if the service is disabled.
        if ( !enabled )
        {
            next.moveAndRename( moveAndRenameContext );
            return;
        }
        
        // Gather supplementary data.        
        Invocation invocation = InvocationStack.getInstance().peek();
        PartitionNexusProxy proxy = invocation.getProxy();
        Attributes movedEntry = proxy.lookup( new LookupOperationContext( oriChildName ), PartitionNexusProxy.LOOKUP_BYPASS );
        ServerLdapContext callerRootCtx = ( ServerLdapContext ) ( ( ServerLdapContext ) invocation.getCaller() ).getRootContext();
        
        LdapDN oldRDN = new LdapDN( oriChildName.getRdn().getUpName() );
        LdapDN newRDN = new LdapDN( newRn );
        LdapDN oldSuperiorDN = ( LdapDN ) oriChildName.clone();
        oldSuperiorDN.remove( oldSuperiorDN.size() - 1 );
        LdapDN newSuperiorDN = ( LdapDN ) parent.clone();
        LdapDN oldDN = ( LdapDN ) oriChildName.clone();
        LdapDN newDN = ( LdapDN ) parent.clone();
        newDN.add( newRn );

        StoredProcedureParameterInjector injector = new ModifyDNStoredProcedureParameterInjector(
            invocation, deleteOldRn, oldRDN, newRDN, oldSuperiorDN, newSuperiorDN, oldDN, newDN );

        // Gather Trigger Specifications which apply to the entry being exported.
        List<TriggerSpecification> exportTriggerSpecs = new ArrayList<TriggerSpecification>();
        addPrescriptiveTriggerSpecs( exportTriggerSpecs, proxy, oriChildName, movedEntry );
        addEntryTriggerSpecs( exportTriggerSpecs, movedEntry );
        
        // Get the entry again without operational attributes
        // because access control subentry operational attributes
        // will not be valid at the new location.
        // This will certainly be fixed by the SubentryService,
        // but after this service.
        Attributes importedEntry = proxy.lookup( new LookupOperationContext( oriChildName ), PartitionNexusProxy.LOOKUP_EXCLUDING_OPR_ATTRS_BYPASS );
        // As the target entry does not exist yet and so
        // its subentry operational attributes are not there,
        // we need to construct an entry to represent it
        // at least with minimal requirements which are object class
        // and access control subentry operational attributes.
        SubentryService subentryService = ( SubentryService ) chain.get( StartupConfiguration.SUBENTRY_SERVICE_NAME );
        Attributes fakeImportedEntry = subentryService.getSubentryAttributes( newDN, importedEntry );
        NamingEnumeration attrList = importedEntry.getAll();
        while ( attrList.hasMore() )
        {
            fakeImportedEntry.put( ( Attribute ) attrList.next() );
        }
        
        // Gather Trigger Specifications which apply to the entry being imported.
        // Note: Entry Trigger Specifications are not valid for Import.
        List<TriggerSpecification> importTriggerSpecs = new ArrayList<TriggerSpecification>();
        addPrescriptiveTriggerSpecs( importTriggerSpecs, proxy, newDN, fakeImportedEntry );
        
        Map<ActionTime, List<TriggerSpecification>> exportTriggerMap = getActionTimeMappedTriggerSpecsForOperation( exportTriggerSpecs, LdapOperation.MODIFYDN_EXPORT );
        
        Map<ActionTime, List<TriggerSpecification>> importTriggerMap = getActionTimeMappedTriggerSpecsForOperation( importTriggerSpecs, LdapOperation.MODIFYDN_IMPORT );
        
        next.moveAndRename( moveAndRenameContext );
        triggerSpecCache.subentryRenamed( oldDN, newDN );
        
        // Fire AFTER Triggers.
        List<TriggerSpecification> afterExportTriggerSpecs = exportTriggerMap.get( ActionTime.AFTER );
        List<TriggerSpecification> afterImportTriggerSpecs = importTriggerMap.get( ActionTime.AFTER );
        executeTriggers( afterExportTriggerSpecs, injector, callerRootCtx );
        executeTriggers( afterImportTriggerSpecs, injector, callerRootCtx );
    }
    
    
    public void move( NextInterceptor next, OperationContext moveContext ) throws NamingException
    {
        // Bypass trigger handling if the service is disabled.
        if ( !enabled )
        {
            next.move( moveContext );
            return;
        }
        
        LdapDN oriChildName = moveContext.getDn();
        LdapDN newParentName = ((MoveOperationContext)moveContext).getParent();
        
        // Gather supplementary data.        
        Invocation invocation = InvocationStack.getInstance().peek();
        PartitionNexusProxy proxy = invocation.getProxy();
        Attributes movedEntry = proxy.lookup( new LookupOperationContext( oriChildName ), PartitionNexusProxy.LOOKUP_BYPASS );
        ServerLdapContext callerRootCtx = ( ServerLdapContext ) ( ( ServerLdapContext ) invocation.getCaller() ).getRootContext();
        
        LdapDN oldRDN = new LdapDN( oriChildName.getRdn().getUpName() );
        LdapDN newRDN = new LdapDN( oriChildName.getRdn().getUpName() );
        LdapDN oldSuperiorDN = ( LdapDN ) oriChildName.clone();
        oldSuperiorDN.remove( oldSuperiorDN.size() - 1 );
        LdapDN newSuperiorDN = ( LdapDN ) newParentName.clone();
        LdapDN oldDN = ( LdapDN ) oriChildName.clone();
        LdapDN newDN = ( LdapDN ) newParentName.clone();
        newDN.add( newRDN.getUpName() );

        StoredProcedureParameterInjector injector = new ModifyDNStoredProcedureParameterInjector(
            invocation, false, oldRDN, newRDN, oldSuperiorDN, newSuperiorDN, oldDN, newDN );

        // Gather Trigger Specifications which apply to the entry being exported.
        List<TriggerSpecification> exportTriggerSpecs = new ArrayList<TriggerSpecification>();
        addPrescriptiveTriggerSpecs( exportTriggerSpecs, proxy, oriChildName, movedEntry );
        addEntryTriggerSpecs( exportTriggerSpecs, movedEntry );
        
        // Get the entry again without operational attributes
        // because access control subentry operational attributes
        // will not be valid at the new location.
        // This will certainly be fixed by the SubentryService,
        // but after this service.
        Attributes importedEntry = proxy.lookup( new LookupOperationContext( oriChildName ), PartitionNexusProxy.LOOKUP_EXCLUDING_OPR_ATTRS_BYPASS );
        // As the target entry does not exist yet and so
        // its subentry operational attributes are not there,
        // we need to construct an entry to represent it
        // at least with minimal requirements which are object class
        // and access control subentry operational attributes.
        SubentryService subentryService = ( SubentryService ) chain.get( StartupConfiguration.SUBENTRY_SERVICE_NAME );
        Attributes fakeImportedEntry = subentryService.getSubentryAttributes( newDN, importedEntry );
        NamingEnumeration attrList = importedEntry.getAll();
        
        while ( attrList.hasMore() )
        {
            fakeImportedEntry.put( ( Attribute ) attrList.next() );
        }
        
        // Gather Trigger Specifications which apply to the entry being imported.
        // Note: Entry Trigger Specifications are not valid for Import.
        List<TriggerSpecification> importTriggerSpecs = new ArrayList<TriggerSpecification>();
        addPrescriptiveTriggerSpecs( importTriggerSpecs, proxy, newDN, fakeImportedEntry );
        
        Map<ActionTime, List<TriggerSpecification>> exportTriggerMap = getActionTimeMappedTriggerSpecsForOperation( exportTriggerSpecs, LdapOperation.MODIFYDN_EXPORT );
        
        Map<ActionTime, List<TriggerSpecification>> importTriggerMap = getActionTimeMappedTriggerSpecsForOperation( importTriggerSpecs, LdapOperation.MODIFYDN_IMPORT );
        
        next.move( moveContext );
        triggerSpecCache.subentryRenamed( oldDN, newDN );
        
        // Fire AFTER Triggers.
        List<TriggerSpecification> afterExportTriggerSpecs = exportTriggerMap.get( ActionTime.AFTER );
        List<TriggerSpecification> afterImportTriggerSpecs = importTriggerMap.get( ActionTime.AFTER );
        executeTriggers( afterExportTriggerSpecs, injector, callerRootCtx );
        executeTriggers( afterImportTriggerSpecs, injector, callerRootCtx );
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Utility Methods
    ////////////////////////////////////////////////////////////////////////////
    
    private Object executeTriggers( List<TriggerSpecification> triggerSpecs, StoredProcedureParameterInjector injector, ServerLdapContext callerRootCtx ) throws NamingException
    {
        Object result = null;
        
        Iterator<TriggerSpecification> it = triggerSpecs.iterator();
        
        while( it.hasNext() )
        {
            TriggerSpecification tsec = it.next();
            
            // TODO: Replace the Authorization Code with a REAL one.
            if ( triggerExecutionAuthorizer.hasPermission() )
            {
                /**
                 * If there is only one Trigger to be executed, this assignment
                 * will make sense (as in INSTEADOF search Triggers).
                 */
                result = executeTrigger( tsec, injector, callerRootCtx );
            }
        }
        
        /**
         * If only one Trigger has been executed, returning its result
         * can make sense (as in INSTEADOF Search Triggers).
         */
        return result;
    }

    private Object executeTrigger( TriggerSpecification tsec, StoredProcedureParameterInjector injector, ServerLdapContext callerRootCtx ) throws NamingException
    {
    	List<Object> returnValues = new ArrayList<Object>();
    	List<SPSpec> spSpecs = tsec.getSPSpecs();
        for ( SPSpec spSpec : spSpecs )
        {
        	List<Object> arguments = new ArrayList<Object>();
        	arguments.addAll( injector.getArgumentsToInject( spSpec.getParameters() ) );
        	List<Class> typeList = new ArrayList<Class>();
            typeList.addAll( getTypesFromValues( arguments ) );
            Class[] types = getTypesFromValues( arguments ).toArray( EMPTY_CLASS_ARRAY );
            Object[] values = arguments.toArray();
            Object returnValue = executeProcedure( callerRootCtx, spSpec.getName(), types, values );
            returnValues.add(returnValue);
		}
        
        return returnValues; 
    }
    
    private static Class[] EMPTY_CLASS_ARRAY = new Class[ 0 ];
    
    private List<Class> getTypesFromValues( List objects )
    {
        List<Class> types = new ArrayList<Class>();
        
        Iterator it = objects.iterator();
        
        while( it.hasNext() )
        {
            types.add( it.next().getClass() );
        }
        
        return types;
    }
    
    private Object executeProcedure( ServerLdapContext ctx, String procedure, Class[] types, Object[] values ) throws NamingException
    {
        int lastDot = procedure.lastIndexOf( '.' );
        String className = procedure.substring( 0, lastDot );
        String methodName = procedure.substring( lastDot + 1 );
        LdapClassLoader loader = new LdapClassLoader( ctx );
        
        try
        {
            Class clazz = loader.loadClass( className );
            Method proc = DirectoryClassUtils.getAssignmentCompatibleMethod( clazz, methodName, types );
            return proc.invoke( null, values );
        }
        catch ( Exception e )
        {
            log.debug( "Exception occured during executing stored procedure:\n" +
                e.getMessage() + "\n" + e.getStackTrace() );
            LdapNamingException lne = new LdapNamingException( ResultCodeEnum.OTHER );
            lne.setRootCause( e );
            throw lne;
        }
    }

}
