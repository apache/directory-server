/*
 * $Id: ProtocolListener.java,v 1.2 2003/03/13 18:27:29 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.event.protocol ;


public interface ProtocolListener
	extends java.util.EventListener
{
	void before(AbandonEvent a_event) ;
    void after(AbandonEvent a_event) ;

	void before(AddEvent a_event) ;
    void after(AddEvent a_event) ;

	void before(BindEvent a_event) ;
    void after(BindEvent a_event) ;

	void before(CompareEvent a_event) ;
    void after(CompareEvent a_event) ;

	void before(DelEvent a_event) ;
    void after(DelEvent a_event) ;

	void before(ExtendedEvent a_event) ;
    void after(ExtendedEvent a_event) ;

	void before(ModifyEvent a_event) ;
    void after(ModifyEvent a_event) ;

	void before(ModifyDnEvent a_event) ;
    void after(ModifyDnEvent a_event) ;

	void before(SearchEvent a_event) ;
    void after(SearchEvent a_event) ;

	void before(UnbindEvent a_event) ;
    void after(UnbindEvent a_event) ;
}
