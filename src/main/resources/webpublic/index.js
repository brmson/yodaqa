/* Create a fancy score bar representing confidence in the answer. */
function score_bar(score) {
	var green = Math.round(200 * score + 25);
	var red = Math.round(200 * (1-score) + 25);
	return '<hr class="scorebar" style="width:'+(score*100)+'%; background-color:rgb('+red+','+green+',0)"> ';
}

$(function() {
var qid;

$("#ask").ajaxForm({
	beforeSubmit: function() {
		$("#results").slideDown();
		return true;
	},
	success: function(response) {
		// we posed the question, now wait
		qid = response;
		$("#spinner").show();
		$("#answers").empty();
		// ...until we are finished
		function processResult(r) {
			if (!r.finished) {
				setTimeout(getResult, 500);
				return;
			}
			$("#spinner").hide();
			var i = 1;
			r.answers.forEach(function(a) {
				// FIXME: also deal with < > &
				text = a.text.replace(/"/g, "&#34;");
				$('#answers').append('<tr><td class="i">'+i+'.</td>'
						+ '<td class="text" title="'+text+'">'+text+'</td>'
						+ '<td class="scorebar">'+score_bar(a.confidence)+'</td>'
						+ '<td class="score">'+(a.confidence*100).toFixed(1)+'%</td></tr>');
				i++;
			})
		}
		function getResult() {
			$.get("/q/"+qid, processResult);
		}
		setTimeout(getResult, 500);
	}});

$("#results").hide();
});
