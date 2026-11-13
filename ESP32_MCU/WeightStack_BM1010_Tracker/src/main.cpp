#include <Arduino.h>
#include "bm11010.h"
#include "bm11010_states.h"
#include "ble_tracker.h"
#include "ble_packet.h"

#define SONAR_PIN 3

StateMachine m;
sensor s;
data d;

int start_time;
int top_time;
int end_time;
int timer;

int current_time;
int last_time_moved;

float actual_distance = 0;
float precedent_distance = 0;
float start_distance = 0;

float topdistance = 0;
float bottom_distance = MAXFLOAT;
float refdistance_3reps[2];
float refdistance = 0 ;

bool isMoving = 0; 
bool isLowering = 0;
bool isLifting = 0;

int samples_move = 0;
int samples_lift = 0;
int samples_lower = 0;

bool isSubscribed = 0;

uint16_t bleSeq = 0;
int lastSentState = -1;   // -1 = send state on subscribe

uint32_t bleTimeMs() {
  return millis() - timer;
}

void sendRep() {
  RepPacket p = ble_buildRepPacket(bleSeq, bleTimeMs(), d.rep_count, d.valid_rep,
                                   m.getState(), d.ROM, d.lift_time, d.lower_time);
  if (ble_send(p)) {
    ble_printPacket(p);
    bleSeq++;
  }
}

void sendStateIfChanged() {
  int state = m.getState();
  if (state == lastSentState) return;

  StatePacket p = ble_buildStatePacket(bleSeq, bleTimeMs(), state);
  if (ble_send(p)) {
    ble_printPacket(p);
    bleSeq++;
    lastSentState = state;
  }
}

void setup() {
  Serial.begin(115200);
  delay(3000);

  initializeSensor(SONAR_PIN);  
  Serial.printf("BM11010 Sonar initialized\n");
  
  readPulse(SONAR_PIN, s);
  setDistance(s);
  actual_distance = s.distanceCm; 
  start_distance = actual_distance;
  m.setState(IDLE);

  ble_begin();
}


void loop() {
  readPulse(SONAR_PIN, s);
  setDistance(s);

  precedent_distance = actual_distance;
  actual_distance = s.distanceCm;
  current_time = millis();

  // reset timer and sequence on subscribe
    if(ble_isSubscribed() && !isSubscribed) {
      timer = millis();
      bleSeq = 0;
      lastSentState = -1;
      isSubscribed = 1;
      Serial.println("BLE: phone subscribed, timer reset");
      m.setState(IDLE);
    }
    else if(!ble_isSubscribed()){
      isSubscribed = 0;
    }

  //Check if sensor moving
  if (actual_distance + 1.5f >= precedent_distance && actual_distance - 1.5f <= precedent_distance){
    if (samples_move < 15) {
      samples_move++;
    }
    else if(samples_move == 15){
      isMoving = 0;
    }
  }
  else {
    samples_move = 0;
    isMoving = 1;
    last_time_moved = millis();
  }
  

  //Check if sensor going down
  if (actual_distance < precedent_distance){
    if (samples_lower < 5)
      samples_lower++;
    else if (samples_lower == 5)
      isLowering = 1;
  }
  else if (actual_distance > precedent_distance) {
    samples_lower = 0;
    isLowering = 0;
  }


  //Check if sensor going up
  if (actual_distance > precedent_distance){
    if(samples_lift < 5) {
      samples_lift++;
    }
    else if (samples_lift == 5) {
      isLifting = 1;
    }
  }
  else if (actual_distance < precedent_distance){
    samples_lift = 0;
    isLifting = 0;
  }
  

  switch (m.getState()) {
    case IDLE: //if for x samples no movement, save start dstance
      if(!isMoving && !isLifting && !isLowering){  
        start_distance = actual_distance; //Lowest Point. When Plates are down. Still
      }
      start_time = millis();
      d.rep_count = 0;
      
      if ( (start_distance + 5.0f) <= actual_distance){
          m.setState(START);
          start_distance = actual_distance;
      }

      break;

    case START:
      if (!isMoving && !isLifting && !isLowering) {
        m.setState(IDLE);
      }
      if (actual_distance >= ( start_distance + 10.0f)){
        m.setState(LIFT);
        sendStateIfChanged();
      }

      break;

    case LIFT:
      if (topdistance < actual_distance){
        topdistance = actual_distance;
        top_time = millis();
      }

      if (isLowering) {
        if (d.rep_count < 3) {
          if (refdistance < topdistance) {
            refdistance = topdistance;
          }
        }
        if (d.rep_count < 1) {
          end_time = start_time;
        }
        setTimeLift(top_time, end_time, d); //Lift Time, Lower Time
        m.setState(LOWER);
      }
      else if(!isMoving && !isLifting){ 
        if((last_time_moved + 7000) <= current_time ) {
          m.setState(IDLE);
          sendStateIfChanged();

          //initialize for idle state
          topdistance = 0;
          bottom_distance = MAXFLOAT; 
          refdistance = 0;
          start_distance = 0;
          clearData(d);
        }
      }

      break;

    case LOWER:
      if (bottom_distance > actual_distance) {
        end_time = millis();
        bottom_distance = actual_distance;
      }
    
      if(isLifting){
        setROM(topdistance, bottom_distance, d);   //ROM. (top-bottom)
        setTimeLower(end_time, top_time, d);
        Updaterep_counts(topdistance, refdistance, d); //verify rep, countrep++
        setCurrentTime(current_time, timer, d);
        
        //initialize for lifting state
        topdistance = 0;
        bottom_distance = MAXFLOAT; 
        m.setState(LIFT);

        printdata(d);
        sendRep();

      }
      else if(!isMoving && !isLowering) {
        if((last_time_moved + 5000) <= current_time ) {
          setROM(topdistance, bottom_distance, d);   //ROM. (top-bottom)
          setTimeLower(end_time, top_time, d);
          Updaterep_counts(topdistance, refdistance, d); //verify rep, countrep++
          setCurrentTime(current_time, timer, d);
          m.setState(IDLE);

          printdata(d);
          sendStateIfChanged();
          sendRep();

          //initialize for idle state
          topdistance = 0;
          bottom_distance = MAXFLOAT; 
          refdistance = 0;
          start_distance = 0;
          clearData(d);
        }
      }
      break;
  }
}