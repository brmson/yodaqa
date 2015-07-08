var qid;  // id of the last posed question
var gen_sources, gen_answers;  // when this number changes, re-render

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
function showAnswers(container, answers, passages) {
	container.empty();
	var i = 1;
	answers.forEach(function(a) {
		// FIXME: also deal with < > &
		text = a.text.replace(/"/g, "&#34;");
        var str="found using "+ a.source+" \n";
        for(var index = 0; index< a.passageIDs.length; index++) {
         str += " "+index+". "+passages[a.passageIDs[index]].replace(/"/g, "&#34;")+"\n";
        }
		container.append('<tr><td class="i">'+i+'.</td>'
				+ '<td class="text" title="'+str+'">'+text+'</td>'
				+ '<td class="scorebar">'+score_bar(a.confidence)+'</td>'
				+ '<td class="score">'+(a.confidence*100).toFixed(1)+'%</td></tr>');
		i++;
	});
}

/* Retrieve, process and display json question information. */
function getQuestionJson() {
	$.get("/q/"+qid, function(r) {
		$('input[name="text"]').val(r.text);

		if (r.summary) {
			/* Show the question summary. */
			container = $("#summary");
			if (!container.length) {
				container = $('<div id="summary"></div>');
				$("#metadata_area").prepend(container);
				showSummary(container, r.summary);
			}
		}

		if (r.sources.length && gen_sources != r.gen_sources) {
			/* Show the answer sources. */
			container = $("#sources");
			if (!container.length) {
				container = $('<div id="sources"></div>');
				$("#metadata_area").prepend(container);
			}
			showSources(container, r.sources);
			gen_sources = r.gen_sources;
		}

		if (r.answers && gen_answers != r.gen_answers) {
			/* Show the list of answers. */
			container = $("#answers");
			if (!container.length) {
				container = $('<table id="answers"></table>');
				$("#answers_area").prepend(container);
			}
			showAnswers(container, r.answers, r.passages);
			gen_answers = r.gen_answers;
		}

		if (r.finished) {
			$("#spinner").hide();
		} else {
			// keep watching
			setTimeout(getQuestionJson, 500);
		}
	});
}

/* Create a titled listing of questions. */
function showQuestionList(container, title, list) {
	container.empty();
	container.append('<h2>' + title + '</h2>');
	list.forEach(function(q) {
		container.append('<p class="qline"><a href="javascript:loadQuestion('+q.id+')">'+q.text+'</a></p>');
	});
}

/* Retrieve, process and display json question list. */
function getToAnswerJson() {
	$.get("/q/?toAnswer", function(r) { showQuestionList($("#toAnswer"), "Question Queue", r); });
}
function getInProgressJson() {
	$.get("/q/?inProgress", function(r) { showQuestionList($("#inProgress"), "In Progress", r); });
}
function getAnsweredJson() {
	$.get("/q/?answered", function(r) { showQuestionList($("#answered"), "Answered", r); });
}

function loadQuestion(q) {
	$("#metadata_area").empty();
	$("#answers_area").empty();
	$("#spinner").show();
	qid = q;
	gen_sources = 0;
	gen_answers = 0;
	getQuestionJson();
}

$(function() {
$("#ask").ajaxForm({
	success: function(response) {
		setTimeout(function() { loadQuestion(response) }, 500);
	}});

getToAnswerJson(); setInterval(getToAnswerJson, 3100);
getInProgressJson(); setInterval(getInProgressJson, 3000);
getAnsweredJson(); setInterval(getAnsweredJson, 2900);
});
