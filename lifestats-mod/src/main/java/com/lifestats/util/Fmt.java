package com.lifestats.util;

import java.util.ArrayList;
import java.util.List;

public final class Fmt {
    public static final int SET = 64;
    public static final int SHULKER = SET * 27; // 1설커 = 27세트 = 1728개

    private Fmt() {
    }

    /** 1086만 9990원 형식 */
    public static String won(long n) {
        if (n <= 0) return "0원";
        long eok = n / 100_000_000L;
        long man = (n % 100_000_000L) / 10_000L;
        long rest = n % 10_000L;
        StringBuilder sb = new StringBuilder();
        if (eok > 0) sb.append(eok).append("억 ");
        if (man > 0) sb.append(man).append("만 ");
        if (rest > 0) sb.append(rest).append("원");
        else sb.append("원");
        return sb.toString();
    }

    /** 3설커 18세트 36개 형식 */
    public static String count(long n) {
        if (n <= 0) return "0개";
        long sk = n / SHULKER;
        long rem = n % SHULKER;
        long set = rem / SET;
        long ea = rem % SET;
        List<String> parts = new ArrayList<>();
        if (sk > 0) parts.add(sk + "설커");
        if (set > 0) parts.add(set + "세트");
        if (ea > 0) parts.add(ea + "개");
        return String.join(" ", parts);
    }

    /** 2시간 18분 형식 */
    public static String duration(long seconds) {
        if (seconds < 60) return seconds + "초";
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        if (h > 0) return h + "시간 " + m + "분";
        return m + "분";
    }

    /** 35:26 형식 */
    public static String mmss(long seconds) {
        if (seconds < 0) seconds = 0;
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    /** 94.58% 형식 (0~1 입력) */
    public static String percent(float progress) {
        return String.format("%.2f%%", progress * 100f);
    }

    /** 정수면 소수점 없이, 아니면 소수 둘째 자리까지 */
    public static String plain(double v) {
        if (v == Math.rint(v)) return Long.toString((long) v);
        String s = String.format("%.2f", v);
        if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }

    public static String unitLabel(int unit) {
        if (unit >= SHULKER) return "1설커";
        if (unit >= SET) return "1세트";
        return "1개";
    }

    public static int nextUnit(int unit) {
        if (unit >= SHULKER) return 1;
        if (unit >= SET) return SHULKER;
        return SET;
    }
}
