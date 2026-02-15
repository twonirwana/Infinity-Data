package de.twonirwana.infinity;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class JsonDiffTest {
    private static Stream<Arguments> testData() {
        return Stream.of(

                Arguments.of("""
                        {"name": "A1"}""", """
                        {"name": "A1", "ignore": "a"}""", List.of()),
                Arguments.of("""
                        {"name": "A1", "ignore": "a"}""", """
                        {"name": "A1", "ignore": "b"}""", List.of()),
                Arguments.of("""
                        {"name": "A1", "ignore": "a"}""", """
                        {"name": "A1"}""", List.of()),

                Arguments.of("""
                        {"name": "A1"}""", """
                        {"name": "A1", "value": [{"id": 1, "p1": "a" }]}""", List.of("value.1.id: ''->'1'", "value.1.p1: ''->'a'")),
                Arguments.of("""
                        {"name": "A1", "value": [{"id": 1, "p1": "a" }]}""", """
                        {"name": "A1", "value": [{"id": 1, "p1": "b" }]}""", List.of("value.1.p1: 'a'->'b'")),
                Arguments.of("""
                        {"name": "A1", "value": [{"id": 1, "p1": "a" }]}""", """
                        {"name": "A1", "value": [{"id": 2, "p1": "b" }]}""", List.of("value.1.id: '1'->''",
                        "value.1.p1: 'a'->''",
                        "value.2.id: ''->'2'",
                        "value.2.p1: ''->'b'")),
                Arguments.of("""
                        {"name": "A1", "value": [{"id": 1, "p1": "a" }]}""", """
                        {"name": "A1"}""", List.of("value.1.id: '1'->''", "value.1.p1: 'a'->''")),

                Arguments.of("""
                        {"name": "A1"}""", """
                        {"name": "A1", "value": [{"p1": "a" }]}""", List.of("value.0.p1: ''->'a'")),
                Arguments.of("""
                        {"name": "A1", "value": [{"p1": "a" }]}""", """
                        {"name": "A1", "value": [{"p1": "b" }]}""", List.of("value.0.p1: 'a'->'b'")),
                Arguments.of("""
                        {"name": "A1", "value": [{"p1": "a" }]}""", """
                        {"name": "A1"}""", List.of("value.0.p1: 'a'->''")),

                Arguments.of("""
                        {"name": "A1"}""", """
                        {"name": "A1", "value": {"p1": "a" }}""", List.of("value.p1: ''->'a'")),
                Arguments.of("""
                        {"name": "A1", "value": {"p1": "a" }}""", """
                        {"name": "A1", "value": {"p2": "b" }}""", List.of("value.p1: 'a'->''", "value.p2: ''->'b'")),
                Arguments.of("""
                        {"name": "A1", "value": {"p1": "a" }}""", """
                        {"name": "A1", "value": {"p1": "b" }}""", List.of("value.p1: 'a'->'b'")),
                Arguments.of("""
                        {"name": "A1", "value": {"p1": "a" }}""", """
                        {"name": "A1"}""", List.of("value.p1: 'a'->''")),

                Arguments.of("""
                        {"name": "A1", "value": ["a","c"]}""", """
                        {"name": "A1", "value": ["a","b","c"]}""", List.of("value.b_0: ''->'b'")),
                Arguments.of("""
                        {"name": "A1", "value": ["a","b","c"]}""", """
                        {"name": "A1", "value": ["a","b","b","c"]}""", List.of("value.b_1: ''->'b'")),
                Arguments.of("""
                        {"name": "A1", "value": ["a","b","c"]}""", """
                        {"name": "A1", "value": ["a","c"]}""", List.of("value.b_0: 'b'->''")),
                Arguments.of("""
                        {"name": "A1", "value": ["a","b","b","c"]}""", """
                        {"name": "A1", "value": ["a","b","c"]}""", List.of("value.b_1: 'b'->''")),
                Arguments.of("""
                        {"name": "A1", "value": ["a","b","c"]}""", """
                        {"name": "A1", "value": ["a","d","c"]}""", List.of("value.b_0: 'b'->''", "value.d_0: ''->'d'")),

                Arguments.of("""
                        {"name": "A1"}""", """
                        {"name": "A1", "value": "a"}""", List.of("value: ''->'a'")),
                Arguments.of("""
                        {"name": "A1", "value": "a"}""", """
                        {"name": "A1", "value": "b"}""", List.of("value: 'a'->'b'")),
                Arguments.of("""
                        {"name": "A1", "value": "a"}""", """
                        {"name": "A1"}""", List.of("value: 'a'->''")),

                Arguments.of("""
                        {"name": "A1"}""", """
                        {"name": "A1", "value": true}""", List.of("value: ''->'true'")),
                Arguments.of("""
                        {"name": "A1", "value": true}""", """
                        {"name": "A1", "value": false}""", List.of("value: 'true'->'false'")),
                Arguments.of("""
                        {"name": "A1", "value": true}""", """
                        {"name": "A1"}""", List.of("value: 'true'->''")),

                Arguments.of("""
                        {"name": "A1"}""", """
                        {"name": "A1", "value": 1}""", List.of("value: ''->'1'")),
                Arguments.of("""
                        {"name": "A1", "value": 1}""", """
                        {"name": "A1", "value": 2}""", List.of("value: '1'->'2'")),
                Arguments.of("""
                        {"name": "A1", "value": 1}""", """
                        {"name": "A1"}""", List.of("value: '1'->''"))

        );
    }

    @ParameterizedTest
    @MethodSource("testData")
    void testJsonDiff(String jsonLeft, String jsonRight, List<String> diff) {
        assertThat(JsonDiff.getDiffs(jsonLeft, jsonRight, List.of("ignore")).stream().map(JsonDiff.Diff::toString)).isEqualTo(diff);
    }
}