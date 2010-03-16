/**
 * Copyright (C) 2010 Olafur Gauti Gudmundsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.code.morphia;

import com.google.code.morphia.testmodel.Hotel;
import com.google.code.morphia.testmodel.Address;
import com.google.code.morphia.testmodel.Article;
import com.google.code.morphia.testmodel.Circle;
import com.google.code.morphia.testmodel.PhoneNumber;
import com.google.code.morphia.testmodel.RecursiveChild;
import com.google.code.morphia.testmodel.RecursiveParent;
import com.google.code.morphia.testmodel.Translation;
import com.google.code.morphia.testmodel.TravelAgency;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import java.util.Date;
import java.util.Vector;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Olafur Gauti Gudmundsson
 */
public class TestMapping {

    @Test
    public void testBasicMapping() throws Exception {
        Mongo mongo = new Mongo();
        DB db = mongo.getDB("morphia_test");
        try {

            DBCollection hotels = db.getCollection("hotels");
            DBCollection agencies = db.getCollection("agencies");

            Morphia morphia = new Morphia();
            morphia.map(Hotel.class);
            morphia.map(TravelAgency.class);

            Hotel borg = Hotel.create();
            borg.setName("Hotel Borg");
            borg.setStars(4);
            borg.setTakesCreditCards(true);
            borg.setStartDate(new Date());
            borg.setType(Hotel.Type.LEISURE);
            borg.getTags().add("Swimming pool");
            borg.getTags().add("Room service");
            borg.setTemp("A temporary transient value");
            borg.getPhoneNumbers().add(new PhoneNumber(354,5152233,PhoneNumber.Type.PHONE));
            borg.getPhoneNumbers().add(new PhoneNumber(354,5152244,PhoneNumber.Type.FAX));

            Address borgAddr = new Address();
            borgAddr.setStreet("Posthusstraeti 11");
            borgAddr.setPostCode("101");
            borg.setAddress(borgAddr);

            BasicDBObject hotelDbObj = (BasicDBObject) morphia.toDBObject(borg);
            hotels.save(hotelDbObj);

            Hotel borgLoaded = morphia.fromDBObject(Hotel.class, hotelDbObj);

            assertEquals(borg.getName(), borgLoaded.getName());
            assertEquals(borg.getStars(), borgLoaded.getStars());
            assertEquals(borg.getStartDate(), borgLoaded.getStartDate());
            assertEquals(borg.getType(), borgLoaded.getType());
            assertEquals(borg.getAddress().getStreet(), borgLoaded.getAddress().getStreet());
            assertEquals(borg.getTags().size(), borgLoaded.getTags().size());
            assertEquals(borg.getTags(), borgLoaded.getTags());
            assertEquals(borg.getPhoneNumbers().size(), borgLoaded.getPhoneNumbers().size());
            assertEquals(borg.getPhoneNumbers().get(1), borgLoaded.getPhoneNumbers().get(1));
            assertNull(borgLoaded.getTemp());
            assertTrue(borgLoaded.getPhoneNumbers() instanceof Vector);

            TravelAgency agency = new TravelAgency();
            agency.setName("Lastminute.com");
            agency.getHotels().add(borgLoaded);

            BasicDBObject agencyDbObj = (BasicDBObject) morphia.toDBObject(agency);
            agencies.save(agencyDbObj);

            TravelAgency agencyLoaded = morphia.fromDBObject(TravelAgency.class, (BasicDBObject)agencies.findOne(new BasicDBObject("_id", agencyDbObj.get("_id"))));

            assertEquals(agency.getName(), agencyLoaded.getName());
            assertEquals(agency.getHotels().size(), 1);
            assertEquals(agency.getHotels().get(0).getName(), borg.getName());

        } finally {
            db.dropDatabase();
        }
    }

