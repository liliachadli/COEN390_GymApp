#include "bm11010.h"

void initializeSensor(uint8_t pwPin){
    pinMode(pwPin, INPUT); //Configure the pin 3 to behave as as input
}

void readPulse(uint8_t pwPin, sensor &s){
    s.pulseWidth = pulseIn(pwPin, HIGH, 45000); //Reads a pulse high on pin 3
}

void setDistance(sensor &s){
    if (s.pulseWidth > 0) {
        s.distanceCm = s.pulseWidth / 147.0f * 2.54f; //Convert pulse width to distance in cm
    }
}

float readDistance(sensor s){
    return s.distanceCm;
}

void setROM(float top, float bottom, data& d){
    d.ROM = top - bottom;
}
void setTimeLift(int top, int start, data& d){
    d.lift_time = top - start;
}
void setTimeLower(int end, int top, data& d){
    d.lower_time = end - top;
}
void setCurrentTime(int current_time, int timer, data &d){
    d.current_time = current_time - timer;
}

void Updaterep_counts(float top_d, float ref_d, data& d){
    d.valid_rep = 0;
    if (top_d/ref_d > 0.8) {
        d.rep_count++;
        d.valid_rep = 1;
    }
}

void clearData(data &d){
    d.lift_time = 0;
    d.lower_time = 0;
    d.rep_count = 0;
    d.ROM = 0;
    d.valid_rep = 0;
    d.current_time = 0;
}

void printdata(data &d) {
    Serial.printf("Timer: ");
    Serial.print(d.current_time);
    Serial.printf("Lift Time: ");
    Serial.print(d.lift_time);
    Serial.printf(" | Lower Time: ");
    Serial.print(d.lower_time);
    Serial.printf(" | Rep count: ");
    Serial.print(d.rep_count);
    Serial.printf(" | ROM: ");
    Serial.print(d.ROM);
    Serial.printf(" | Rep Validity: ");
    Serial.print(d.valid_rep);
    Serial.printf("\n");
}