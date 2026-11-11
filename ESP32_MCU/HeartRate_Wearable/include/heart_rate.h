#pragma once
#include <Arduino.h>

void hr_reset();
void hr_addSample(uint32_t ir);

float hr_bpm();
bool hr_goodQuality();
