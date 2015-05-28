//------------------------------------------------------------------------------
// <copyright project="BEmu_maven" file="/BEmu_maven/bemu/src/main/java/com/bloomberglp/blpapi/MarketMessageSessionOpened.java" company="Jordan Robinson">
//     Copyright (c) 2013 Jordan Robinson. All rights reserved.
//
//     The use of this software is governed by the Microsoft Public License
//     which is included with this distribution.
// </copyright>
//------------------------------------------------------------------------------

package sra_london.bloomberg_emulator;

import sra_london.bloomberg_emulator.Name;
import sra_london.bloomberg_emulator.Message;

public class MarketMessageSessionOpened extends Message
{
    MarketMessageSessionOpened() throws Exception
    {
    	super(new Name("SessionStarted"), null, null);
    }

    public String topicName()
    {
    	return "";
    }
}
