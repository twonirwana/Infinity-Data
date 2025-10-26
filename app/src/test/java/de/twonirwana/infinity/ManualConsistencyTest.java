package de.twonirwana.infinity;

import de.twonirwana.infinity.unit.api.Trooper;
import de.twonirwana.infinity.unit.api.TrooperProfile;
import de.twonirwana.infinity.unit.api.UnitOption;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
public class ManualConsistencyTest {
    static Database db;

    @BeforeAll
    static void setUp() {
        db = new DatabaseImp();
    }

    @Test
    void testSectorials() {
        List<Sectorial> res = db.getAllSectorials();

        assertThat(res.stream().map(Sectorial::getId)).containsExactly(101, 102, 103, 104, 105, 106, 107, 199, 201, 202, 204, 205, 299, 301, 302, 303, 304, 305, 306, 399, 401, 402, 403, 404, 499, 501, 502, 503, 504, 599, 601, 602, 603, 604, 605, 699, 701, 702, 703, 799, 801, 899, 902, 904, 905, 908, 909, 998, 999, 1001, 1002, 1003, 1099, 1101, 1102, 1103, 1199);
        assertThat(res.stream().map(Sectorial::getSlug)).containsExactly("panoceania",
                "shock-army-of-acontecimento",
                "military-orders",
                "neoterran-capitaline-army",
                "varuna-immediate-reaction-division",
                "svalarheima-s-winter-force",
                "kestrel-colonial-force",
                "code-capital",
                "yu-jing",
                "imperial-service",
                "invincible-army",
                "white-banner",
                "daebak-force",
                "ariadna",
                "caledonian-highlander-army",
                "force-de-reponse-rapide-merovingienne",
                "usariadna",
                "tartary",
                "kosmoflot",
                "l-equipe-argent",
                "haqqislam",
                "hassassin-bahram",
                "qapu-khalqi",
                "ramah-taskforce",
                "melek-reaction-group",
                "nomads",
                "corregidor",
                "bakunin",
                "tunguska",
                "vipera-pursuit-force",
                "combined-army",
                "morat",
                "shasvastii",
                "onyx",
                "next-wave",
                "the-exrah-comissariat",
                "aleph",
                "steel-phalanx",
                "operations",
                "ank-program",
                "tohaa",
                "deras-kaar",
                "druze",
                "ikari",
                "starco-free-company-of-the-star",
                "dahshat",
                "white-company",
                "contracted-back-up",
                "contracted-back-up",
                "o-12",
                "starmada",
                "torchlight-brigade",
                "teams-gladius",
                "jsa",
                "shindenbutai",
                "oban",
                "hayabusa");
    }

    @Test
    void uniqueIds() {
        Map<String, List<UnitOption>> res = db.getAllUnitOptions().stream()
                .collect(Collectors.groupingBy(UnitOption::getCombinedId));

        assertThat(res.entrySet().stream()).noneMatch(e -> e.getValue().size() > 1);
    }

    @Test
    void unitCount() {
        List<UnitOption> res = db.getAllUnitOptions();
        List<Trooper> troopers = res.stream().flatMap(o -> o.getAllTrooper().stream()).toList();
        List<TrooperProfile> trooperProfiles = troopers.stream().flatMap(t -> t.getProfiles().stream()).toList();
        long unitIdCount = res.stream().map(UnitOption::getUnitId).distinct().count();
        long unitOptionCount = res.stream().map(u -> u.getUnitId() + "-" + u.getOptionId()).distinct().count();

        SoftAssertions.assertSoftly(a -> {
            a.assertThat(res.size()).isEqualTo(11192);
            a.assertThat(troopers.size()).isEqualTo(12155);
            a.assertThat(trooperProfiles.size()).isEqualTo(12716);
            a.assertThat(unitIdCount).isEqualTo(877L);
            a.assertThat(unitOptionCount).isEqualTo(3091L);
        });
    }
}
