package org.apache.directory.server.hub.api.component;


public class DCRuntime
{
    private Object pojo;
    private Object specialObject;


    public DCRuntime( Object specialObject, Object pojo )
    {
        this.specialObject = specialObject;
        this.pojo = pojo;
    }


    public void setPojo( Object pojo )
    {
        this.pojo = pojo;
    }


    public Object getPojo()
    {
        return pojo;
    }


    public void setSpecialObject( Object specialObject )
    {
        this.specialObject = specialObject;
    }


    public Object getSpecialObject()
    {
        return specialObject;
    }
}
