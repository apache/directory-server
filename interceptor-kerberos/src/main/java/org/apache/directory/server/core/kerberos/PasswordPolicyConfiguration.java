/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.server.core.kerberos;


import org.apache.directory.shared.ldap.constants.SchemaConstants;


/**
 * A simple pojo holding the password policy configuration base on 
 * <a href="http://tools.ietf.org/html/draft-behera-ldap-password-policy-10">this draft</a>.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PasswordPolicyConfiguration
{
    /** the name of the attribute to which the password policy is applied. 
     * Currently only "userPassword" attribute is supported
     */
    private String pwdAttribute = SchemaConstants.USER_PASSWORD_AT;

    /** 
     * holds the number of seconds that must elapse between modifications to the password. 
     * Default value is 0 
     */
    private int pwdMinAge = 0;

    /**
     *  holds the number of seconds after which a modified password will expire.
     *  Default value is 0, does not expire.  If not 0, the value must be greater than or equal
     *  to the value of the pwdMinAge.
     */
    private int pwdMaxAge;

    /**
     *  specifies the maximum number of used passwords stored in the pwdHistory attribute.
     *  Default value is 0, no password history maintained
     */
    private int pwdInHistory = 0;

    /** indicates how the password quality will be verified while being modified or added.
     *  Default value 0, do not check 
     */
    private int pwdCheckQuality = 0;

    /** this attribute holds the minimum number of characters that must be used in a password. 
     *  Default value -1, no minimum length enforced
     */
    private int pwdMinLength = -1;

    /**
     * this attribute holds the maximum number of characters that may be used in a password.
     * Default value -1, no maximum length enforced
     */
    private int pwdMaxLength = -1;

    /**
     * the maximum number of seconds before a password is due to expire that expiration warning
     * messages will be returned to an authenticating user.
     * Default value is 0, never send a warning message.
     */
    private int pwdExpireWarning = 0;

    /** 
     * the number of times an expired password can be used to authenticate.
     * Default value is 0, do not allow a expired password for authentication.
     */
    private int pwdGraceAuthNLimit = 0;

    /** 
     * specifies the number of seconds the grace authentications are valid
     * Default value is 0, no limit.
     */
    private int pwdGraceExpire = 0;

    /**
     * flag to indicate if the account needs to be locked after a specified number of
     * consecutive failed bind attempts. The maximum number of consecutive
     * failed bind attempts is specified in {@link #pwdMaxFailure}
     */
    private boolean pwdLockout;

    /**
     * the number of seconds that the password cannot be used to authenticate due to 
     * too many failed bind attempts.
     * Default value is 300 seconds.
     */
    private int pwdLockoutDuration = 300;

    /**
     * the number of consecutive failed bind attempts after which the password may not 
     * be used to authenticate.
     * Default value is 0, no limit on the number of authentication failures
     */
    private int pwdMaxFailure;

    /** the number of seconds after which the password failures are purged from the failure counter. */
    private int pwdFailureCountInterval;

    /** 
     * flag to indicate if the password must be changed by the user after they bind to the 
     * directory after a password is set or reset by a password administrator.
     * Default value is false, no need to change the password by user.
     */
    private boolean pwdMustChange = false;

    /** indicates whether users can change their own passwords. Default value is true, allow change */
    private boolean pwdAllowUserChange = true;

    /**
     *  flag to specify whether or not the existing password must be sent along with the
     *  new password when being changed.
     *  Default value is false.
     */
    private boolean pwdSafeModify = false;

    /** 
     * the number of seconds to delay responding to the first failed authentication attempt
     * Default value 0, no delay.
     */
    private int pwdMinDelay = 0;

    /** the maximum number of seconds to delay when responding to a failed authentication attempt.*/
    private int pwdMaxDelay;

    /** 
     * the number of seconds an account may remain unused before it becomes locked
     * Default value is 0, no check for idle time.
     */
    private int pwdMaxIdle;


    public String getPwdAttribute()
    {
        return pwdAttribute;
    }


    public void setPwdAttribute( String pwdAttribute )
    {
        this.pwdAttribute = pwdAttribute;
    }


    public int getPwdMinAge()
    {
        return pwdMinAge;
    }


    public void setPwdMinAge( int pwdMinAge )
    {
        this.pwdMinAge = pwdMinAge;
    }


    public int getPwdMaxAge()
    {
        return pwdMaxAge;
    }


    public void setPwdMaxAge( int pwdMaxAge )
    {
        this.pwdMaxAge = pwdMaxAge;
    }


    public int getPwdInHistory()
    {
        return pwdInHistory;
    }


    public void setPwdInHistory( int pwdInHistory )
    {
        this.pwdInHistory = pwdInHistory;
    }


    public int getPwdCheckQuality()
    {
        return pwdCheckQuality;
    }


    public void setPwdCheckQuality( int pwdCheckQuality )
    {
        this.pwdCheckQuality = pwdCheckQuality;
    }


    public int getPwdMinLength()
    {
        return pwdMinLength;
    }


    public void setPwdMinLength( int pwdMinLength )
    {
        this.pwdMinLength = pwdMinLength;
    }


    public int getPwdMaxLength()
    {
        return pwdMaxLength;
    }


    public void setPwdMaxLength( int pwdMaxLength )
    {
        this.pwdMaxLength = pwdMaxLength;
    }


    public int getPwdExpireWarning()
    {
        return pwdExpireWarning;
    }


    public void setPwdExpireWarning( int pwdExpireWarning )
    {
        this.pwdExpireWarning = pwdExpireWarning;
    }


    public int getPwdGraceAuthNLimit()
    {
        return pwdGraceAuthNLimit;
    }


    public void setPwdGraceAuthNLimit( int pwdGraceAuthNLimit )
    {
        this.pwdGraceAuthNLimit = pwdGraceAuthNLimit;
    }


    public int getPwdGraceExpire()
    {
        return pwdGraceExpire;
    }


    public void setPwdGraceExpire( int pwdGraceExpire )
    {
        this.pwdGraceExpire = pwdGraceExpire;
    }


    public boolean isPwdLockout()
    {
        return pwdLockout;
    }


    public void setPwdLockout( boolean pwdLockout )
    {
        this.pwdLockout = pwdLockout;
    }


    public int getPwdLockoutDuration()
    {
        return pwdLockoutDuration;
    }


    public void setPwdLockoutDuration( int pwdLockoutDuration )
    {
        this.pwdLockoutDuration = pwdLockoutDuration;
    }


    public int getPwdMaxFailure()
    {
        return pwdMaxFailure;
    }


    public void setPwdMaxFailure( int pwdMaxFailure )
    {
        this.pwdMaxFailure = pwdMaxFailure;
    }


    public int getPwdFailureCountInterval()
    {
        return pwdFailureCountInterval;
    }


    public void setPwdFailureCountInterval( int pwdFailureCountInterval )
    {
        this.pwdFailureCountInterval = pwdFailureCountInterval;
    }


    public boolean isPwdMustChange()
    {
        return pwdMustChange;
    }


    public void setPwdMustChange( boolean pwdMustChange )
    {
        this.pwdMustChange = pwdMustChange;
    }


    public boolean isPwdAllowUserChange()
    {
        return pwdAllowUserChange;
    }


    public void setPwdAllowUserChange( boolean pwdAllowUserChange )
    {
        this.pwdAllowUserChange = pwdAllowUserChange;
    }


    public boolean isPwdSafeModify()
    {
        return pwdSafeModify;
    }


    public void setPwdSafeModify( boolean pwdSafeModify )
    {
        this.pwdSafeModify = pwdSafeModify;
    }


    public int getPwdMinDelay()
    {
        return pwdMinDelay;
    }


    public void setPwdMinDelay( int pwdMinDelay )
    {
        this.pwdMinDelay = pwdMinDelay;
    }


    public int getPwdMaxDelay()
    {
        return pwdMaxDelay;
    }


    public void setPwdMaxDelay( int pwdMaxDelay )
    {
        this.pwdMaxDelay = pwdMaxDelay;
    }


    public int getPwdMaxIdle()
    {
        return pwdMaxIdle;
    }


    public void setPwdMaxIdle( int pwdMaxIdle )
    {
        this.pwdMaxIdle = pwdMaxIdle;
    }

}
