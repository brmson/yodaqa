var qid;  // id of the last posed question

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
				+ ' <a href="http://en.wikipedia.org/?curid='+c.pageId+'">'
				+ c.title + '</a></p>'); // TODO also include the first sentence?
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

		if (r.answers) {
			/* Show the list of answers. */
			container = $("#answers");
			if (!container.length) {
				container = $('<table id="answers"></table>');
				$("#output").prepend(container);
			} // but keep re-rendering the answers in case the list gets updated
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
		setTimeout(getQuestionJson, 500);
	}});
});
