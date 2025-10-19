package de.twonirwana.infinity;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
class ExportArmyCodeTest {

    @Test
    void testShas() {
        ExportArmyCode.main(new String[]{
                "glsKc2hhc3Zhc3RpaQdTaGFzIHYzgSwCAQEACgCCFQEEAACB9gEEAACCEAEDAACB9QEJAACB%2FQEBAACCFAEBAACCFAEBAACB9wEDAACCEAEEAACCDAEBAAIBAAYAgf8BAQAAhQoBAwAAhQoBCAAAhQoBBgAAhRABAgAAggEBhyUA",
                "false"});
    }

    @Test
    void testMorat() {
        ExportArmyCode.main(new String[]{"gloFbW9yYXQhIE1vcmF0cyAzMDBwdCAtIFNpbmdsZSBTb2dlcmF0IHYygSwCAQEACgCCCgEFAACCBgEDAACGJAEBAACCEwEBAACB%2BQEBAACB%2BQEBAACB%2BQEBAACB%2FQECAACB%2BAEFAACCBwECAAIBAAUAgfEBBwAAgfEBCwAAhicBBAAAghQBAQAAgfgBBQA%3D"});
    }
}