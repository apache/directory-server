/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.ldap.server.jndi;


import java.util.Map;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;

import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.server.auth.LdapPrincipal;


/**
 * An interceptor base class which delegates handling of specific Invocations
 * to member methods within this Interceptor.  These handler methods are
 * analogous to the methods assocated with the Invocation.  They have the same
 * name and arguments as do the method associated with the Invocation.  The
 * analog member methods simply serve as a clean way to handle interception
 * without having to cast parameter Objects or recode this huge switch statement
 * for each concrete Interceptor implementation.
 *
 * A ThreadLocal is used by all BaseInterceptors to associate the current
 * Thread of execution with an Invocation object.  This is done to optimize
 * the use of a single thread local for all instances of the BaseInterceptor
 * class.  It also removes the need for the invoke() method implementation to
 * have to set and [un]set the thread local Invocation on each invoke call of
 * every BaseInterceptor instance.
 *
 * The question then arrises, "Why do we need the ThreadLocal?"  Well why pass
 * around the Invocation object to all analog methods.  Plus we use member
 * methods rather than static methods to access thread locals and make the
 * analogs appear cleaner matching their respective invocation methods.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class BaseInterceptor implements Interceptor
{
    /** stores invocation objects by thread so we don't pass em around */
    private static ThreadLocal invocations = new ThreadLocal();


    // ------------------------------------------------------------------------
    // S T A T I C   M E T H O D S
    // ------------------------------------------------------------------------


    /**
     * Sets the Invocation object for the current thread of execution.  This is
     * automatically called twice by the interceptor framework for each new
     * Invocation.  First right after creation to set the Invocation for the
     * current thread, and after all Interceptors have been called to unset
     * the Invocation for the current thread.
     *
     * @param invocation the invocation for the current thread of execution
     */
    static void setInvocation( Invocation invocation )
    {
        invocations.set( invocation );
    }


    /**
     * Gets the invocation's current context's Principal.
     *
     * @param invocation the current invocation context's principal
     * @return the principal making the call
     */
    public static LdapPrincipal getPrincipal( Invocation invocation )
    {
        ServerContext ctx = ( ServerContext ) invocation.getContextStack().peek();
        return ctx.getPrincipal();
    }


    // ------------------------------------------------------------------------
    // Interceptor's Invoke Method
    // ------------------------------------------------------------------------


    /**
     * Uses a switch on the invocation method type to call the respective member
     * analog method that does the work of the Interceptor for that Invocation
     * method.
     *
     * @see org.apache.ldap.server.jndi.Interceptor#invoke(Invocation)
     */
    public void invoke( Invocation invocation ) throws NamingException
    {
        InvocationMethodEnum e = invocation.getInvocationMethodEnum();

        switch ( e.getValue() )
        {
            case( InvocationMethodEnum.ADD_VAL ):
                add( ( String ) invocation.getParameters()[0],
                     ( Name ) invocation.getParameters()[1],
                     ( Attributes ) invocation.getParameters()[2] );
                break;
            case( InvocationMethodEnum.DELETE_VAL ):
                delete( ( Name ) invocation.getParameters()[0] );
                break;
            case( InvocationMethodEnum.GETMATCHEDDN_VAL ):
                getMatchchedDn( ( Name ) invocation.getParameters()[0],
                        ( ( Boolean ) invocation.getParameters()[1] ).booleanValue() );
                break;
            case( InvocationMethodEnum.GETSUFFIX_VAL ):
                getSuffix( ( Name ) invocation.getParameters()[0],
                        ( ( Boolean ) invocation.getParameters()[1] ).booleanValue() );
                break;
            case( InvocationMethodEnum.HASENTRY_VAL ):
                hasEntry( ( Name ) invocation.getParameters()[0] );
                break;
            case( InvocationMethodEnum.ISSUFFIX_VAL ):
                isSuffix( ( Name ) invocation.getParameters()[0] );
                break;
            case( InvocationMethodEnum.LIST_VAL ):
                list( ( Name ) invocation.getParameters()[0] );
                break;
            case( InvocationMethodEnum.LISTSUFFIXES_VAL ):
                listSuffixes( ( ( Boolean ) invocation.getParameters()[1] )
                        .booleanValue() );
                break;
            case( InvocationMethodEnum.LOOKUP_NAME_VAL ):
                lookup( ( Name ) invocation.getParameters()[0] );
                break;
            case( InvocationMethodEnum.LOOKUP_NAME_STRINGARR_VAL ):
                lookup( ( Name ) invocation.getParameters()[0],
                        ( String[] ) invocation.getParameters()[1] );
                break;
            case( InvocationMethodEnum.MODIFY_NAME_INT_ATTRIBUTES_VAL ):
                modify( ( Name ) invocation.getParameters()[0],
                        ( ( Integer ) invocation.getParameters()[1]).intValue(),
                        ( Attributes ) invocation.getParameters()[2] );
                break;
            case( InvocationMethodEnum.MODIFY_NAME_MODIFICATIONITEMARR_VAL ):
                modify( ( Name ) invocation.getParameters()[0],
                        ( ModificationItem[] ) invocation.getParameters()[1] );
                break;
            case( InvocationMethodEnum.MODIFYRDN_VAL ):
                modifyRdn( ( Name ) invocation.getParameters()[0],
                        ( String ) invocation.getParameters()[1],
                        ( ( Boolean ) invocation.getParameters()[2] ).booleanValue() );
                break;
            case( InvocationMethodEnum.MOVE_NAME_NAME_VAL ):
                move( ( Name ) invocation.getParameters()[0],
                        ( Name ) invocation.getParameters()[1] );
                break;
            case( InvocationMethodEnum.MOVE_NAME_NAME_STRING_BOOL_VAL ):
                move( ( Name ) invocation.getParameters()[0],
                        ( Name ) invocation.getParameters()[1],
                        ( String ) invocation.getParameters()[2],
                        ( ( Boolean ) invocation.getParameters()[3] ).booleanValue() );
                break;
            case( InvocationMethodEnum.SEARCH_VAL ):
                search( ( Name ) invocation.getParameters()[0],
                        ( Map ) invocation.getParameters()[1],
                        ( ExprNode ) invocation.getParameters()[2],
                        ( SearchControls ) invocation.getParameters()[3] );
                break;
            default:
                throw new IllegalStateException( "Unexpected invocation type "
                    + e );
        }
    }


    /**
     * Gets the Invocation associated with the current thread of execution.
     *
     * @return the Invocation associated with the current thread of execution
     */
    protected Invocation getInvocation()
    {
        return ( Invocation ) invocations.get();
    }


    // ------------------------------------------------------------------------
    // Invocation Analogs
    // ------------------------------------------------------------------------


    /**
     * Override to inject functionality before, after or on error to
     * {@link org.apache.ldap.server.BackingStore#add(String, Name, Attributes)}.
     *
     * @see org.apache.ldap.server.BackingStore#add(String, Name, Attributes)
     */
    protected void add( String upName, Name normName, Attributes entry ) throws NamingException
    {
    }


    /**
     * Override to inject functionality before, after or on error to
     * {@link org.apache.ldap.server.BackingStore#delete(Name)}.
     *
     * @see org.apache.ldap.server.BackingStore#delete(Name)}
     */
    protected void delete( Name name ) throws NamingException
    {
    }


    /**
     * Override to inject functionality before, after or on error to
     * {@link org.apache.ldap.server.PartitionNexus#getMatchedDn(Name, boolean)}.
     *
     * @see org.apache.ldap.server.PartitionNexus#getMatchedDn(Name, boolean)
     */
    protected void getMatchchedDn( Name dn, boolean normalized ) throws NamingException
    {
    }


    /**
     * Override to inject functionality before, after or on error to
     * {@link org.apache.ldap.server.ContextPartition#getSuffix(boolean)}.
     *
     * @see org.apache.ldap.server.ContextPartition#getSuffix(boolean)
     */
    protected void getSuffix( Name dn, boolean normalized ) throws NamingException
    {
    }


    /**
     * Override to inject functionality before, after or on error to
     * {@link org.apache.ldap.server.BackingStore#hasEntry(Name)}.
     *
     * @see org.apache.ldap.server.BackingStore#hasEntry(Name)
     */
    protected void hasEntry( Name dn ) throws NamingException
    {
    }


    /**
     * Override to inject functionality before, after or on error to
     * {@link org.apache.ldap.server.BackingStore#isSuffix(Name)}.
     *
     * @see org.apache.ldap.server.BackingStore#isSuffix(Name)}
     */
    protected void isSuffix( Name name ) throws NamingException
    {
    }


    /**
     * Override to inject functionality before, after or on error to
     * {@link org.apache.ldap.server.BackingStore#list(Name)}.
     *
     * @see org.apache.ldap.server.BackingStore#list(Name)
     */
    protected void list( Name base ) throws NamingException
    {
    }


    /**
     * Override to inject functionality before, after or on error to
     * {@link org.apache.ldap.server.PartitionNexus#listSuffixes(boolean)}.
     *
     * @see org.apache.ldap.server.PartitionNexus#listSuffixes(boolean)
     */
    protected void listSuffixes( boolean normalized ) throws NamingException
    {
    }


    /**
     * Override to inject functionality before, after or on error to
     * {@link org.apache.ldap.server.BackingStore#lookup(javax.naming.Name)}.
     *
     * @see org.apache.ldap.server.BackingStore#lookup(javax.naming.Name)
     */
    protected void lookup( Name dn ) throws NamingException
    {
    }


    /**
     * Override to inject functionality before, after or on error to
     * {@link org.apache.ldap.server.PartitionNexus#lookup(javax.naming.Name, String[])}.
     *
     * @see org.apache.ldap.server.PartitionNexus#lookup(javax.naming.Name, String[])
     */
    protected void lookup( Name dn, String[] attrIds ) throws NamingException
    {
    }


    /**
     * Override to inject functionality before, after or on error to
     * {@link org.apache.ldap.server.BackingStore#modify(Name, int, Attributes)}.
     *
     * @see org.apache.ldap.server.BackingStore#modify(Name, int, Attributes)
     */
    protected void modify( Name dn, int modOp, Attributes mods ) throws NamingException
    {
    }


    /**
     * Override to inject functionality before, after or on error to
     * {@link org.apache.ldap.server.BackingStore#modify(Name, ModificationItem[])}.
     *
     * @see org.apache.ldap.server.BackingStore#modify(Name, ModificationItem[])
     */
    protected void modify( Name dn, ModificationItem[] mods ) throws NamingException
    {
    }


    /**
     * Override to inject functionality before, after or on error to
     * {@link org.apache.ldap.server.BackingStore#modifyRn(Name, String, boolean)}.
     *
     * @see org.apache.ldap.server.BackingStore#modifyRn(Name, String, boolean)
     */
    protected void modifyRdn( Name dn, String newRdn, boolean deleteOldRdn )
        throws NamingException
    {
    }


    /**
     * Override to inject functionality before, after or on error to
     * {@link org.apache.ldap.server.BackingStore#move(Name, Name)}.
     *
     * @see org.apache.ldap.server.BackingStore#move(Name, Name)
     */
    protected void move( Name oriChildName, Name newParentName ) throws NamingException
    {
    }


    /**
     * Override to inject functionality before, after or on error to
     * {@link org.apache.ldap.server.BackingStore#move(Name, Name, String, boolean)}.
     *
     * @see org.apache.ldap.server.BackingStore#move(Name, Name, String, boolean)
     */
    protected void move( Name oriChildName, Name newParentName, String newRdn,
                         boolean deleteOldRdn ) throws NamingException
    {
    }


    /**
     * Override to inject functionality before, after or on error to
     * {@link org.apache.ldap.server.BackingStore#search(Name, Map, ExprNode, SearchControls)}.
     *
     * @see org.apache.ldap.server.BackingStore#search(Name, Map, ExprNode, SearchControls)
     */
    protected void search( Name base, Map env, ExprNode filter,
                           SearchControls searchControls ) throws NamingException
    {
    }
}
