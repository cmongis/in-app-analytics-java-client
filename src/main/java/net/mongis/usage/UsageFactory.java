/*
    This file is part of ImageJ FX.

    ImageJ FX is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    ImageJ FX is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ImageJ FX.  If not, see <http://www.gnu.org/licenses/>. 
    
     Copyright 2015,2016 Cyril MONGIS, Michael Knop
	
 */
package net.mongis.usage;


/**
 *  Factory used to report usage report to the server
 * 
 *  @Parameter
 *  UsageReportService usageService;
 * 
 *  usageService
 *      .createUsage(UsageType.CLICK,"Activity",UsageLocation.SIDE_PANEL)
 *      .send();
 *  
 *  usageService.
 *      .createUsage(UsageType.SWITCH,"Threshold min/max",UsageLocation.LUT_PANEL)
 *      .setValue(true)
 *      .send();
 *  usageService.
 *      createUsage(UsageType.SET,"filter",UsageLocation.EXPLORER)
 *      .setValue("well")
 *      .send();
 * 
 * @author cyril
 */
public interface UsageFactory {

    
    public boolean hasDecided();
    public void setDecision(Boolean accept);
    
    /**
     * 
     * @param type of usage event (click, value setting, etc.)
     * @param name id of the event or of the value that has been set
     * @param location location of the event
     * @return an usage log that needs to be sent
     */
    UsageLog createUsageLog(UsageType type, String name, UsageLocation location);
    
}
