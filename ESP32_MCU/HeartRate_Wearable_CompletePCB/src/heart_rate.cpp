#include "heart_rate.h"
#include "max30102_config.h"
#include <math.h>

constexpr float    FS               = (float)MAX30102_SAMPLE_RATE / MAX30102_SAMPLE_AVERAGE;
constexpr uint32_t FINGER_THRESHOLD = 10000;
constexpr float    DC_ALPHA         = 0.02f;
constexpr float    LP_CUTOFF_HZ     = 2.5f;
constexpr float    SETTLE_SECONDS   = 1.5f;
constexpr float    ENV_DECAY        = 0.995f;
constexpr float    PEAK_FRACTION    = 0.7f;
constexpr float    MIN_BPM          = 40.0f;
constexpr float    MAX_BPM          = 200.0f;
constexpr int      HISTORY          = 4;
constexpr float    ACCEPT_RANGE     = 0.25f;
constexpr int      MAX_REJECTS      = 3;
constexpr float    QUALITY_SECONDS  = 3.0f;
constexpr uint32_t SATURATION_LEVEL = 260000;   // 18-bit ADC max is 262143
constexpr float    SPIKE_FRACTION   = 0.10f;    // pulse is ~1% of DC, a 10% jump is not a pulse
constexpr float    MAX_BAD_SECONDS  = 0.5f;     // this long of bad samples resets the algorithm
constexpr bool     PRINT_PEAKS      = true;     // PEAK,timeMs,intervalMs on serial

constexpr uint32_t SETTLE_SAMPLES  = (uint32_t)(SETTLE_SECONDS * FS);
constexpr uint32_t MIN_PEAK_GAP    = (uint32_t)(FS * 60.0f / MAX_BPM);
constexpr uint32_t QUALITY_SAMPLES = (uint32_t)(QUALITY_SECONDS * FS);
constexpr uint32_t MAX_BAD_SAMPLES = (uint32_t)(MAX_BAD_SECONDS * FS);

static float b0, b1, b2, a1, a2;
static bool  coeffsReady = false;
static float lpX1, lpX2, lpY1, lpY2;
static float dc = 0;

static bool     fingerOn = false;
static uint32_t n = 0;
static float    prev1 = 0, prev2 = 0;
static float    env = 0;
static bool     havePeak = false;
static uint32_t lastPeak = 0;
static bool     haveRef = false;
static uint32_t refBeat = 0;
static uint32_t badRun = 0;

static float    history[HISTORY];
static int      count = 0, idx = 0;
static int      rejects = 0;
static bool     haveAccepted = false;
static uint32_t lastAccepted = 0;

// 2nd-order Butterworth low-pass
static void computeCoeffs() {
  const float w0 = 2.0f * PI * LP_CUTOFF_HZ / FS;
  const float c = cosf(w0);
  const float alpha = sinf(w0) / (2.0f * 0.70710678f);
  const float a0 = 1.0f + alpha;
  b0 = ((1.0f - c) / 2.0f) / a0;
  b1 = (1.0f - c) / a0;
  b2 = b0;
  a1 = (-2.0f * c) / a0;
  a2 = (1.0f - alpha) / a0;
  coeffsReady = true;
}

static float averageBpm() {
  if (count == 0) return 0;
  float sum = 0;
  for (int i = 0; i < count; i++) sum += history[i];
  return sum / count;
}

static void clearHistory() {
  count = idx = 0;
  rejects = 0;
  haveAccepted = false;
}

static void handlePeak(uint32_t peak) {
  if (!haveRef) {
    haveRef = true;
    refBeat = peak;
    return;
  }

  const float beatBpm = 60.0f * FS / (float)(peak - refBeat);
  const float avg = averageBpm();

  bool accept;
  if (count == 0) accept = beatBpm >= MIN_BPM && beatBpm <= MAX_BPM;
  else            accept = fabsf(beatBpm - avg) <= ACCEPT_RANGE * avg;

  if (accept) {
    history[idx] = beatBpm;
    idx = (idx + 1) % HISTORY;
    if (count < HISTORY) count++;
    rejects = 0;
    refBeat = peak;
    haveAccepted = true;
    lastAccepted = peak;
    return;
  }

  // too slow means a missed beat, so this peak is real; too fast means an extra bump
  if (count == 0 || beatBpm < avg) refBeat = peak;

  if (++rejects >= MAX_REJECTS) {
    clearHistory();
    refBeat = peak;
  }
}

void hr_reset() {
  fingerOn = false;
  n = 0;
  lpX1 = lpX2 = lpY1 = lpY2 = 0;
  prev1 = prev2 = 0;
  env = 0;
  havePeak = false;
  haveRef = false;
  badRun = 0;
  clearHistory();
}

void hr_addSample(uint32_t ir) {
  if (!coeffsReady) computeCoeffs();

  if (ir < FINGER_THRESHOLD) {
    if (fingerOn) hr_reset();
    return;
  }
  if (!fingerOn) {
    hr_reset();
    fingerOn = true;
    dc = ir;
  }
  n++;

  // skip saturated samples and sudden jumps, too many in a row = start over
  const bool saturated = ir >= SATURATION_LEVEL;
  const bool spike = fabsf((float)ir - dc) > SPIKE_FRACTION * dc;
  if (saturated || spike) {
    if (++badRun >= MAX_BAD_SAMPLES) hr_reset();
    return;
  }
  badRun = 0;

  // inverted because the pulse makes IR dip
  dc += DC_ALPHA * ((float)ir - dc);
  const float ac = dc - (float)ir;

  const float y = b0 * ac + b1 * lpX1 + b2 * lpX2 - a1 * lpY1 - a2 * lpY2;
  lpX2 = lpX1; lpX1 = ac;
  lpY2 = lpY1; lpY1 = y;

  env *= ENV_DECAY;
  if (y > env) env = y;

  if (n > SETTLE_SAMPLES) {
    const bool isPeak = prev1 > prev2 && prev1 >= y &&
                        prev1 > 0 && prev1 > PEAK_FRACTION * env;
    const uint32_t peak = n - 1;
    if (isPeak && (!havePeak || peak - lastPeak >= MIN_PEAK_GAP)) {
      if (PRINT_PEAKS) {
        Serial.printf("PEAK,%lu,%lu\n",
                      (unsigned long)(peak * 1000.0f / FS),
                      havePeak ? (unsigned long)((peak - lastPeak) * 1000.0f / FS) : 0UL);
      }
      havePeak = true;
      lastPeak = peak;
      handlePeak(peak);
    }
  }

  prev2 = prev1;
  prev1 = y;
}

float hr_bpm() {
  return averageBpm();
}

bool hr_goodQuality() {
  return fingerOn && haveAccepted && (n - lastAccepted) <= QUALITY_SAMPLES;
}
