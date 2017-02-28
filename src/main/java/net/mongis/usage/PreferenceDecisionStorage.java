/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.mongis.usage;

import io.socket.client.Socket;
import java.util.prefs.Preferences;

/**
 *
 * @author cyril
 */
public class PreferenceDecisionStorage implements DecisionStorage {

    private static final String ACCEPTED = "IJFX_USAGE_REPORT_ACCEPTED";
    private static final String DECIDED = "IJFX_USAGE_REPORT_DECIDED";

    private final Preferences preferences;

    public PreferenceDecisionStorage(Class<?> userNodeForPackage) {
        preferences = Preferences.userNodeForPackage(DefaultUsageFactory.class);
    }
    
    public PreferenceDecisionStorage(Preferences pref) {
        preferences = pref;
    }
    
    public PreferenceDecisionStorage(Class<?> userNode,String id) {
        preferences = Preferences.userNodeForPackage(PreferenceDecisionStorage.class).node(id);
    }
    
    
    
    @Override
    public boolean hasAccepted() {
        return preferences.getBoolean(ACCEPTED, false);
    }

    @Override
    public void setDecision(boolean accept) {
        preferences.putBoolean(DECIDED, true);
        preferences.putBoolean(ACCEPTED, accept);
    }

    @Override
    public boolean hasDecided() {
        return preferences.getBoolean(DECIDED, false);
    }

}
