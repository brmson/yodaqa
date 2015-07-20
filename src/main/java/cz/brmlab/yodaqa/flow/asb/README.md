Multi-Thread Flow for UIMA
==========================

The set of classes in this package provide a multi-threaded flow
of CAS structures for the UIMA system.  Traditionally, UIMA will
process everything in a single thread, taking each CAS available
at that point and passing it in turn through all AEs.  This flow,
within each aggregate AE, is executed by a component called ASB
(Abstract Structure Broker).  The main contribution of this package
is a multi-threaded version of this ASB component.

This module does not improve anything for the baseline case with
simple linear aggregate AEs.  However, it gets advantageous when
you start employing CAS multipliers, getting many CASes that
wait for their turn to flow through the AE piepeline.

Traditionally, UIMA-AS has been used for scale-out and it allows
even network-transparent scale-out on a cluster.  However, its
disadvantage when scaling out on a single machine is that we get
N independent processes executing the pipeline, which means that
if your pipeline loads multi-gigabyte linguistic models, it gets
very memory-expensive.  With StanfordNLP rnn parser + OpenNLP NER,
I cannot fit 8 pipeline processes on my machine with 24GB RAM.
Also, UIMA-AS is non-trivial to deploy due to its client-server
architecture and combining UIMA-AS with UIMAfit seems rather
non-trivial too without rewriting your whole pipeline building code;
on the other hand, this package is meant to be used with UIMAfit.


Usage
-----

To use this in your project, it should be as easy as adding

        ParallelEngineFactory.registerFactory();

call before the execution of your pipeline.  By default #CPUs/2
worker threads will be used (``MultiThreadASB.maxJobs``); you can
also override that with environment variable ``YODAQA_N_THREADS``.
As soon as your flow hits a CAS multiplier, the generated CASes will
start being evaluated in parallel automatically!

### CAS Multipliers

Note that for a CAS multiplier to be able to produce multiple CASes
at once instead of hanging or throwing an exception, you will need
to add an extra method to it:

        @Override
        public int getCasInstancesRequired() {
                return MultiThreadASB.maxJobs * 2;
        }

(see the Caveats section below for some explanation).

### CAS Mergers

By default, each AE will be instantiated multiple times, once per
worker thread (see the MultiprocessingAnalysisEngine_MultiplierOk
description below); sometimes, this is explicitly undesirable,
e.g. in case of CAS mergers (where a *single* AE instance needs
to collect all its CASes), or when relying on some global resources
(like writing data to a single file).  You may pass the parameter

        ParallelEngineFactory.PARAM_NO_MULTIPROCESSING, 1

to such an AE to prevent this behavior.  You must then make usage
of your AE class thread-safe, at least marking its ``initialize``,
``process`` and ``hasNext`` classes as *synchronized*.  (This breaks
the default UIMAj AE API contract!)

Also, typically you would use some *isLast* marker in the last CAS
issued by a multiplier to notify the merger about the end of input.
However, with asynchronous CAS flow there is no guarantee that the
isLast CAS will actually arrive last to the merger - in fact, there
are no CAS ordering guarantees at all!  Modify your multiplier to store
the total number of generated CASes in the *isLast* marker, and make
the merger count the arrived CASes.

### Parallel Steps

N.B. a single CAS may be processed by multiple AEs in parallel
as well - issuing ParallelStep instead of SimpleStep flow commands
to the ASB.  Use this just if you want to process a single input CAS
by several independent CAS multipliers, treating their produced CASes
the same as well.  To generate ParallelSteps, the easiest way is to
group all the concerned CAS multipliers in a single aggregate AE and
set its flow controller to FixedParallelFlowController (in the parent
directory); see its documentation and usage in YodaQA for details.


Structure
---------

