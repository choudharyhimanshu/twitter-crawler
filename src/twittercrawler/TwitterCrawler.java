package twittercrawler;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.bson.Document;

import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.util.Util;

import twitter4j.JSONArray;
import twitter4j.JSONException;
import twitter4j.JSONObject;
import twitter4j.PagableResponseList;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;

@Path("/crawl")
public class TwitterCrawler {
	
	private static String OAUTH_CONSUMER_KEY = "XX2eIQUlJ953cycPefn7OnUZC";
    private static String OAUTH_CONSUMER_SECRET = "QoVWLO0mQmdhbSpIIFXCa3YXxabtYcYB3bx5GCOVsTc6WCbEcN";
    private static String OAUTH_TOKEN = "743148529279975424-chaf1gSS0bABtm6rk0hPGro28pFjWrp";
    private static String OAUTH_TOKEN_SECRET = "S8D6IeelSipjlSRv1cJyRvnbXM6uESwbpd3Ih28iW6yzS";
	
    private static int API_LIMIT_TWEETS=180;
    private static int API_LIMIT_FOLLOWERS=15;
    private static int API_LIMIT_FOLLOWING=15;
    
    private static int LIMIT_TWEETS=50;
    private static int LIMIT_FOLLOWERS=50;
    private static int LIMIT_FOLLOWING=50;
    private static boolean ONLY_GEO=true;
    
    private static String DB_HOST="localhost";
    private static int DB_PORT=27017;
    
    private static ConfigurationBuilder cb;
    private static TwitterFactory tf;
    private static Twitter twitter;
    
    private static MongoClient mongoClient;
    private static MongoDatabase db;
    private static MongoCollection<Document> collection_user;
    private static MongoCollection<Document> collection_follows;
    private static MongoCollection<Document> collection_tweets;
    
    private void initializeTwitterObject() throws TwitterException{
    	cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
            .setOAuthConsumerKey(OAUTH_CONSUMER_KEY)
            .setOAuthConsumerSecret(OAUTH_CONSUMER_SECRET)
            .setOAuthAccessToken(OAUTH_TOKEN)
            .setOAuthAccessTokenSecret(OAUTH_TOKEN_SECRET);
        
        tf = new TwitterFactory(cb.build());
        twitter = tf.getInstance();
    }
    
    private void connectDB(){
    	mongoClient = new MongoClient(DB_HOST,DB_PORT);
    	db = mongoClient.getDatabase("twitter-crawler");
    	collection_user = db.getCollection("user");
    	collection_follows = db.getCollection("follows");
    	collection_tweets = db.getCollection("tweets");
    }
    
