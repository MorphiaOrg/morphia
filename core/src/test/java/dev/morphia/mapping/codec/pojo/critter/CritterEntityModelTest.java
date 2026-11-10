package dev.morphia.mapping.codec.pojo.critter;

import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CritterEntityModelTest {
    @Test
    public void caseRulesDistinguishOnlyLanguagesThatConvertCaseDifferently() {
        // the built-in strategies lower-case "customerID" to "customerid", but to "customerıd" in Turkish
        Assertions.assertEquals("root", CritterEntityModel.caseRules(Locale.US));
        Assertions.assertEquals("root", CritterEntityModel.caseRules(Locale.GERMANY));
        Assertions.assertEquals("root", CritterEntityModel.caseRules(Locale.ROOT));
        Assertions.assertEquals("tr", CritterEntityModel.caseRules(Locale.forLanguageTag("tr-TR")));
        Assertions.assertEquals("az", CritterEntityModel.caseRules(Locale.forLanguageTag("az")));
        Assertions.assertEquals("lt", CritterEntityModel.caseRules(Locale.forLanguageTag("lt-LT")));
    }
}
