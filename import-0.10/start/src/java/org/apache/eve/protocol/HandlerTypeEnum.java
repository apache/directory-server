/*
 * $Id: HandlerTypeEnum.java,v 1.2 2003/08/22 21:15:56 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.protocol ;


import org.apache.avalon.framework.ValuedEnum ;


/**
 * Valued enumeration for the three types of handlers: NOREPLY, SINGLEREPLY,
 * and SEARCH.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.2 $
 */
public class HandlerTypeEnum
    extends ValuedEnum
{
    /** Value for noreply enumeration type */
    public static final int NOREPLY_VAL = 0 ;
    /** Value for singlereply enumeration type */
    public static final int SINGLEREPLY_VAL = 1 ;
    /** Value for search enumeration type */
    public static final int SEARCH_VAL = 2 ;

    /** Enum for noreply type */
	public static final HandlerTypeEnum NOREPLY =
        new HandlerTypeEnum("NOREPLY", NOREPLY_VAL) ;
    /** Enum for singlereply type */
	public static final HandlerTypeEnum SINGLEREPLY =
        new HandlerTypeEnum("SINGLEREPLY", SINGLEREPLY_VAL) ;
    /** Enum for search type */
	public static final HandlerTypeEnum SEARCH =
        new HandlerTypeEnum("SEARCH", SEARCH_VAL) ;


    /**
     * Enables creation of constants in this class only.
     *
     * @param a_name the name of the enum
     * @param a_value the value of the enum
     */
	private HandlerTypeEnum( String a_name, int a_value )
    {
        super( a_name, a_value ) ;
    }
}
