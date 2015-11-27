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
        $.each(sources, function(sid, source) {
		var state_stags = ['<i>', '<b>', ''];
		var state_etags = ['</i>', '</b>', ''];
        if (source.origin != "document title") {
		if (source.type == "enwiki") {
			url = 'http://en.wikipedia.org/?curid=' + source.pageId;
		} else {
			url = source.URL;
		}
            container.append('<p class="source">'
                + '<img src="/wikipedia-w-logo.png" alt="W" class="wlogo" />'
                + ' <a href="' + url + '" target="_blank">'
                + state_stags[source.state] + source.title + state_etags[source.state]
                + '</a> (' + source.type + ' ' + source.origin + ')</p>'); // TODO also include the first sentence?
        }
	});
}

/* Create a table with answers. */
function showAnswers(container, answers, snippets, sources) {
	container.empty();
	var i = 1;
	answers.forEach(function(a) {
		// FIXME: also deal with < > &
		text = a.text.replace(/"/g, "&#34;");
        var str="";

        for(var index = 0; index< a.snippetIDs.length; index++) {
            //origin is (fulltext)/(title-in-clue)/(documented search)
	    source = sources[snippets[a.snippetIDs[index]].sourceID]
            str += "(" + source.type + " " + source.origin + ") \n";
            str += source.title + " \n";

            //add either wikipedia document ID or source URL
	    if (source.type == "enwiki") {
		    str += source.pageId + "\n";
	    } else {
		    str += source.URL + "\n";
	    }

            //add either passage text or property label
            if (!(typeof (snippets[a.snippetIDs[index]].passageText) ==="undefined")) {
                str += snippets[a.snippetIDs[index]].passageText.replace(/"/g, "&#34;") + "\n";
            }
            else if (!(typeof (snippets[a.snippetIDs[index]].propertyLabel) ==="undefined")) {
                str += snippets[a.snippetIDs[index]].propertyLabel;
                if (!(typeof (snippets[a.snippetIDs[index]].witnessLabel) ==="undefined")) {
		    str += " (" + snippets[a.snippetIDs[index]].witnessLabel + ")";
		}
		str += "\n";
            }
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

		if (!$.isEmptyObject(r.sources) && gen_sources != r.gen_sources) {
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
			showAnswers(container, r.answers, r.snippets, r.sources);
			gen_answers = r.gen_answers;
		}

		if (r.finished) {
			$("#spinner").hide();
			if (r.answerSentence) {
			    $("#answersent").append(r.answerSentence);
			}
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
	$("#answersent").empty();
	qid = q;
	gen_sources = 0;
	gen_answers = 0;
	getQuestionJson();
}

$(function() {
$("#ask").ajaxForm({
	success: function(response) {
		setTimeout(function() { loadQuestion(JSON.parse(response).id) }, 500);
	}});

getToAnswerJson(); setInterval(getToAnswerJson, 3100);
getInProgressJson(); setInterval(getInProgressJson, 3000);
getAnsweredJson(); setInterval(getAnsweredJson, 2900);
});
