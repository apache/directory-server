
package org.apache.directory.shared.converter.schema;

import java.util.ArrayList;
import java.util.List;

public class SchemaElementImpl implements SchemaElement
{
    protected boolean obsolete = false;
    protected String oid;
    protected String description;
    protected List<String> names = new ArrayList<String>();
    
    public boolean isObsolete()
    {
        return obsolete;
    }
    
    public String getOid()
    {
        return oid;
    }
    
    public String getDescription()
    {
        return description;
    }
    
    public List<String> getNames()
    {
        return names;
    }
    
    public void setNames( List<String> names )
    {
        this.names = names;
    }




    public String getShortAlias()
    {
        return ( names.size() == 0 ? "" : names.get( 0 ) );
    }
}
