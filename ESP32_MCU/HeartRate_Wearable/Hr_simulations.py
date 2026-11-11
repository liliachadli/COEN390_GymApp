"""
Smart Gym Tracker - Heart-rate (MAX30102) simulation (SIM-1.3)
"""
import math, random

# ---------------------------------------------------------------- constants (heart_rate.cpp)
FS = 100.0
FINGER_THRESHOLD = 10000; DC_ALPHA = 0.02; LP_CUTOFF_HZ = 2.5; SETTLE_SECONDS = 1.5
ENV_DECAY = 0.995; PEAK_FRACTION = 0.7; MIN_BPM = 40.0; MAX_BPM = 200.0; HISTORY = 4
ACCEPT_RANGE = 0.25; MAX_REJECTS = 3; QUALITY_SECONDS = 3.0; SATURATION_LEVEL = 260000
SPIKE_FRACTION = 0.10; MAX_BAD_SECONDS = 0.5
SETTLE_SAMPLES = int(SETTLE_SECONDS * FS); MIN_PEAK_GAP = int(FS * 60.0 / MAX_BPM)
QUALITY_SAMPLES = int(QUALITY_SECONDS * FS); MAX_BAD_SAMPLES = int(MAX_BAD_SECONDS * FS)
MOTION_HOLD_MS = 2000; MOTION_MATCH = 0.15                      # main.cpp


# ---------------------------------------------------------------- firmware logic
class HeartRate:
    def __init__(self):
        w0 = 2 * math.pi * LP_CUTOFF_HZ / FS; c = math.cos(w0)
        alpha = math.sin(w0) / (2 * 0.70710678); a0 = 1 + alpha
        self.b0 = (1 - c) / 2 / a0; self.b1 = (1 - c) / a0; self.b2 = self.b0
        self.a1 = -2 * c / a0; self.a2 = (1 - alpha) / a0
        self.dc = 0.0
        self.reset()

    def clear_history(self):
        self.history = [0.0] * HISTORY; self.count = self.idx = 0
        self.rejects = 0; self.have_accepted = False

    def reset(self):
        self.finger_on = False; self.n = 0
        self.x1 = self.x2 = self.y1 = self.y2 = 0.0
        self.prev1 = self.prev2 = 0.0; self.env = 0.0
        self.have_peak = False; self.last_peak = 0
        self.have_ref = False; self.ref_beat = 0
        self.bad_run = 0; self.last_accepted = 0
        self.clear_history()

    def average(self):
        return sum(self.history[:self.count]) / self.count if self.count else 0.0

    def handle_peak(self, peak):
        if not self.have_ref:
            self.have_ref = True; self.ref_beat = peak; return
        beat = 60.0 * FS / (peak - self.ref_beat)
        avg = self.average()
        accept = (MIN_BPM <= beat <= MAX_BPM) if self.count == 0 else abs(beat - avg) <= ACCEPT_RANGE * avg
        if accept:
            self.history[self.idx] = beat; self.idx = (self.idx + 1) % HISTORY
            if self.count < HISTORY: self.count += 1
            self.rejects = 0; self.ref_beat = peak
            self.have_accepted = True; self.last_accepted = peak
            return
        if self.count == 0 or beat < avg: self.ref_beat = peak
        self.rejects += 1
        if self.rejects >= MAX_REJECTS:
            self.clear_history(); self.ref_beat = peak

    def add_sample(self, ir):
        if ir < FINGER_THRESHOLD:
            if self.finger_on: self.reset()
            return
        if not self.finger_on:
            self.reset(); self.finger_on = True; self.dc = float(ir)
        self.n += 1
        if ir >= SATURATION_LEVEL or abs(ir - self.dc) > SPIKE_FRACTION * self.dc:
            self.bad_run += 1
            if self.bad_run >= MAX_BAD_SAMPLES: self.reset()
            return
        self.bad_run = 0
        self.dc += DC_ALPHA * (ir - self.dc)
        ac = self.dc - ir
        y = self.b0 * ac + self.b1 * self.x1 + self.b2 * self.x2 - self.a1 * self.y1 - self.a2 * self.y2
        self.x2, self.x1 = self.x1, ac
        self.y2, self.y1 = self.y1, y
        self.env *= ENV_DECAY
        if y > self.env: self.env = y
        if self.n > SETTLE_SAMPLES:
            is_peak = (self.prev1 > self.prev2 and self.prev1 >= y and self.prev1 > 0
                       and self.prev1 > PEAK_FRACTION * self.env)
            peak = self.n - 1
            if is_peak and (not self.have_peak or peak - self.last_peak >= MIN_PEAK_GAP):
                self.have_peak = True; self.last_peak = peak
                self.handle_peak(peak)
        self.prev2, self.prev1 = self.prev1, y

    def bpm(self):
        return self.average()

    def good_quality(self):
        return self.finger_on and self.have_accepted and (self.n - self.last_accepted) <= QUALITY_SAMPLES


