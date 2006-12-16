package org.apache.directory.shared.ldap.schema.syntax;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class AbstractSchemaDescription
{

    protected String numericOid;
    protected List<String> names;
    protected String description;
    protected boolean isObsolete;
    protected Map<String, List<String>> extensions;


    protected AbstractSchemaDescription()
    {
        numericOid = "";
        names = new ArrayList<String>();
        description = "";
        isObsolete = false;
        extensions = new LinkedHashMap<String, List<String>>();
    }


    public String getDescription()
    {
        return description;
    }


    public void setDescription( String description )
    {
        this.description = description;
    }


    public Map<String, List<String>> getExtensions()
    {
        return extensions;
    }


    public void setExtensions( Map<String, List<String>> extensions )
    {
        this.extensions = extensions;
    }


    public boolean isObsolete()
    {
        return isObsolete;
    }


    public void setObsolete( boolean isObsolete )
    {
        this.isObsolete = isObsolete;
    }


    public List<String> getNames()
    {
        return names;
    }


    public void setNames( List<String> names )
    {
        this.names = names;
    }


    public String getNumericOid()
    {
        return numericOid;
    }


    public void setNumericOid( String oid )
    {
        this.numericOid = oid;
    }


    public void addExtension( String key, List<String> values )
    {
        extensions.put( key, values );
    }

}
