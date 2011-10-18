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

import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.InterceptorChain;
import org.apache.directory.server.core.api.interceptor.NextInterceptor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.OperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.partition.ByPassConstants;
import org.apache.directory.server.core.api.sp.StoredProcEngine;
import org.apache.directory.server.core.api.sp.StoredProcEngineConfig;
import org.apache.directory.server.core.api.sp.StoredProcExecutionManager;
import org.apache.directory.server.core.api.sp.java.JavaStoredProcEngineConfig;
import org.apache.directory.server.core.api.subtree.SubentryUtils;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapOperationErrorException;
import org.apache.directory.shared.ldap.model.exception.LdapOtherException;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.apache.directory.shared.ldap.model.schema.NormalizerMappingResolver;
import org.apache.directory.shared.ldap.model.schema.normalizers.OidNormalizer;
import org.apache.directory.shared.ldap.trigger.ActionTime;
import org.apache.directory.shared.ldap.trigger.LdapOperation;
import org.apache.directory.shared.ldap.trigger.TriggerSpecification;
import org.apache.directory.shared.ldap.trigger.TriggerSpecification.SPSpec;
import org.apache.directory.shared.ldap.trigger.TriggerSpecificationParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The Trigger Service based on the Trigger Specification.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
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
    
    /** The SubentryUtils instance */
    private static SubentryUtils subentryUtils;

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
        Dn dn, Entry entry ) throws LdapException
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
        if ( entry.contains( OBJECT_CLASS_AT, SchemaConstants.SUBENTRY_OC ) )
        {
            Dn parentDn = dn.getParent();

            entry = opContext.lookup( parentDn, ByPassConstants.LOOKUP_BYPASS, SchemaConstants.ALL_ATTRIBUTES_ARRAY );
        }

        Attribute subentries = entry.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );

        if ( subentries == null )
        {
            return;
        }

        for ( Value<?> value : subentries )
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
    private void addEntryTriggerSpecs( List<TriggerSpecification> triggerSpecs, Entry entry ) throws LdapException
    {
        Attribute entryTrigger = entry.get( ENTRY_TRIGGER_ATTR );

        if ( entryTrigger == null )
        {
            return;
        }

        for ( Value<?> value : entryTrigger )
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
    public Map<ActionTime, List<TriggerSpecification>> getActionTimeMappedTriggerSpecsForOperation(
        List<TriggerSpecification> triggerSpecs, LdapOperation ldapOperation )
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

    public void init( DirectoryService directoryService ) throws LdapException
    {
        super.init( directoryService );

        triggerSpecCache = new TriggerSpecCache( directoryService );

        triggerParser = new TriggerSpecificationParser( new NormalizerMappingResolver()
        {
            public Map<String, OidNormalizer> getNormalizerMapping() throws Exception
            {
                return schemaManager.getNormalizerMapping();
            }
        } );

        chain = directoryService.getInterceptorChain();

        //StoredProcEngineConfig javaxScriptSPEngineConfig = new JavaxStoredProcEngineConfig();
        StoredProcEngineConfig javaSPEngineConfig = new JavaStoredProcEngineConfig();
        List<StoredProcEngineConfig> spEngineConfigs = new ArrayList<StoredProcEngineConfig>();
        //spEngineConfigs.add( javaxScriptSPEngineConfig );
        spEngineConfigs.add( javaSPEngineConfig );
        String spContainer = "ou=Stored Procedures,ou=system";
        manager = new StoredProcExecutionManager( spContainer, spEngineConfigs );

        this.enabled = true; // TODO: Get this from the configuration if needed.
        
        // Init the SubentryUtils instance
        subentryUtils = new SubentryUtils( directoryService );
    }


    public void add( NextInterceptor next, AddOperationContext addContext ) throws LdapException
    {
        Dn name = addContext.getDn();
        Entry entry = addContext.getEntry();

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

        Map<ActionTime, List<TriggerSpecification>> triggerMap = getActionTimeMappedTriggerSpecsForOperation(
            triggerSpecs, LdapOperation.ADD );

        next.add( addContext );
        triggerSpecCache.subentryAdded( name, entry );

        // Fire AFTER Triggers.
        List<TriggerSpecification> afterTriggerSpecs = triggerMap.get( ActionTime.AFTER );
        executeTriggers( addContext, afterTriggerSpecs, injector );
    }


    public void delete( NextInterceptor next, DeleteOperationContext deleteContext ) throws LdapException
    {
        Dn name = deleteContext.getDn();

        // Bypass trigger handling if the service is disabled.
        if ( !enabled )
        {
            next.delete( deleteContext );
            return;
        }

        // Gather supplementary data.
        Entry deletedEntry = deleteContext.getEntry();

        StoredProcedureParameterInjector injector = new DeleteStoredProcedureParameterInjector( deleteContext, name );

        // Gather Trigger Specifications which apply to the entry being deleted.
        List<TriggerSpecification> triggerSpecs = new ArrayList<TriggerSpecification>();
        addPrescriptiveTriggerSpecs( deleteContext, triggerSpecs, name, deletedEntry );
        addEntryTriggerSpecs( triggerSpecs, deletedEntry );

        Map<ActionTime, List<TriggerSpecification>> triggerMap = getActionTimeMappedTriggerSpecsForOperation(
            triggerSpecs, LdapOperation.DELETE );

        next.delete( deleteContext );

        triggerSpecCache.subentryDeleted( name, deletedEntry );

        // Fire AFTER Triggers.
        List<TriggerSpecification> afterTriggerSpecs = triggerMap.get( ActionTime.AFTER );
        executeTriggers( deleteContext, afterTriggerSpecs, injector );
    }


    public void modify( NextInterceptor next, ModifyOperationContext modifyContext ) throws LdapException
    {
        // Bypass trigger handling if the service is disabled.
        if ( !enabled )
        {
            next.modify( modifyContext );
            return;
        }

        Dn normName = modifyContext.getDn();

        // Gather supplementary data.
        Entry originalEntry = modifyContext.getEntry();

        StoredProcedureParameterInjector injector = new ModifyStoredProcedureParameterInjector( modifyContext );

        // Gather Trigger Specifications which apply to the entry being modified.
        List<TriggerSpecification> triggerSpecs = new ArrayList<TriggerSpecification>();
        addPrescriptiveTriggerSpecs( modifyContext, triggerSpecs, normName, originalEntry );
        addEntryTriggerSpecs( triggerSpecs, originalEntry );

        Map<ActionTime, List<TriggerSpecification>> triggerMap = getActionTimeMappedTriggerSpecsForOperation(
            triggerSpecs, LdapOperation.MODIFY );

        next.modify( modifyContext );

        triggerSpecCache.subentryModified( modifyContext, originalEntry );

        // Fire AFTER Triggers.
        List<TriggerSpecification> afterTriggerSpecs = triggerMap.get( ActionTime.AFTER );
        executeTriggers( modifyContext, afterTriggerSpecs, injector );
    }


    public void rename( NextInterceptor next, RenameOperationContext renameContext ) throws LdapException
    {
        Dn name = renameContext.getDn();
        Rdn newRdn = renameContext.getNewRdn();
        boolean deleteOldRn = renameContext.getDeleteOldRdn();

        // Bypass trigger handling if the service is disabled.
        if ( !enabled )
        {
            next.rename( renameContext );
            return;
        }

        // Gather supplementary data.        
        Entry renamedEntry = ((ClonedServerEntry)renameContext.getEntry()).getClonedEntry();

        // @TODO : To be completely reviewed !!!
        Rdn oldRdn = name.getRdn();
        Dn oldSuperiorDn = name.getParent();
        Dn newSuperiorDn = oldSuperiorDn;
        Dn oldDn = name;
        Dn newDn = name;
        newDn = newDn.add( newRdn );

        StoredProcedureParameterInjector injector = new ModifyDNStoredProcedureParameterInjector( renameContext,
            deleteOldRn, oldRdn, newRdn, oldSuperiorDn, newSuperiorDn, oldDn, newDn);

        // Gather Trigger Specifications which apply to the entry being renamed.
        List<TriggerSpecification> triggerSpecs = new ArrayList<TriggerSpecification>();
        addPrescriptiveTriggerSpecs( renameContext, triggerSpecs, name, renamedEntry );
        addEntryTriggerSpecs( triggerSpecs, renamedEntry );

        Map<ActionTime, List<TriggerSpecification>> triggerMap = getActionTimeMappedTriggerSpecsForOperation(
            triggerSpecs, LdapOperation.MODIFYDN_RENAME );

        next.rename( renameContext );
        triggerSpecCache.subentryRenamed( name, newDn);

        // Fire AFTER Triggers.
        List<TriggerSpecification> afterTriggerSpecs = triggerMap.get( ActionTime.AFTER );
        executeTriggers( renameContext, afterTriggerSpecs, injector );
    }


    public void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        Dn oldDn = moveAndRenameContext.getDn();
        Dn newSuperiorDn = moveAndRenameContext.getNewSuperiorDn();
        Rdn newRdn = moveAndRenameContext.getNewRdn();
        boolean deleteOldRn = moveAndRenameContext.getDeleteOldRdn();

        // Bypass trigger handling if the service is disabled.
        if ( !enabled )
        {
            next.moveAndRename( moveAndRenameContext );
            return;
        }

        // Gather supplementary data.        
        Entry movedEntry = moveAndRenameContext.getOriginalEntry();

        Rdn oldRdn = oldDn.getRdn();
        Dn oldSuperiorDn = oldDn.getParent();
        Dn oldDN = oldDn;
        Dn newDn = moveAndRenameContext.getNewDn();

        StoredProcedureParameterInjector injector = new ModifyDNStoredProcedureParameterInjector( moveAndRenameContext,
            deleteOldRn, oldRdn, newRdn, oldSuperiorDn, newSuperiorDn, oldDN, newDn);

        // Gather Trigger Specifications which apply to the entry being exported.
        List<TriggerSpecification> exportTriggerSpecs = new ArrayList<TriggerSpecification>();
        addPrescriptiveTriggerSpecs( moveAndRenameContext, exportTriggerSpecs, oldDn, movedEntry );
        addEntryTriggerSpecs( exportTriggerSpecs, movedEntry );

        // Get the entry again without operational attributes
        // because access control subentry operational attributes
        // will not be valid at the new location.
        // This will certainly be fixed by the SubentryInterceptor,
        // but after this service.
        Entry importedEntry = moveAndRenameContext.lookup( oldDn, ByPassConstants.LOOKUP_EXCLUDING_OPR_ATTRS_BYPASS, SchemaConstants.ALL_ATTRIBUTES_ARRAY );

        // As the target entry does not exist yet and so
        // its subentry operational attributes are not there,
        // we need to construct an entry to represent it
        // at least with minimal requirements which are object class
        // and access control subentry operational attributes.
        Entry fakeImportedEntry = subentryUtils.getSubentryAttributes(newDn, importedEntry );

        for ( Attribute attribute : importedEntry )
        {
            fakeImportedEntry.put( attribute );
        }

        // Gather Trigger Specifications which apply to the entry being imported.
        // Note: Entry Trigger Specifications are not valid for Import.
        List<TriggerSpecification> importTriggerSpecs = new ArrayList<TriggerSpecification>();
        addPrescriptiveTriggerSpecs( moveAndRenameContext, importTriggerSpecs, newDn, fakeImportedEntry );

        Map<ActionTime, List<TriggerSpecification>> exportTriggerMap = getActionTimeMappedTriggerSpecsForOperation(
            exportTriggerSpecs, LdapOperation.MODIFYDN_EXPORT );

        Map<ActionTime, List<TriggerSpecification>> importTriggerMap = getActionTimeMappedTriggerSpecsForOperation(
            importTriggerSpecs, LdapOperation.MODIFYDN_IMPORT );

        next.moveAndRename( moveAndRenameContext );
        triggerSpecCache.subentryRenamed( oldDN, newDn);

        // Fire AFTER Triggers.
        List<TriggerSpecification> afterExportTriggerSpecs = exportTriggerMap.get( ActionTime.AFTER );
        List<TriggerSpecification> afterImportTriggerSpecs = importTriggerMap.get( ActionTime.AFTER );
        executeTriggers( moveAndRenameContext, afterExportTriggerSpecs, injector );
        executeTriggers( moveAndRenameContext, afterImportTriggerSpecs, injector );
    }


    /**
     * {@inheritDoc}
     */
    public void move( NextInterceptor next, MoveOperationContext moveContext ) throws LdapException
    {
        // Bypass trigger handling if the service is disabled.
        if ( !enabled )
        {
            next.move( moveContext );
            return;
        }

        Rdn rdn = moveContext.getRdn();
        Dn dn = moveContext.getDn();
        Dn newDn = moveContext.getNewDn();
        Dn oldSuperior = moveContext.getOldSuperior();
        Dn newSuperior = moveContext.getNewSuperior();

        // Gather supplementary data.        
        Entry movedEntry = moveContext.getOriginalEntry();

        //Rdn newRDN = dn.getRdn();

        StoredProcedureParameterInjector injector = new ModifyDNStoredProcedureParameterInjector( moveContext, false,
            rdn, rdn, oldSuperior, newSuperior, dn, newDn );

        // Gather Trigger Specifications which apply to the entry being exported.
        List<TriggerSpecification> exportTriggerSpecs = new ArrayList<TriggerSpecification>();
        addPrescriptiveTriggerSpecs( moveContext, exportTriggerSpecs, dn, movedEntry );
        addEntryTriggerSpecs( exportTriggerSpecs, movedEntry );

        // Get the entry again without operational attributes
        // because access control subentry operational attributes
        // will not be valid at the new location.
        // This will certainly be fixed by the SubentryInterceptor,
        // but after this service.
        Entry importedEntry = moveContext.lookup( dn, ByPassConstants.LOOKUP_EXCLUDING_OPR_ATTRS_BYPASS, SchemaConstants.ALL_ATTRIBUTES_ARRAY );

        // As the target entry does not exist yet and so
        // its subentry operational attributes are not there,
        // we need to construct an entry to represent it
        // at least with minimal requirements which are object class
        // and access control subentry operational attributes.
        Entry fakeImportedEntry = subentryUtils.getSubentryAttributes( newDn, importedEntry );

        for ( Attribute attribute : importedEntry )
        {
            fakeImportedEntry.put( attribute );
        }

        // Gather Trigger Specifications which apply to the entry being imported.
        // Note: Entry Trigger Specifications are not valid for Import.
        List<TriggerSpecification> importTriggerSpecs = new ArrayList<TriggerSpecification>();
        addPrescriptiveTriggerSpecs( moveContext, importTriggerSpecs, newDn, fakeImportedEntry );

        Map<ActionTime, List<TriggerSpecification>> exportTriggerMap = getActionTimeMappedTriggerSpecsForOperation(
            exportTriggerSpecs, LdapOperation.MODIFYDN_EXPORT );

        Map<ActionTime, List<TriggerSpecification>> importTriggerMap = getActionTimeMappedTriggerSpecsForOperation(
            importTriggerSpecs, LdapOperation.MODIFYDN_IMPORT );

        next.move( moveContext );
        triggerSpecCache.subentryRenamed( dn, newDn );

        // Fire AFTER Triggers.
        List<TriggerSpecification> afterExportTriggerSpecs = exportTriggerMap.get( ActionTime.AFTER );
        List<TriggerSpecification> afterImportTriggerSpecs = importTriggerMap.get( ActionTime.AFTER );
        executeTriggers( moveContext, afterExportTriggerSpecs, injector );
        executeTriggers( moveContext, afterImportTriggerSpecs, injector );
    }


    ////////////////////////////////////////////////////////////////////////////
    // Utility Methods
    ////////////////////////////////////////////////////////////////////////////

    private Object executeTriggers( OperationContext opContext, List<TriggerSpecification> triggerSpecs,
        StoredProcedureParameterInjector injector ) throws LdapException
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
        StoredProcedureParameterInjector injector ) throws LdapException
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


    private Object executeProcedure( OperationContext opContext, String procedure, Object[] values ) throws LdapException
    {
        try
        {
            Entry spUnit = manager.findStoredProcUnit( opContext.getSession(), procedure );
            StoredProcEngine engine = manager.getStoredProcEngineInstance( spUnit );
            
            return engine.invokeProcedure( opContext.getSession(), procedure, values );
        }
        catch ( Exception e )
        {
            LdapOtherException lne = new LdapOtherException( e.getMessage(), e );
            lne.initCause( e );
            throw lne;
        }
    }
}
