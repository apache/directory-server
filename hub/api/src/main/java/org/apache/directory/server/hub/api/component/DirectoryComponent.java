/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.server.hub.api.component;


import java.util.ArrayList;
import java.util.List;


public class DirectoryComponent
{
    private String componentManagerPID;
    private String componentName;
    private String configLocation;
    private DcConfiguration configuration;

    private List<String> errors = new ArrayList<String>();
    private List<String> warnings = new ArrayList<String>();

    private boolean instantiationFailure;

    private boolean dirty;

    private DcRuntime runtimeInfo;


    public DirectoryComponent( String componentManagerPID, String componentName,
        DcConfiguration configuration )
    {
        this.componentManagerPID = componentManagerPID;
        this.componentName = componentName;
        this.configuration = configuration;
    }


    public String getComponentManagerPID()
    {
        return componentManagerPID;
    }


    public void setComponentName( String componentPID )
    {
        this.componentName = componentPID;
    }


    public String getComponentName()
    {
        return componentName;
    }


    public void setConfiguration( DcConfiguration configuration )
    {
        this.configuration = configuration;
    }


    public DcConfiguration getConfiguration()
    {
        return configuration;
    }


    public void setRuntimeInfo( DcRuntime runtieInfo )
    {
        this.runtimeInfo = runtieInfo;
    }


    public DcRuntime getRuntimeInfo()
    {
        return runtimeInfo;
    }


    public String getComponentPID()
    {
        return componentManagerPID + "[" + getComponentName() + "]";
    }


    public String getConfigLocation()
    {
        return configLocation;
    }


    public void setConfigLocation( String configLocation )
    {
        this.configLocation = configLocation;
    }


    public boolean instantiationFailed()
    {
        return instantiationFailure;
    }


    public void setFailFlag( boolean dirty )
    {
        this.instantiationFailure = dirty;
    }


    public boolean isDirty()
    {
        return dirty;
    }


    public void setDirty( boolean dirty )
    {
        this.dirty = dirty;
    }


    public void clearErrorsAndWarns()
    {
        errors.clear();
        warnings.clear();
    }


    public void addError( String error )
    {
        errors.add( error );
    }


    public void addWarn( String warn )
    {
        warnings.add( warn );
    }


    public List<String> getErrors()
    {
        return errors;
    }


    public List<String> getWarnings()
    {
        return warnings;
    }

}
