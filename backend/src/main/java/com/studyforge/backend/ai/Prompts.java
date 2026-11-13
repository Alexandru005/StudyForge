package com.studyforge.backend.ai;

import java.time.LocalDate;
import java.util.List;

import com.studyforge.backend.common.Dates;
import com.studyforge.backend.plan.PlanContent;

/**
 * Prompt-urile trimise la Gemini. Totul in romana, ca raspunsurile sa fie in romana.
 */
public final class Prompts {

    public static final String SYSTEM = """
            Ești un asistent pedagogic pentru studenți și elevi din România.
            Răspunzi exclusiv în limba română, cu diacritice corecte (ă, â, î, ș, ț).
            Folosești doar informațiile din materialele primite și cunoștințe standard despre subiectele lor;
            nu inventezi capitole, evaluări sau date care nu apar în material.
            Răspunzi DOAR cu JSON valid, conform schemei cerute, fără text în afara JSON-ului.
            """;

    private Prompts() {
    }

    public static String plan(String syllabusText, LocalDate today, LocalDate startMonday, int maxWeeks) {
        return """
                Data de azi: %s.
                Planul începe în săptămâna de luni %s.

                Mai jos este textul extras dintr-un PDF care conține programa unei materii și informații despre
                evaluări (teste, colocvii, laboratoare, proiecte, examene): când au loc și ce materie intră.

                Sarcina ta:
                1. Identifică materia, toate temele/capitolele și toate evaluările, cu datele lor și materia care intră.
                2. Construiește un plan de învățare pe săptămâni (luni–duminică), începând cu săptămâna de luni %s
                   și continuând până la săptămâna ultimei evaluări, inclusiv.
                   Dacă documentul nu conține date pentru evaluări, fă un plan de 14 săptămâni.
                   Maximum %d săptămâni. weekNumber începe de la 1 și crește din 1 în 1, fără goluri.
                3. Ordonează temele astfel încât materia fiecărei evaluări să fie parcursă complet înainte de evaluare,
                   iar în săptămâna dinaintea evaluării (sau în aceeași săptămână, dacă nu e timp) să existe recapitulare.
                4. Pentru fiecare săptămână:
                   - focus: obiectivul săptămânii, o propoziție;
                   - topics: temele concrete de studiat (folosește denumirile din programă);
                   - tasks: 2–5 activități concrete (ex. „Rezolvă 5 probleme despre…”, „Fă o fișă cu formulele pentru…”);
                   - assessments: numele evaluărilor care au loc în acea săptămână (listă goală dacă nu e niciuna).
                5. assessments (la nivel de plan): toate evaluările găsite, cu data în format YYYY-MM-DD
                   (sau "" dacă nu e cunoscută) și temele care intră (coverage).
                   Dacă anul nu e specificat, alege anul care face ca data să cadă cel mai aproape după data de azi.
                6. tips: 3–5 sfaturi scurte, specifice materiei.
                7. Dacă unele evaluări au avut deja loc înainte de %s, listează-le în assessments,
                   dar nu le planifica.

                === TEXTUL PDF-ULUI ===
                %s
                === SFÂRȘITUL TEXTULUI ===
                """.formatted(today, startMonday, startMonday, maxWeeks, startMonday, syllabusText);
    }

    /** Flashcard-uri pornind de la structura salvata a planului (cazul normal). */
    public static String flashcardsFromPlanWeek(PlanContent plan, PlanContent.Week week, int count) {
        LocalDate start = Dates.parse(week.startDate());
        LocalDate end = Dates.parse(week.endDate());
        StringBuilder relatedAssessments = new StringBuilder();
        if (plan.assessments() != null && week.assessments() != null) {
            for (PlanContent.Assessment a : plan.assessments()) {
                if (a.name() != null && week.assessments().stream().anyMatch(n -> n.equalsIgnoreCase(a.name()))) {
                    relatedAssessments.append("- ").append(a.name());
                    if (a.coverage() != null && !a.coverage().isEmpty()) {
                        relatedAssessments.append(": ").append(String.join("; ", a.coverage()));
                    }
                    relatedAssessments.append('\n');
                }
            }
        }
        return """
                Materia: %s
                Săptămâna %d din planul de învățare (%s).
                Obiectivul săptămânii: %s
                Teme:
                %s
                Activități planificate:
                %s
                Evaluări în această săptămână:
                %s

                Generează exact %d flashcard-uri (întrebare + răspuns) care acoperă temele acestei săptămâni.
                Dacă săptămâna este de recapitulare pentru o evaluare, acoperă materia evaluării respective.
                Reguli:
                - întrebări clare, de sine stătătoare (să poată fi înțelese fără context);
                - amestecă tipuri: definiții, înțelegere conceptuală, comparații, aplicații/exemple scurte;
                - răspunsuri corecte și concise (1–4 propoziții), fără numerotare la început;
                - fără întrebări duplicate sau aproape identice;
                - weekLabel: „Săptămâna %d – <tema principală pe scurt>”.
                """.formatted(
                orDefault(plan.subject(), "necunoscută"),
                week.weekNumber(),
                Dates.range(start, end),
                orDefault(week.focus(), "-"),
                bulletList(week.topics()),
                bulletList(week.tasks()),
                relatedAssessments.isEmpty() ? "- niciuna" : relatedAssessments.toString().trim(),
                count,
                week.weekNumber());
    }

    /** Flashcard-uri direct din textul unui PDF de plan care nu e in baza de date. */
    public static String flashcardsFromPlanText(String planText, int weekNumber, int count) {
        return """
                Mai jos este textul unui plan de învățare împărțit pe săptămâni.
                Găsește secțiunea pentru Săptămâna %d și generează exact %d flashcard-uri (întrebare + răspuns)
                care acoperă temele acelei săptămâni.
                Reguli:
                - întrebări clare, de sine stătătoare;
                - amestecă tipuri: definiții, înțelegere conceptuală, comparații, aplicații/exemple scurte;
                - răspunsuri corecte și concise (1–4 propoziții), fără numerotare la început;
                - fără întrebări duplicate;
                - weekLabel: „Săptămâna %d – <tema principală pe scurt>”.
                Dacă planul NU conține Săptămâna %d, întoarce flashcards ca listă goală și weekLabel "".

                === PLANUL ===
                %s
                === SFÂRȘITUL PLANULUI ===
                """.formatted(weekNumber, count, weekNumber, weekNumber, planText);
    }

    private static String bulletList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "- (nespecificat)";
        }
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            sb.append("- ").append(item).append('\n');
        }
        return sb.toString().trim();
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
