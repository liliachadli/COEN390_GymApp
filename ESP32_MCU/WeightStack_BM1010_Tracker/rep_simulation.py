"""
Smart Gym Tracker - Weight stack (MB1010) rep-detection simulation (SIM-1.1)
"""
import random

SAMPLE_PERIOD_MS = 49      # MB1010 updates its output every 49 ms
INCH_CM = 2.54             # MB1010 resolution is 1 inch
IDLE, START, LIFT, LOWER = 0, 1, 2, 3


# ---------------------------------------------------------------- firmware logic (main.cpp)
def rep_algorithm(samples):
    """samples: list of (time_ms, distance_cm). Returns one dict per rep, like printdata()."""
    state = IDLE
    rep_count = lift_time = lower_time = valid = 0
    rom = 0.0
    start_time = top_time = end_time = last_time_moved = 0
    actual = samples[0][1]
    start_distance = actual
    top, bottom, ref = 0.0, float("inf"), 0.0
    is_moving = is_lowering = is_lifting = False
    samples_move = samples_lift = samples_lower = 0
    reps = []

    for now, dist in samples:
        prev, actual = actual, dist

        if prev - 1.5 <= actual <= prev + 1.5:          # still?
            if samples_move < 15: samples_move += 1
            elif samples_move == 15: is_moving = False
        else:
            samples_move = 0; is_moving = True; last_time_moved = now

        if actual < prev:                                 # going down?
            if samples_lower < 5: samples_lower += 1
            elif samples_lower == 5: is_lowering = True
        elif actual > prev:
            samples_lower = 0; is_lowering = False

        if actual > prev:                                 # going up?
            if samples_lift < 5: samples_lift += 1
            elif samples_lift == 5: is_lifting = True
        elif actual < prev:
            samples_lift = 0; is_lifting = False

        def finish_rep():
            nonlocal rom, lower_time, valid, rep_count
            rom = top - bottom
            lower_time = end_time - top_time
            valid = 0
            if ref > 0 and top / ref > 0.8:
                rep_count += 1; valid = 1
            reps.append(dict(timer=now, lift=lift_time, lower=lower_time,
                             count=rep_count, rom=round(rom, 2), valid=valid))

        if state == IDLE:
            if not is_moving and not is_lifting and not is_lowering:
                start_distance = actual
            start_time = now
            rep_count = 0
            if start_distance + 5.0 <= actual:
                state = START; start_distance = actual

        elif state == START:
            if not is_moving and not is_lifting and not is_lowering:
                state = IDLE
            if actual >= start_distance + 10.0:
                state = LIFT

        elif state == LIFT:
            if top < actual:
                top = actual; top_time = now
            if is_lowering:
                if rep_count < 3 and ref < top: ref = top
                if rep_count < 1: end_time = start_time
                lift_time = top_time - end_time
                state = LOWER
            elif not is_moving and not is_lifting and last_time_moved + 7000 <= now:
                state = IDLE
                top, bottom, ref, start_distance = 0.0, float("inf"), 0.0, 0.0
                rep_count = 0

        elif state == LOWER:
            if bottom > actual:
                end_time = now; bottom = actual
            if is_lifting:
                finish_rep()
                top, bottom = 0.0, float("inf")
                state = LIFT
            elif not is_moving and not is_lowering and last_time_moved + 5000 <= now:
                finish_rep()
                state = IDLE
                top, bottom, ref, start_distance = 0.0, float("inf"), 0.0, 0.0
                rep_count = 0
    return reps


# ---------------------------------------------------------------- simulated signal
def smooth(x):                               # smooth 0->1 movement
    x = min(max(x, 0.0), 1.0)
    return 10 * x**3 - 15 * x**4 + 6 * x**5

def make_set(n_reps, rom, lift_s, lower_s, pause_s=0.2, partial=(), partial_frac=0.6, seed=0):
    rng = random.Random(seed)
    reps = []
    for i in range(n_reps):
        r = rom * (partial_frac if i in partial else 1.0) * rng.uniform(0.96, 1.04)
        reps.append(dict(rom=r, lift=lift_s * rng.uniform(0.9, 1.1), lower=lower_s * rng.uniform(0.9, 1.1),
                         top=pause_s, bottom=pause_s))
    return reps

def simulate_sensor(reps, base_cm=25.0, noise_cm=0.3, dropout=0.0, outlier=0.0, seed=1):
    """Distance seen by the MB1010: 1-inch steps, small timing jitter, optional noise,
    dropouts (pulseIn timeout -> old value kept) and stray echoes."""
    rng = random.Random(seed)
    segs, t = [], 3.0                                     # 3 s rest before the set
    for r in reps:
        segs.append((t, r)); t += r["lift"] + r["top"] + r["lower"] + r["bottom"]
    total = t + 9.0                                       # 9 s rest after the set

    def true_distance(ts):
        for t0, r in segs:
            t1 = t0 + r["lift"]; t2 = t1 + r["top"]; t3 = t2 + r["lower"]
            if t0 <= ts < t1: return base_cm + r["rom"] * smooth((ts - t0) / r["lift"])
            if t1 <= ts < t2: return base_cm + r["rom"]
            if t2 <= ts < t3: return base_cm + r["rom"] * (1 - smooth((ts - t2) / r["lower"]))
        return base_cm

    out, last, now = [], None, 0
    while now <= total * 1000:
        inches = max(6, round((true_distance(now / 1000) + rng.gauss(0, noise_cm)) / INCH_CM))
        pw = inches * 147 + rng.randint(-10, 10)          # pulse width in us
        if rng.random() < outlier:
            pw = int(pw * rng.uniform(1.3, 1.8))
        cm = pw / 147.0 * 2.54
        if last is not None and rng.random() < dropout:
            cm = last
        out.append((now, cm)); last = cm
        now += SAMPLE_PERIOD_MS
    return out


# ---------------------------------------------------------------- output (same as printdata)
def print_like_firmware(reps):
    for i, r in enumerate(reps):
        prefix = "State: 0 | " if i == len(reps) - 1 else ""
        print(f"{prefix}Timer: {r['timer']} | Lift Time: {r['lift']} | Lower Time: {r['lower']} | "
              f"Rep count: {r['count']} | ROM: {r['rom']:.2f} | Rep Validity: {r['valid']}")


# ---------------------------------------------------------------- test sets
# Same tests as on the real tracker. Expected = (valid reps, invalid reps).
TESTS = [
    ("10 Normal Valid Reps",                        make_set(10, 100, 1.5, 1.5, seed=1),                  (10, 0)),
    ("6 Slow Reps",                                 make_set(6,  100, 3.5, 3.5, seed=2),                  (6, 0)),
    ("13 Fast Reps: 3 NonValid Reps + 10 Valid Reps", make_set(13, 100, 1.0, 0.8, partial=(3, 5, 9), seed=3), (10, 3)),
]


def main():
    for title, reps, (exp_valid, exp_invalid) in TESTS:
        out = rep_algorithm(simulate_sensor(reps))
        valid = sum(r["valid"] for r in out)
        print(title)
        print_like_firmware(out)
        print(f"-> expected {exp_valid} valid / {exp_invalid} invalid, "
              f"simulation gave {valid} valid / {len(out) - valid} invalid\n")


if __name__ == "__main__":
    main()