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
import org.apache.directory.server.component.hub.InstanceManager;


/**
 * Class that represents an individual instance of an ADSComponent
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ComponentInstance
{
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

    /*
     * ConfigurationManager to manage instance's DIT hooks.
     */
    private InstanceManager instanceManager;

    /*
     * Specifies whether this instance's DIT hook will be watched for changes.
     */
    private boolean isReconfigurable;

    /*
     * Specifies whether instance is avtive or cached
     */
    private boolean isActive = false;


    /**
     * @return the instance
     */
    public Object getInstance()
    {
        return instance;
    }


    /**
     * @param instance the instance to set
     */
    public void setInstance( Object instance )
    {
        this.instance = instance;
    }


    /**
     * @return the parentComponent
     */
    public ADSComponent getParentComponent()
    {
        return parentComponent;
    }


    /**
     * @param parentComponent the parentComponent to set
     */
    public void setParentComponent( ADSComponent parentComponent )
    {
        this.parentComponent = parentComponent;
    }


    /**
     * @return the instanceConfiguration
     */
    public Properties getInstanceConfiguration()
    {
        return instanceConfiguration;
    }


    /**
     * @param instanceConfiguration the instanceConfiguration to set
     */
    public void setInstanceConfiguration( Properties instanceConfiguration )
    {
        this.instanceConfiguration = instanceConfiguration;
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
     * @param configManager the configManager to set
     */
    public void setInstanceManager( InstanceManager instanceManager )
    {
        this.instanceManager = instanceManager;
    }


    /**
     * @return the isActive
     */
    public boolean isActive()
    {
        return isActive;
    }


    /**
     * Sets the instance active.
     */
    public void setActive()
    {
        this.isActive = true;
    }


    /**
     * @return the isReconfigurable
     */
    public boolean isReconfigurable()
    {
        return isReconfigurable;
    }


    /**
     * @param isReconfigurable the isReconfigurable to set
     */
    public void setReconfigurable( boolean isReconfigurable )
    {
        this.isReconfigurable = isReconfigurable;
    }

}
