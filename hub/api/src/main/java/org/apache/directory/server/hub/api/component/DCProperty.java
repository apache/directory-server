package org.apache.directory.server.hub.api.component;


public class DCProperty
{
    private String name;
    private String value;
    private Object object;


    public DCProperty( String name, String value )
    {
        this.name = name;
        this.value = value;
    }


    public String getName()
    {
        return name;
    }


    public String getValue()
    {
        return value;
    }


    public void setValue( String value )
    {
        this.value = value;
    }


    public Object getObject()
    {
        return object;
    }


    public void setObject( Object object )
    {
        this.object = object;
    }

}
