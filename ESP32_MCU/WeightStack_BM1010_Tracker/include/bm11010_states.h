#pragma once
#include <Arduino.h>

enum State {
    IDLE,
    START, 
    LIFT,
    LOWER,
};

class StateMachine {
    private:
        State currentState = State::IDLE;
    public:
        void setState(State newState);  //empecher de passer dune state a une llgeal one (ex: dle-> lft)   
        State getState(); 
};
