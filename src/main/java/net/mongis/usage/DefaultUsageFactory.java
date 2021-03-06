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

import io.socket.client.Socket;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.json.JSONException;
import org.json.JSONObject;
import rx.subjects.PublishSubject;

public class DefaultUsageFactory implements UsageFactory {

    private int idCount = 0;

    private final UUID sessionId = UUID.randomUUID();

    private final PublishSubject<MyUsageLog> sendQueue = PublishSubject.create();

    private final List<JSONObject> notSent = new ArrayList<>();

   
    private Socket socket;

   private final Logger logger = Logger.getLogger(this.getClass().getName());
    
    private static final boolean debug = new File("./src").exists();

    private DecisionStorage decisionStorage = new NullDecisionStorage();
    
    
   
    public DefaultUsageFactory(Socket socket,DecisionStorage storage) {
        this.socket = socket;
        initialize();
        this.decisionStorage = storage;
        logger.info("Usage factory created");
    }
    
    
    private DecisionStorage decisionStorage() {
        return  decisionStorage;
    }

    public void initialize() {
       
        sendQueue
                .buffer(2, TimeUnit.SECONDS)
                .filter(list -> list.isEmpty() == false)
                .subscribe(this::handleUsageReports);
        
         logger.info("Initialized");
    }

    @Override
    public UsageLog createUsageLog(UsageType type, String name, UsageLocation location) {
        return new MyUsageLog(type, name, location);
    }

    @Override
    public boolean hasDecided() {
        return decisionStorage().hasDecided();
    }

    private boolean hasAccepted() {
        return decisionStorage().hasAccepted();
    }

    private Socket getSocket() {
        return socket;
    }

    @Override
    public UsageFactory setDecision(Boolean accept) {
        
        logger.info("Setting decision = "+accept);
        
        decisionStorage().setDecision(accept.booleanValue());

        if (accept == false) {
            sendQueue.onCompleted();
        }
        return this;

    }

    private class MyUsageLog extends AbstractUsageLog {

        private final int orderId;

        public MyUsageLog(UsageType type, String name, UsageLocation location) {

            super(type, name, location);

            orderId = idCount++;

        }

        @Override
        public UsageLog send() {
            logger.info(String.format("Sending Usage log (decided=%s,accepted=%s", hasDecided(),hasAccepted()));
            if (!hasDecided() || hasAccepted()) {
                logger.info("Usage log added to queue");
                sendQueue.onNext(this);
            }

            return this;
        }

        @Override
        public JSONObject toJSON() {

            try {
                return super
                        .toJSON()
                        .put("session_id", new StringBuilder()
                                .append(debug ? "debug-" : "")
                                .append(sessionId.toString())
                                 // appending debug if it's in a debug environment
                                .toString())
                        .put("position", getOrderId());
            } catch (JSONException ex) {
                Logger.getLogger(DefaultUsageFactory.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        }

        public int getOrderId() {
            return orderId;
        }

    }

    public void handleUsageReports(List<MyUsageLog> objects) {

        
        logger.info("Treating usage logs : "+objects.size());
        
        // if the user decided to not send
        if (hasDecided() && hasAccepted() == false) {
            if(sendQueue.hasCompleted()== false) {
                sendQueue.onCompleted();
            }
            logger.info("Aborting usage logging");
            return;
        }

        
        Consumer<MyUsageLog> delayMethod = delayMethod = sendQueue::onNext;
        
        List<MyUsageLog> notSent = objects
                .stream()
                .sorted((usage1, usage2) -> Integer.compare(usage1.getOrderId(), usage2.getOrderId()))
                .collect(Collectors.toList());
        
        
       
        boolean delaySend = false;

        for (MyUsageLog usage : notSent) {
                      // if the socket is not available, we abort
            if (getSocket() == null || getSocket().connected() == false || hasDecided() == false) {
                logger.info("Usage logging delaid");
                delaySend = true;
            }

            // if it has been aborted once, all the following messages has to be put
            // back in the stack in order to avoid sending messages in disorder
            // (even if it's wouldn't be a problem later since there is always
            // an order id)
            if (delaySend) {
                delayMethod.accept(usage);
            } else {
                
                logger.info("Treaing Usage log #"+usage.getOrderId());

                
                try {
                   
                    getSocket().emit("usage", usage.toJSON());
                    logger.info("Usage log sent #"+usage.getOrderId());
                } // if sending fails for an other reason, we also pospone
                // sending the event and abort the current procedure
                catch (Exception e) {
                    e.printStackTrace();
                    delayMethod.accept(usage);
                    delaySend = true;
                }

            }
        }
    }

}
