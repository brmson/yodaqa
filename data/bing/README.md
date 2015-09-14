QA From Web Using Bing
======================

As one of our answer sources, YodaQA can also use web search using the Bing
search engine.  It can improve recall nicely, but has the disadvantage of
producing non-reproducible results as the response to a query can change
any day.

As of f04cce6 and 2015-07-20, the performance impact is:

	large2180-te  f04cce6 2015-07-21 Merge branch 'master... 264/520/694 38.0%/74.9% mrr 0.477 avgtime 6248.368
	large2180-te uf04cce6 2015-07-21 Merge branch 'master... 230/587/694 33.1%/84.6% mrr 0.430 avgtime 5976.657
	large2180-te vf04cce6 2015-07-21 Merge branch 'master... 259/520/694 37.3%/74.9% mrr 0.474 avgtime 6166.965
	large2180-tr  f04cce6 2015-07-21 Merge branch 'master... 599/1052/1479 40.5%/71.1% mrr 0.498 avgtime 12523.736
	large2180-tr uf04cce6 2015-07-21 Merge branch 'master... 510/1191/1479 34.5%/80.5% mrr 0.437 avgtime 11852.452
	large2180-tr vf04cce6 2015-07-21 Merge branch 'master... 585/1052/1479 39.6%/71.1% mrr 0.490 avgtime 12329.911

vs. baseline master (just a few revisions off):

	large2180-te  f023195 2015-07-19 Enable IPv6 only whe... 202/449/694 29.1%/64.7% mrr 0.387 avgtime 4363.590
	large2180-te uf023195 2015-07-19 Enable IPv6 only whe... 226/532/694 32.6%/76.7% mrr 0.407 avgtime 4107.277
	large2180-te vf023195 2015-07-19 Enable IPv6 only whe... 203/449/694 29.3%/64.7% mrr 0.388 avgtime 4305.919
	large2180-tr  f023195 2015-07-19 Enable IPv6 only whe... 475/915/1479 32.1%/61.9% mrr 0.410 avgtime 10319.411
	large2180-tr uf023195 2015-07-19 Enable IPv6 only whe... 464/1050/1479 31.4%/71.0% mrr 0.392 avgtime 9715.436
	large2180-tr vf023195 2015-07-19 Enable IPv6 only whe... 474/915/1479 32.0%/61.9% mrr 0.409 avgtime 10182.262

I.e. both much higher performance and much higher recall, plus the
later pipeline stages actually become beneficial.

You need an API key to use the Bing interface.  If you want to just
test on large2180 or curated datasets, you can also download our
pre-populated cache with the respective query results, see below.

**CAVEAT:**  The default YodaQA answer classification models come
pre-trained for a non-Bing scenario.  Check out the d/live branch
to get the models trained for YodaQA+Bing instead.

Obtaining API key
-----------------

Microsoft Bing search API provides 5000 free web search transactions per month.
You can apply for an API key here:

    https://datamarket.azure.com/dataset/bing/search

You must be logged in with your Microsoft account. If you don't have one, you can register here:

    https://signup.live.com/

After you have the account created, click on Sing up button near to 5000 transactions/month text at azure datamarket web page.
No you can get Primary account key in "My account" section.

Using your API key
------------------

Once you have your key generated you need to place it into property file. The application searches for key in

    conf/bingapi.properties

The name of the property is simply "apikey":

	apikey=+gTQThisIsAnExample/kk26UseYourOwnn+znFree8

Once you key the key int he file, the application starts using snippets
from bing search results as an answer source. If the file is not presented or it does not contain proper API key,
the application skips bing search.

Enable bing
-----------

Bing search is disabled by default. You need to set system property "cz.brmlab.yodaqa.use_bing=yes" to enabled it.
You can run yodaqa with this command:

	./gradlew run -q -Dcz.brmlab.yodaqa.use_bing=yes	

If you want to run data/eval/train-and-eval.py you need to pass the parameter as well:

	data/eval/train-and-eval.sh -Dcz.brmlab.yodaqa.use_bing=yes

Caching results
---------------

Because of the limited number of transactions per month, it is good idea to cache results.
The result (query, title, description and url) is saved into Sqlite database:

	~/bingresults-master.db

If no file is found, a new one is created. If you search for query which generates same clues
as some query you have already searched for, the cached results will be used instead of a new search from Bing.

(N.B. This file is called differently from the db in the d/live branch to
prevent accidental usage within the master branch.  FIXME: A more elegant
approach like a .properties conf/ entry...)

Pre-cached Data for the Large2180 Dataset
-----------------------------------------

Results of Bing queries for the large2180 dataset (and therefore also the
curated dataset) are available at:

	http://pasky.or.cz/dev/brmson/bingresults-large2180-980c388.db
