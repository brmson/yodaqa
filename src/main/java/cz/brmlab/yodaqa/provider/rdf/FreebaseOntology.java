package cz.brmlab.yodaqa.provider.rdf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Literal;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;

/** A wrapper around Freebase dataset that maps concepts to curated
 * entity properties, at least those that seem generally useful.
 * This can then serve as an information source. */

public class FreebaseOntology extends FreebaseLookup {
	private static final Log logger =
		LogFactory.getLog(FreebaseOntology.class);

	/** Maximum number of topics to take queries from. */
	public static final int TOPIC_LIMIT = 5;
	/** Maximum number of properties per topic to return. */
	public static final int PROP_LIMIT = 40;

	/** Specific blacklist of Freebase relations. These properties may
	 * sometimes carry useful information but they flush out all other
	 * results, so we better avoid them completely.
	 *
	 * Our rule is to take a run on the test set and blacklist all
	 * properties that ever appeared 20 times (50% of PROP_LIMIT)
	 * in at least two topics, so it can be a pipeline like this:
	 *   grep FreebaseOntology.*property: logs/curated-train-8c3f62a.log | sed 's#[^/]*./##; s# ->.*##' | uniq -c | sort -n | uniq | awk '$1>=20{print$0}' | sed 's/.*://' | sort | uniq -c | sort -n | awk '$1>1{print$0}' | sed -re 's#^ *([0-9]+) *([^/]*)/(.*)#\t\t/\* \1x \2 *\/ "\3",#'
	 */
	String propBlacklist[] = {
		/* 2x Categories */ "award.award.category",
		/* 2x Contains */ "location.location.contains",
		/* 2x Editions */ "book.book.editions",
		/* 2x Events */ "olympics.olympic_games.events",
		/* 2x Isotopes */ "chemistry.chemical_element.isotopes",
		/* 2x Military personnel involved */ "military.military_conflict.military_personnel_involved",
		/* 2x Offices */ "government.government_office_category.offices",
		/* 2x Works written */ "book.author.works_written",
		/* 3x Episodes */ "tv.tv_program.episodes",
		/* 4x Artists */ "music.genre.artists",
		/* 4x Organizations in this sector */ "organization.organization_sector.organizations_in_this_sector",
		/* 4x People With This Profession */ "people.profession.people_with_this_profession",
		/* 5x Profession */ "people.person.profession",
		/* 6x Recorded versions */ "music.composition.recordings",
		/* 14x Works Written About This Topic */ "book.book_subject.works",
		/* 39x Recordings */ "music.artist.track",
		/* 58x People born here */ "location.location.people_born_here",
		/*  This one was already included before 8c3f62a: */
		"astronomy.orbital_relationship.orbited_by",
		/*  Second pass with f620f16: */
		/* 2x Competitions */ "olympics.olympic_games.competitions",
		/* 2x People Who Died This Way */ "people.cause_of_death.people",
		/* 2x Postal codes */ "location.citytown.postal_codes",
		/* 2x Time zone(s) */ "location.location.time_zones",
		/* 2x Titles */ "media_common.netflix_genre.titles",
		/* 2x TV programs of this genre */ "tv.tv_genre.programs",
		/* 3x Albums */ "music.genre.albums",
		/* 3x Characters With This Occupation */ "fictional_universe.character_occupation.characters_with_this_occupation",
		/* 3x Organizations with this scope */ "organization.organization_scope.organizations_with_this_scope",
		/* 3x Works Composed */ "music.composer.compositions",
		/* 4x Films On This Subject */ "film.film_subject.films",
		/* 4x Tourist attractions */ "travel.travel_destination.tourist_attractions",
		/* 7x Albums */ "music.artist.album",
		/* 24x Events */ "location.location.events",
		/*  Third pass with a751bd3: */
		/* 2x Compositions */ "music.compositional_form.compositions",
		/* 2x Computer games */ "cvg.cvg_genre.games",
		/* 2x Featured In Films */ "film.film_location.featured_in_films",
		/* 4x Includes event */ "time.event.includes_event",
		/* 5x Newspapers */ "periodicals.newspaper_circulation_area.newspapers",
		/* 6x Partially contains */ "location.location.partially_contains",
		/*  And manually: */
		"symbols.name_source.namesakes",
		/*  Next pass with 1d51097: */
		/* 2x Affected by cyclones */ "meteorology.cyclone_affected_area.cyclones",
		/* 2x Fictional Characters Born Here */ "fictional_universe.fictional_setting.fictional_characters_born_here",
		/* 2x Films of this genre */ "film.film_genre.films_in_this_genre",
		/* 2x Organizations in this industry */ "business.industry.companies",
		/* 3x Military units */ "military.military_unit_place_of_origin.military_units",
		/*  And manually: */
		"sports.sports_team_location.teams",
	};