This package provides several classes:

  * **MultiThreadASB** uses an application-wide shared thread pool
    of workers to execute Analysis Engines in parallel (either
    on the same CAS in case of ParallelStep, or SimpleStepping
    different waiting CASes), and one thread per CAS and aggregateAE
    that governs the flow of the CAS through that AE asynchronously.

  * **ParallelAnalysisEngine** is a simple modification of stock UIMAj
    AggregateAnalysisEngine which uses MultiThreadASB instead of the
    internal UIMAj ASB_impl to govern the aggregate AE flow.  (Sadly,
    there is no easy way to replace ASB without subclassing.)

  * **ParallelEngineFactory** is a modification of AggregateEngineFactory
    which produces ParallelAnalysisEngine instead of AggregateAnalysisEngine
    instances (i.e. multi-thread executed pipelines) for aggregate AEs
    and automatically wraps the primitive AEs within
    MultiprocessingAnalysisEngine_MultiplierOk.

  * **MultiprocessingAnalysisEngine_MultiplierOk** is a simple modification
    of the stock UIMAj MultiprocessingAnalysisEngine, which is a transparent
    wrapper around PrimitiveAnalysisEngines that creates a pool of AE
    instances to hold the UIMA contract that AEs do not need to be thread-safe
    (each thread will instantiate and use its own AE instance).  However,
    the default UIMAj implementation is racy for CAS multipliers - a different
    thread can check out an AE instance from the pool to process() before that
    instance has been fully next() drained after the last process().


Caveats
-------

  * While AE instances generally do not need to be modified to be thread safe,
    their usage of global resources (like non-read-only static variables or
    external files) must be made thread safe, typically by using monitors
    (code blocks or functions marked as *synchronized*; but of course they must
    be synchronized on something static, not the per-thread AE instance).

  * Some external libraries may not be thread safe.  Always check.  Sometimes
    this is surprising, e.g. OpenNLP NER (even wrapped in DKpro) has bugs.
    Use a wrapper that wraps a shared static (synchronized) annotator instance
    to work around these problems.  (This is less trivial to do than it sounds;
    see cz.brmlab.yodaqa.provider.SyncOpenNlpNameFinder for an example.)

  * ``ParallelEngineFactory.registerFactory();`` is application-wide statement
    and all UIMA pipelines in the application will be affected by that (how
    unfortunate, unfortunately I know no easy way to contextualize this) and
    use the same single shared thread worker pool to run AEs on CASes.
    This *threatens a deadlock* if you use nested UIMA pipelines within AEs.
    A workaround when instantiating such nested pipelines is to replace

        pipeline = AnalysisEngineFactory.createEngine(pipelineDesc);

    with something like

        AnalysisEngineFactory_impl aeFactory = new AnalysisEngineFactory_impl();
        pipeline = (AnalysisEngine) aeFactory.produceResource(AnalysisEngine.class, pipelineDesc, null);

    (see cz.brmlab.yodaqa.analysis.tycor.LATNormalize for an example).

  * ParallelStep should be used only for CAS multipliers which treat their
    input CAS as read-only.  Modifying the CAS passed to ``process()``
    within an AE that is a part of ParallelStep is undefined!  (Multiple
    threads may share the same CAS instance.)

  * When creating new CASes, UIMAj automatically uses so-called "CAS pools"
    to speed things up.  CAS pools have fixed size (by default 1 - sic)
    and no graceful behavior when they run out of CASes - they will either
    hang (waiting for a CAS to return to the pool) or throw an exception.
    But of course our CAS multipliers may need to generate a second CAS
    before the first one gets recycled.  This is the reason for the ugly
    ``getCasInstancesRequired()`` method override.  A proper solution will
    be providing our own, more flexible CasManager that uses dynamically
    sized pool.

  * Continue-on-failure exception handling or timeout functionality are
    probably totally broken at this point.

  * Custom sofa mappings or class loaders are not supported in conjunction
    with ParallelStep, as ParallelStep violates UIMA's assumption that
    a CAS is processed only by a single AE at once.  If you aren't sure,
    you probably do not use these (a little obscure) features.  Also,
    your AEs in ParallelStep must take a base CAS, not a specific view.
    These restrictions are to avoid some race conditions arising deep in
    the UIMA internals (see the MultiThreadASB javadoc).
