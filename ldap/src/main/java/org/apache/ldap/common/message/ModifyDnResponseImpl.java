package org.apache.ldap.common.message ;


/**
 * Lockable ModifyDnResponse implementation
 * 
 * @author <a href="mailto:dev@directory.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class ModifyDnResponseImpl
    extends AbstractResultResponse implements ModifyDnResponse
{

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------


    static final long serialVersionUID = 996870775343263543L;

    /**
     * Creates a Lockable ModifyDnResponse as a reply to an ModifyDnRequest.
     *
     * @param id the sequence if of this response
     */
    public ModifyDnResponseImpl( final int id )
    {
        super( id, TYPE ) ;
    }
}
