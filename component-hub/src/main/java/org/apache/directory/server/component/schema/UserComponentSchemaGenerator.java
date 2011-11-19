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
package org.apache.directory.server.component.schema;


import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.directory.server.component.ADSComponentHelper;
import org.apache.directory.server.component.ADSConstants;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.ldif.LdapLdifException;
import org.apache.directory.shared.ldap.model.ldif.LdifEntry;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UserComponentSchemaGenerator implements ComponentSchemaGenerator
{

    private final Logger LOG = LoggerFactory.getLogger( UserComponentSchemaGenerator.class );
    private final String ADS_USER_COMPONENTS_SCHEMA_DN = "cn=usercomponents,ou=schema";


    @Override
    public ADSComponentSchema generateOrGetSchemaElements( Factory componentFactory )
    {
        List<LdifEntry> schemaElements = new ArrayList<LdifEntry>();

        String componentName = ADSComponentHelper.getComponentName( componentFactory );

        String attribsDn = "ou=attributeTypes," + ADS_USER_COMPONENTS_SCHEMA_DN;
        String ocsDn = "ou=objectClasses," + ADS_USER_COMPONENTS_SCHEMA_DN;

        //Will hold the m-must attributes while iterating over properties of the component
        List<String> ocAttribs = new ArrayList<String>();

        //Will hold default values for factory proeprties.
        Dictionary defaultConf = new Hashtable<String, Object>();

        // Creating schema elements with proper order and pushing them into the list
        try
        {

            String componentBaseOID = ComponentOIDGenerator.generateComponentOID();

            for ( PropertyDescription prop : componentFactory.getComponentDescription().getProperties() )
            {

                //Must be lower case, alphanumeric+'-' only
                String propname = prop.getName();

                String propvalue = prop.getValue();
                String proptype = prop.getType();
                String propoid = ComponentOIDGenerator.generateAttribOID( componentBaseOID );
                String propdn = "m-oid=" + propoid + "," + attribsDn;

                if ( !( proptype.equals( "int" ) || proptype.equals( "java.lang.String" ) || proptype
                    .equals( "boolean" ) ) )
                {
                    LOG.info( "Property found with an incompatible type:  "
                        + propname + ":" + proptype );
                    continue;
                }

                String syntax = ADSConstants.syntaxMappings.get( proptype );
                String equality = ADSConstants.equalityMappings.get( proptype );
                String ordering = ADSConstants.orderingMappings.get( proptype );
                String substr = ADSConstants.substringMappings.get( proptype );

                schemaElements.add( new LdifEntry( propdn,
                    "objectclass:metaAttributeType",
                    "objectclass:metaTop",
                    "objectclass:top",
                    "m-oid:" + propoid,
                    "m-name:" + propname,
                    "m-description:Property of component type " + componentName,
                    "m-equality:" + equality,
                    "m-ordering:" + ordering,
                    "m-substr:" + substr,
                    "m-syntax:" + syntax,
                    "m-length:0",
                    "m-singleValue:TRUE" ) );

                if ( prop.isMandatory() )
                {
                    ocAttribs.add( "m-must:" + propname );
                }
                else
                {
                    ocAttribs.add( "m-may:" + propname );
                }

                defaultConf.put( propname, propvalue );

            }

            schemaElements.add( new LdifEntry( ocsDn,
                "objectclass:organizationalUnit",
                "objectClass:top",
                "ou:objectClasses" ) );

            String ocoid = ComponentOIDGenerator.generateOCOID( componentBaseOID );
            String ocDn = "m-oid=" + ocoid + "," + ocsDn;

            ocAttribs.add( 0, "m-may:cached" );
            ocAttribs.add( 0, "m-must:ins" );
            ocAttribs.add( 0, "m-must:active" );
            ocAttribs.add( 0, "m-supObjectClass: top" );
            ocAttribs.add( 0, "m-description:Object Class for generating instances of:" + componentName );
            ocAttribs.add( 0, "m-name:" + componentName );
            ocAttribs.add( 0, "m-oid:" + ocoid );
            ocAttribs.add( 0, "objectclass: top" );
            ocAttribs.add( 0, "objectclass: metaTop" );
            ocAttribs.add( 0, "objectclass: metaObjectClass" );

            schemaElements.add( new LdifEntry( ocDn, ocAttribs.toArray() ) );

        }
        catch ( LdapInvalidAttributeValueException e )
        {
            LOG.info( "ADSSchemaManager#generateSchema:  Error(LdapInvalidAttributeValueException) while creating LdifEntry for:  "
                + componentFactory );
        }
        catch ( LdapInvalidDnException e )
        {
            LOG.info( "ADSSchemaManager#generateSchema:  Error(LdapInvalidDnException) while creating LdifEntry for:  "
                + componentFactory );
        }
        catch ( LdapLdifException e )
        {
            LOG.info( "ADSSchemaManager#generateSchema:  Error(LdapLdifException) while creating LdifEntry for:  "
                + componentFactory );
        }

        ADSComponentSchema compSchema = new ADSComponentSchema( ADS_USER_COMPONENTS_SCHEMA_DN,
            schemaElements );
        compSchema.setDefaultConf( defaultConf );

        return compSchema;
    }

}