	/** Query for a given title, returning a set of PropertyValue instances. */
	public List<PropertyValue> query(String title, Logger logger) {
		for (String titleForm : cookedTitles(title)) {
			Set<String> topics = queryTitleForm(titleForm, logger);
			List<PropertyValue> results = new ArrayList<PropertyValue>();
			for (String mid : topics)
				results.addAll(queryTopic(titleForm, mid, logger));
			if (!results.isEmpty())
				return results;
		}
		return new ArrayList<PropertyValue>();
	}

	/** Query for a given specific title form, returning a set of
	 * topic MIDs. */
	public Set<String> queryTitleForm(String title, Logger logger) {
		/* XXX: Case-insensitive search via SPARQL turns out
		 * to be surprisingly tricky.  Cover 90% of all cases
		 * by force-capitalizing the first letter in the sought
		 * after title. */
		title = Character.toUpperCase(title.charAt(0)) + title.substring(1);

		String quotedTitle = title.replaceAll("\"", "").replaceAll("\\\\", "").replaceAll("\n", " ");
		/* If you want to paste this to SPARQL query interface,
		 * just pass the block below through
		 * 	echo 'PREFIX ns: <http://rdf.freebase.com/ns/>'; echo 'PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>'; echo 'SELECT ?topic WHERE {'; perl -pe 'undef $_ unless /"/; s/\s*"//; s/\\n" \+$//;'; echo '}'
		 */
		String rawQueryStr =
			"?topic rdfs:label \"" + quotedTitle + "\"@en.\n" +
			/* Keep only topics that are associated with
			 * an enwiki concept.  The rest is junk. */
			"?topic ns:type.object.key ?key .\n" +
			"FILTER( REGEX(STR(?key), '^/wikipedia/en/') )" +
			"";
		// logger.debug("executing sparql query: {}", rawQueryStr);
		List<Literal[]> rawResults = rawQuery(rawQueryStr,
			new String[] { "topic", "key" }, TOPIC_LIMIT);

		Set<String> results = new HashSet<String>(rawResults.size());
		for (Literal[] rawResult : rawResults) {
			String mid = rawResult[0].getString();
			String key = rawResult[1].getString();
			logger.debug("Freebase {} topic MID {} ({})", title, mid, key);
			results.add(mid);
		}

		return results;
	}

