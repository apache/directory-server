package org.apache.eve.db;


import java.util.Map;
import java.math.BigInteger;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.SearchControls;

import org.apache.ldap.common.filter.ExprNode;


/**
 * Given a search filter and a scope the search engine identifies valid
 * candidate entries returning their ids.
 * 
 */
public interface SearchEngine extends DatabaseEnabled
{
    /**
     * @todo put this in the right place
     * The alias dereferencing mode key for JNDI providers 
     */
    String ALIASMODE_KEY = "java.naming.ldap.derefAliases";
    /** 
     * @todo put this in the right place
     * The alias dereferencing mode value for JNDI providers 
     */
    String ALWAYS = "always";
    /** 
     * @todo put this in the right place
     * The alias dereferencing mode value for JNDI providers 
     */
    String NEVER = "never";
    /** 
     * @todo put this in the right place
     * The alias dereferencing mode value for JNDI providers 
     */
    String FINDING = "finding";
    /** 
     * @todo put this in the right place
     * The alias dereferencing mode value for JNDI providers 
     */
    String SEARCHING = "searching";
    
    /**
     * Gets the optimizer for this DefaultSearchEngine.
     *
     * @return the optimizer
     */
    Optimizer getOptimizer();
    
    /**
     * Conducts a search on a database.
     * 
     * @param base the search base
     * @param env the environment for the search
     * @param filter the search filter AST root
     * @param searchCtls the JNDI search controls
     * @return enumeration over SearchResults
     * @throws NamingException if the search fails
     */
    NamingEnumeration search( Name base, Map env, ExprNode filter,
        SearchControls searchCtls ) throws NamingException;

    /**
     * Evaluates a filter on an entry with a id.
     * 
     * @param filter the filter root AST node
     * @param id the id of the entry to test
     * @return true if the filter passes the entry, false otherwise
     * @throws NamingException if something goes wrong while accessing the db
     */
    boolean evaluate( ExprNode filter, BigInteger id )  throws NamingException;
}