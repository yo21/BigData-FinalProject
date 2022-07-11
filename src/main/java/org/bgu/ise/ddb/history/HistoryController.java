/**
 * 
 */
package org.bgu.ise.ddb.history;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.bgu.ise.ddb.ParentController;
import org.bgu.ise.ddb.User;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;

/**
 * @author Alex
 *
 */
@RestController
@RequestMapping(value = "/history")
public class HistoryController extends ParentController{
	
	
	
	/**
	 * The function inserts to the system storage triple(s)(username, title, timestamp). 
	 * The timestamp - in ms since 1970
	 * Advice: better to insert the history into two structures( tables) in order to extract it fast one with the key - username, another with the key - title
	 * @param username
	 * @param title
	 * @param response
	 */
	@RequestMapping(value = "insert_to_history", method={RequestMethod.GET})
	public void insertToHistory (@RequestParam("username")    String username,
			@RequestParam("title")   String title,
			HttpServletResponse response){
		System.out.println(username+" "+title);
		//:TODO your implementation
		HttpStatus status = HttpStatus.CONFLICT;

		MongoClient mongoClient = new MongoClient("localhost", 27017);
		MongoDatabase db = mongoClient.getDatabase("FinalP");
		MongoCollection<Document> userH = db.getCollection("UsersHistory");
		MongoCollection<Document> titleH = db.getCollection("TitleHistory");
		MongoCollection<Document> usersCo = db.getCollection("UsersCollection");

		Document tempDoc = new Document("username", username);
		MongoCursor<Document> srcRes = usersCo.find(tempDoc).iterator();
		if(!srcRes.hasNext()) {
			response.setStatus(status.value());
			mongoClient.close();
			System.out.println("\nNO USERNAME FOUND!\n");
			return;
		}
		int flag = 0;
		
			
		try {
//			insert to title indexed collection
			Date date = new Date();
		    long timeMilli = date.getTime();
		    
		    
		    Document doc2 = new Document("username", username);
            doc2.append("timestamp", timeMilli);
            List<Document> history = new ArrayList<Document>();
            history.add(doc2);
			Document doc = new Document("history", history);
           
			
            Document query = new Document("_id", title);
            UpdateOptions options = new UpdateOptions().upsert(true);

            titleH.updateOne(query, new Document("$push",doc), options);

            flag++;
            
//			insert to user indexed collection
		    
		    doc2 = new Document("title", title);
            doc2.append("timestamp", timeMilli);
            history = new ArrayList<Document>();
            history.add(doc2);
			doc = new Document("history", history);
           
			
            query = new Document("_id", username);
            options = new UpdateOptions().upsert(true);

            userH.updateOne(query, new Document("$push",doc), options);

            flag++;
		}
		catch (MongoException me) {
            System.err.println("Unable to insert due to an error: " + me);
		}
		//TODO change flag to 2 
		if(flag == 2) {
			status = HttpStatus.OK;
		}
		mongoClient.close();
		response.setStatus(status.value());

	}
	
	
	/**
	 * The function retrieves  users' history
	 * The function return array of pairs <title,viewtime> sorted by VIEWTIME in descending order
	 * @param username
	 * @return
	 */
	@RequestMapping(value = "get_history_by_users",headers="Accept=*/*", method={RequestMethod.GET},produces="application/json")
	@ResponseBody
	@org.codehaus.jackson.map.annotate.JsonView(HistoryPair.class)
	public  HistoryPair[] getHistoryByUser(@RequestParam("entity")    String username){
		//:TODO your implementation
		MongoClient mongoClient = new MongoClient("localhost", 27017);
		MongoDatabase db = mongoClient.getDatabase("FinalP");
		MongoCollection<Document> userH = db.getCollection("UsersHistory");
        Document query = new Document("_id", username);

        AggregateIterable<Document> findIterable = userH.aggregate(
                Arrays.asList(
                        new Document("$match", query),
                        new Document("$unwind", "$history"),
                        new Document("$sort", new Document("history.timestamp", -1))
                    )   
            );
        int j = 0;
        System.out.println("\n\nHello\n\n");
        System.out.println(findIterable.first().toJson());
        System.out.println("Hello\n\n");

        for(Document d : findIterable)
        {

        	@SuppressWarnings("unchecked")
			ArrayList<Document> tmp = (ArrayList<Document>) d.get("history");
            for (Document i : tmp)
            {
                System.out.println(i.toJson());
            	j++;
            }
        }

        HistoryPair[] res = new HistoryPair[j];
        j = 0;
        for(Document d : findIterable)
        {

        	@SuppressWarnings("unchecked")
			ArrayList<Document> tmp = (ArrayList<Document>) d.get("history");
            for (Document i : tmp)
            {
            	HistoryPair hp = new HistoryPair((String)i.get("title"), new Date((long)i.get("timestamp")));
            	res[j] = hp;
            	j++;
            }
        }
        mongoClient.close();
		return res;
	}
	
	
	/**
	 * The function retrieves  items' history
	 * The function return array of pairs <username,viewtime> sorted by VIEWTIME in descending order
	 * @param title
	 * @return
	 */
	@RequestMapping(value = "get_history_by_items",headers="Accept=*/*", method={RequestMethod.GET},produces="application/json")
	@ResponseBody
	@org.codehaus.jackson.map.annotate.JsonView(HistoryPair.class)
	public  HistoryPair[] getHistoryByItems(@RequestParam("entity")    String title){
		//:TODO your implementation
		MongoClient mongoClient = new MongoClient("localhost", 27017);
		MongoDatabase db = mongoClient.getDatabase("FinalP");
		MongoCollection<Document> titleH = db.getCollection("TitleHistory");
        Document query = new Document("_id", title);

        AggregateIterable<Document> findIterable = titleH.aggregate(
                Arrays.asList(
                        new Document("$match", query),
                        new Document("$unwind", "$history"),
                        new Document("$sort", new Document("history.timestamp", -1))
                    )   
            );
        int j = 0;

        for(Document d : findIterable)
        {

        	@SuppressWarnings("unchecked")
			ArrayList<Document> tmp = (ArrayList<Document>) d.get("history");
            for (Document i : tmp)
            {
            	j++;
            }
        }

        HistoryPair[] res = new HistoryPair[j];
        j = 0;
        for(Document d : findIterable)
        {

        	@SuppressWarnings("unchecked")
			ArrayList<Document> tmp = (ArrayList<Document>) d.get("history");
            for (Document i : tmp)
            {
            	HistoryPair hp = new HistoryPair((String)i.get("username"), new Date((long)i.get("timestamp")));
            	res[j] = hp;
            	j++;
            }
        }
        mongoClient.close();
		return res;
	}
	
