package com.studyforge.backend.progress;

/**
 * Totalurile unei saptamani (toate materiile).
 * score = procentul de raspunsuri corecte, rotunjit ca in frontend ({@code Math.round(ok / n * 100)}),
 * sau null daca nu exista raspunsuri.
 */
public record WeekStats(int correct, int wrong, int answered, Integer score, long studySeconds) {

    public static final WeekStats EMPTY = of(0, 0, 0);

    public static WeekStats of(long correct, long wrong, long studySeconds) {
        int c = (int) Math.min(correct, Integer.MAX_VALUE);
        int w = (int) Math.min(wrong, Integer.MAX_VALUE);
        return new WeekStats(c, w, c + w, percent(c, w), studySeconds);
    }

    public static Integer percent(long correct, long wrong) {
        long answered = correct + wrong;
        if (answered <= 0) {
            return null;
        }
        return (int) Math.round(((double) correct / answered) * 100);
    }
}
