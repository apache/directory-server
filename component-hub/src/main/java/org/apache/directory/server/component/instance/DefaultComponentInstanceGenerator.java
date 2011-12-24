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
package org.apache.directory.server.component.instance;


import java.util.Properties;

import org.apache.directory.server.component.ADSComponent;
import org.apache.directory.server.component.schema.DefaultComponentSchemaGenerator;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Class that generates an instances of "user" typed components.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DefaultComponentInstanceGenerator implements ComponentInstanceGenerator
{

    private final Logger LOG = LoggerFactory.getLogger( DefaultComponentSchemaGenerator.class );


    /*
     * (non-Javadoc)
     * @see org.apache.directory.server.component.instance.ComponentInstanceGenerator#createInstance(org.apache.directory.server.component.ADSComponent)
     */
    @Override
    public ADSComponentInstance createInstance( ADSComponent component, Properties properties )
    {
        if ( properties == null && component.getDefaultConfiguration() == null )
        {
            component.setDefaultConfiguration( extractDefaultConfiguration( component ) );
            properties = component.getDefaultConfiguration();
        }

        // We create the IPojo instance using supplied configuration, 
        // configuration have instance.name property at that point.
        try
        {
            ComponentInstance comp = component.getFactory().createComponentInstance( properties );
            Object pojo = ( ( InstanceManager ) comp ).getPojoObject();

            //Create the ADSComponentInstance reference using the pojo just created.
            ADSComponentInstance instance = new ADSComponentInstance( component, pojo, properties );

            return instance;
        }
        catch ( UnacceptableConfiguration e )
        {
            LOG.info( "Configuration is not valid to create instance of component:" + component );
            e.printStackTrace();
            return null;
        }
        catch ( MissingHandlerException e )
        {
            LOG.info( "Instance creation is lacking an IPojo handler for component" + component );
            e.printStackTrace();
            return null;
        }
        catch ( ConfigurationException e )
        {
            LOG.info( "Instance configuration failed for component:" + component );
            e.printStackTrace();
            return null;
        }
    }


    /*
     * (non-Javadoc)
     * @see org.apache.directory.server.component.instance.ComponentInstanceGenerator#extractDefaultConfiguration(org.apache.directory.server.component.ADSComponent)
     */
    @Override
    public Properties extractDefaultConfiguration( ADSComponent component )
    {
        Properties defaultConf = new Properties();

        for ( PropertyDescription prop : component.getFactory().getComponentDescription().getProperties() )
        {

            //Must be lower case, alphanumeric+'-' only
            String propname = prop.getName();

            String propvalue = prop.getValue();
            String proptype = prop.getType();

            if ( !( proptype.equals( "int" ) || proptype.equals( "java.lang.String" ) || proptype
                .equals( "boolean" ) ) )
            {
                LOG.info( "Property found with an incompatible type:  "
                    + propname + ":" + proptype );
                continue;
            }

            if ( propvalue != null )
            {
                defaultConf.put( propname, propvalue );
            }

        }

        return defaultConf;

    }

}