def run_firmware(ir, motion=None):
    """ir: IR samples at 100 Hz, motion: True where the MPU6050 would see big motion.
    Returns a list of (seq, time_ms, bpm, quality) - one per second, like the serial output."""
    hr = HeartRate(); out = []
    last_motion = None; last_good = 0.0
    for i, v in enumerate(ir):
        t_ms = i * 10
        if motion and motion[i]: last_motion = t_ms
        hr.add_sample(v)
        if (i + 1) % 100 == 0:
            now = t_ms + 10
            bpm = hr.bpm(); good = hr.good_quality()
            moving = last_motion is not None and now - last_motion < MOTION_HOLD_MS
            if not moving:
                if good: last_good = bpm
            else:
                good = good and last_good > 0 and abs(bpm - last_good) <= MOTION_MATCH * last_good
            out.append((len(out), now, int(bpm + 0.5), 1 if good else 0))
    return out


# ---------------------------------------------------------------- simulated pulse signal
def pulse_shape(phase):
    """One heartbeat: main (systolic) wave + smaller dicrotic wave."""
    return math.exp(-((phase - 0.18) / 0.07) ** 2) + 0.35 * math.exp(-((phase - 0.45) / 0.09) ** 2)

def simulate_ir(bpm, seconds, noise=0.05, hrv=0.02, motion=None, seed=0, dc=120000, ac=0.01):
    """bpm: number or function of time (s). noise: fraction of the pulse size.
    hrv: random beat-to-beat change. motion: list of (start_s, end_s, freq_hz, size)."""
    rng = random.Random(seed)
    bpm_at = bpm if callable(bpm) else (lambda t: bpm)
    ir, phase, jitter = [], 0.0, 1.0
    for i in range(int(seconds * FS)):
        t = i / FS
        phase += bpm_at(t) / 60.0 * jitter / FS
        if phase >= 1.0:
            phase -= 1.0; jitter = 1 + rng.gauss(0, hrv)
        v = dc - dc * ac * pulse_shape(phase)              # blood pulse makes the IR dip
        v += rng.gauss(0, dc * ac * noise)
        for (t0, t1, f, size) in (motion or []):
            if t0 <= t < t1: v += dc * size * math.sin(2 * math.pi * f * t)
        ir.append(int(v))
    return ir

def motion_flags(seconds, motion):
    return [any(m[0] <= i / FS < m[1] for m in motion) for i in range(int(seconds * FS))]


# ---------------------------------------------------------------- test signals
# Same tests as on the real wearable.
SECONDS = 30
TESTS = [
    # title, BPM over time, seconds, signal options, big-motion windows (s)
    ("Normal heart beat (resting)",
     lambda t: 65 + 4 * math.sin(2 * math.pi * 0.1 * t), SECONDS, dict(hrv=0.03), []),
    ("After doing a few jumping jacks and push ups (light)",
     lambda t: 85 - 25 * min(t, 55) / 55, 60, dict(hrv=0.03), []),
    ("Spinning my arm",
     lambda t: 90, SECONDS, dict(motion=[(10, 30, 0.7, 0.02)]), [(10, 30)]),
]


def main():
    for title, bpm, secs, opts, moving in TESTS:
        ir = simulate_ir(bpm, secs, seed=1, **opts)
        out = run_firmware(ir, motion_flags(secs, moving) if moving else None)
        print(title)
        for seq, t, b, q in out:
            print(f"HR,{seq},{t},{b},{q}")
        true = [bpm(i / 10) for i in range(secs * 10)]
        print(f"-> true BPM: start {true[0]:.0f}, end {true[-1]:.0f}, range {min(true):.0f}-{max(true):.0f}"
              + (f", big arm motion from {moving[0][0]} s to {moving[0][1]} s" if moving else "") + "\n")


if __name__ == "__main__":
    main()