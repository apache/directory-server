package org.apache.directory.server.hub.core.store;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.directory.server.hub.api.component.DCConfiguration;
import org.apache.directory.server.hub.api.component.DCProperty;
import org.apache.directory.server.hub.api.component.DirectoryComponent;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.schema.ObjectClass;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;


public class StoreDCBuilder
{
    private SchemaManager schemaManager;


    public StoreDCBuilder( SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;
    }


    public DirectoryComponent buildComponentFromEntry( Entry componentEntry ) throws LdapException
    {
        String componentName = null;
        String managerPID = null;
        Integer collectionIndex = null;

        Attribute ocAttrib = componentEntry.get( schemaManager.getAttributeType( "objectclass" ) );
        for ( Value<?> val : ocAttrib )
        {
            String ocName = val.getString();
            String ocOID = schemaManager.getObjectClassRegistry().getOidByName( ocName );

            ObjectClass oc = schemaManager.getObjectClassRegistry().get( ocOID );

            if ( oc.isStructural() )
            {
                managerPID = ocName;
            }

            if ( oc.isAuxiliary() )
            {
                if ( ocName.equals( StoreSchemaConstants.HUB_OC_COLLECTION_ITEM ) )
                {
                    collectionIndex = 0;
                }
            }
        }

        List<DCProperty> properties = new ArrayList<DCProperty>();

        Collection<Attribute> attribs = componentEntry.getAttributes();
        for ( Attribute attrib : attribs )
        {
            if ( attrib.getUpId().equals( StoreSchemaConstants.HUB_AT_COMPONENT_NAME.toLowerCase() ) )
            {
                componentName = attrib.getString();
            }
            else if ( attrib.getUpId().equals( StoreSchemaConstants.HUB_AT_COLL_ITEM_INDEX.toLowerCase() ) )
            {
                collectionIndex = Integer.parseInt( attrib.getString() );
            }
            else if ( attrib.getUpId().equals( SchemaConstants.ENTRY_UUID_AT.toLowerCase() )
                || attrib.getUpId().equals( SchemaConstants.ENTRY_CSN_AT.toLowerCase() )
                || attrib.getUpId().equals( SchemaConstants.CREATORS_NAME_AT.toLowerCase() )
                || attrib.getUpId().equals( SchemaConstants.CREATE_TIMESTAMP_AT.toLowerCase() )
                || attrib.getUpId().equals( SchemaConstants.OBJECT_CLASS_AT.toLowerCase() )
                || attrib.getUpId().equals( SchemaConstants.MODIFIERS_NAME_AT.toLowerCase() )
                || attrib.getUpId().equals( SchemaConstants.MODIFY_TIMESTAMP_AT.toCharArray() )
                || attrib.getUpId().equals( SchemaConstants.ENTRY_PARENT_ID_AT ) )
            {
                continue;
            }
            else
            {
                properties.add( new DCProperty( attrib.getUpId(), attrib.getString() ) );
            }
        }

        DCConfiguration componentConf = new DCConfiguration( properties );
        componentConf.setCollectionIndex( collectionIndex );

        DirectoryComponent component = new DirectoryComponent( managerPID, componentName, componentConf );
        component.setConfigLocation( componentEntry.getDn().getName() );

        return component;
    }
}
