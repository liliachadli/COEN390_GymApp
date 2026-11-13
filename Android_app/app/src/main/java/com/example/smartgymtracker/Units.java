package com.example.smartgymtracker;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Weights are always stored in lbs. If the user picked kg in their profile,
// we only convert when we show a weight or when the user types one.
public class Units {

    private static final double KG_PER_LB = 0.45359237;

    public static boolean useKg = false;

    public static String label() {
        return useKg ? "kg" : "lbs";
    }

    // 220 lbs -> "100" (kg) or "220" (lbs). kg is rounded to 0.5 like gym plates.
    public static String number(int lbs) {
        return number(lbs, useKg);
    }

    public static String number(int lbs, boolean kgUnit) {
        if (!kgUnit) {
            return String.valueOf(lbs);
        }
        double kg = Math.round(lbs * KG_PER_LB * 2) / 2.0;
        if (kg == Math.floor(kg)) {
            return String.valueOf((int) kg);
        }
        return String.format(Locale.ENGLISH, "%.1f", kg);
    }

    // 220 -> "100 kg" or "220 lbs"
    public static String format(int lbs) {
        return number(lbs) + " " + label();
    }

    // Big totals (volume): no decimals, with a thousands separator
    public static String formatTotal(int lbs) {
        long value = useKg ? Math.round(lbs * KG_PER_LB) : lbs;
        return String.format(Locale.ENGLISH, "%,d %s", value, label());
    }

    // What the user typed (in their unit) -> lbs to store. "" or bad text -> 0
    public static int toLbs(String typed) {
        return toLbs(typed, useKg);
    }

    public static int toLbs(String typed, boolean kgUnit) {
        try {
            double value = Double.parseDouble(typed.trim().replace(',', '.'));
            return (int) Math.round(kgUnit ? value / KG_PER_LB : value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // "210 lbs Chest Press" -> "95 kg Chest Press" when the user uses kg
    public static String convertText(String text) {
        if (!useKg || text == null) {
            return text;
        }
        Matcher m = Pattern.compile("(\\d+) lbs").matcher(text);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(out, format(Integer.parseInt(m.group(1))));
        }
        m.appendTail(out);
        return out.toString();
    }
}
