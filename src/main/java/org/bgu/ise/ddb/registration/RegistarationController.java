/**
 * 
 */
package org.bgu.ise.ddb.registration;



import java.io.IOException;
import java.sql.DatabaseMetaData;

import javax.servlet.http.HttpServletResponse;

import org.bgu.ise.ddb.ParentController;
import org.bgu.ise.ddb.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;


import com.mongodb.*;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;


/**
 * @author Alex
 *
 */
@RestController
@RequestMapping(value = "/registration")
public class RegistarationController extends ParentController{
	
	
	/**
	 * The function checks if the username exist,
	 * in case of positive answer HttpStatus in HttpServletResponse should be set to HttpStatus.CONFLICT,
	 * else insert the user to the system  and set to HttpStatus in HttpServletResponse HttpStatus.OK
	 * @param username
	 * @param password
	 * @param firstName
	 * @param lastName
	 * @param response
	 */
	@RequestMapping(value = "register_new_customer", method={RequestMethod.POST})
	public void registerNewUser(@RequestParam("username") String username,
			@RequestParam("password")    String password,
			@RequestParam("firstName")   String firstName,
			@RequestParam("lastName")  String lastName,
			HttpServletResponse response){
		System.out.println(username+" "+password+" "+lastName+" "+firstName);
		HttpStatus status = HttpStatus.CONFLICT;
		
		MongoClient mongoClient = new MongoClient("localhost", 27017);
		
		MongoDatabase db = mongoClient.getDatabase("FinalP");
		
		MongoCollection<Document> usr = db.getCollection("UsersCollection");

		try {
			if (isExistUser(username) == false) {
				status = HttpStatus.OK;
				try {
					Date date = new Date();
					
					Document doc = new Document("username", username);
	                doc.append("password", password);
	                doc.append("firstName", firstName);
	                doc.append("lastName", lastName);
	                doc.append("addTime", date);
					usr.insertOne(doc);
	                
	            } catch (MongoException me) {
	                System.err.println("Unable to insert due to an error: " + me);
	            }
	
			}
		}
		catch(java.io.IOException e) {
			 System.out.println("failed to validate user name:" + e);
		}
		response.setStatus(status.value());
		
	}
	
	/**
	 * The function returns true if the received username exist in the system otherwise false
	 * @param username
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value = "is_exist_user", method={RequestMethod.GET})
	public boolean isExistUser(@RequestParam("username") String username) throws IOException{
		System.out.println(username);
		boolean result = false;
		MongoClient mongoClient = new MongoClient("localhost", 27017);
		MongoDatabase db = mongoClient.getDatabase("FinalP");
		MongoCollection<Document> usr = db.getCollection("UsersCollection");

		Document doc = new Document("username", username);
		MongoCursor<Document> srcRes = usr.find(doc).iterator();
		if (srcRes.hasNext()) {
			result = true;
		}
		return result;
	}
	
	
	/**
	 * The function returns true if the received username and password match a system storage entry, otherwise false
	 * @param username
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value = "validate_user", method={RequestMethod.POST})
	public boolean validateUser(@RequestParam("username") String username,
			@RequestParam("password")    String password) throws IOException{
		System.out.println(username+" "+password);
		boolean result = false;
		Document doc = new Document("username", username);
		
		MongoClient mongoClient = new MongoClient("localhost", 27017);
		MongoDatabase db = mongoClient.getDatabase("FinalP");
		MongoCollection<Document> usr = db.getCollection("UsersCollection");
		
		MongoCursor<Document> srcRes = usr.find(doc).iterator();
		if (srcRes.hasNext()) {
			
			Document res = srcRes.next();
			if(res.get("password").equals(password)) {
				result = true;
			}
		}
		
		return result;
		
	}
	
	/**
	 * The function retrieves number of the registered users in the past n days
	 * @param days
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value = "get_number_of_registred_users", method={RequestMethod.GET})
	public int getNumberOfRegistredUsers(@RequestParam("days") int days) throws IOException{
		System.out.println(days+"");
		int result = 0;
		MongoClient mongoClient = new MongoClient("localhost", 27017);
		MongoDatabase db = mongoClient.getDatabase("FinalP");
		MongoCollection<Document> usr = db.getCollection("UsersCollection");
		
		LocalDateTime temp = LocalDateTime.now().minusDays(days);
		Date startDate = Date.from(temp.atZone(ZoneId.systemDefault()).toInstant());
		
		MongoCursor<Document> srcRes = usr.find().iterator();
		while (srcRes.hasNext()) {
			Document res = srcRes.next();
			Date tmp = (Date) res.get("addTime");
			if(startDate.before(tmp)) {
				result++;
			}
		}

		return result;
	}
	
	/**
	 * The function retrieves all the users
	 * @return
	 */
	@RequestMapping(value = "get_all_users",headers="Accept=*/*", method={RequestMethod.GET},produces="application/json")
	@ResponseBody
	@org.codehaus.jackson.map.annotate.JsonView(User.class)
	public  User[] getAllUsers(){
		
		MongoClient mongoClient = new MongoClient("localhost", 27017);
		MongoDatabase db = mongoClient.getDatabase("FinalP");
		MongoCollection<Document> usr = db.getCollection("UsersCollection");
		
		MongoCursor<Document> srcRes = usr.find().iterator();
		int size = 0;
		while (srcRes.hasNext()) {
			Document res = srcRes.next();
			size++;
		}
		
		int i = 0;
		User[] users = new User[size];
		srcRes = usr.find().iterator();
		while (srcRes.hasNext()) {
			Document res = srcRes.next();
			User u = new User((String)res.get("username"), (String)res.get("firstName"), (String)res.get("lastName"));
			users[i] = u;
			i++;
		}		
		
		return users;
	}

}
