package org.apache.directory.server.hub.core.util;


import org.apache.directory.server.hub.api.component.DirectoryComponent;


public class ParentLink
{
    private DirectoryComponent parent;
    private String linkPoint;


    public ParentLink( DirectoryComponent parent, String linkPoint )
    {
        this.parent = parent;
        this.linkPoint = linkPoint;
    }


    public DirectoryComponent getParent()
    {
        return parent;
    }


    public String getLinkPoint()
    {
        return linkPoint;
    }


    @Override
    public boolean equals( Object obj )
    {
        if ( !( obj instanceof ParentLink ) )
        {
            return false;
        }

        ParentLink pl = ( ParentLink ) obj;

        return parent.equals( pl.getParent() ) && linkPoint.equals( pl.getLinkPoint() );
    }


    @Override
    public int hashCode()
    {
        return ( parent.getComponentPID() + linkPoint ).hashCode();
    }
}
