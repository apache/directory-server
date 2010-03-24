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


import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.InterceptorChain;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.partition.ByPassConstants;
import org.apache.directory.server.core.sp.StoredProcEngine;
import org.apache.directory.server.core.sp.StoredProcEngineConfig;
import org.apache.directory.server.core.sp.StoredProcExecutionManager;
import org.apache.directory.server.core.sp.java.JavaStoredProcEngineConfig;
import org.apache.directory.server.core.subtree.SubentryInterceptor;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapOperationErrorException;
import org.apache.directory.shared.ldap.exception.LdapOtherException;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.RDN;
import org.apache.directory.shared.ldap.schema.NormalizerMappingResolver;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.normalizers.OidNormalizer;
import org.apache.directory.shared.ldap.trigger.ActionTime;
import org.apache.directory.shared.ldap.trigger.LdapOperation;
import org.apache.directory.shared.ldap.trigger.TriggerSpecification;
import org.apache.directory.shared.ldap.trigger.TriggerSpecificationParser;
import org.apache.directory.shared.ldap.trigger.TriggerSpecification.SPSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The Trigger Service based on the Trigger Specification.
 *
 * @org.apache.xbean.XBean
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$
 */
public class TriggerInterceptor extends BaseInterceptor
{
    /** the logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( TriggerInterceptor.class );
    
    /** the entry trigger attribute string: entryTrigger */
    private static final String ENTRY_TRIGGER_ATTR = "entryTriggerSpecification";

    /** a triggerSpecCache that responds to add, delete, and modify attempts */
    private TriggerSpecCache triggerSpecCache;
    
    /** a normalizing Trigger Specification parser */
    private TriggerSpecificationParser triggerParser;
    
    /** */
    private InterceptorChain chain;
    
    /** whether or not this interceptor is activated */
    private boolean enabled = true;

    /** a Trigger Execution Authorizer */
    private TriggerExecutionAuthorizer triggerExecutionAuthorizer = new SimpleTriggerExecutionAuthorizer();
    
    private StoredProcExecutionManager manager;

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
     * @throws Exception if there are problems accessing attribute values
     * @param proxy the partition nexus proxy 
     */
    private void addPrescriptiveTriggerSpecs( OperationContext opContext, List<TriggerSpecification> triggerSpecs, 
        DN dn, ServerEntry entry ) throws Exception
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
        if ( entry.contains( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.SUBENTRY_OC ) )
        {
            DN parentDn = ( DN ) dn.clone();
            parentDn.remove( dn.size() - 1 );
            
            entry = opContext.lookup( parentDn, ByPassConstants.LOOKUP_BYPASS );
        }

        EntryAttribute subentries = entry.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
        
        if ( subentries == null )
        {
            return;
        }
        
