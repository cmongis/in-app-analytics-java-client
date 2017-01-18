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


import io.reactivex.subjects.PublishSubject;
import io.socket.client.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import org.json.JSONException;
import org.json.JSONObject;


/**
 *  Service used to report usage report to the server
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

public class DefaultUsageFactory implements UsageFactory{
    
    
    
    
    
    
    private int idCount = 0;
    
    private final UUID sessionId = UUID.randomUUID();
  
   
    private final PublishSubject<JSONObject> sendQueue = PublishSubject.create();
    
    private final List<JSONObject> notSent = new ArrayList<>();
    
    private static final String ACCEPTED = "IJFX_USAGE_REPORT_ACCEPTED";
    private static final String DECIDED = "IJFX_USAGE_REPORT_DECIDED";
   
    
    
    Preferences prefService = Preferences.userNodeForPackage(DefaultUsageFactory.class);
    
    private Socket socket;
    
    
   
    
    public DefaultUsageFactory(Socket socket) {
        
        
        initialize();
    }
    
    
    public void initialize() {
        
        
        
        
                
        
        sendQueue
                
                .buffer(2, TimeUnit.SECONDS)
                .filter(list->list.isEmpty() == false)
                .subscribe(this::handleUsageReports);
        
        
        if(hasDecided() && hasAccepted() == false) {
            sendQueue.onComplete();
        }
        
        createUsageLog(UsageType.SET, "CPU", UsageLocation.INFO)
                .setValue(Runtime.getRuntime().availableProcessors())
                .send();
        
        createUsageLog(UsageType.SET,"RAM",UsageLocation.INFO)
                .setValue(Runtime.getRuntime().totalMemory() / 1000 / 1000)
                .send();
        
    }
    
    
    @Override
    public UsageLog createUsageLog(UsageType type, String name, UsageLocation location) {
        return new MyUsageLog(type, name, location);
    }

    @Override
    public boolean hasDecided() {
        return prefService.getBoolean(DECIDED, false);
    }
    
    private boolean hasAccepted() {
        return prefService.getBoolean(ACCEPTED, false);
    }
    
   private Socket getSocket() {
       return socket;
   }

    @Override
    public void setDecision(Boolean accept) {
        prefService.putBoolean(DECIDED,true);
        prefService.putBoolean(ACCEPTED, accept.booleanValue());
        
        if(accept == false) {
            sendQueue.onComplete();
        }
        
    }
    
    
     
    
    private class MyUsageLog extends AbstractUsageLog {
    
        public MyUsageLog(UsageType type, String name, UsageLocation location) {
            super(type, name, location);
        }
    
        @Override
        public UsageLog send() {
            if(!hasDecided() || hasAccepted()) {
                sendQueue.onNext(toJSON());
            }
            
            return this;
        }
        
        @Override
        public JSONObject toJSON() {
           
            
            try {
                return super
                        .toJSON()
                        .put("session_id", sessionId.toString())
                        .put("position",idCount++);
            } catch (JSONException ex) {
                Logger.getLogger(DefaultUsageFactory.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        }
        
    }   
    
    public void handleUsageReports(List<JSONObject> objects) {
        
        
        // if the user decided to not send
        if(hasDecided() && hasAccepted() == false) return;
        
        
        notSent.addAll(objects);
       
       
        
        if(hasDecided() == false) {
            return;
        }
        Iterator<JSONObject> iterator = notSent.iterator();
        
        // emptying the list of object to send
        while(iterator.hasNext()) {
            
            JSONObject object = iterator.next();
            
            // if the socket is not available, we abort
            if(getSocket() == null || getSocket().connected() == false) {
                return;
            }
            // if not, we send the usage and remove it from the send queue
            getSocket().emit("usage",object);
            
            iterator.remove();
            
        }
       
    }
    
    
  
    
}
