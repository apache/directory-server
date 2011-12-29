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
import org.apache.directory.server.component.utilities.ADSConstants;
import org.apache.felix.ipojo.InstanceManager;


/**
 * Class that represents an individual instance of an ADSComponent
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ADSComponentInstance
{
    /*
     * IPojo instance name
     */
    private String instanceName;

    /*
     * An IPojo instance manager reference
     */
    private InstanceManager instanceManager;

    /*
     * Actual instance reference.
     */
    private Object instance;

    /*
     * The parent component of this spesific instance
     */
    private ADSComponent parentComponent;

    /*
     * LdifEntry of configuration hook.
     */
    private Properties instanceConfiguration;

    /*
     * Dn value shows where the configuration hook is set on DIT.
     */
    private String DITHookDn;


    /**
     * 
     * Creates a new instance of ADSComponentInstance.
     *
     * @param component Parent component of the component instance
     * @param pojo Underlying Pojo object if the component instance
     * @param configuration Configuration of the Pojo
     */
    public ADSComponentInstance( ADSComponent component, InstanceManager instanceManager, Properties configuration )
    {
        this.parentComponent = component;
        this.instanceManager = instanceManager;
        this.instanceConfiguration = configuration;
        this.instance = instanceManager.getPojoObject();
        this.instanceName = configuration.getProperty( ADSConstants.ADS_COMPONENT_INSTANCE_PROP_NAME );
    }


    /**
     * @return the instance name
     */
    public String getInstanceName()
    {
        return instanceName;
    }


    /**
     * @return the instance
     */
    public Object getInstance()
    {
        return instance;
    }


    /**
     * @return the parentComponent
     */
    public ADSComponent getParentComponent()
    {
        return parentComponent;
    }


    /**
     * @return the instanceConfiguration
     */
    public Properties getInstanceConfiguration()
    {
        return instanceConfiguration;
    }


    /**
     * @return the configHookDn
     */
    public String getDITHookDn()
    {
        return DITHookDn;
    }


    /**
     * @param configHookDn the configHookDn to set
     */
    public void setDITHookDn( String DITHookDn )
    {
        this.DITHookDn = DITHookDn;
    }


    /**
     * Reconfigures the underlying IPojo instance with the supplied configuration.
     * When called with null configuration, it reconfigures itself with the current configuration
     *
     * @param instanceConfiguration Instance configuration
     */
    public void reconfigure( Properties instanceConfiguration )
    {
        if ( instanceConfiguration != null )
        {
            this.instanceConfiguration = instanceConfiguration;
        }

        instanceManager.reconfigure( instanceConfiguration );
    }


    /**
     * Stops the IPojo instance management for this instance
     *
     */
    public void stop()
    {
        instanceManager.stop();
    }

}
