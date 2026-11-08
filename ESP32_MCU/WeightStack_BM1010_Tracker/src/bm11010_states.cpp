#include "bm11010_states.h"

void StateMachine::setState(State newState){
    currentState = newState;
}

State StateMachine::getState(){
    return currentState;
}