package org.apache.ldap.server.jndi.request;

import java.util.Map;

import javax.naming.Name;
import javax.naming.directory.SearchControls;

import org.apache.ldap.common.filter.ExprNode;

public class Search extends Call {

    private final Name baseName;
    private final Map environment;
    private final ExprNode expressionNode;
    private final SearchControls searchControls;
    
    public Search( Name baseName, Map environment, ExprNode expressionNode,
                          SearchControls searchControls )
    {
        if( baseName == null )
        {
            throw new NullPointerException( "baseName" );
        }
        if( environment == null )
        {
            throw new NullPointerException( "environment" );
        }
        if( expressionNode == null )
        {
            throw new NullPointerException( "expressionNode" );
        }
        if( searchControls == null )
        {
            throw new NullPointerException( "searchControls" );
        }
        
        this.baseName = baseName;
        this.environment = environment;
        this.expressionNode = expressionNode;
        this.searchControls = searchControls;
    }

    public Name getBaseName()
    {
        return baseName;
    }
    
    public Map getEnvironment()
    {
        return environment;
    }
    
    public ExprNode getExpressionNode()
    {
        return expressionNode;
    }
    
    public SearchControls getSearchControls()
    {
        return searchControls;
    }
}