	@GET
	@Path("/profile/{handle}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getUserProfile(@PathParam("handle")String handle) throws JSONException{
		
		JSONObject response = new JSONObject();
        response.put("success",false);
        response.put("data", JSONObject.NULL);
		
		try {
			initializeTwitterObject();
			connectDB();
		}
		catch (Exception e){
			response.put("message", e.toString());
			return response.toString();
		}		
        
		try {
	        User user = twitter.showUser(handle); // this line
	        if (user.isProtected() == false) {
	            JSONObject user_data = new JSONObject();
	            
	            user_data.put("handle", user.getScreenName());
	            user_data.put("id", user.getId());
	            user_data.put("name", user.getName());
	            user_data.put("description", user.getDescription());
	            user_data.put("photo", user.getBiggerProfileImageURL());
	            user_data.put("location", user.getLocation());
	            user_data.put("is_geo_enabled", user.isGeoEnabled());
	            user_data.put("followers_count",user.getFollowersCount());
	            user_data.put("following_count",user.getFriendsCount());       
	            
	            JSONArray user_followers = new JSONArray();
	            long cursor = -1;
	            int count_api_usage=0;
	            PagableResponseList<User> followers;
	            do {
	            	if(count_api_usage>=API_LIMIT_FOLLOWERS){
	            		break;
	            	}
	                followers = twitter.getFollowersList(handle,cursor,200);
	                count_api_usage++;
	                for (User follower : followers) {
	                	if(ONLY_GEO && follower.getLocation().equals("")){
	                		continue;
	                	}
	                    JSONObject follower_data = new JSONObject();
	                    follower_data.put("handle", follower.getScreenName());
	                    follower_data.put("id", follower.getId());
	                    follower_data.put("name", follower.getName());
	                    follower_data.put("description", follower.getDescription());
	                    follower_data.put("photo", follower.getBiggerProfileImageURL());
	                    follower_data.put("location", follower.getLocation());
	                    follower_data.put("is_geo_enabled", follower.isGeoEnabled());
	                    follower_data.put("followers_count", follower.getFollowersCount());
	                    follower_data.put("following_count", follower.getFriendsCount());
	                    user_followers.put(follower_data);
	                    
	                    try {
	    	            	Document doc = new Document();
	    	            	doc.append("uid", follower_data.getLong("id"));
	    	            	doc.append("handle", follower_data.getString("handle"));
	    	            	doc.append("name", follower_data.getString("name"));
	    	            	doc.append("description", follower_data.getString("description"));
	    	            	doc.append("location", follower_data.getString("location"));
	    	            	doc.append("photo", follower_data.getString("photo"));
	    	            	doc.append("followers_count", follower_data.getInt("followers_count"));
	    	            	doc.append("following_count", follower_data.getInt("following_count"));
	    	            	collection_user.insertOne(doc);
	    	            	
	    	            	doc = new Document();
	    	            	doc.append("fk_uid", follower_data.getLong("id"));
	    	            	doc.append("fk_following_uid", user_data.getLong("id"));
	    	            	collection_follows.insertOne(doc);
	    	            }
	    	            catch (Exception e){
	    	            	response.put("debug", e.toString());     	
	    	            }
	                    
	                    if(user_followers.length() >= LIMIT_FOLLOWERS){
	                    	break;
	                    }
	                }
	            } while ((cursor = followers.getNextCursor()) != 0);
	            user_data.put("followers",user_followers);
	            
	            JSONArray user_followings = new JSONArray();
	            cursor = -1;
	            count_api_usage=0;
	            PagableResponseList<User> followings;
	            do {
	            	if(count_api_usage>=API_LIMIT_FOLLOWING){
	            		break;
	            	}
	            	followings = twitter.getFriendsList(handle,cursor,200);
	            	count_api_usage++;
	                for (User following : followings) {
	                	if(ONLY_GEO && following.getLocation().equals("")){
	                		continue;
	                	}
	                    JSONObject following_data = new JSONObject();
	                    following_data.put("handle", following.getScreenName());
	                    following_data.put("id", following.getId());
	                    following_data.put("name", following.getName());
	                    following_data.put("description", following.getDescription());
	                    following_data.put("photo", following.getBiggerProfileImageURL());
	                    following_data.put("location", following.getLocation());
	                    following_data.put("is_geo_enabled", following.isGeoEnabled());
	                    following_data.put("followers_count", following.getFollowersCount());
	                    following_data.put("following_count", following.getFriendsCount());
	                    user_followings.put(following_data);
	                    
	                    try {
	    	            	Document doc = new Document();
	    	            	doc.append("uid", following_data.getLong("id"));
	    	            	doc.append("handle", following_data.getString("handle"));
	    	            	doc.append("name", following_data.getString("name"));
	    	            	doc.append("description", following_data.getString("description"));
	    	            	doc.append("location", following_data.getString("location"));
	    	            	doc.append("photo", following_data.getString("photo"));
	    	            	doc.append("followers_count", following_data.getInt("followers_count"));
	    	            	doc.append("following_count", following_data.getInt("following_count"));
	    	            	collection_user.insertOne(doc);
	    	            	
	    	            	doc = new Document();
	    	            	doc.append("fk_uid", user_data.getLong("id"));
	    	            	doc.append("fk_following_uid", following_data.getLong("id"));
	    	            	collection_follows.insertOne(doc);
	    	            }
	    	            catch (Exception e){
	    	            	response.put("debug", e.toString());        	
	    	            }	  
	                    
	                    if(user_followings.length() >= LIMIT_FOLLOWING){
	                    	break;
	                    }
	                }
	            } while ((cursor = followings.getNextCursor()) != 0);
	            user_data.put("following",user_followings);
	            
	            response.put("data", user_data);
	            response.put("success", true);
	            response.put("message", "Successfully fetched the data.");
	            
	            try {
	            	Document doc = new Document();
	            	doc.append("uid", user_data.getLong("id"));
	            	doc.append("handle", user_data.getString("handle"));
	            	doc.append("name", user_data.getString("name"));
	            	doc.append("description", user_data.getString("description"));
	            	doc.append("location", user_data.getString("location"));
	            	doc.append("photo", user_data.getString("photo"));
	            	doc.append("followers_count", user_data.getInt("followers_count"));
	            	doc.append("following_count", user_data.getInt("following_count"));
	            	collection_user.insertOne(doc);
	            }
	            catch (Exception e){
	            	response.put("debug", e.toString());        	
	            }
	        } else {
	            // protected account
	            response.put("message", "Protected account");
	        }
		}
		catch (Exception e){
			response.put("message",e.toString());
		}
		mongoClient.close();
        return response.toString();
	}
	
