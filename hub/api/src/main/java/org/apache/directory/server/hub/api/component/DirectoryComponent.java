package org.apache.directory.server.hub.api.component;


import java.util.ArrayList;
import java.util.List;




public class DirectoryComponent
{
    private String componentManagerPID;
    private String componentName;
    private String configLocation;
    private DCConfiguration configuration;

    private List<String> errors = new ArrayList<String>();
    private List<String> warnings = new ArrayList<String>();

    private boolean instantiationFailure;

    private boolean dirty;

    private DCRuntime runtimeInfo;


    public DirectoryComponent( String componentManagerPID, String componentName,
        DCConfiguration configuration )
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


    public void setConfiguration( DCConfiguration configuration )
    {
        this.configuration = configuration;
    }


    public DCConfiguration getConfiguration()
    {
        return configuration;
    }


    public void setRuntimeInfo( DCRuntime runtieInfo )
    {
        this.runtimeInfo = runtieInfo;
    }


    public DCRuntime getRuntimeInfo()
    {
        return runtimeInfo;
    }


    public String getComponentPID()
    {
        return componentManagerPID + "-" + getComponentName();
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