	/**
	 * The function retrieves all the  users that have viewed the given item
	 * @param title
	 * @return
	 */
	@RequestMapping(value = "get_users_by_item",headers="Accept=*/*", method={RequestMethod.GET},produces="application/json")
	@ResponseBody
	@org.codehaus.jackson.map.annotate.JsonView(HistoryPair.class)
	public  User[] getUsersByItem(@RequestParam("title") String title){
		//:TODO your implementation
		MongoClient mongoClient = new MongoClient("localhost", 27017);
		MongoDatabase db = mongoClient.getDatabase("FinalP");
		MongoCollection<Document> titleH = db.getCollection("TitleHistory");
		MongoCollection<Document> usersCo = db.getCollection("UsersCollection");

        Document query = new Document("_id", title);

        AggregateIterable<Document> findIterable = titleH.aggregate(
                Arrays.asList(
                        new Document("$match", query),
                        new Document("$unwind", "$history")
                    )   
            );

        Set<String> set = new HashSet<String> (); 

        for(Document d : findIterable)
        {
        	@SuppressWarnings("unchecked")
			ArrayList<Document> tmp = (ArrayList<Document>) d.get("history");
            for (Document i : tmp)
            {
            	set.add(i.getString("username"));
            }
        }
        
        User[] res = new User[set.size()];
        int j = 0;
        for(String username : set)
        {
        	Document doc = new Document("username", username);
    		MongoCursor<Document> srcRes = usersCo.find(doc).iterator();
			Document tmp = srcRes.next();
			User hp = new User(username,(String)tmp.get("firstName"),(String)tmp.get("lastName"));
			res[j] = hp;
			j++;
        }
        
        mongoClient.close();
		return res;

	}
	
	/**
	 * The function calculates the similarity score using Jaccard similarity function:
	 *  sim(i,j) = |U(i) intersection U(j)|/|U(i) union U(j)|,
	 *  where U(i) is the set of usernames which exist in the history of the item i.
	 * @param title1
	 * @param title2
	 * @return
	 */
	@RequestMapping(value = "get_items_similarity",headers="Accept=*/*", method={RequestMethod.GET},produces="application/json")
	@ResponseBody
	public double  getItemsSimilarity(@RequestParam("title1") String title1,
			@RequestParam("title2") String title2){
		//:TODO your implementation
        int j = 0;
        for(User i : getUsersByItem(title1)) {
        	for(User k : getUsersByItem(title2)) {      		
        		if(i.getUsername().equals(k.getUsername())) {
        			j++;
        		}
        	}
        }
        	
		double ret = (double)j/(getUsersByItem(title1).length+getUsersByItem(title2).length);
		return ret;
	}
}
