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
package org.apache.ldap.common.exception;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ReferralException;

import org.apache.ldap.common.NotImplementedException;
import org.apache.ldap.common.message.ResultCodeEnum;


/**
 * A ReferralException which associates a resultCode namely the
 * {@link ResultCodeEnum#REFERRAL} resultCode with the exception.
 *
 * @see LdapException
 * @see ReferralException
 * @see <a href="http://java.sun.com/j2se/1.4.2/docs/guide/jndi/jndi-ldap-gl.html#EXCEPT">
 * LDAP ResultCode to JNDI Exception Mappings</a>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class LdapReferralException extends ReferralException implements LdapException
{
    static final long serialVersionUID = -8611970137960601723L;
    private final List refs;
    private int index = 0;

    
    /**
     * @see ReferralException#ReferralException()
     */
    public LdapReferralException( Collection refs )
    {
        this.refs = new ArrayList( refs );
    }


    /**
     * @see ReferralException#ReferralException(java.lang.String)
     */
    public LdapReferralException( Collection refs, String explanation )
    {
        super( explanation );
        this.refs = new ArrayList( refs );
    }


    /**
     * Always returns {@link ResultCodeEnum#REFERRAL}
     *
     * @see LdapException#getResultCode()
     */
    public ResultCodeEnum getResultCode()
    {
        return ResultCodeEnum.REFERRAL;
    }


    public Object getReferralInfo()
    {
        return refs.get( index );
    }


    public Context getReferralContext() throws NamingException
    {
        throw new NotImplementedException();
    }


    public Context getReferralContext( Hashtable arg ) throws NamingException
    {
        throw new NotImplementedException();
    }


    public boolean skipReferral()
    {
        index++;
        return index < refs.size();
    }


    public void retryReferral()
    {
        throw new NotImplementedException();
    }
}
