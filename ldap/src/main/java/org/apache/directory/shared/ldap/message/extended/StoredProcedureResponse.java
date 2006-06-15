package org.apache.directory.shared.ldap.message.extended;


import org.apache.directory.shared.ldap.message.ExtendedResponseImpl;


public class StoredProcedureResponse extends ExtendedResponseImpl
{
    public StoredProcedureResponse( int messageId )
    {
        super( messageId, EXTENSION_OID );
    }

    private static final long serialVersionUID = 1L;
    public static final String EXTENSION_OID = "1.2.6.1.4.1.18060.1.1.1.100.7";

}
