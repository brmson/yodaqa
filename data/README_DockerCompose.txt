To setup Yoda in Docker some preparation is needed to create the data files and afterwards there are two options: Pull prebuilt images from Docker Hub or build them yourself.

.:: Preparation ::.

Follow the READMEs in YodaQA's data directory to setup the individual backends. This should yield files for the label services, DBpedia, Freebase and Solr. To run YodaQA in Docker, you need to map those files as volumes into the containers. After the preparation, you need to have a directory called "data" with the following subdirectories:

.:: Subdirectory d-freebase
GOSP.dat  GSPO.idn      OSP.dat   POSG.dat       prefixes.dat   SPOG.idn
GOSP.idn  journal.jrnl  OSPG.dat  POSG.idn       prefixIdx.dat  SPO.idn
GPOS.dat  node2id.dat   OSPG.idn  POS.idn        prefixIdx.idn  stats.opt
GPOS.idn  node2id.idn   OSP.idn   prefix2id.dat  SPO.dat        tdb.lock
GSPO.dat  nodes.dat     POS.dat   prefix2id.idn  SPOG.dat

.:: Subdirectory enwiki
enwiki
└── collection1
    ├── conf
    │   ├── admin-extra.html
    │   ├── admin-extra.menu-bottom.html
    │   ├── admin-extra.menu-top.html
    │   ├── clustering
    │   │   └── carrot2
    │   │       ├── kmeans-attributes.xml
    │   │       ├── lingo-attributes.xml
    │   │       └── stc-attributes.xml
    │   ├── currency.xml
    │   ├── data-config.xml
    │   ├── data-config.xml~
    │   ├── dataimport.properties
    │   ├── elevate.xml
    │   ├── lang
    │   │   ├── contractions_ca.txt
    │   │   ├── contractions_fr.txt
    │   │   ├── contractions_ga.txt
    │   │   ├── contractions_it.txt
    │   │   ├── hyphenations_ga.txt
    │   │   ├── stemdict_nl.txt
    │   │   ├── stoptags_ja.txt
    │   │   ├── stopwords_ar.txt
    │   │   ├── stopwords_bg.txt
    │   │   ├── stopwords_ca.txt
    │   │   ├── stopwords_cz.txt
    │   │   ├── stopwords_da.txt
    │   │   ├── stopwords_de.txt
    │   │   ├── stopwords_el.txt
    │   │   ├── stopwords_en.txt
    │   │   ├── stopwords_es.txt
    │   │   ├── stopwords_eu.txt
    │   │   ├── stopwords_fa.txt
    │   │   ├── stopwords_fi.txt
    │   │   ├── stopwords_fr.txt
    │   │   ├── stopwords_ga.txt
    │   │   ├── stopwords_gl.txt
    │   │   ├── stopwords_hi.txt
    │   │   ├── stopwords_hu.txt
    │   │   ├── stopwords_hy.txt
    │   │   ├── stopwords_id.txt
    │   │   ├── stopwords_it.txt
    │   │   ├── stopwords_ja.txt
    │   │   ├── stopwords_lv.txt
    │   │   ├── stopwords_nl.txt
    │   │   ├── stopwords_no.txt
    │   │   ├── stopwords_pt.txt
    │   │   ├── stopwords_ro.txt
    │   │   ├── stopwords_ru.txt
    │   │   ├── stopwords_sv.txt
    │   │   ├── stopwords_th.txt
    │   │   ├── stopwords_tr.txt
    │   │   └── userdict_ja.txt
    │   ├── mapping-FoldToASCII.txt
    │   ├── mapping-ISOLatin1Accent.txt
    │   ├── protwords.txt
    │   ├── schema.xml
    │   ├── scripts.conf
    │   ├── solrconfig.xml
    │   ├── spellings.txt
    │   ├── stopwords.txt
    │   ├── synonyms.txt
    │   ├── update-script.js
    │   ├── velocity
    │   │   ├── browse.vm
    │   │   ├── cluster_results.vm
    │   │   ├── cluster.vm
    │   │   ├── debug.vm
    │   │   ├── did_you_mean.vm
    │   │   ├── error.vm
    │   │   ├── facet_fields.vm
    │   │   ├── facet_pivot.vm
    │   │   ├── facet_queries.vm
    │   │   ├── facet_ranges.vm
    │   │   ├── facets.vm
    │   │   ├── footer.vm
    │   │   ├── header.vm
    │   │   ├── head.vm
    │   │   ├── hit_grouped.vm
    │   │   ├── hit_plain.vm
    │   │   ├── hit.vm
    │   │   ├── join_doc.vm
    │   │   ├── jquery.autocomplete.css
    │   │   ├── jquery.autocomplete.js
    │   │   ├── layout.vm
    │   │   ├── main.css
    │   │   ├── mime_type_lists.vm
    │   │   ├── pagination_bottom.vm
    │   │   ├── pagination_top.vm
    │   │   ├── product_doc.vm
    │   │   ├── query_form.vm
    │   │   ├── query_group.vm
    │   │   ├── query_spatial.vm
    │   │   ├── query.vm
    │   │   ├── README.txt
    │   │   ├── results_list.vm
    │   │   ├── richtext_doc.vm
    │   │   ├── suggest.vm
    │   │   ├── tabs.vm
    │   │   └── VM_global_library.vm
    │   └── xslt
    │       ├── example_atom.xsl
    │       ├── example_rss.xsl
    │       ├── example.xsl
    │       ├── luke.xsl
    │       └── updateXml.xsl
    ├── core.properties
    ├── data
    │   ├── index
    │   │   ├── _7l.fdt
    │   │   ├── _7l.fdx
    │   │   ├── _7l.fnm
    │   │   ├── _7l_Lucene41_0.doc
    │   │   ├── _7l_Lucene41_0.pos
    │   │   ├── _7l_Lucene41_0.tim
    │   │   ├── _7l_Lucene41_0.tip
    │   │   ├── _7l.nvd
    │   │   ├── _7l.nvm
    │   │   ├── _7l.si
    │   │   ├── _ay.fdt
    │   │   ├── _ay.fdx
    │   │   ├── _ay.fnm
    │   │   ├── _ay_Lucene41_0.doc
    │   │   ├── _ay_Lucene41_0.pos
    │   │   ├── _ay_Lucene41_0.tim
    │   │   ├── _ay_Lucene41_0.tip
    │   │   ├── _ay.nvd
    │   │   ├── _ay.nvm
    │   │   ├── _ay.si
    │   │   ├── _bi.fdt
    │   │   ├── _bi.fdx
    │   │   ├── _bi.fnm
    │   │   ├── _bi_Lucene41_0.doc
    │   │   ├── _bi_Lucene41_0.pos
    │   │   ├── _bi_Lucene41_0.tim
    │   │   ├── _bi_Lucene41_0.tip
    │   │   ├── _bi.nvd
    │   │   ├── _bi.nvm
    │   │   ├── _bi.si
    │   │   ├── _bs.fdt
    │   │   ├── _bs.fdx
    │   │   ├── _bs.fnm
    │   │   ├── _bs_Lucene41_0.doc
    │   │   ├── _bs_Lucene41_0.pos
    │   │   ├── _bs_Lucene41_0.tim
    │   │   ├── _bs_Lucene41_0.tip
    │   │   ├── _bs.nvd
    │   │   ├── _bs.nvm
    │   │   ├── _bs.si
    │   │   ├── _bv.fdt
    │   │   ├── _bv.fdx
    │   │   ├── _bv.fnm
    │   │   ├── _bv_Lucene41_0.doc
    │   │   ├── _bv_Lucene41_0.pos
    │   │   ├── _bv_Lucene41_0.tim
    │   │   ├── _bv_Lucene41_0.tip
    │   │   ├── _bv.nvd
    │   │   ├── _bv.nvm
    │   │   ├── _bv.si
    │   │   ├── _bw.fdt
    │   │   ├── _bw.fdx
    │   │   ├── _bw.fnm
    │   │   ├── _bw_Lucene41_0.doc
    │   │   ├── _bw_Lucene41_0.pos
    │   │   ├── _bw_Lucene41_0.tim
    │   │   ├── _bw_Lucene41_0.tip
    │   │   ├── _bw.nvd
    │   │   ├── _bw.nvm
    │   │   ├── _bw.si
    │   │   ├── _bx.fdt
    │   │   ├── _bx.fdx
    │   │   ├── _bx.fnm
    │   │   ├── _bx_Lucene41_0.doc
    │   │   ├── _bx_Lucene41_0.pos
    │   │   ├── _bx_Lucene41_0.tim
    │   │   ├── _bx_Lucene41_0.tip
    │   │   ├── _bx.nvd
    │   │   ├── _bx.nvm
    │   │   ├── _bx.si
    │   │   ├── _bz.fdt
    │   │   ├── _bz.fdx
    │   │   ├── _bz.fnm
    │   │   ├── _bz_Lucene41_0.doc
    │   │   ├── _bz_Lucene41_0.pos
    │   │   ├── _bz_Lucene41_0.tim
    │   │   ├── _bz_Lucene41_0.tip
    │   │   ├── _bz.nvd
    │   │   ├── _bz.nvm
    │   │   ├── _bz.si
    │   │   ├── _c2.fdt
    │   │   ├── _c2.fdx
    │   │   ├── _c2.fnm
    │   │   ├── _c2_Lucene41_0.doc
    │   │   ├── _c2_Lucene41_0.pos
    │   │   ├── _c2_Lucene41_0.tim
    │   │   ├── _c2_Lucene41_0.tip
    │   │   ├── _c2.nvd
    │   │   ├── _c2.nvm
    │   │   ├── _c2.si
    │   │   ├── _c3.fdt
    │   │   ├── _c3.fdx
    │   │   ├── _c3.fnm
    │   │   ├── _c3_Lucene41_0.doc
    │   │   ├── _c3_Lucene41_0.pos
    │   │   ├── _c3_Lucene41_0.tim
    │   │   ├── _c3_Lucene41_0.tip
    │   │   ├── _c3.nvd
    │   │   ├── _c3.nvm
    │   │   ├── _c3.si
    │   │   ├── _c4.fdt
    │   │   ├── _c4.fdx
    │   │   ├── _c4.fnm
    │   │   ├── _c4_Lucene41_0.doc
    │   │   ├── _c4_Lucene41_0.pos
    │   │   ├── _c4_Lucene41_0.tim
    │   │   ├── _c4_Lucene41_0.tip
    │   │   ├── _c4.nvd
    │   │   ├── _c4.nvm
    │   │   ├── _c4.si
    │   │   ├── _c6.fdt
    │   │   ├── _c6.fdx
    │   │   ├── _c6.fnm
    │   │   ├── _c6_Lucene41_0.doc
    │   │   ├── _c6_Lucene41_0.pos
    │   │   ├── _c6_Lucene41_0.tim
    │   │   ├── _c6_Lucene41_0.tip
    │   │   ├── _c6.nvd
    │   │   ├── _c6.nvm
    │   │   ├── _c6.si
    │   │   ├── _c7.fdt
    │   │   ├── _c7.fdx
    │   │   ├── _c7.fnm
    │   │   ├── _c7_Lucene41_0.doc
    │   │   ├── _c7_Lucene41_0.pos
    │   │   ├── _c7_Lucene41_0.tim
    │   │   ├── _c7_Lucene41_0.tip
    │   │   ├── _c7.nvd
    │   │   ├── _c7.nvm
    │   │   ├── _c7.si
    │   │   ├── _c8.fdt
    │   │   ├── _c8.fdx
    │   │   ├── _c8.fnm
    │   │   ├── _c8_Lucene41_0.doc
    │   │   ├── _c8_Lucene41_0.pos
    │   │   ├── _c8_Lucene41_0.tim
    │   │   ├── _c8_Lucene41_0.tip
    │   │   ├── _c8.nvd
    │   │   ├── _c8.nvm
    │   │   ├── _c8.si
    │   │   ├── _c9.fdt
    │   │   ├── _c9.fdx
    │   │   ├── _c9.fnm
    │   │   ├── _c9_Lucene41_0.doc
    │   │   ├── _c9_Lucene41_0.pos
    │   │   ├── _c9_Lucene41_0.tim
    │   │   ├── _c9_Lucene41_0.tip
    │   │   ├── _c9.nvd
    │   │   ├── _c9.nvm
    │   │   ├── _c9.si
    │   │   ├── _cc.fdt
    │   │   ├── _cc.fdx
    │   │   ├── _cc.fnm
    │   │   ├── _cc_Lucene41_0.doc
    │   │   ├── _cc_Lucene41_0.pos
    │   │   ├── _cc_Lucene41_0.tim
    │   │   ├── _cc_Lucene41_0.tip
    │   │   ├── _cc.nvd
    │   │   ├── _cc.nvm
    │   │   ├── _cc.si
    │   │   ├── _ce.fdt
    │   │   ├── _ce.fdx
    │   │   ├── _ce.fnm
    │   │   ├── _ce_Lucene41_0.doc
    │   │   ├── _ce_Lucene41_0.pos
    │   │   ├── _ce_Lucene41_0.tim
    │   │   ├── _ce_Lucene41_0.tip
    │   │   ├── _ce.nvd
    │   │   ├── _ce.nvm
    │   │   ├── _ce.si
    │   │   ├── _cf.fdt
    │   │   ├── _cf.fdx
    │   │   ├── _cf.fnm
    │   │   ├── _cf_Lucene41_0.doc
    │   │   ├── _cf_Lucene41_0.pos
    │   │   ├── _cf_Lucene41_0.tim
    │   │   ├── _cf_Lucene41_0.tip
    │   │   ├── _cf.nvd
    │   │   ├── _cf.nvm
    │   │   ├── _cf.si
    │   │   ├── _cg.fdt
    │   │   ├── _cg.fdx
    │   │   ├── _cg.fnm
    │   │   ├── _cg_Lucene41_0.doc
    │   │   ├── _cg_Lucene41_0.pos
    │   │   ├── _cg_Lucene41_0.tim
    │   │   ├── _cg_Lucene41_0.tip
    │   │   ├── _cg.nvd
    │   │   ├── _cg.nvm
    │   │   ├── _cg.si
    │   │   ├── _ch.fdt
    │   │   ├── _ch.fdx
    │   │   ├── _ch.fnm
    │   │   ├── _ch_Lucene41_0.doc
    │   │   ├── _ch_Lucene41_0.pos
    │   │   ├── _ch_Lucene41_0.tim
    │   │   ├── _ch_Lucene41_0.tip
    │   │   ├── _ch.nvd
    │   │   ├── _ch.nvm
    │   │   ├── _ch.si
    │   │   ├── _ci.fdt
    │   │   ├── _ci.fdx
    │   │   ├── _ci.fnm
    │   │   ├── _ci_Lucene41_0.doc
    │   │   ├── _ci_Lucene41_0.pos
    │   │   ├── _ci_Lucene41_0.tim
    │   │   ├── _ci_Lucene41_0.tip
    │   │   ├── _ci.nvd
    │   │   ├── _ci.nvm
    │   │   ├── _ci.si
    │   │   ├── _cj.fdt
    │   │   ├── _cj.fdx
    │   │   ├── _cj.fnm
    │   │   ├── _cj_Lucene41_0.doc
    │   │   ├── _cj_Lucene41_0.pos
    │   │   ├── _cj_Lucene41_0.tim
    │   │   ├── _cj_Lucene41_0.tip
    │   │   ├── _cj.nvd
    │   │   ├── _cj.nvm
    │   │   ├── _cj.si
    │   │   ├── _ck.fdt
    │   │   ├── _ck.fdx
    │   │   ├── _ck.fnm
    │   │   ├── _ck_Lucene41_0.doc
    │   │   ├── _ck_Lucene41_0.pos
    │   │   ├── _ck_Lucene41_0.tim
    │   │   ├── _ck_Lucene41_0.tip
    │   │   ├── _ck.nvd
    │   │   ├── _ck.nvm
    │   │   ├── _ck.si
    │   │   ├── _cl.fdt
    │   │   ├── _cl.fdx
    │   │   ├── _cl.fnm
    │   │   ├── _cl_Lucene41_0.doc
    │   │   ├── _cl_Lucene41_0.pos
    │   │   ├── _cl_Lucene41_0.tim
    │   │   ├── _cl_Lucene41_0.tip
    │   │   ├── _cl.nvd
    │   │   ├── _cl.nvm
    │   │   ├── _cl.si
    │   │   ├── _cm.fdt
    │   │   ├── _cm.fdx
    │   │   ├── _cm.fnm
    │   │   ├── _cm_Lucene41_0.doc
    │   │   ├── _cm_Lucene41_0.pos
    │   │   ├── _cm_Lucene41_0.tim
    │   │   ├── _cm_Lucene41_0.tip
    │   │   ├── _cm.nvd
    │   │   ├── _cm.nvm
    │   │   ├── _cm.si
    │   │   ├── _cn.fdt
    │   │   ├── _cn.fdx
    │   │   ├── _cn.fnm
    │   │   ├── _cn_Lucene41_0.doc
    │   │   ├── _cn_Lucene41_0.pos
    │   │   ├── _cn_Lucene41_0.tim
    │   │   ├── _cn_Lucene41_0.tip
    │   │   ├── _cn.nvd
    │   │   ├── _cn.nvm
    │   │   ├── _cn.si
    │   │   ├── segments_5t
    │   │   └── segments.gen
    │   └── tlog
    │       └── tlog.0000000000000000206
    └── README.txt

