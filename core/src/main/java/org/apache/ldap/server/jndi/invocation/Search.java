package org.apache.ldap.server.jndi.invocation;

import java.util.Map;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;

import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.server.BackingStore;

public class Search extends Invocation {

    private final Name baseName;
    private final Map environment;
    private final ExprNode filter;
    private final SearchControls controls;
    
    public Search( Name baseName, Map environment, ExprNode filters,
                   SearchControls controls )
    {
        if( baseName == null )
        {
            throw new NullPointerException( "baseName" );
        }
        if( environment == null )
        {
            throw new NullPointerException( "environment" );
        }
        if( filters == null )
        {
            throw new NullPointerException( "filter" );
        }
        if( controls == null )
        {
            throw new NullPointerException( "controls" );
        }
        
        this.baseName = baseName;
        this.environment = environment;
        this.filter = filters;
        this.controls = controls;
    }

    public Name getBaseName()
    {
        return baseName;
    }
    
    public Map getEnvironment()
    {
        return environment;
    }
    
    public ExprNode getFilter()
    {
        return filter;
    }
    
    public SearchControls getControls()
    {
        return controls;
    }

    protected Object doExecute( BackingStore store ) throws NamingException {
        return store.search( baseName, environment, filter, controls );
    }
}
