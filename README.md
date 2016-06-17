
### What is this repository for? ###

* REST API to fetch data from twitter.
* Version 1.0

### How do I get set up? ###

* Install Java JDK
* Install Apache Tomcat
* Set global variables for ACCESS_TOKEN & CONSUMER_KEY to your own in the TwitterCrawler.java file and then recomplile
* All the required libraries are already added, so just place the repository's 'WebContent' directory into Tomcat's webapps directory and replace the TwitterCralwer.class file with your own compiled file
* Now start the tomcat server
* Read the API Docs section to know how to use the API

### API Docs ###

Currently there are two APIs available.

1. User Profile: To use this API, open your browser and go to following link,

```
http://localhost:8080/twitter-crawler/crawl/profile/*HANDLE*

```
2. Tweets Profile: To use this API, open your browser and go to following link,

```
http://localhost:8080/twitter-crawler/crawl/tweets/*HANDLE*

```

### DEMO ###

[http://crawl-twitter.herokuapp.com/crawl/profile/hc12298](http://crawl-twitter.herokuapp.com/crawl/profile/hc12298)

[http://crawl-twitter.herokuapp.com/crawl/tweets/hc12298](http://crawl-twitter.herokuapp.com/crawl/tweets/hc12298)

P.S.: Demo may not work due to usage limit of Twitter APIs.

### Who do I talk to? ###

* Himanshu Choudhary
* Email: himanshuchoudhary@live.com
* Homepage: http://www.himanshuchoudhary.com