package de.twonirwana.infinity.armylist;


import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ArmyCodeLoaderTest {

    private static Stream<Arguments> testData() {
        return Stream.of(
                Arguments.of("gfUGbm9tYWRzASCBLAEBAQACAIbiAQEAAIGeAQEA", "ArmyCodeData[sectorialId=501, sectorialName=nomads, armyName= , maxPoints=300, combatGroups={1=[1762-1-1, 414-1-1]}]"),
                Arguments.of("glsKc2hhc3Zhc3RpaQdTaGFzIHYzgSwCAQEACgCCFQEEAACB9gEEAACCEAEDAACB9QEJAACB%2FQEBAACCFAEBAACCFAEBAACB9wEDAACCEAEEAACCDAEBAAIBAAYAgf8BAQAAhQoBAwAAhQoBCAAAhQoBBgAAhRABAgAAggEBhyUA", "ArmyCodeData[sectorialId=603, sectorialName=shasvastii, armyName=Shas v3, maxPoints=300, combatGroups={1=[533-1-4, 502-1-4, 528-1-3, 501-1-9, 509-1-1, 532-1-1, 532-1-1, 503-1-3, 528-1-4, 524-1-1], 2=[511-1-1, 1290-1-3, 1290-1-8, 1290-1-6, 1296-1-2, 513-1-1829]}]"),
                Arguments.of("gfcHYmFrdW5pbg9Kb05pUm8tRGljaG9yZCGBLAIBAAgBhC8BAQACga8BAwADga8BBQAEga0BAgAFgaABBgAGhkMBAQAHgZ4BAQAIgZ4BCQACAAcBgZkBAQAChtUBAQADgZoBAQAEgZsBAgAFgZEBBgAGgZEBAQAHhkQBBAA", "ArmyCodeData[sectorialId=503, sectorialName=bakunin, armyName=JoNiRo-Dichord!, maxPoints=300, combatGroups={1=[1071-1-1, 431-1-3, 431-1-5, 429-1-2, 416-1-6, 1603-1-1, 414-1-1, 414-1-9], 2=[409-1-1, 1749-1-1, 410-1-1, 411-1-2, 401-1-6, 401-1-1, 1604-1-4]}]"),
                Arguments.of("gloFbW9yYXQhIE1vcmF0cyAzMDBwdCAtIFNpbmdsZSBTb2dlcmF0IHYygSwCAQEACgCCCgEFAACCBgEDAACGJAEBAACCEwEBAACB%2BQEBAACB%2BQEBAACB%2BQEBAACB%2FQECAACB%2BAEFAACCBwECAAIBAAUAgfEBBwAAgfEBCwAAhicBBAAAghQBAQAAgfgBBQA%3D", "ArmyCodeData[sectorialId=602, sectorialName=morat, armyName= Morats 300pt - Single Sogerat v2, maxPoints=300, combatGroups={1=[522-1-5, 518-1-3, 1572-1-1, 531-1-1, 505-1-1, 505-1-1, 505-1-1, 509-1-2, 504-1-5, 519-1-2], 2=[497-1-7, 497-1-11, 1575-1-4, 532-1-1, 504-1-5]}]"),
                Arguments.of("aSJ2YXJ1bmEtaW1tZWRpYXRlLXJlYWN0aW9uLWRpdmlzaW9uD0FzeW1tZXRyaWMgRmlzaIEsAgEACgGEwAECAAKEwAEGAAOEwAEDAASEvgECAAUBAQEABgEBCgAHhMYBBAAIhMYBAwAJhMkBAQAKhMYBCAACAAUBMAEGAAIwAQMAAzABAgAEMAEEAAUTAQEA", "ArmyCodeData[sectorialId=105, sectorialName=varuna-immediate-reaction-division, armyName=Asymmetric Fish, maxPoints=300, combatGroups={1=[1216-1-2, 1216-1-6, 1216-1-3, 1214-1-2, 1-1-1, 1-1-10, 1222-1-4, 1222-1-3, 1225-1-1, 1222-1-8], 2=[48-1-6, 48-1-3, 48-1-2, 48-1-4, 19-1-1]}]"),
                Arguments.of("gZEJaGFxcWlzbGFtAIEsAgEACQGBUAECAAKBRAEPAAOBRAEMAASDUQEBAAWBNgEDAAaGHAEDAAeBUQECAAiBTAECAAmBegEDAAIABgGBOAEEAAKDqwEBAAOBVAEFAASBVAEFAAWBQwEGAAaBQwEGAA%3D%3D", "ArmyCodeData[sectorialId=401, sectorialName=haqqislam, armyName=null, maxPoints=300, combatGroups={1=[336-1-2, 324-1-15, 324-1-12, 849-1-1, 310-1-3, 1564-1-3, 337-1-2, 332-1-2, 378-1-3], 2=[312-1-4, 939-1-1, 340-1-5, 340-1-5, 323-1-6, 323-1-6]}]"),
                Arguments.of("gfcHYmFrdW5pbg9Kb05pUm8tRGljaG9yZCGBLAIBAAgBhC8BAQACga8BAwADga8BBQAEga0BAgAFgaABBgAGhkMBAQAHgZ4BAQAIgZ4BCQACAAcBgZkBAQAChtUBAQADgZoBAQAEgZsBAgAFgZEBBgAGgZEBAQAHhkQBBAA%3D", "ArmyCodeData[sectorialId=503, sectorialName=bakunin, armyName=JoNiRo-Dichord!, maxPoints=300, combatGroups={1=[1071-1-1, 431-1-3, 431-1-5, 429-1-2, 416-1-6, 1603-1-1, 414-1-1, 414-1-9], 2=[409-1-1, 1749-1-1, 410-1-1, 411-1-2, 401-1-6, 401-1-1, 1604-1-4]}]"),
                Arguments.of("gfcHYmFrdW5pbhdKb05pUm8tQXJvSE1HLURpc2Nob3JkIYEsAgEACAGELwEBAAKBrwEDAAOBrwEFAASBrQECAAWBoAEGAAaGQwEBAAeBngEBAAiBngEJAAIABwGBmQEBAAKG1QEBAAOBmgEBAASBmwECAAWBkQEGAAaBkQEBAAeGRAEEAA%3D%3D", "ArmyCodeData[sectorialId=503, sectorialName=bakunin, armyName=JoNiRo-AroHMG-Dischord!, maxPoints=300, combatGroups={1=[1071-1-1, 431-1-3, 431-1-5, 429-1-2, 416-1-6, 1603-1-1, 414-1-1, 414-1-9], 2=[409-1-1, 1749-1-1, 410-1-1, 411-1-2, 401-1-6, 401-1-1, 1604-1-4]}]"),
                Arguments.of("hE4Mc2hpbmRlbmJ1dGFpFlNoaW5kZW5idXRhaSBTTlQgNy8xMCCBLAIBAAYBhxsBAwAChxsBBQADhyABAgAEhxkBBwAFgKcBAwAGhyUBAQACAAYBhxoBAgAChx8BAQADhx0BBAAEhx4BAQAFhyABAgAGgJ8BAgA%3D", "ArmyCodeData[sectorialId=1102, sectorialName=shindenbutai, armyName=Shindenbutai SNT 7/10 , maxPoints=300, combatGroups={1=[1819-1-3, 1819-1-5, 1824-1-2, 1817-1-7, 167-1-3, 1829-1-1], 2=[1818-1-2, 1823-1-1, 1821-1-4, 1822-1-1, 1824-1-2, 159-1-2]}]"),
                Arguments.of("gS4aY2FsZWRvbmlhbi1oaWdobGFuZGVyLWFybXkKU2NvdHMgQ29yZYEsAgEACgGA%2BwEEAAKBBQEDAAOBBQELAASBBQEKAAWBBQEEAAaBBwECAAeBAQECAAiBAQEDAAmA9QEHAAqBGwEBAAIABQGG1QEBAAKA%2FgEBAAOA%2BAEDAASGIgEDAAWA7wECAA%3D%3D", "ArmyCodeData[sectorialId=302, sectorialName=caledonian-highlander-army, armyName=Scots Core, maxPoints=300, combatGroups={1=[251-1-4, 261-1-3, 261-1-11, 261-1-10, 261-1-4, 263-1-2, 257-1-2, 257-1-3, 245-1-7, 283-1-1], 2=[1749-1-1, 254-1-1, 248-1-3, 1570-1-3, 239-1-2]}]"),
                Arguments.of("g%2BoIc3Rhcm1hZGEAgSwCAQAKAYWnAQUAAoWnAQUAA4WnAQYABIWnAQcABYWnAQMABoW9AQEAB4W9AQEACIW7AQEACYW4AQIACoW4AQIAAgAZAYdQAQEAAoZKAQIAA4ZKAQIABIXnAQUABYXnAQQABoWwAQQAB4XhAQEACIW1AQQACYW1AQQACoZJAQMAC4X8AQMADIX8AQYADYX%2FAQIADoWuAQIAD4XoAQQAEIMUAQYAEYbtAQMAEobtAQEAE4W3AQEAFIW3AQQAFYY0AQEAFoY0AQEAF4X7AQMAGIW2AQEAGYWyAQIA", "ArmyCodeData[sectorialId=1002, sectorialName=starmada, armyName=null, maxPoints=300, combatGroups={1=[1447-1-5, 1447-1-5, 1447-1-6, 1447-1-7, 1447-1-3, 1469-1-1, 1469-1-1, 1467-1-1, 1464-1-2, 1464-1-2], 2=[1872-1-1, 1610-1-2, 1610-1-2, 1511-1-5, 1511-1-4, 1456-1-4, 1505-1-1, 1461-1-4, 1461-1-4, 1609-1-3, 1532-1-3, 1532-1-6, 1535-1-2, 1454-1-2, 1512-1-4, 788-1-6, 1773-1-3, 1773-1-1, 1463-1-1, 1463-1-4, 1588-1-1, 1588-1-1, 1531-1-3, 1462-1-1, 1458-1-2]}]"),
                Arguments.of("g%2BoIc3Rhcm1hZGEKRnVyb3JlIDIuMIEsAgEACgGFsgEDAAKFsAEFAAOFtwEBAASFrgECAAWFuwEBAAaFvQEBAAeFtQEEAAiFtQEEAAmGSQEFAAqFuAECAAIABQGGSQEGAAKHUgEBAAMyAQIABIZKAQIABYZKAQIA", "ArmyCodeData[sectorialId=1002, sectorialName=starmada, armyName=Furore 2.0, maxPoints=300, combatGroups={1=[1458-1-3, 1456-1-5, 1463-1-1, 1454-1-2, 1467-1-1, 1469-1-1, 1461-1-4, 1461-1-4, 1609-1-5, 1464-1-2], 2=[1609-1-6, 1874-1-1, 50-1-2, 1610-1-2, 1610-1-2]}]"),
                Arguments.of("gZIQaGFzc2Fzc2luLWJhaHJhbQCBLAIBAAoBgVEBBAACgVEBAgADgS0BDgAEgT0BAQAFgT4BAQAGgUwBAQAHgwsBAQAIgwsBAQAJgTgBBAAKgTgBBAACAAUBgU4BAQACgVQBBQADgVQBBQAEhgsBAwAFhgsBAwA%3D", "ArmyCodeData[sectorialId=402, sectorialName=hassassin-bahram, armyName=null, maxPoints=300, combatGroups={1=[337-1-4, 337-1-2, 301-1-14, 317-1-1, 318-1-1, 332-1-1, 779-1-1, 779-1-1, 312-1-4, 312-1-4], 2=[334-1-1, 340-1-5, 340-1-5, 1547-1-3, 1547-1-3]}]"),
                Arguments.of("gr4Nc3RlZWwtcGhhbGFueAkgMjAwIG15cm2AyAEBAQAIAIJdAQEAAILRAQMAAIJMAQMAAIJMAQIAAIJMAQQAAIJZAQMAAIJjAYhhAACCUwEBAA%3D%3D", "ArmyCodeData[sectorialId=702, sectorialName=steel-phalanx, armyName= 200 myrm, maxPoints=200, combatGroups={1=[605-1-1, 721-1-3, 588-1-3, 588-1-2, 588-1-4, 601-1-3, 611-1-2145, 595-1-1]}]"),
                Arguments.of("gr4Nc3RlZWwtcGhhbGFueAkgMjAwIG15cm2AyAEBAQAIAIJdAQEAAILRAQMAAIJMAQMAAIJMAQIAAIJMAQQAAIJZAQMAAIJjAYhhAACCUwEBAA", "ArmyCodeData[sectorialId=702, sectorialName=steel-phalanx, armyName= 200 myrm, maxPoints=200, combatGroups={1=[605-1-1, 721-1-3, 588-1-3, 588-1-2, 588-1-4, 601-1-3, 611-1-2145, 595-1-1]}]"),
                //with reinforcement
                Arguments.of("gloFbW9yYXQBIIEsBAEBAAMAhiQBAQAAggYBBwAAgewBCwACAQABAIYoAQIAAwEBAwCGTwEBAACGTgEBAACGTgEBAAQBAQIAhk0BAQAAhk0BAQA%3D", "ArmyCodeData[sectorialId=602, sectorialName=morat, armyName= , maxPoints=300, combatGroups={1=[1572-1-1, 518-1-7, 492-1-11], 2=[1576-1-2], 3=[1615-1-1, 1614-1-1, 1614-1-1], 4=[1613-1-1, 1613-1-1]}]"),
                Arguments.of("gloFbW9yYXQBIIEsAgEBAQEAhkwBAgACAQEBAIZQAQEA", "ArmyCodeData[sectorialId=602, sectorialName=morat, armyName= , maxPoints=300, combatGroups={1=[1612-1-2], 2=[1616-1-1]}]"),
                Arguments.of("gloFbW9yYXQBIIEsBAEBAAEAhiQBAQACAQABAIYoAQIAAwEBBACGTwEBAACGTwECAACGTwEDAACGTwEEAAQBAQQAhk0BAQAAhk0BAgAAhk0BAwAAhk0BBAA%3D", "ArmyCodeData[sectorialId=602, sectorialName=morat, armyName= , maxPoints=300, combatGroups={1=[1572-1-1], 2=[1576-1-2], 3=[1615-1-1, 1615-1-2, 1615-1-3, 1615-1-4], 4=[1613-1-1, 1613-1-2, 1613-1-3, 1613-1-4]}]"),
                Arguments.of("gloFbW9yYXQBIIEsBAEBAAEAhiQBAQACAQABAIYoAQIAAwEBAQCGTwEBAAQBAQEAhk4BAQA%3D", "ArmyCodeData[sectorialId=602, sectorialName=morat, armyName= , maxPoints=300, combatGroups={1=[1572-1-1], 2=[1576-1-2], 3=[1615-1-1], 4=[1614-1-1]}]")
        );
    }

    @ParameterizedTest
    @MethodSource("testData")
    void testArmyCodeGeneration(String armyCode, String expectedArmyData) {
        // System.out.printf("   Arguments.of(\"%s\", \"%s\"),%n", armyCode, ArmyCodeLoader.mapArmyCode(armyCode).toString());
        assertThat(ArmyCodeLoader.mapArmyCode(armyCode).toString()).isEqualTo(expectedArmyData);
    }


}