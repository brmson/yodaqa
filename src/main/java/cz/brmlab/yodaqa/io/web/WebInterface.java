package cz.brmlab.yodaqa.io.web;

/* XXX: This would be much nicer with Java8 -> lambda syntax,
 * of course.  But Java8 is still not easily deployable
 * in Debian Jessie, so out of bounds for us for now.
 *
 * So we use Spark 1.  To view its README etc.,
 * 	https://github.com/perwendel/spark/tree/ab8de107373acbd7f0eb8f78405024813b9ffe68
 * */
import static spark.Spark.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cz.brmlab.yodaqa.provider.url.UrlManager;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;

import cz.brmlab.yodaqa.flow.dashboard.Question;
import cz.brmlab.yodaqa.flow.dashboard.QuestionConcept;
import cz.brmlab.yodaqa.flow.dashboard.QuestionDashboard;


/**
 * A runnable that takes care of some rudimentary web interace.
 * We use Spark as that seems the simplest option nowadays.
 *
 * The web interface is meant to run in a separate thread so that
 * it can service requests even while a question is being processed
 * by the UIMA pipeline.  To facilitate communication with the pipeline,
 * the WebInterface provides two methods to be called externally
 * from a different thread - for retrieving questions to ask and
 * registering answers of past questions. */

public class WebInterface implements Runnable {
	final Logger logger = LoggerFactory.getLogger(WebInterface.class);

	public void run() {
		staticFileLocation("/webpublic");

		// Ask a question
		post(new Route("/q") {
			Random idgen = new Random();
			@Override
			public Object handle(Request request, Response response) {
				String id = Integer.toString(idgen.nextInt(Integer.MAX_VALUE));
				String text=request.queryParams("text");
				if (text == null) {
					response.status(422);
					return "missing parameter: text";
				}
				logger.info("{} :: new question {} <<{}>>", request.ip(), id, text);
				List<QuestionConcept> artificialConcepts = retrieveArtificialConcepts(request);
				Question q = new Question(id, text);
				if (!artificialConcepts.isEmpty()) {
					q.setArtificialConcepts(artificialConcepts);
					q.setHasOnlyArtificialConcept(true);
				}
				String artificialClueText = request.queryParams("artificialClue");
				if (artificialClueText != null){
					q.setArtificialClueText(artificialClueText);
				}
				QuestionDashboard.getInstance().askQuestion(q);
				response.header("Access-Control-Allow-Origin", "*");
				response.status(201);
				return "{\"id\":" + "\""+q.getId() + "\"" + "}";
			}
		});

		// Retrieve question data
		get(new Route("/q/:id") {
			@Override
			public Object handle(Request request, Response response) {
				response.type("application/json");
				response.header("Access-Control-Allow-Origin", "*");
				String id;
				try {
					id = request.params("id");
				} catch (NumberFormatException e) {
					response.status(404);
					return "{}";
				}
				Question q = QuestionDashboard.getInstance().get(id);
				if (q == null) {
					logger.debug("{} :: /q <<{}>> ???", request.ip(), id);
					response.status(404);
					return "{}";
				}
				// logger.debug("{} :: /q <<{}>> -> <<{}>>", request.ip(), id, json);
				return q.toJson();
			}
		});

		// Retrieve sets of questions
		get(new Route("/q/") {
			@Override
			public Object handle(Request request, Response response) {
				logger.debug("{} :: /q list", request.ip());
				response.type("application/json");
				response.header("Access-Control-Allow-Origin", "*");
				QuestionDashboard qd = QuestionDashboard.getInstance();
				List<String> qJson = new ArrayList<>();

				if (request.queryParams("toAnswer") != null) {
					for (Question q : qd.getQuestionsToAnswer())
						qJson.add(q.toJson());

				} else if (request.queryParams("inProgress") != null) {
					for (Question q : qd.getQuestionsInProgress())
						qJson.add(q.toJson());

				} else if (request.queryParams("answered") != null) {
					for (Question q : qd.getQuestionsAnswered())
						qJson.add(q.toJson());

				} else {
					response.type("text/plain");
					response.status(501);
					return "for now, please use either ?toAnswer, ?inProgress or ?answered";
				}

				if (request.queryParams("all") == null)
					try {
						qJson = qJson.subList(0, 6); // yield just a few by default
					} catch (IndexOutOfBoundsException e) {
						// ignore
					}

				return "[" + StringUtils.join(qJson, ",\n") + "]";
			}
		});

		get(new Route("/dataUrls") {
			@Override
			public Object handle(Request request, Response response) {
				response.type("application/json");
				response.header("Access-Control-Allow-Origin", "*");
				return UrlManager.getInstance().printState();

			}
		});
	}

	private List<QuestionConcept> retrieveArtificialConcepts(Request request){
		List<QuestionConcept> artificialConcepts = new ArrayList<>();

		String numberOfArtificialConceptsS = request.queryParams("numberOfConcepts");
		if (numberOfArtificialConceptsS == null)
			return artificialConcepts;

		int numberOfArtificialConcepts = Integer.parseInt(numberOfArtificialConceptsS);
		for (int i = 1; i <= numberOfArtificialConcepts; i++) {
			String fullLabel = request.queryParams("fullLabel" + i);
			if (fullLabel == null || fullLabel.equals(""))
				continue;

			String pageIDS = request.queryParams("pageID" + i);
			if (pageIDS == null || pageIDS.equals(""))
				continue;
			int pageID = Integer.parseInt(pageIDS);

			artificialConcepts.add(new QuestionConcept(fullLabel, pageID));
		}

		return artificialConcepts;
	}
}
