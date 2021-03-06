package com.app.server.services;

import com.app.server.http.exceptions.APPBadRequestException;
import com.app.server.http.exceptions.APPInternalServerException;
import com.app.server.http.exceptions.APPUnauthorizedException;
import com.app.server.models.Event.Event;
import com.app.server.models.Preferences.Ailment;
import com.app.server.models.Preferences.Habit;
import com.app.server.models.Preferences.Interest;
import com.app.server.models.User.FitnessUser;
import com.app.server.models.User.User;
import com.app.server.util.MongoPool;
import com.app.server.util.parser.FitnessUserDocumentParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * EventOrganizerService
 *
 * <a href="mailto:sivashav@andrew.cmu.edu">Siva Shankaran Vasanth</a>
 */
public class FitnessUserService {

    private static FitnessUserService instance;
    private static UserService userServiceInstance;
    private ObjectWriter ow;
    private MongoCollection<Document> fitnessUserCollection = null;

    private FitnessUserService() {
        this.fitnessUserCollection = MongoPool.getInstance().getCollection("fitnessUser");
        ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        this.userServiceInstance = UserService.getInstance();
    }

    public static FitnessUserService getInstance(){
        if (instance == null) {
            synchronized (FitnessUserService.class) {
                if (instance == null) {
                    instance = new FitnessUserService();
                }
            }
        }
        return instance;
    }

    public FitnessUser getFitnessUser(String id) {
        BasicDBObject query = new BasicDBObject();
        query.put("_id", new ObjectId(id));
        Document item = this.fitnessUserCollection.find(query).first();
        if (item == null) {
            return null;
        }
        FitnessUser fitnessUser = FitnessUserDocumentParser.convertDocumentToFitnessUser(item);
        User user = userServiceInstance.getUser(fitnessUser.getUserId());
        fitnessUser.setUserDetails(user);
        return fitnessUser;
    }

    public FitnessUser create(Object request, String userId) {

        try {
            JSONObject json = null;
            json = new JSONObject(ow.writeValueAsString(request));
            validateRequest(json);
            Document doc = FitnessUserDocumentParser.convertJsonToFitnessUserDocument(json, userId);
            fitnessUserCollection.insertOne(doc);
            //TODO: fix data inconsistency
            FitnessUser fitnessUser = FitnessUserDocumentParser.convertJsonToFitnessUser(json, userId);
            ObjectId id = (ObjectId)doc.get("_id");
            fitnessUser.setId(id.toString());
            return fitnessUser;
        } catch(JsonProcessingException e) {
            System.out.println("Failed to create a document");
            return null;
        } catch(APPBadRequestException e) {
            throw new APPBadRequestException(33, e.getMessage());
        } catch(APPUnauthorizedException e) {
            throw new APPUnauthorizedException(34, e.getMessage());
        } catch(Exception e) {
            System.out.println("EXCEPTION!!!!");
            e.printStackTrace();
            throw new APPInternalServerException(99, e.getMessage());
        }
    }

    public Object update(String id, Object request) {
        try {
            JSONObject json = null;
            json = new JSONObject(ow.writeValueAsString(request));

            BasicDBObject query = new BasicDBObject();
            query.put("_id", new ObjectId(id));

            Document set = new Document("$set", FitnessUserDocumentParser.
                    convertJsonToFitnessUserDocument(json, json.getString("userId")));
            fitnessUserCollection.updateOne(query,set);
            return request;
        } catch(JsonProcessingException e) {
            System.out.println("Failed to create a document");
            return null;
        } catch(APPBadRequestException e) {
            throw new APPBadRequestException(33, e.getMessage());
        } catch(APPUnauthorizedException e) {
            throw new APPUnauthorizedException(34, e.getMessage());
        } catch(Exception e) {
            System.out.println("EXCEPTION!!!!");
            e.printStackTrace();
            throw new APPInternalServerException(99, e.getMessage());
        }
    }

    public Object delete(String id) {
        BasicDBObject query = new BasicDBObject();
        query.put("_id", new ObjectId(id));
        fitnessUserCollection.deleteOne(query);
        return new JSONObject();
    }

    public Object deleteAll() {
        fitnessUserCollection.deleteMany(new BasicDBObject());
        return new JSONObject();
    }

    public Boolean validateRequest(JSONObject json){
        if (!json.has("gender"))
            throw new APPBadRequestException(55, "gender Missing");
        if (!json.has("height"))
            throw new APPBadRequestException(55, "height Missing");
        if (!json.has("weight"))
            throw new APPBadRequestException(55, "weight Missing");
        if (!json.has("city"))
            throw new APPBadRequestException(55, "city Missing");
        if (!json.has("state"))
            throw new APPBadRequestException(55, "state Missing");
        if (!json.has("zipCode"))
            throw new APPBadRequestException(55, "zipCode Missing");
        if (!json.has("ailmentTags"))
            throw new APPBadRequestException(55, "ailments Missing");
        if (!json.has("interestTags"))
            throw new APPBadRequestException(55, "interests Missing");
        if (!json.has("habitTags"))
            throw new APPBadRequestException(55, "habits Missing");
        if (!json.has("termsConsent"))
            throw new APPBadRequestException(55, "terms Missing");
        if (!json.has("receiveEmailNotifications"))
            throw new APPBadRequestException(55, "receive email notif Missing");
        return true;
    }
}