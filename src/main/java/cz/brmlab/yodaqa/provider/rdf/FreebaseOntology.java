package cz.brmlab.yodaqa.provider.rdf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Literal;

import cz.brmlab.yodaqa.analysis.rdf.FBPathLogistic.PathScore;
import cz.brmlab.yodaqa.flow.dashboard.AnswerSourceStructured;
import cz.brmlab.yodaqa.analysis.ansscore.AF;
import cz.brmlab.yodaqa.analysis.ansscore.AnswerFV;
import cz.brmlab.yodaqa.model.Question.Concept;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

/** A wrapper around Freebase dataset that maps concepts to curated
 * entity properties, at least those that seem generally useful.
 * This can then serve as an information source. */

public class FreebaseOntology extends FreebaseLookup {
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
		/*  Next pass, temporarily with PROP_LIMIT==80: */
                /* 2x Awards in this discipline */ "award.award_discipline.awards_in_this_discipline",
                /* 2x Beers from here */ "food.beer_country_region.beers_from_here",
                /* 2x Books In This Genre */ "media_common.literary_genre.books_in_this_genre",
                /* 2x Breeds orginating here */ "biology.breed_origin.breeds_originating_here",
                /* 2x Characters of This Species */ "fictional_universe.character_species.characters_of_this_species",
                /* 2x First level divisions */ "location.country.first_level_divisions",
                /* 2x Recordings produced */ "music.producer.tracks_produced",
                /* 2x Second level divisions */ "location.country.second_level_divisions",
                /* 2x Wines */ "wine.wine_region.wines",
                /* 3x Influenced */ "influence.influence_node.influenced",
                /* 3x Locations */ "astronomy.celestial_object.locations",
                /* 3x Lyrics Written */ "music.lyricist.lyrics_written",
                /* 4x Languages spoken */ "location.country.languages_spoken",
                /* 5x Administrative Divisions */ "location.country.administrative_divisions",
                /* 5x Content */ "broadcast.artist.content",
                /* 7x Agencies */ "government.governmental_jurisdiction.agencies",
                /* 17x Quotations */ "people.person.quotations",
		/*  Next pass, temporarily with PROP_LIMIT==120: */
                /* 2x Artwork on the Subject */ "visual_art.art_subject.artwork_on_the_subject",
                /* 2x Content */ "broadcast.genre.content",
                /* 2x Lower classifications */ "biology.organism_classification.lower_classifications",
                /* 2x Subgenres */ "music.genre.subgenre",
                /* 2x Websites of this genre */ "internet.website_category.sites",
                /* 4x Book editions published */ "book.author.book_editions_published",
	};

	/** Query for a given title, returning a set of PropertyValue instances.
	 * The paths set are extra properties to specifically query for:
	 * they bypass the blacklist and can traverse multiple nodes. */
	public List<PropertyValue> query(String title, List<PathScore> paths, List<Concept> concepts, List<String> witnessLabels, Logger logger) {
		for (String titleForm : cookedTitles(title)) {
			Set<String> topics = queryTopicByTitleForm(titleForm, logger);
			List<PropertyValue> results = new ArrayList<PropertyValue>();
			for (String mid : topics) {
				results.addAll(queryTopicGeneric(titleForm, mid, logger));
				if (!paths.isEmpty())
					results.addAll(queryTopicSpecific(titleForm, mid, paths, concepts, witnessLabels, logger));
			}
			if (!results.isEmpty())
				return results;
		}
		return new ArrayList<PropertyValue>();
	}

	class TitledMid {
		String mid;
		String title;

		public TitledMid(String mid, String title) {
			this.mid = mid;
			this.title = title;
		}
	}

	/** Query for a given enwiki pageID, returning a set of PropertyValue
	 * instances.
	 * The paths set are extra properties to specifically query for:
	 * they bypass the blacklist and can traverse multiple nodes. */
	public List<PropertyValue> queryPageID(int pageID, List<PathScore> paths, List<Concept> concepts, List<String> witnessLabels, Logger logger) {
		Set<TitledMid> topics = queryTopicByPageID(pageID, logger);
		List<PropertyValue> results = new ArrayList<PropertyValue>();
		for (TitledMid topic : topics) {
			results.addAll(queryTopicGeneric(topic.title, topic.mid, logger));
			if (!paths.isEmpty())
				results.addAll(queryTopicSpecific(topic.title, topic.mid, paths, concepts, witnessLabels, logger));
		}
		return results;
	}

	/** Query for a given specific title form, returning a set of
	 * topic MIDs. */
	public Set<String> queryTopicByTitleForm(String title, Logger logger) {
		/* XXX: Case-insensitive search via SPARQL turns out
		 * to be surprisingly tricky.  Cover 91% of all cases
		 * by capitalizing words that are not stopwords  */
		title = super.capitalizeTitle(title);

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

	/** Query for a given enwiki pageID, returning a set of topic MIDs. */
	public Set<TitledMid> queryTopicByPageID(int pageID, Logger logger) {
		String rawQueryStr =
			"?topic <http://rdf.freebase.com/key/wikipedia.en_id> \"" + pageID + "\" .\n" +
			"?topic rdfs:label ?label .\n" +
			"FILTER( LANGMATCHES(LANG(?label), \"en\") )\n" +
			"";
		// logger.debug("executing sparql query: {}", rawQueryStr);
		List<Literal[]> rawResults = rawQuery(rawQueryStr,
			new String[] { "topic", "label" }, TOPIC_LIMIT);

		Set<TitledMid> results = new HashSet<>(rawResults.size());
		for (Literal[] rawResult : rawResults) {
			String mid = rawResult[0].getString();
			String title = rawResult[1].getString();
			logger.debug("Freebase {} topic MID {} ({})", title, mid, pageID);
			results.add(new TitledMid(mid, title));
		}

		return results;
	}

	/** Query for a given MID, returning a set of PropertyValue instances
	 * that cover all non-spammy, direct RDF properties.
	 *
	 * This has relatively low recall, not just due to the limited set
	 * of results and blacklist, but also because there are some
	 * intermediate nodes ("virtual topics") for many properties.
	 * E.g. some physical quantities have separate topics and
	 * no labels, like ns:chemistry.chemical_element.atomic_mass.
	 * Other important examples include ns:people.person.sibling_s,
	 * ns:film.person_or_entity_appearing_in_film.films,
	 * ns:people.person.education or awards:
	 * ns:base.nobelprizes.nobel_subject_area.nobel_awards,
	 * ns:award.award_winner.awards_won.
	 * For these, we apply a fbpath label prediction machine learning
	 * model and handle them in queryTopicSpecific(). */
	protected List<PropertyValue> queryTopicGeneric(String titleForm, String mid, Logger logger) {
		String rawQueryStr =
			/* Grab all properties of the topic, for starters. */
			"ns:" + mid + " ?prop ?val .\n" +
			"BIND(ns:" + mid + " AS ?res)\n" +
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
			/* Ignore non-English values (this checks even literals,
			 * not target labels like the filter above. */
			"FILTER( LANG(?value) = \"\" || LANGMATCHES(LANG(?value), \"en\") )\n" +
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
			new String[] { "property", "value", "prop", "/val", "/res" }, PROP_LIMIT);

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
			String valRes = rawResult[3] != null ? rawResult[3].getString() : null;
			String objRes = rawResult[4].getString();
			logger.debug("Freebase {}/{} property: {}/{} -> {} ({})", titleForm, mid, propLabel, prop, value, valRes);
			AnswerFV fv = new AnswerFV();
			fv.setFeature(AF.OriginFreebaseOntology, 1.0);
			results.add(new PropertyValue(titleForm, objRes, propLabel,
						value, valRes, null,
						fv, AnswerSourceStructured.ORIGIN_ONTOLOGY));
		}

		return results;
	}

	/** Query for a given MID, returning a set of PropertyValue instances
	 * that cover the specified property paths.  This generalizes poorly
	 * to lightly covered topics, but has high precision+recall for some
	 * common topics where it can reach through the meta-nodes. */
	protected List<PropertyValue> queryTopicSpecific(String titleForm, String mid, List<PathScore> paths, List<Concept> concepts, List<String> witnessLabels, Logger logger) {
		/* Test query:
		   PREFIX ns: <http://rdf.freebase.com/ns/>
		   PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
		   SELECT ?prop ?t0 ?value WHERE {
		     {
		       ns:m.09l3p ns:film.actor.film ?t0 .
		       ?t0 ns:film.performance.character ?val .
		       ns:film.actor.film rdfs:label ?p0 .
		       ns:film.performance.character rdfs:label ?p1 .
		       BIND(CONCAT(?p0, ": ", ?p1) AS ?prop)
		     } UNION {
		       ns:m.09l3p ns:film.actor.film ?t0 .
		       ?t0 ns:film.performance.film ?val .
		       ns:film.actor.film rdfs:label ?p0 .
		       ns:film.performance.film rdfs:label ?p1 .
		       BIND(CONCAT(?p0, ": ", ?p1) AS ?prop)
		     } UNION {
		       ns:m.09l3p ns:people.person.nationality ?val .
		       ns:people.person.nationality rdfs:label ?prop .
		     }
		     OPTIONAL { ?val rdfs:label ?vallabel . FILTER( LANGMATCHES(LANG(?vallabel), "en") ) } 
		     BIND( IF(BOUND(?vallabel), ?vallabel, ?val) AS ?value )
		   }
		 */
		List<String> pathQueries = new ArrayList<>();
		for (PathScore ps : paths) {
			PropertyPath path = ps.path;
			assert(path.size() <= 3);  // longer paths don't occur in our dataset
			logger.debug("specific path {} {}", path, path.size());
			if (path.size() == 1) {
				String pathQueryStr = "{" +
					"  ns:" + mid + " ns:" + path.get(0) + " ?val .\n" +
					"  BIND(\"ns:" + path.get(0) + "\" AS ?prop)\n" +
					"  BIND(" + ps.proba + " AS ?score)\n" +
					"  BIND(0 AS ?branched)\n" +
					"  BIND(ns:" + mid + " AS ?res)\n" +
					"  OPTIONAL {\n" +
					"    ns:" + path.get(0) + " rdfs:label ?proplabel .\n" +
					"    FILTER(LANGMATCHES(LANG(?proplabel), \"en\"))\n" +
					"  }\n" +
					"}";
				pathQueries.add(pathQueryStr);
			} else if (path.size() == 2) {
				String pathQueryStr = "{" +
					"  ns:" + mid + " ns:" + path.get(0) + "/ns:" + path.get(1) + " ?val .\n" +
					"  BIND(\"ns:" + path.get(0) + "/ns:" + path.get(1) + "\" AS ?prop)\n" +
					"  BIND(" + ps.proba + " AS ?score)\n" +
					"  BIND(0 AS ?branched)\n" +
					"  BIND(ns:" + mid + " AS ?res)\n" +
					"  OPTIONAL {\n" +
					"    ns:" + path.get(0) + " rdfs:label ?pl0 .\n" +
					"    ns:" + path.get(1) + " rdfs:label ?pl1 .\n" +
					"    FILTER(LANGMATCHES(LANG(?pl0), \"en\"))\n" +
					"    FILTER(LANGMATCHES(LANG(?pl1), \"en\"))\n" +
					"    BIND(CONCAT(?pl0, \": \", ?pl1) AS ?proplabel)\n" +
					"  }\n" +
					"}";
				pathQueries.add(pathQueryStr);
			} else if (path.size() == 3) {
				for (Concept c: concepts) {
					pathQueries.add(getWitnessQuery(mid, ps, c.getFullLabel(), c.getPageID()));
				}
				for (String w: witnessLabels) {
					pathQueries.add(getWitnessQuery(mid, ps, w, null));
				}
			}
		}
		String rawQueryStr =
			StringUtils.join(pathQueries, " UNION ") +
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
			/* Ignore non-English values (this checks even literals,
			 * not target labels like the filter above. */
			"FILTER( LANG(?value) = \"\" || LANGMATCHES(LANG(?value), \"en\") )\n" +
			"";
		// logger.debug("executing sparql query: {}", rawQueryStr);
		List<Literal[]> rawResults = rawQuery(rawQueryStr,
			new String[] { "property", "value", "prop", "/val", "/res", "score", "branched", "witnessAF", "wlabel" },
			/* We want to be fairly liberal and look at all the properties
			 * as the interesting ones may be far down in the list,
			 * but there is some super-spammy stuff like all locations
			 * contained in Poland that we just need to avoid. */
			PROP_LIMIT * 10);

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
			String valRes = rawResult[3] != null ? rawResult[3].getString() : null;
			String objRes = rawResult[4].getString();
			double score = rawResult[5].getDouble();
			boolean isBranched = rawResult[6].getInt() != 0;
			String witnessAF = rawResult[7] != null ? rawResult[7].getString() : null;
			String wLabel = rawResult[8] != null ? rawResult[8].getString() : null;
			logger.debug("Freebase {}/{} property: {}/{} -> {} ({}) {} {},{},<<{}>>",
				titleForm, mid, propLabel, prop, value, valRes, score,
				isBranched ? "branched" : "straight", witnessAF,
				wLabel);
			AnswerFV fv = new AnswerFV();
			fv.setFeature(AF.OriginFreebaseSpecific, 1.0);
			if (isBranched)
				fv.setFeature(AF.OriginFreebaseBranched, 1.0);
			if (witnessAF != null)
				fv.setFeature(witnessAF, 1.0);
			PropertyValue pv = new PropertyValue(titleForm, objRes, propLabel,
					value, valRes, wLabel,
					fv, AnswerSourceStructured.ORIGIN_SPECIFIC);
			pv.setPropRes(prop);
			pv.setScore(score);
			results.add(pv);
		}

		return results;
	}

	/** Return a SPARQL select fragment for matching 3-relation path with
	 * a given witness (connected extra selector clue). */
	protected String getWitnessQuery(String mid, PathScore ps, String fullLabel, Integer pageID) {
		PropertyPath path = ps.path;
		String witnessRel = path.get(2);
		String quotedTitle = fullLabel.replaceAll("\"", "").replaceAll("\\\\", "").replaceAll("\n", " ");
		String pathQueryStr = "{" +
			"  ns:" + mid + " ns:" + path.get(0) + " ?med .\n" +
			"  ?med ns:" + path.get(1) + " ?val .\n";

		if (pageID != null) {
			/* MID witness match */
			pathQueryStr +=
				"  {\n" +
				"    ?med ns:" + witnessRel + " ?concept .\n" +
				"    ?concept <http://rdf.freebase.com/key/wikipedia.en_id> \"" + pageID + "\" .\n" +
				"    ?concept rdfs:label ?wlabel .\n" +
				"    FILTER(LANGMATCHES(LANG(?wlabel), \"en\"))\n" +
				"    BIND(\"" + AF.OriginFreebaseWitnessMid + "\" AS ?witnessAF)\n" +
				"  } UNION";
		}

		/* Label witness match */
		pathQueryStr +=
			" {\n" +
			"    {\n" +
			"      ?med ns:" + witnessRel + " ?wlabel .\n" +
			"      FILTER(!ISURI(?wlabel))\n" +
			"    } UNION {\n" +
			"      ?med ns:" + witnessRel + " ?concept .\n" +
			"      ?concept rdfs:label ?wlabel .\n" +
			"    }\n" +
			"    FILTER(LANGMATCHES(LANG(?wlabel), \"en\"))\n" +
			"    FILTER(CONTAINS(LCASE(?wlabel), LCASE(\"" + quotedTitle + "\")))\n" +
			"    BIND(\"" + AF.OriginFreebaseWitnessLabel + "\" AS ?witnessAF)\n" +
			"  }\n" +

			"  BIND(\"ns:" + path.get(0) + "/ns:" + path.get(1) + "\" AS ?prop)\n" +
			"  BIND(" + ps.proba + " AS ?score)\n" +
			"  BIND(1 AS ?branched)\n" +
			"  BIND(ns:" + mid + " AS ?res)\n" +
			"  OPTIONAL {\n" +
			"    ns:" + path.get(0) + " rdfs:label ?pl0 .\n" +
			"    ns:" + path.get(1) + " rdfs:label ?pl1 .\n" +
			"    FILTER(LANGMATCHES(LANG(?pl0), \"en\"))\n" +
			"    FILTER(LANGMATCHES(LANG(?pl1), \"en\"))\n" +
			"    BIND(CONCAT(?pl0, \": \", ?pl1) AS ?proplabel)\n" +
			"  }\n" +
			"}";
		return pathQueryStr;
	}
}
