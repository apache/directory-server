package org.apache.directory.server.hub.core.store;


import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.server.hub.api.ComponentHub;
import org.apache.directory.server.hub.api.component.util.ComponentConstants;
import org.apache.directory.server.hub.api.exception.HubAbortException;
import org.apache.directory.server.hub.api.exception.StoreNotValidException;
import org.apache.directory.server.hub.api.meta.DCMetadataDescriptor;
import org.apache.directory.server.hub.api.meta.DCPropertyDescription;
import org.apache.directory.server.hub.api.meta.DCPropertyType;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.message.DeleteRequestImpl;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.util.DateUtils;


public class StoreSchemaManager
{
    public static final String OID_BASE = "1.3.6.1.4.1.18060.0.4.4";
    public static final String METADATAS_SCHEMA_BASE = "cn=components";

    private static Dictionary<String, String> syntaxMappings;
    private static Dictionary<String, String> equalityMappings;
    private static Dictionary<String, String> orderingMappings;
    private static Dictionary<String, String> substringMappings;

    private ComponentHub hub;
    private SchemaPartition schemaPartition;
    private OIDManager oidManager;


    public StoreSchemaManager( ComponentHub hub )
    {
        this.hub = hub;
    }


    public void init( SchemaPartition schemaPartition ) throws StoreNotValidException
    {
        this.schemaPartition = schemaPartition;

        oidManager = new OIDManager();
        oidManager.init( schemaPartition );

        syntaxMappings = new Hashtable<String, String>();
        equalityMappings = new Hashtable<String, String>();
        orderingMappings = new Hashtable<String, String>();
        substringMappings = new Hashtable<String, String>();

        syntaxMappings = new Hashtable<String, String>();
        syntaxMappings.put( ComponentConstants.PRIMITIVE_INT, "1.3.6.1.4.1.1466.115.121.1.27" );
        syntaxMappings.put( ComponentConstants.PRIMITIVE_FLOAT, "1.3.6.1.4.1.1466.115.121.1.15" );//using String syntax
        syntaxMappings.put( ComponentConstants.PRIMITIVE_STR, "1.3.6.1.4.1.1466.115.121.1.15" );
        syntaxMappings.put( ComponentConstants.PRIMITIVE_BOOL, "1.3.6.1.4.1.1466.115.121.1.7" );

        equalityMappings = new Hashtable<String, String>();
        equalityMappings.put( ComponentConstants.PRIMITIVE_INT, "integerMatch" );
        equalityMappings.put( ComponentConstants.PRIMITIVE_FLOAT, "caseExactMatch" );
        equalityMappings.put( ComponentConstants.PRIMITIVE_STR, "caseExactMatch" );
        equalityMappings.put( ComponentConstants.PRIMITIVE_BOOL, "booleanMatch" );

        orderingMappings = new Hashtable<String, String>();
        orderingMappings.put( ComponentConstants.PRIMITIVE_INT, "integerOrderingMatch" );
        orderingMappings.put( ComponentConstants.PRIMITIVE_FLOAT, "caseExactOrderingMatch" );
        orderingMappings.put( ComponentConstants.PRIMITIVE_STR, "caseExactOrderingMatch" );
        orderingMappings.put( ComponentConstants.PRIMITIVE_BOOL, "caseIgnoreOrderingMatch" );

        substringMappings = new Hashtable<String, String>();
        substringMappings.put( ComponentConstants.PRIMITIVE_INT, "numericStringSubstringsMatch" );
        substringMappings.put( ComponentConstants.PRIMITIVE_FLOAT, "caseExactSubstringsMatch" );
        substringMappings.put( ComponentConstants.PRIMITIVE_STR, "caseExactSubstringsMatch" );
        substringMappings.put( ComponentConstants.PRIMITIVE_BOOL, "caseIgnoreSubstringsMatch" );
    }


