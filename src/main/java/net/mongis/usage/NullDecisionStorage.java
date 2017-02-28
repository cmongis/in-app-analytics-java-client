/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.mongis.usage;

/**
 *
 * @author cyril
 */
public class NullDecisionStorage implements DecisionStorage{

    @Override
    public boolean hasDecided() {
       return false;
    }

    @Override
    public boolean hasAccepted() {
       return false;
    }

    @Override
    public void setDecision(boolean decision) {
       
    }
    
}
