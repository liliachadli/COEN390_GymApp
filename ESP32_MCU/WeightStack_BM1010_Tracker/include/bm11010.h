#include <Arduino.h>

struct sensor{
    unsigned long pulseWidth;
    float distanceCm;
};

struct data{
    float ROM;
    int lift_time;
    int lower_time;
    int rep_count;
    bool valid_rep;
    int current_time;
};

void initializeSensor(uint8_t pwPin);
void readPulse(uint8_t pwPin, sensor &s);
void setDistance(sensor &s);
float readDistance(sensor &s);
//float getSpeed(sensor &s, float c_time, float s_time);
//float setStartDistance();

void setROM(float top, float bottom, data &d);        //ROM. (top-bottom)
void setTimeLift(int top, int start, data &d);        //Lift Time, Lower Time
void setTimeLower(int end, int top, data &d);
void setCurrentTime(int current_time, int timer, data &d);

void Updaterep_counts(float top_d, float start_d, data &d);        //verify rep, countrep++

void clearData(data &d);

void printdata(data &d);