    public void installMetadata( DCMetadataDescriptor metadata ) throws LdapException
    {
        for ( DCPropertyDescription pd : metadata.getPropertyDescriptons() )
        {
            if ( pd.getPropertyContext() == DCPropertyType.INJECTION )
            {
                continue;
            }

            if ( pd.getPropertyContext() == DCPropertyType.CONSTANT )
            {
                continue;
            }

            installAttribute( pd );
        }

        installOCEntry( generateOC( metadata, installNamers( metadata ) ) );
    }


    public void installAttribute( DCPropertyDescription propertyDescription ) throws LdapException
    {

        if ( attributeExists( propertyDescription.getName() ) )
        {
            throw new LdapException( propertyDescription.getName() + "already registered with the schema" );
        }

        Entry attribEntry = generateAttributeEntry( propertyDescription );
        AddOperationContext addOp = new AddOperationContext( null, attribEntry.getDn(), attribEntry );

        schemaPartition.getRegistrySynchronizerAdaptor().add( addOp );
        schemaPartition.getWrappedPartition().add( addOp );

    }


    public void installAttributes( List<DCPropertyDescription> propertyDescriptions ) throws LdapException
    {
        for ( DCPropertyDescription pd : propertyDescriptions )
        {
            if ( attributeExists( pd.getName() ) )
            {
                throw new LdapException( pd.getName() + "already registered with the schema" );
            }
        }

        for ( DCPropertyDescription pd : propertyDescriptions )
        {
            installAttribute( pd );
        }
    }


    public List<String> installNamers( DCMetadataDescriptor metadata ) throws LdapException
    {
        String[] types = metadata.getImplementedInterfaces();

        List<String> registered = new ArrayList<String>();
        try
        {
            for ( String type : types )
            {
                String fixedName = null;
                if ( type.contains( "." ) )
                {
                    fixedName = "ads--" + type.substring( type.lastIndexOf( '.' ) + 1 ).toLowerCase();
                }

                if ( attributeExists( fixedName ) )
                {
                    registered.add( fixedName );
                    continue;
                }

                SchemaManager sm = schemaPartition.getSchemaManager();

                String attribOID = oidManager.generateNewAttributeOID( fixedName );
                Dn attribDn = new Dn( sm, "m-oid", attribOID, SchemaConstants.ATTRIBUTE_TYPES_PATH,
                    METADATAS_SCHEMA_BASE,
                    SchemaConstants.OU_SCHEMA );

                Entry attribEntry = new DefaultEntry( schemaPartition.getSchemaManager(), attribDn );

                attribEntry.add( sm.getAttributeType( "objectclass" ), "metaAttributeType" );
                attribEntry.add( sm.getAttributeType( "m-oid" ), attribOID );
                attribEntry.add( sm.getAttributeType( "m-name" ), fixedName );
                attribEntry.add( sm.getAttributeType( "m-description" ), "To help better naming an entry." );
                attribEntry.add( sm.getAttributeType( "m-singleValue" ), "TRUE" );
                attribEntry.add( sm.getAttributeType( "m-syntax" ), "1.3.6.1.4.1.1466.115.121.1.15" );
                attribEntry.add( sm.getAttributeType( "m-equality" ), "caseExactMatch" );
                attribEntry.add( sm.getAttributeType( "m-ordering" ), "caseExactOrderingMatch" );
                attribEntry.add( sm.getAttributeType( "m-substr" ), "caseExactSubstringsMatch" );
                attribEntry.add( sm.getAttributeType( "m-length" ), "0" );

                attribEntry.add( SchemaConstants.ENTRY_UUID_AT, UUID.randomUUID().toString() );
                attribEntry.add( SchemaConstants.ENTRY_CSN_AT, ApacheDSConfigStore.csnFactory.newInstance().toString() );
                attribEntry.add( SchemaConstants.CREATORS_NAME_AT, StoreSchemaConstants.SYSTEM_ADMIN_DN );
                attribEntry.add( SchemaConstants.CREATE_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );

                AddOperationContext addOp = new AddOperationContext( null, attribDn, attribEntry );

                schemaPartition.getRegistrySynchronizerAdaptor().add( addOp );
                schemaPartition.getWrappedPartition().add( addOp );

                registered.add( fixedName );
            }
        }
        catch ( LdapException e )
        {
            // Do nothing.
        }
        return registered;
    }


