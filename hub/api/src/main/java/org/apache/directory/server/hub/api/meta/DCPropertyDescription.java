package org.apache.directory.server.hub.api.meta;




public class DCPropertyDescription
{
    private DCPropertyType propertyContext;
    private String name;
    private String type;
    private String defaultValue;
    private String description;
    private boolean mandatory;
    private String containerFor;


    public DCPropertyDescription( String name, String type, String defaultValue, String description, boolean mandatory,
        String containerFor )
    {
        this( null, name, type, defaultValue, description, mandatory, containerFor );
    }


    public DCPropertyDescription( DCPropertyType propertyContext, String name, String type, String defaultValue,
        String description, boolean mandatory,
        String containerFor )
    {
        this.propertyContext = propertyContext;
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
        this.description = description;
        this.mandatory = mandatory;
        this.containerFor = containerFor;
    }


    public String getName()
    {
        return name;
    }


    public String getType()
    {
        return type;
    }


    public String getDefaultValue()
    {
        return defaultValue;
    }


    public void setDefaultValue( String value )
    {
        this.defaultValue = value;
    }


    public String getDescription()
    {
        return description;
    }


    public boolean isMandatory()
    {
        return mandatory;
    }


    public String getContainerFor()
    {
        return containerFor;
    }


    public DCPropertyType getPropertyContext()
    {
        return propertyContext;
    }


    public void setPropertyContext( DCPropertyType propertyContext )
    {
        this.propertyContext = propertyContext;
    }

}
