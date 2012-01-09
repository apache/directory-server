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
package org.apache.directory.server.component.utilities;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.directory.server.component.ADSComponent;
import org.apache.directory.server.component.instance.ADSComponentInstance;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.ldif.LdapLdifException;
import org.apache.directory.shared.ldap.model.ldif.LdifEntry;
import org.apache.directory.shared.ldap.model.name.Dn;


public class LdifConfigHelper
{

    /**
     * Transforms a component instance into its corresponding entry in ldif format.
     *
     * @param instance ADSComponentInstance to transform into Ldif entry.
     * @return Generated Ldif entry.
     */
    public static LdifEntry instanceToLdif( ADSComponentInstance instance )
    {
        ADSComponent parentComponent = instance.getParentComponent();
        Properties instanceConfiguration = instance.getInstanceConfiguration();

        String instanceName = ( String ) instanceConfiguration.remove(
            ADSConstants.ADS_COMPONENT_INSTANCE_PROP_NAME );

        if ( instanceName == null )
        {
            return null;
        }

        String instancesDn = ADSComponentHelper.getComponentInstancesDn( parentComponent );
        String instanceRdn = ADSSchemaConstants.ADS_COMPONENT_INSTANCE_ATTRIB_NAME + "=" + instanceName;
        String instanceDn = instanceRdn + "," + instancesDn;

        List<String> attributes = new ArrayList<String>();

        attributes.add( ADSSchemaConstants.ADS_COMPONENT_INSTANCE_ATTRIB_NAME + ":" + instanceName );
        attributes.add( SchemaConstants.OBJECT_CLASS_AT + ":"
            + ADSComponentHelper.getComponentObjectClass( parentComponent ) );

        for ( Object key : instanceConfiguration.keySet() )
        {
            String attribute = ( String ) key + ":" + ( String ) instanceConfiguration.get( key );
            attributes.add( attribute );
        }

        LdifEntry instanceEntry;
        try
        {
            instanceEntry = new LdifEntry( new Dn( instanceDn ), attributes.toArray() );
        }
        catch ( LdapInvalidDnException e )
        {
            e.printStackTrace();
            return null;
        }
        catch ( LdapInvalidAttributeValueException e )
        {
            e.printStackTrace();
            return null;
        }
        catch ( LdapLdifException e )
        {
            e.printStackTrace();
            return null;
        }

        return instanceEntry;
    }


    /**
     * It extracts the configuration information from Ldif entry.
     *
     * @param entry LdifEntry reference to extract instance configuration
     * @return Extracted instance configuration
     */
    public static Properties instanceEntryToConfiguration( Entry instanceEntry )
    {
        Properties configuration = new Properties();

        try
        {
            String instanceName = instanceEntry.get( ADSSchemaConstants.ADS_COMPONENT_INSTANCE_ATTRIB_NAME )
                .getString();

            if ( instanceName == null )
            {
                //Entry is not instance entry.
                return null;
            }

            configuration.put( ADSConstants.ADS_COMPONENT_INSTANCE_PROP_NAME, instanceName );

            Collection<Attribute> attributes = instanceEntry.getAttributes();
            for ( Attribute attribute : attributes )
            {
                String attribName = attribute.getId();

                if ( attribName.equals( ADSSchemaConstants.ADS_COMPONENT_INSTANCE_ATTRIB_NAME_OID ) ||
                    attribName.equals( SchemaConstants.OBJECT_CLASS_AT_OID ) ||
                    attribName.equals( SchemaConstants.ENTRY_PARENT_ID_OID ) ||
                    attribName.equals( SchemaConstants.ENTRY_UUID_AT_OID ) ||
                    attribName.equals( SchemaConstants.CREATE_TIMESTAMP_AT_OID ) ||
                    attribName.equals( SchemaConstants.CREATORS_NAME_AT_OID ) ||
                    attribName.equals( SchemaConstants.ENTRY_CSN_AT_OID ) )
                {
                    continue;
                }

                String attribVal = attribute.getString();

                configuration.put( attribName, attribVal );
            }

        }
        catch ( LdapInvalidAttributeValueException e )
        {
            e.printStackTrace();
            return null;
        }

        return configuration;
    }
}