    public Entry generateAttributeEntry( DCPropertyDescription propertyDescription ) throws LdapException
    {
        SchemaManager sm = schemaPartition.getSchemaManager();

        String attribOID = oidManager.generateNewAttributeOID( propertyDescription.getName() );
        Dn attribDn = new Dn( sm, "m-oid", attribOID, SchemaConstants.ATTRIBUTE_TYPES_PATH, METADATAS_SCHEMA_BASE,
            SchemaConstants.OU_SCHEMA );

        String syntax, equality, substr, ordering;
        String selectingType = ( propertyDescription.getPropertyContext() == DCPropertyType.PRIMITIVE ) ? propertyDescription
            .getType()
            : ComponentConstants.PRIMITIVE_STR;

        syntax = syntaxMappings.get( selectingType );
        equality = equalityMappings.get( selectingType );
        substr = substringMappings.get( selectingType );
        ordering = orderingMappings.get( selectingType );

        Entry attribEntry = new DefaultEntry( schemaPartition.getSchemaManager(), attribDn );

        attribEntry.add( sm.getAttributeType( "objectclass" ), "metaAttributeType" );
        attribEntry.add( sm.getAttributeType( "m-oid" ), attribOID );
        attribEntry.add( sm.getAttributeType( "m-name" ), propertyDescription.getName() );
        if ( propertyDescription.getDescription() != null )
        {
            attribEntry.add( sm.getAttributeType( "m-description" ), propertyDescription.getDescription() );
        }
        attribEntry.add( sm.getAttributeType( "m-singleValue" ),
            ( propertyDescription.getPropertyContext() == DCPropertyType.PRIMITIVE_COLLECTION ) ? "TRUE"
                : "FALSE" );
        attribEntry.add( sm.getAttributeType( "m-syntax" ), syntax );
        attribEntry.add( sm.getAttributeType( "m-equality" ), equality );
        attribEntry.add( sm.getAttributeType( "m-ordering" ), ordering );
        attribEntry.add( sm.getAttributeType( "m-substr" ), substr );
        attribEntry.add( sm.getAttributeType( "m-length" ), "0" );

        attribEntry.add( SchemaConstants.ENTRY_UUID_AT, UUID.randomUUID().toString() );
        attribEntry.add( SchemaConstants.ENTRY_CSN_AT, ApacheDSConfigStore.csnFactory.newInstance().toString() );
        attribEntry.add( SchemaConstants.CREATORS_NAME_AT, StoreSchemaConstants.SYSTEM_ADMIN_DN );
        attribEntry.add( SchemaConstants.CREATE_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );

        return attribEntry;
    }


    public void uninstallAttribute( String propertyName ) throws LdapException
    {
        String attribOID = oidManager.getAttributeOID( propertyName );
        if ( attribOID == null )
        {
            return;
        }

        Dn attribDn = new Dn( schemaPartition.getSchemaManager(), "m-oid", attribOID,
            SchemaConstants.ATTRIBUTE_TYPES_PATH, METADATAS_SCHEMA_BASE
            , SchemaConstants.OU_SCHEMA );

        Entry attribEntry = schemaPartition.getWrappedPartition().lookup( new LookupOperationContext( null, attribDn ) );
        if ( attribEntry != null )
        {
            DeleteOperationContext deleteOp = new DeleteOperationContext( null, attribDn );

            // Necessary because synchronizer directly access the entry.
            deleteOp.setEntry( attribEntry );

            schemaPartition.getRegistrySynchronizerAdaptor().delete( deleteOp, false );
            schemaPartition.getWrappedPartition().delete( deleteOp );
        }

    }


    public void uninstallAttributes( List<DCPropertyDescription> configurables ) throws LdapException
    {
        for ( DCPropertyDescription pd : configurables )
        {
            uninstallAttribute( pd.getName() );
        }
    }


