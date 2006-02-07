/*
 *   Copyright 2005 The Apache Software Foundation
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

package org.apache.ntp.messages;

public class NtpMessage
{
    private LeapIndicatorType leapIndicator;
    private int versionNumber;
    private ModeType mode;
    private StratumType stratumType;
    private byte pollInterval;
    private byte precision;
    private int rootDelay;
    private int rootDispersion;

    private ReferenceIdentifier referenceIdentifier;

    private NtpTimeStamp referenceTimestamp;
    private NtpTimeStamp originateTimestamp;
    private NtpTimeStamp receiveTimestamp;
    private NtpTimeStamp transmitTimestamp;

    public NtpMessage( LeapIndicatorType leapIndicator, int versionNumber, ModeType mode, StratumType stratumType,
            byte pollInterval, byte precision, int rootDelay, int rootDispersion,
            ReferenceIdentifier referenceIdentifier, NtpTimeStamp referenceTimestamp, NtpTimeStamp originateTimestamp,
            NtpTimeStamp receiveTimestamp, NtpTimeStamp transmitTimestamp )
    {
        this.leapIndicator = leapIndicator;
        this.versionNumber = versionNumber;
        this.mode = mode;
        this.stratumType = stratumType;
        this.pollInterval = pollInterval;
        this.precision = precision;
        this.rootDelay = rootDelay;
        this.rootDispersion = rootDispersion;
        this.referenceIdentifier = referenceIdentifier;
        this.referenceTimestamp = referenceTimestamp;
        this.originateTimestamp = originateTimestamp;
        this.receiveTimestamp = receiveTimestamp;
        this.transmitTimestamp = transmitTimestamp;
    }

    /**
     * @return Returns the Leap Indicator.
     */
    public LeapIndicatorType getLeapIndicator()
    {
        return leapIndicator;
    }

    /**
     * @return Returns the Mode.
     */
    public ModeType getMode()
    {
        return mode;
    }

    /**
     * @return Returns the Originate Timestamp.
     */
    public NtpTimeStamp getOriginateTimestamp()
    {
        return originateTimestamp;
    }

    /**
     * @return Returns the Poll Interval.
     */
    public byte getPollInterval()
    {
        return pollInterval;
    }

    /**
     * @return Returns the Precision.
     */
    public byte getPrecision()
    {
        return precision;
    }

    /**
     * @return Returns the Receive Timestamp.
     */
    public NtpTimeStamp getReceiveTimestamp()
    {
        return receiveTimestamp;
    }

    /**
     * @return Returns the Reference Identifier.
     */
    public ReferenceIdentifier getReferenceIdentifier()
    {
        return referenceIdentifier;
    }

    /**
     * @return Returns the Reference Timestamp.
     */
    public NtpTimeStamp getReferenceTimestamp()
    {
        return referenceTimestamp;
    }

    /**
     * @return Returns the Root Delay.
     */
    public int getRootDelay()
    {
        return rootDelay;
    }

    /**
     * @return Returns the Root Dispersion.
     */
    public int getRootDispersion()
    {
        return rootDispersion;
    }

    /**
     * @return Returns the Stratum.
     */
    public StratumType getStratum()
    {
        return stratumType;
    }

    /**
     * @return Returns the Transmit Timestamp.
     */
    public NtpTimeStamp getTransmitTimestamp()
    {
        return transmitTimestamp;
    }

    /**
     * @return Returns the Version Number.
     */
    public int getVersionNumber()
    {
        return versionNumber;
    }
}
