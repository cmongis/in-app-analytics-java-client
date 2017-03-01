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

    boolean decided = false;
    boolean hasAccepted = false;
    
    
    @Override
    public boolean hasDecided() {
       return decided;
    }

    @Override
    public boolean hasAccepted() {
       return hasAccepted;
    }

    @Override
    public void setDecision(boolean decision) {
       decided = true;
       hasAccepted = decision;
    }
    
}