    public Entry generateOC( DCMetadataDescriptor metadata, List<String> auxiliaryNaming ) throws LdapException
    {
        SchemaManager sm = schemaPartition.getSchemaManager();

        String ocOID = oidManager.generateNewOCOID( metadata.getMetadataPID() );
        Dn ocDn = new Dn( sm, "m-oid", ocOID, SchemaConstants.OBJECT_CLASSES_PATH, METADATAS_SCHEMA_BASE,
            SchemaConstants.OU_SCHEMA );

        Entry ocEntry = new DefaultEntry( sm, ocDn );

        ocEntry.add( sm.getAttributeType( "objectclass" ), "metaObjectClass" );
        ocEntry.add( sm.getAttributeType( "m-oid" ), ocOID );
        ocEntry.add( sm.getAttributeType( "m-name" ), metadata.getMetadataPID() );
        ocEntry.add( sm.getAttributeType( "m-description" ),
            "OC for generating instances of" + metadata.getMetadataPID() );
        ocEntry.add( sm.getAttributeType( "m-supObjectClass" ), StoreSchemaConstants.HUB_OC_COMPONENT );

        for ( DCPropertyDescription pd : metadata.getPropertyDescriptons() )
        {
            if ( pd.getPropertyContext() == DCPropertyType.INJECTION )
            {
                continue;
            }

            if ( pd.getPropertyContext() == DCPropertyType.CONSTANT )
            {
                continue;
            }

            if ( pd.isMandatory() )
            {
                ocEntry.add( sm.getAttributeType( "m-must" ), pd.getName() );
            }
            else
            {
                ocEntry.add( sm.getAttributeType( "m-may" ), pd.getName() );
            }
        }

        if ( auxiliaryNaming != null )
        {
            for ( String namingAttrib : auxiliaryNaming )
            {
                ocEntry.add( sm.getAttributeType( "m-may" ), namingAttrib );
            }
        }

        ocEntry.add( SchemaConstants.ENTRY_UUID_AT, UUID.randomUUID().toString() );
        ocEntry.add( SchemaConstants.ENTRY_CSN_AT, ApacheDSConfigStore.csnFactory.newInstance().toString() );
        ocEntry.add( SchemaConstants.CREATORS_NAME_AT, StoreSchemaConstants.SYSTEM_ADMIN_DN );
        ocEntry.add( SchemaConstants.CREATE_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );

        return ocEntry;
    }


    public void installOCEntry( Entry ocEntry ) throws LdapException
    {
        AddOperationContext addOp = new AddOperationContext( null, ocEntry.getDn(), ocEntry );

        schemaPartition.getRegistrySynchronizerAdaptor().add( addOp );
        schemaPartition.getWrappedPartition().add( addOp );
    }


    public void uninstallOC( String metaPID ) throws LdapException
    {
        String metaOID = oidManager.getOCBase( metaPID );
        if ( metaOID == null )
        {
            return;
        }

        Dn ocDn = new Dn( schemaPartition.getSchemaManager(), "m-oid", metaOID, SchemaConstants.OBJECT_CLASSES_PATH,
            METADATAS_SCHEMA_BASE,
            SchemaConstants.OU_SCHEMA );

        Entry ocEntry = schemaPartition.getWrappedPartition().lookup( new LookupOperationContext( null, ocDn ) );
        if ( ocEntry != null )
        {
            DeleteOperationContext deleteOp = new DeleteOperationContext( null, ocDn );

            // Necessary because synchronizer directly access the entry.
            deleteOp.setEntry( ocEntry );

            schemaPartition.getRegistrySynchronizerAdaptor().delete( deleteOp, false );
            schemaPartition.getWrappedPartition().delete( deleteOp );
        }
    }


    public boolean attributeExists( String attributeName )
    {
        return oidManager.getAttributeOID( attributeName ) != null;
    }


    public boolean ocExists( String metaPID )
    {
        return oidManager.getOCBase( metaPID ) != null;
    }


    public void updateOC( DCMetadataDescriptor metadata ) throws LdapException
    {
        Entry ocEntry = generateOC( metadata, installNamers( metadata ) );

        uninstallOC( metadata.getMetadataPID() );
        installOCEntry( ocEntry );
    }
}
