package de.twonirwana.infinity;

import de.twonirwana.infinity.unit.api.ExtraValue;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PrintCardTest {

    @Test
    void toPsExtra_matches() {
        Optional<String> res = PrintCard.toPsExtra(new ExtraValue(1, "PS=4", ExtraValue.Type.Text, null));
        assertThat(res).contains("4");
    }

    @Test
    void toPsExtra_noMatches() {
        Optional<String> res = PrintCard.toPsExtra(new ExtraValue(1, "+5", ExtraValue.Type.Text, null));
        assertThat(res).isEmpty();
    }

    @Test
    void toBurstExtra_matches() {
        Optional<String> res = PrintCard.toBurstExtra(new ExtraValue(1, "+2B", ExtraValue.Type.Text, null));
        assertThat(res).contains("2");
    }

    @Test
    void toBurstExtra_noMatches() {
        Optional<String> res = PrintCard.toBurstExtra(new ExtraValue(1, "+5", ExtraValue.Type.Text, null));
        assertThat(res).isEmpty();
    }
}