    @Test
    public void testMaps() throws Exception {
        Mongo mongo = new Mongo();
        DB db = mongo.getDB("morphia_test");
        try {
            DBCollection articles = db.getCollection("articles");
            Morphia morphia = new Morphia();
            morphia.map(Article.class).map(Translation.class).map(Circle.class);

            Article related = new Article();
            BasicDBObject relatedDbObj = (BasicDBObject) morphia.toDBObject(related);
            articles.save(relatedDbObj);

            Article relatedLoaded = morphia.fromDBObject(Article.class, (BasicDBObject)articles.findOne(new BasicDBObject("_id", relatedDbObj.get("_id"))));

            Article article = new Article();
            article.setTranslation("en", new Translation("Hello World", "Just a test"));
            article.setTranslation("is", new Translation("Halló heimur", "Bara að prófa"));

            article.setAttribute("myDate", new Date());
            article.setAttribute("myString", "Test");
            article.setAttribute("myInt", 123);

            article.putRelated("test", relatedLoaded);

            BasicDBObject articleDbObj = (BasicDBObject) morphia.toDBObject(article);
            articles.save(articleDbObj);

            Article articleLoaded = morphia.fromDBObject(Article.class, (BasicDBObject)articles.findOne(new BasicDBObject("_id", articleDbObj.get("_id"))));

            assertEquals(article.getTranslations().size(), articleLoaded.getTranslations().size());
            assertEquals(article.getTranslation("en").getTitle(), articleLoaded.getTranslation("en").getTitle());
            assertEquals(article.getTranslation("is").getBody(), articleLoaded.getTranslation("is").getBody());
            assertEquals(article.getAttributes().size(), articleLoaded.getAttributes().size());
            assertEquals(article.getAttribute("myDate"), articleLoaded.getAttribute("myDate"));
            assertEquals(article.getAttribute("myString"), articleLoaded.getAttribute("myString"));
            assertEquals(article.getAttribute("myInt"), articleLoaded.getAttribute("myInt"));
            assertEquals(article.getRelated().size(), articleLoaded.getRelated().size());
            assertEquals(article.getRelated("test").getId(), articleLoaded.getRelated("test").getId());

        } finally {
            db.dropDatabase();
        }
    }

    @Test
    public void testRecursiveReference() throws Exception {
        Mongo mongo = new Mongo();
        DB db = mongo.getDB("morphia_test");
        try {
            DBCollection stuff = db.getCollection("stuff");

            Morphia morphia = new Morphia();
            morphia.map(RecursiveParent.class).map(RecursiveChild.class);

            RecursiveParent parent = new RecursiveParent();
            BasicDBObject parentDbObj = (BasicDBObject) morphia.toDBObject(parent);
            stuff.save(parentDbObj);

            RecursiveChild child = new RecursiveChild();
            BasicDBObject childDbObj = (BasicDBObject) morphia.toDBObject(child);
            stuff.save(childDbObj);

            RecursiveParent parentLoaded = morphia.fromDBObject(RecursiveParent.class, (BasicDBObject)stuff.findOne(new BasicDBObject("_id", parentDbObj.get("_id"))));
            RecursiveChild childLoaded = morphia.fromDBObject(RecursiveChild.class, (BasicDBObject)stuff.findOne(new BasicDBObject("_id", childDbObj.get("_id"))));

            parentLoaded.setChild(childLoaded);
            childLoaded.setParent(parentLoaded);

            stuff.save(morphia.toDBObject(parentLoaded));
            stuff.save(morphia.toDBObject(childLoaded));

            RecursiveParent finalParentLoaded = morphia.fromDBObject(RecursiveParent.class, (BasicDBObject)stuff.findOne(new BasicDBObject("_id", parentDbObj.get("_id"))));
            RecursiveChild finalChildLoaded = morphia.fromDBObject(RecursiveChild.class, (BasicDBObject)stuff.findOne(new BasicDBObject("_id", childDbObj.get("_id"))));

            assertNotNull(finalParentLoaded.getChild());
            assertNotNull(finalChildLoaded.getParent());

        } finally {
            db.dropDatabase();
        }
    }
}