.:: Subdirectory labels
labels.db  sorted_list.dat

.:: Subdirectory db
GOSP.dat  GSPO.idn      OSP.dat   POSG.dat       prefixes.dat   SPOG.idn
GOSP.idn  journal.jrnl  OSPG.dat  POSG.idn       prefixIdx.dat  SPO.idn
GPOS.dat  node2id.dat   OSPG.idn  POS.idn        prefixIdx.idn  stats.opt
GPOS.idn  node2id.idn   OSP.idn   prefix2id.dat  SPO.dat        tdb.lock
GSPO.dat  nodes.dat     POS.dat   prefix2id.idn  SPOG.dat

Remember to run chcon -Rt svirt_sandbox_file_t <path_to_volume> on SELinux systems for every directory you want to map as a volume as described in the Dockerfiles, since otherwise the paths won't be mounted correctly.

Now about the two ways to orchestrate the containers:

a) Just go into the directory with the docker-compose.yml file, change the <path> entries so they are the absolute path to your data directory created in the previous step and run docker-compose up (as root).
This should download the Docker images from Docker Hub and run them.

b) Build the Docker containers yourself. For this, you need to look at the instructions again and this time, instead of the data files you only need the created application part. For instance, for Fuseki you only need the jena-fuseki-1.1.1 directory. Then run the docker build commands, e.g. docker build -t fuseki .

Since the two label services as well as Freebase and DBpedia can share one image, you need to repeat this 4 times: For YodaQA itself, for the fuseki image, for labels and for solr. Then edit the docker-compose.yml file, change the <path> entries so they are the absolute path to your data directory created in the previous step, remove the k0105/ prefix and run docker-compose up (as root) in the same directory docker-compose.yml is located in. This will also launch all containers.


.:: Further notes:
- Why weren't the data files provided as download? Because they are 320 GB in size.

- Try to place the data directory on an SSD. This will accelerate Yoda significantly (factor 2.5 to 3).

- Once you have built the data backends, you should backup the results - they can be reused and will not have to be generated again, which is esp. helpful for Freebase.
