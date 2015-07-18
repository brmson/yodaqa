QA From Web Using Bing
======================

As one of our answer sources, YodaQA can also use web search using the Bing
search engine.  It can improve recall nicely, but has the disadvantage of
producing non-reproducible results as the response to a query can change
any day.

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

The name of the property is simply "apikey". Once you key the key int he file, the application starts using snippets
from bing search results as an answer source. If the file is not presented or it does not contain proper API key,
the application skips bing search.

Caching results
---------------

Because of the limited number of transactions per month, it is good idea to cache results.
The result (query, title, description and url) is saved into Sqlite database. The database file is located in
user's home directory. If no file is found, the new one is created. If you search for query which generates same clues
as some query you have already searched for, the cached results will be used instead of a new search from Bing.
