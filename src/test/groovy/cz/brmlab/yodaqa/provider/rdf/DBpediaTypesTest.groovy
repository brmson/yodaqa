package cz.brmlab.yodaqa.provider.rdf

import cz.brmlab.yodaqa.provider.rdf.*
import org.junit.Test;
 /**
 *
 */
class DBpediaTypesTest extends GroovyTestCase {

    @Test void testQueryTitleForm() {
        DBpediaTypes dbt = new DBpediaTypes();
        List<String> expectedResult = ["Mountain", "Mountain", "Mountain", "Mountain", "International Mountains Of Europe", "Mountain Peak", "Mountain Peaks Of The Sudetes", "Mountains And Hills Of The Czech Republic", "Mountains Of Poland"]

        List<String> resultForSnezka = dbt.query("Sněžka", null); //First search and compare without caching
        assertArrayEquals(expectedResult.toArray(), resultForSnezka.toArray());

        List<String> resultForSnezkaCached = dbt.query("Sněžka", null); //Cached version should be the same
        assertArrayEquals(expectedResult.toArray(), resultForSnezkaCached.toArray());
     }
}
