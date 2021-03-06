# Birdwatch using Clojure, ClojureScript and Om
This is an **all-Clojure** implementation of the **BirdWatch** application.

## Building blocks:
A series of detailed articles will follow soon. Here is an overview of the building blocks:

* The server side is organized as a few independent **components** that are wired together by **[@stuartsierra](https://twitter.com/stuartsierra)**'s **[component](https://github.com/stuartsierra/component)** library.
* A twitter client on top of **Adam Wynne**'s **[twitter-api](https://github.com/adamwynne/twitter-api)** connects to the **[Twitter Streaming API](https://dev.twitter.com/streaming/overview)** and subscribes to a number of terms.
* The *information grid* or pipes and tubes of the application is provided by **[core.async](https://github.com/clojure/core.async)**.
* Tweets are persisted into **[ElasticSearch](http://www.elasticsearch.org)** using **[@ClojureWerkz](https://twitter.com/clojurewerkz)** **[Elastisch](https://github.com/clojurewerkz/elastisch)** client.
* Clients connect over **[WebSockets](http://en.wikipedia.org/wiki/WebSocket)** provided by **[@ptaoussanis](https://twitter.com/ptaoussanis)** **[sente](https://github.com/ptaoussanis/sente)** library.
* Clients can perform **live searches**, where matching new tweets with running searches is done via ElasticSearch's **[Percolator](http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-percolate.html)** feature. The client UI is then updated in (near)-real time over the existing WebSockets connection.
* The client UI is built using **[@swannodette](https://twitter.com/swannodette)**'s **[Om](https://github.com/swannodette/om)** library on top of Facebook's **[react](https://github.com/facebook/react)**.

## Installation
First of all, **[Leiningen](http://leiningen.org)** must be installed on your system. Then, you need to build the client-side application:

    lein cljsbuild once release
    lein cljsbuild once dev

Alternatively, you can use the following during development to detect file system changes and recompile MUCH FASTER:

    lein cljsbuild auto release
    lein cljsbuild auto dev

Then you will need to create a file named twitterconf.edn. You can copy **twitterconf-tpl.edn** as a template. Edit it and fill out the credentials you have obtained from dev.twitter.com:

      { :consumer-key             "<YOUR API KEY HERE>"
        :consumer-secret          "<YOUR API SECRET HERE>"
        :user-access-token        "<YOUR ACCESS TOKEN HERE>"
        :user-access-token-secret "<YOUR ACCESS TOKEN SECRET HERE>"
        :es-address               "http://127.0.0.1:9200"
        :es-native-address        ["127.0.0.1" 9300]
        :es-cluster-name          "<YOUR CLUSTER NAME HERE>"
        :es-index                 "birdwatch"
        :track                    "clojure,love"
        :tw-check-interval-sec    120
        :port                     8888
        :pidfile-name             "birdwatch.pid"}

In the twitterconf.edn you can also specify the address under which your installation of **[ElasticSearch](http://www.elasticsearch.org)** is accessible, the index to use in ElasticSearch and which terms to track from Twitter. Previous tweets are retrieved using a native ElasticSearch client (for performance reasons). In case you don't know your cluster name, for example because you are just running a local instance on your machine, here's how you can find it out:

    curl -XGET 'http://localhost:9200/_cluster/health?pretty=true'

You also need **[Bower](http://bower.io)** for managing the client-side dependencies. Once you have it installed, all you need to do is run it once:

    bower install

## Usage
Once the steps described above are completed, usage is easy. You can start up the application like this:

    lein run

Alternatively, you can use the REPL:

    lein repl
    birdwatch.main=> (alter-var-root #'system component/start)

## License
Copyright © 2014 Matthias Nehlsen. Distributed under the Apache License, Version 2.0. See separate LICENSE file.
