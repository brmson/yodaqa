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
			r.answers.forEach(function(a) {
				$("#answers").append("<li>"+a+"</li>");
			})
		}
		function getResult() {
			$.get("/q/"+qid, processResult);
		}
		setTimeout(getResult, 500);
	}});

$("#results").hide();
});
