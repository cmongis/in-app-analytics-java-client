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
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import org.json.JSONException;
import org.json.JSONObject;

public class DefaultUsageFactory implements UsageFactory {

    private int idCount = 0;

    private final UUID sessionId = UUID.randomUUID();

    private final PublishSubject<MyUsageLog> sendQueue = PublishSubject.create();

    private final List<JSONObject> notSent = new ArrayList<>();

    private static final String ACCEPTED = "IJFX_USAGE_REPORT_ACCEPTED";
    private static final String DECIDED = "IJFX_USAGE_REPORT_DECIDED";

    Preferences prefService = Preferences.userNodeForPackage(DefaultUsageFactory.class);

    private Socket socket;

    private static final boolean debug = new File("./src").exists();

    public DefaultUsageFactory(Socket socket) {
        this.socket = socket;
        initialize();
    }

    public void initialize() {

        sendQueue
                .buffer(2, TimeUnit.SECONDS)
                .filter(list -> list.isEmpty() == false)
                .subscribe(this::handleUsageReports);

        if (hasDecided() && hasAccepted() == false) {
            sendQueue.onComplete();
        }

        createUsageLog(UsageType.SET, "CPU", UsageLocation.GENERAL)
                .setValue(Runtime.getRuntime().availableProcessors())
                .send();

        createUsageLog(UsageType.SET, "RAM", UsageLocation.GENERAL)
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
    public UsageFactory setDecision(Boolean accept) {
        prefService.putBoolean(DECIDED, true);
        prefService.putBoolean(ACCEPTED, accept.booleanValue());

        if (accept == false) {
            sendQueue.onComplete();
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
            if (!hasDecided() || hasAccepted()) {
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
                                .append(sessionId.toString())
                                .append(debug ? "debug" : "") // appending debug if it's in a debug environment
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

        // if the user decided to not send
        if (hasDecided() && hasAccepted() == false) {
            return;
        }

        List<MyUsageLog> notSent = objects
                .stream()
                .sorted((usage1, usage2) -> Integer.compare(usage1.getOrderId(), usage2.getOrderId()))
                .collect(Collectors.toList());
        if (hasDecided() == false) {
            return;
        }
        boolean abort = false;

        for (MyUsageLog usage : objects) {

            // if the socket is not available, we abort
            if (getSocket() == null || getSocket().connected() == false) {

                abort = true;
            }

            // if it has been aborted once, all the following messages has to be put
            // back in the stack in order to avoid sending messages in disorder
            // (even if it's wouldn't be a problem later since there is always
            // an order id)
            if (abort) {
                sendQueue.onNext(usage);
            } else {
                try {
                    getSocket().emit("usage", usage.toJSON());

                } // if sending fails for an other reason, we also pospone
                // sending the event and abort the current procedure
                catch (Exception e) {
                    e.printStackTrace();
                    sendQueue.onNext(usage);
                    abort = true;
                }

            }
        }
    }

}