        for ( Value<?> value:subentries )
        {
            String subentryDn = value.getString();
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
     * @throws Exception if there are problems accessing attribute values
     */
    private void addEntryTriggerSpecs( List<TriggerSpecification> triggerSpecs, ServerEntry entry ) throws Exception
    {
        EntryAttribute entryTrigger = entry.get( ENTRY_TRIGGER_ATTR );
        
        if ( entryTrigger == null )
        {
            return;
        }

        for ( Value<?> value:entryTrigger )
        {
            String triggerString = value.getString();
            TriggerSpecification item;

            try
            {
                item = triggerParser.parse( triggerString );
            }
            catch ( ParseException e )
            {
                String msg = I18n.err( I18n.ERR_72, triggerString );
                LOG.error( msg, e );
                throw new LdapOperationErrorException( msg );
            }

            triggerSpecs.add( item );
        }
    }
    
    /**
     * Return a selection of trigger specifications for a certain type of trigger action time.
     * 
     * @note This method serves as an extion point for new Action Time types.
     * 
     * @param triggerSpecs the trigger specifications
     * @param ldapOperation the ldap operation being performed
     * @return the set of trigger specs for a trigger action 
     */
    public Map<ActionTime, List<TriggerSpecification>> getActionTimeMappedTriggerSpecsForOperation( List<TriggerSpecification> triggerSpecs, LdapOperation ldapOperation )
    {
        List<TriggerSpecification> afterTriggerSpecs = new ArrayList<TriggerSpecification>();
        Map<ActionTime, List<TriggerSpecification>> triggerSpecMap = new HashMap<ActionTime, List<TriggerSpecification>>();

        for ( TriggerSpecification triggerSpec : triggerSpecs )
        {
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
    
    public void init( DirectoryService directoryService ) throws Exception
    {
        super.init( directoryService );
        
        triggerSpecCache = new TriggerSpecCache( directoryService );
        final SchemaManager schemaManager = directoryService.getSchemaManager();

        triggerParser = new TriggerSpecificationParser
            ( new NormalizerMappingResolver()
                {
                    public Map<String, OidNormalizer> getNormalizerMapping() throws Exception
                    {
                        return schemaManager.getNormalizerMapping();
                    }
                }
            );
        chain = directoryService.getInterceptorChain();
        
        //StoredProcEngineConfig javaxScriptSPEngineConfig = new JavaxStoredProcEngineConfig();
        StoredProcEngineConfig javaSPEngineConfig = new JavaStoredProcEngineConfig();
        List<StoredProcEngineConfig> spEngineConfigs = new ArrayList<StoredProcEngineConfig>();
        //spEngineConfigs.add( javaxScriptSPEngineConfig );
        spEngineConfigs.add( javaSPEngineConfig );
        String spContainer = "ou=Stored Procedures,ou=system";
        manager = new StoredProcExecutionManager( spContainer, spEngineConfigs );
        
        this.enabled = true; // TODO: Get this from the configuration if needed.
    }

    
    public void add( NextInterceptor next, AddOperationContext addContext ) throws Exception
    {
        DN name = addContext.getDn();
        ServerEntry entry = addContext.getEntry();
        
        // Bypass trigger handling if the service is disabled.
        if ( !enabled )
        {
            next.add( addContext );
            return;
        }
        
        // Gather supplementary data.
        StoredProcedureParameterInjector injector = new AddStoredProcedureParameterInjector( addContext, name, entry );

        // Gather Trigger Specifications which apply to the entry being added.
        List<TriggerSpecification> triggerSpecs = new ArrayList<TriggerSpecification>();
        addPrescriptiveTriggerSpecs( addContext, triggerSpecs, name, entry );

        /**
         *  NOTE: We do not handle entryTriggerSpecs for ADD operation.
         */
        
        Map<ActionTime, List<TriggerSpecification>> triggerMap 
            = getActionTimeMappedTriggerSpecsForOperation( triggerSpecs, LdapOperation.ADD );
        
        next.add( addContext );
        triggerSpecCache.subentryAdded( name, entry );
        
        // Fire AFTER Triggers.
        List<TriggerSpecification> afterTriggerSpecs = triggerMap.get( ActionTime.AFTER );
        executeTriggers( addContext, afterTriggerSpecs, injector );
    }

    
    public void delete( NextInterceptor next, DeleteOperationContext deleteContext ) throws Exception
    {
        DN name = deleteContext.getDn();
        
        // Bypass trigger handling if the service is disabled.
        if ( !enabled )
        {
            next.delete( deleteContext );
            return;
        }
        
        // Gather supplementary data.
        ClonedServerEntry deletedEntry = deleteContext.lookup( name , ByPassConstants.LOOKUP_BYPASS );
        
        StoredProcedureParameterInjector injector = new DeleteStoredProcedureParameterInjector( deleteContext, name );

        // Gather Trigger Specifications which apply to the entry being deleted.
        List<TriggerSpecification> triggerSpecs = new ArrayList<TriggerSpecification>();
        addPrescriptiveTriggerSpecs( deleteContext, triggerSpecs, name, deletedEntry );
        addEntryTriggerSpecs( triggerSpecs, deletedEntry );
        
        Map<ActionTime, List<TriggerSpecification>> triggerMap = 
            getActionTimeMappedTriggerSpecsForOperation( triggerSpecs, LdapOperation.DELETE );
        
        next.delete( deleteContext );
        triggerSpecCache.subentryDeleted( name, deletedEntry );
        
        // Fire AFTER Triggers.
        List<TriggerSpecification> afterTriggerSpecs = triggerMap.get( ActionTime.AFTER );
        executeTriggers( deleteContext, afterTriggerSpecs, injector );
    }
    
    
    public void modify( NextInterceptor next, ModifyOperationContext opContext ) throws Exception
    {
        // Bypass trigger handling if the service is disabled.
        if ( !enabled )
        {
            next.modify( opContext );
            return;
        }
        
        DN normName = opContext.getDn();
        
        // Gather supplementary data.
        ClonedServerEntry modifiedEntry = opContext.lookup( normName, ByPassConstants.LOOKUP_BYPASS );
        
        StoredProcedureParameterInjector injector = new ModifyStoredProcedureParameterInjector( opContext );

        // Gather Trigger Specifications which apply to the entry being modified.
        List<TriggerSpecification> triggerSpecs = new ArrayList<TriggerSpecification>();
        addPrescriptiveTriggerSpecs( opContext, triggerSpecs, normName, modifiedEntry );
        addEntryTriggerSpecs( triggerSpecs, modifiedEntry );
        
        Map<ActionTime, List<TriggerSpecification>> triggerMap = getActionTimeMappedTriggerSpecsForOperation( triggerSpecs, LdapOperation.MODIFY );
        
        next.modify( opContext );
        triggerSpecCache.subentryModified( opContext, modifiedEntry );
        
        // Fire AFTER Triggers.
        List<TriggerSpecification> afterTriggerSpecs = triggerMap.get( ActionTime.AFTER );
        executeTriggers( opContext, afterTriggerSpecs, injector );
    }
    

    public void rename( NextInterceptor next, RenameOperationContext renameContext ) throws Exception
    {
        DN name = renameContext.getDn();
        RDN newRdn = renameContext.getNewRdn();
        boolean deleteOldRn = renameContext.getDelOldDn();
        
        // Bypass trigger handling if the service is disabled.
        if ( !enabled )
        {
            next.rename( renameContext );
            return;
        }
        
        // Gather supplementary data.        
        ServerEntry renamedEntry = (ServerEntry)renameContext.getEntry().getClonedEntry();
        
        // @TODO : To be completely reviewed !!!
        DN oldRDN = new DN( name.getRdn().getName() );
        DN oldSuperiorDN = ( DN ) name.clone();
        oldSuperiorDN.remove( oldSuperiorDN.size() - 1 );
        DN newSuperiorDN = ( DN ) oldSuperiorDN.clone();
        DN oldDN = ( DN ) name.clone();
        DN newDN = ( DN ) name.clone();
        newDN.add( newRdn );
        
        StoredProcedureParameterInjector injector = new ModifyDNStoredProcedureParameterInjector(
            renameContext, deleteOldRn, oldRDN, newRdn, oldSuperiorDN, newSuperiorDN, oldDN, newDN );
        
        // Gather Trigger Specifications which apply to the entry being renamed.
        List<TriggerSpecification> triggerSpecs = new ArrayList<TriggerSpecification>();
        addPrescriptiveTriggerSpecs( renameContext, triggerSpecs, name, renamedEntry );
        addEntryTriggerSpecs( triggerSpecs, renamedEntry );
        
        Map<ActionTime, List<TriggerSpecification>> triggerMap = 
            getActionTimeMappedTriggerSpecsForOperation( triggerSpecs, LdapOperation.MODIFYDN_RENAME );
        
        next.rename( renameContext );
        triggerSpecCache.subentryRenamed( name, newDN );
        
        // Fire AFTER Triggers.
        List<TriggerSpecification> afterTriggerSpecs = triggerMap.get( ActionTime.AFTER );
        executeTriggers( renameContext, afterTriggerSpecs, injector );
    }
    
    
    public void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext opContext ) 
        throws Exception
    {
        DN oriChildName = opContext.getDn();
        DN parent = opContext.getParent();
        RDN newRdn = opContext.getNewRdn();
        boolean deleteOldRn = opContext.getDelOldDn();

        // Bypass trigger handling if the service is disabled.
        if ( !enabled )
        {
            next.moveAndRename( opContext );
            return;
        }
        
        // Gather supplementary data.        
        ClonedServerEntry movedEntry = opContext.lookup( oriChildName, ByPassConstants.LOOKUP_BYPASS );
        
        DN oldRDN = new DN( oriChildName.getRdn().getName() );
        DN oldSuperiorDN = ( DN ) oriChildName.clone();
        oldSuperiorDN.remove( oldSuperiorDN.size() - 1 );
        DN newSuperiorDN = ( DN ) parent.clone();
        DN oldDN = ( DN ) oriChildName.clone();
        DN newDN = ( DN ) parent.clone();
        newDN.add( newRdn.getName() );

        StoredProcedureParameterInjector injector = new ModifyDNStoredProcedureParameterInjector(
            opContext, deleteOldRn, oldRDN, newRdn, oldSuperiorDN, newSuperiorDN, oldDN, newDN );

        // Gather Trigger Specifications which apply to the entry being exported.
        List<TriggerSpecification> exportTriggerSpecs = new ArrayList<TriggerSpecification>();
        addPrescriptiveTriggerSpecs( opContext, exportTriggerSpecs, oriChildName, movedEntry );
        addEntryTriggerSpecs( exportTriggerSpecs, movedEntry );
        
        // Get the entry again without operational attributes
        // because access control subentry operational attributes
        // will not be valid at the new location.
        // This will certainly be fixed by the SubentryInterceptor,
        // but after this service.
        ClonedServerEntry importedEntry = opContext.lookup( oriChildName, 
            ByPassConstants.LOOKUP_EXCLUDING_OPR_ATTRS_BYPASS );
        
        // As the target entry does not exist yet and so
        // its subentry operational attributes are not there,
        // we need to construct an entry to represent it
        // at least with minimal requirements which are object class
        // and access control subentry operational attributes.
        SubentryInterceptor subentryInterceptor = ( SubentryInterceptor ) chain.get( SubentryInterceptor.class.getName() );
        ServerEntry fakeImportedEntry = subentryInterceptor.getSubentryAttributes( newDN, importedEntry );
        
        for ( EntryAttribute attribute:importedEntry )
        {
            fakeImportedEntry.put( attribute );
        }
        
        // Gather Trigger Specifications which apply to the entry being imported.
        // Note: Entry Trigger Specifications are not valid for Import.
        List<TriggerSpecification> importTriggerSpecs = new ArrayList<TriggerSpecification>();
        addPrescriptiveTriggerSpecs( opContext, importTriggerSpecs, newDN, fakeImportedEntry );
        
        Map<ActionTime, List<TriggerSpecification>> exportTriggerMap = 
            getActionTimeMappedTriggerSpecsForOperation( exportTriggerSpecs, LdapOperation.MODIFYDN_EXPORT );
        
        Map<ActionTime, List<TriggerSpecification>> importTriggerMap = 
            getActionTimeMappedTriggerSpecsForOperation( importTriggerSpecs, LdapOperation.MODIFYDN_IMPORT );
        
        next.moveAndRename( opContext );
        triggerSpecCache.subentryRenamed( oldDN, newDN );
        
        // Fire AFTER Triggers.
        List<TriggerSpecification> afterExportTriggerSpecs = exportTriggerMap.get( ActionTime.AFTER );
        List<TriggerSpecification> afterImportTriggerSpecs = importTriggerMap.get( ActionTime.AFTER );
        executeTriggers( opContext, afterExportTriggerSpecs, injector );
        executeTriggers( opContext, afterImportTriggerSpecs, injector );
    }
    
    
    public void move( NextInterceptor next, MoveOperationContext opContext ) throws Exception
    {
        // Bypass trigger handling if the service is disabled.
        if ( !enabled )
        {
            next.move( opContext );
            return;
        }
        
        DN oriChildName = opContext.getDn();
        DN newParentName = opContext.getParent();
        
        // Gather supplementary data.        
        ClonedServerEntry movedEntry = opContext.lookup( oriChildName, ByPassConstants.LOOKUP_BYPASS );
        
        DN oldRDN = new DN( oriChildName.getRdn().getName() );
        RDN newRDN = new RDN( oriChildName.getRdn().getName() );
        DN oldSuperiorDN = ( DN ) oriChildName.clone();
        oldSuperiorDN.remove( oldSuperiorDN.size() - 1 );
        DN newSuperiorDN = ( DN ) newParentName.clone();
        DN oldDN = ( DN ) oriChildName.clone();
        DN newDN = ( DN ) newParentName.clone();
        newDN.add( newRDN.getName() );

        StoredProcedureParameterInjector injector = new ModifyDNStoredProcedureParameterInjector(
            opContext, false, oldRDN, newRDN, oldSuperiorDN, newSuperiorDN, oldDN, newDN );

        // Gather Trigger Specifications which apply to the entry being exported.
        List<TriggerSpecification> exportTriggerSpecs = new ArrayList<TriggerSpecification>();
        addPrescriptiveTriggerSpecs( opContext, exportTriggerSpecs, oriChildName, movedEntry );
        addEntryTriggerSpecs( exportTriggerSpecs, movedEntry );
        
        // Get the entry again without operational attributes
        // because access control subentry operational attributes
        // will not be valid at the new location.
        // This will certainly be fixed by the SubentryInterceptor,
        // but after this service.
        ClonedServerEntry importedEntry = opContext.lookup( oriChildName, 
            ByPassConstants.LOOKUP_EXCLUDING_OPR_ATTRS_BYPASS );

        // As the target entry does not exist yet and so
        // its subentry operational attributes are not there,
        // we need to construct an entry to represent it
        // at least with minimal requirements which are object class
        // and access control subentry operational attributes.
        SubentryInterceptor subentryInterceptor = ( SubentryInterceptor ) chain.get( SubentryInterceptor.class.getName() );
        ServerEntry fakeImportedEntry = subentryInterceptor.getSubentryAttributes( newDN, importedEntry );
        
        for ( EntryAttribute attribute:importedEntry )
        {
            fakeImportedEntry.put( attribute );
        }
        
        // Gather Trigger Specifications which apply to the entry being imported.
        // Note: Entry Trigger Specifications are not valid for Import.
        List<TriggerSpecification> importTriggerSpecs = new ArrayList<TriggerSpecification>();
        addPrescriptiveTriggerSpecs( opContext, importTriggerSpecs, newDN, fakeImportedEntry );
        
        Map<ActionTime, List<TriggerSpecification>> exportTriggerMap = getActionTimeMappedTriggerSpecsForOperation( exportTriggerSpecs, LdapOperation.MODIFYDN_EXPORT );
        
        Map<ActionTime, List<TriggerSpecification>> importTriggerMap = getActionTimeMappedTriggerSpecsForOperation( importTriggerSpecs, LdapOperation.MODIFYDN_IMPORT );
        
        next.move( opContext );
        triggerSpecCache.subentryRenamed( oldDN, newDN );
        
        // Fire AFTER Triggers.
        List<TriggerSpecification> afterExportTriggerSpecs = exportTriggerMap.get( ActionTime.AFTER );
        List<TriggerSpecification> afterImportTriggerSpecs = importTriggerMap.get( ActionTime.AFTER );
        executeTriggers( opContext, afterExportTriggerSpecs, injector );
        executeTriggers( opContext, afterImportTriggerSpecs, injector );
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // Utility Methods
    ////////////////////////////////////////////////////////////////////////////
    
    
    private Object executeTriggers( OperationContext opContext, List<TriggerSpecification> triggerSpecs, 
        StoredProcedureParameterInjector injector ) throws Exception
    {
        Object result = null;

        for ( TriggerSpecification triggerSpec : triggerSpecs )
        {
            // TODO: Replace the Authorization Code with a REAL one.
            if ( triggerExecutionAuthorizer.hasPermission( opContext ) )
            {
                /**
                 * If there is only one Trigger to be executed, this assignment
                 * will make sense (as in INSTEADOF search Triggers).
                 */
                result = executeTrigger( opContext, triggerSpec, injector );
            }
        }
        
        /**
         * If only one Trigger has been executed, returning its result
         * can make sense (as in INSTEADOF Search Triggers).
         */
        return result;
    }

    private Object executeTrigger( OperationContext opContext, TriggerSpecification tsec, 
        StoredProcedureParameterInjector injector ) throws Exception
    {
        List<Object> returnValues = new ArrayList<Object>();
        List<SPSpec> spSpecs = tsec.getSPSpecs();
        for ( SPSpec spSpec : spSpecs )
        {
            List<Object> arguments = new ArrayList<Object>();
            arguments.addAll( injector.getArgumentsToInject( opContext, spSpec.getParameters() ) );
            Object[] values = arguments.toArray();
            Object returnValue = executeProcedure( opContext, spSpec.getName(), values );
            returnValues.add( returnValue );
        }
        
        return returnValues; 
    }

    
    private Object executeProcedure( OperationContext opContext, String procedure, Object[] values ) throws Exception
    {
        
        try
        {
            ClonedServerEntry spUnit = manager.findStoredProcUnit( opContext.getSession(), procedure );
            StoredProcEngine engine = manager.getStoredProcEngineInstance( spUnit );
            return engine.invokeProcedure( opContext.getSession(), procedure, values );
        }
        catch ( Exception e )
        {
            LdapOtherException lne = new LdapOtherException( e.getMessage() );
            lne.initCause( e );
            throw lne;
        }
    }

}