	/** Query for a given MID, returning a set of PropertyValue instances
	 * that cover relevant-seeming properties. */
	/* FIXME: Some physical quantities have separate topics and
	 * no labels, like ns:chemistry.chemical_element.atomic_mass. */
	/* FIXME: Some relationships could be useful but seem difficult
	 * to traverse uniformly, like ns:people.person.sibling_s,
	 * ns:film.person_or_entity_appearing_in_film.films,
	 * ns:people.person.education or awards:
	 * ns:base.nobelprizes.nobel_subject_area.nobel_awards,
	 * ns:award.award_winner.awards_won. */
	public List<PropertyValue> queryTopic(String titleForm, String mid, Logger logger) {
		/* If you want to paste this to SPARQL query interface,
		 * just pass the block below through
		 * 	echo 'PREFIX ns: <http://rdf.freebase.com/ns/>'; echo 'PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>'; echo 'SELECT ?topic WHERE {'; perl -pe 'undef $_ unless /"/; s/\s*"//; s/\\n" \+$//;'; echo '}'
		 */
		String rawQueryStr =
			/* Grab all properties of the topic, for starters. */
			"ns:" + mid + " ?prop ?val .\n" +
			/* Check if property is a labelled type, and use that
			 * label as property name if so. */
			"OPTIONAL {\n" +
			"  ?prop ns:type.object.name ?proplabel .\n" +
			"  FILTER( LANGMATCHES(LANG(?proplabel), \"en\") )\n" +
			"} .\n" +
			"BIND( IF(BOUND(?proplabel), ?proplabel, ?prop) AS ?property )\n" +
			/* Check if value is not a pointer to another topic
			 * we could resolve to a label. */
			"OPTIONAL {\n" +
			"  ?val rdfs:label ?vallabel .\n" +
			"  FILTER( LANGMATCHES(LANG(?vallabel), \"en\") )\n" +
			"}\n" +
			"BIND( IF(BOUND(?vallabel), ?vallabel, ?val) AS ?value )\n" +
			/* Ignore properties with values that are still URLs,
			 * i.e. pointers to an unlabelled topic. */
			"FILTER( !ISURI(?value) )\n" +
			/* Keep only ns: properties */
			"FILTER( STRSTARTS(STR(?prop), 'http://rdf.freebase.com/ns/') )\n" +
			/* ...but ignore some common junk which yields mostly
			 * no relevant data... */
			"FILTER( !STRSTARTS(STR(?prop), 'http://rdf.freebase.com/ns/type') )\n" +
			"FILTER( !STRSTARTS(STR(?prop), 'http://rdf.freebase.com/ns/common') )\n" +
			"FILTER( !STRSTARTS(STR(?prop), 'http://rdf.freebase.com/ns/freebase') )\n" +
			"FILTER( !STRSTARTS(STR(?prop), 'http://rdf.freebase.com/ns/media_common.quotation') )\n" +
			/* ...stuff that's difficult to trust... */
			"FILTER( !STRSTARTS(STR(?prop), 'http://rdf.freebase.com/ns/user') )\n" +
			/* ...and some more junk - this one is already a bit
			 * trickier as it might contain relevant data, but
			 * often hidden in relationship objects (like Nobel
			 * prize awards), and also a lot of junk (like the
			 * kwebbase experts - though some might be useful
			 * in a specific context). */
			"FILTER( !STRSTARTS(STR(?prop), 'http://rdf.freebase.com/ns/base') )\n" +
			/* topic_server has geolocation (not useful right now)
			 * and population_number (which would be useful, but
			 * needs special handling has a topic may have many
			 * of these, e.g. White House). Also it has crappy
			 * type labels. */
			"FILTER( !STRSTARTS(STR(?prop), 'http://rdf.freebase.com/ns/topic_server') )\n" +
			/* Eventually, a specific blacklist of *mostly*
			 * useless data that flood out the useful results. */
			"FILTER( !STRSTARTS(STR(?prop), 'http://rdf.freebase.com/ns/" +
			StringUtils.join(propBlacklist,
				"') )\nFILTER( !STRSTARTS(STR(?prop), 'http://rdf.freebase.com/ns/") +
				"') )\n" +
			"";
		// logger.debug("executing sparql query: {}", rawQueryStr);
		List<Literal[]> rawResults = rawQuery(rawQueryStr,
			new String[] { "property", "value", "prop" }, PROP_LIMIT);

		List<PropertyValue> results = new ArrayList<PropertyValue>(rawResults.size());
		for (Literal[] rawResult : rawResults) {
			/* ns:astronomy.star.temperature_k -> "temperature"
			 * ns:astronomy.star.planet_s -> "planet"
			 * ns:astronomy.star.spectral_type -> "spectral type"
			 * ns:chemistry.chemical_element.periodic_table_block -> "periodic table block"
			 *
			 * But typically we fetch the property name from
			 * the RDF store too, so this should be irrelevant
			 * in that case.*/
			String propLabel = rawResult[0].getString().
				replaceAll("^.*\\.([^\\. ]*)$", "\\1").
				replaceAll("_.$", "").
				replaceAll("_", " ");
			String value = rawResult[1].getString();
			String prop = rawResult[2].getString();
			logger.debug("Freebase {}/{} property: {}/{} -> {}", titleForm, mid, propLabel, prop, value);
			results.add(new PropertyValue(titleForm, propLabel, value));
		}

		return results;
	}
}