	@GET
	@Path("/tweets/{handle}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getUserTweets(@PathParam("handle")String handle) throws JSONException{
		
		JSONObject response = new JSONObject();
        response.put("success",false);
        response.put("data", JSONObject.NULL);
		
		try {
			initializeTwitterObject();
			connectDB();
		}
		catch (Exception e){
			response.put("message", e.toString());
			return response.toString();
		}
        
		try {
			int curr_page=1;
			JSONArray user_tweets = new JSONArray();
			
			while(true){
				if(curr_page>API_LIMIT_TWEETS){
					break;
				}
				Paging paging = new Paging(curr_page, 200);
				List<Status> statuses = twitter.getUserTimeline(handle,paging);
				if (statuses.size()==0){
					break;
				}
			    for (Status status : statuses) {
			    	Document doc_geo;
			        JSONObject tweet_data = new JSONObject();
			        tweet_data.put("id", status.getId());
			        tweet_data.put("text", status.getText());
			        tweet_data.put("entities", status.getExtendedMediaEntities());
			        tweet_data.put("created_at", status.getCreatedAt());
			        if(status.getGeoLocation() != null){
			        	JSONObject geo_data = new JSONObject();
			        	geo_data.put("lat", status.getGeoLocation().getLatitude());
			        	geo_data.put("lng", status.getGeoLocation().getLongitude());
			        	tweet_data.put("geo", geo_data);
			        	doc_geo = new Document().
			        			append("lat", status.getGeoLocation().getLatitude()).
			        			append("lng", status.getGeoLocation().getLongitude());
			        }
			        else {
			        	tweet_data.put("geo", JSONObject.NULL);
			        	doc_geo = null;
			        }
			        if(status.getPlace() != null){
			        	tweet_data.put("place", status.getPlace().getFullName());
			        }
			        else {
			        	tweet_data.put("place", JSONObject.NULL);
			        }
			        tweet_data.put("retweet_count", status.getRetweetCount());
			        tweet_data.put("is_retweet", status.isRetweet());
			        
			        if(ONLY_GEO && tweet_data.isNull("geo") && tweet_data.isNull("place")){
                		continue;
                	}
			        
			        User user = status.getUser();
			        JSONObject user_data = new JSONObject();
			        user_data.put("id",user.getId());
			        user_data.put("handle",user.getScreenName());
			        user_data.put("name",user.getName());
			        user_data.put("location",user.getLocation());
			        user_data.put("photo",user.getBiggerProfileImageURL());
			        tweet_data.put("user", user_data);
                    
			        user_tweets.put(tweet_data);
			        
			        try {
		            	Document doc = new Document();
		            	doc.append("tid", tweet_data.getLong("id"));
		            	doc.append("text", tweet_data.getString("text"));
		            	doc.append("fk_user_id", user_data.getLong("id"));
		            	doc.append("created_at", tweet_data.getString("created_at"));
		            	doc.append("place", tweet_data.getString("place"));
		            	doc.append("geo", doc_geo);
		            	doc.append("is_retweet", tweet_data.getBoolean("is_retweet"));
		            	doc.append("retweet_count", tweet_data.getInt("retweet_count"));
		            	collection_tweets.insertOne(doc);
		            }
		            catch (Exception e){
		            	response.put("debug", e.toString());        	
		            }
			        if(user_tweets.length()>=LIMIT_TWEETS){
			        	break;
			        }
			    }
			    curr_page++;
			}
			response.put("data",user_tweets);
			response.put("success",true);
			response.put("message","Successfully fetch the tweets.");
		}
		catch (Exception e){
			response.put("message",e.toString());
		}
		mongoClient.close();
        return response.toString();
	}
}
