DBpedia SPARQL Endpoint
=======================

By default, we use the author's personal instance of DBpedia running
on his home computer.  So it follows that it may be offline time by
time.

Alternatively, we could (and had in the past) use the public SPARQL endpoint
at http://dbpedia.org/sparql for our DBpedia queries, however time by time
it would get stuck at HTTP 502 status even for hours.

To set up your own, you need to first download the set of DBpedia data files
and set up a RDF database on top of them.

Download Data Files
===================

	mkdir 3.9
	cd 3.9
	wget http://downloads.dbpedia.org/3.9/dbpedia_3.9.owl.bz2
	wget http://downloads.dbpedia.org/3.9/en/labels_en.nt.bz2
	wget http://downloads.dbpedia.org/3.9/en/redirects_transitive_en.nt.bz2
	wget http://downloads.dbpedia.org/3.9/en/page_ids_en.nt.bz2
	wget http://downloads.dbpedia.org/3.9/en/instance_types_en.nt.bz2
	wget http://downloads.dbpedia.org/3.9/en/instance_types_heuristic_en.nt.bz2
	wget http://downloads.dbpedia.org/3.9/links/wordnet_links.nt.bz2
	bunzip2 *.bz2
	gzip *

Set Up RDF Database
===================

We will use Virtuoso 7.0 as RDF database and SPARQL endpoint.  4store
seems to be in even worse shape wrt. Debian packaging.

	http://joernhees.de/blog/2014/04/23/setting-up-a-local-dbpedia-3-9-mirror-with-virtuoso-7/

seems to be a good reference.

	sudo bash
		wget http://stack.linkeddata.org/ldstable-repository.deb
		dpkg -i ldstable-repository.deb
		apt-get update
		apt-get install virtuoso-opensource-7.0 libvirtodbc0=7.0.1
		# edit /etc/virtuoso-opensource-7.0/virtuoso.ini, add
		# path to the 3.9 directory from above to DirsAllowed
		/etc/init.d/virtuoso-opensource-7.0 restart
		exit
	isql-vt
		ld_dir_all('path to the 3.9 directory from above', '*.*', 'http://dbpedia.org');
		SELECT * FROM DB.DBA.LOAD_LIST;
		rdf_loader_run();
		checkpoint;
		commit WORK;
		checkpoint;
		sparql SELECT COUNT(*) WHERE { ?s ?p ?o } ;
		EXIT;

Now, check ``/var/lib/virtuoso/db/virtuoso.log`` for errors,
and edit ``src/main/java/cz/brmlab/yodaqa/provider/rdf/CachedJenaLookup.java``
changing default value of the ``service`` attribute.  It should work.
