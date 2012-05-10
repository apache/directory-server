package org.apache.directory.server.hub.core.util;


public class DCDependency
{
    private DCDependencyType dependencyType;
    private String dependencyId;


    public DCDependency( DCDependencyType dependencyType, String dependencyId )
    {
        this.dependencyType = dependencyType;
        this.dependencyId = dependencyId;
    }


    public DCDependencyType getType()
    {
        return dependencyType;
    }


    public String getId()
    {
        return dependencyId;
    }


    @Override
    public boolean equals( Object obj )
    {
        if ( !( obj instanceof DCDependency ) )
        {
            return false;
        }

        DCDependency dep = ( DCDependency ) obj;

        return dependencyType.equals( dep.getType() ) && dependencyId.equals( dep.getId() );
    }


    @Override
    public int hashCode()
    {
        return ( dependencyType.name() + dependencyId ).hashCode();
    }

    public enum DCDependencyType
    {
        MANAGER,
        INJECTION,
        REFERENCE
    }
}