var qid;  // id of the last posed question
var gen_sources;  // when this number changes, re-render

/* Create a fancy score bar representing confidence of an answer. */
function score_bar(score) {
	var green = Math.round(200 * score + 25);
	var red = Math.round(200 * (1-score) + 25);
	return '<hr class="scorebar" style="width:'+(score*100)+'%; background-color:rgb('+red+','+green+',0)"> ';
}

/* Create a box with question summary. */
function showSummary(container, summary) {
	container.empty();
	container.append("<p><b>Answer type:</b> " + summary.lats.join(', ') + "</p>");
	summary.concepts.forEach(function(c) {
		container.append('<p class="concept">'
				+ '<img src="/wikipedia-w-logo.png" alt="W" class="wlogo" />'
				+ ' <a href="http://en.wikipedia.org/?curid='+c.pageId+'" target="_blank">'
				+ c.title + '</a></p>'); // TODO also include the first sentence?
	});
}

/* Create a box with answer sources. */
function showSources(container, sources) {
	container.empty();
	sources.forEach(function(s) {
		var state_stags = ['<i>', '<b>', ''];
		var state_etags = ['</i>', '</b>', ''];
		container.append('<p class="source">'
				+ '<img src="/wikipedia-w-logo.png" alt="W" class="wlogo" />'
				+ ' <a href="http://en.wikipedia.org/?curid='+s.pageId+'" target="_blank">'
				+ state_stags[s.state] + s.title + state_etags[s.state]
				+ '</a> (' + s.origin + ')</p>'); // TODO also include the first sentence?
	});
}

/* Create a table with answers. */
function showAnswers(container, answers) {
	container.empty();
	var i = 1;
	answers.forEach(function(a) {
		// FIXME: also deal with < > &
		text = a.text.replace(/"/g, "&#34;");
		container.append('<tr><td class="i">'+i+'.</td>'
				+ '<td class="text" title="'+text+'">'+text+'</td>'
				+ '<td class="scorebar">'+score_bar(a.confidence)+'</td>'
				+ '<td class="score">'+(a.confidence*100).toFixed(1)+'%</td></tr>');
		i++;
	});
}

/* Retrieve, process and display json question information. */
function getQuestionJson() {
	$.get("/q/"+qid, function(r) {
		if (r.summary) {
			/* Show the question summary. */
			container = $("#summary");
			if (!container.length) {
				container = $('<div id="summary"></div>');
				$("#output").prepend(container);
				showSummary(container, r.summary);
			}
		}

		if (r.sources.length && gen_sources != r.gen_sources) {
			/* Show the answer sources. */
			container = $("#sources");
			if (!container.length) {
				container = $('<div id="sources"></div>');
				$("#output").prepend(container);
			}
			showSources(container, r.sources);
			gen_sources = r.gen_sources;
		}

		if (r.answers) {
			/* Show the list of answers. */
			container = $("#answers");
			if (!container.length) {
				container = $('<table id="answers"></table>');
				$("#output").prepend(container);
			} // but keep re-rendering the answers in case the list gets updated; TODO gen_answers
			showAnswers(container, r.answers);
		}

		if (r.finished) {
			$("#spinner").hide();
		} else {
			// keep watching
			setTimeout(getQuestionJson, 500);
		}
	});
}


$(function() {
$("#ask").ajaxForm({
	beforeSubmit: function() {
		$("#output").empty();
		return true;
	},
	success: function(response) {
		// we posed the question, start watching its info
		$("#spinner").show();
		qid = response;
		gen_sources = 0;
		setTimeout(getQuestionJson, 500);
	}});
});
