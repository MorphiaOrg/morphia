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

import com.google.code.morphia.testdaos.HotelDAO;
import com.google.code.morphia.testmodel.Address;
import com.google.code.morphia.testmodel.Hotel;
import com.mongodb.DB;
import com.mongodb.Mongo;
import java.util.Date;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Olafur Gauti Gudmundsson
 */
public class TestDAO {

    @Test
    public void testDAO() throws Exception {
        Mongo mongo = new Mongo();
        DB db = mongo.getDB("morphia_test");
        try {

            Morphia morphia = new Morphia();
            morphia.map(Hotel.class);

            Hotel borg = Hotel.create();
            borg.setName("Hotel Borg");
            borg.setStars(4);
            borg.setTakesCreditCards(true);
            borg.setStartDate(new Date());
            borg.setType(Hotel.Type.LEISURE);
            Address borgAddr = new Address();
            borgAddr.setStreet("Posthusstraeti 11");
            borgAddr.setPostCode("101");
            borg.setAddress(borgAddr);

            HotelDAO hotelDAO = new HotelDAO(morphia, mongo);
            borg = hotelDAO.save(borg);
            assertEquals(1, hotelDAO.getCount());
            assertNotNull(borg.getId());
            assertEquals("hotels", borg.getCollectionName());

            Hotel hotelLoaded = hotelDAO.get(borg.getId());
            assertEquals(borg.getName(), hotelLoaded.getName());
            assertEquals(borg.getAddress().getPostCode(), hotelLoaded.getAddress().getPostCode());

            Hotel hotelByValue = hotelDAO.getByValue("name", "Hotel Borg");
            assertNotNull(hotelByValue);
            assertEquals(borg.getStartDate(), hotelByValue.getStartDate());

            assertTrue(hotelDAO.exists("stars", 4));

            Hotel hilton = Hotel.create();
            hilton.setName("Hilton Hotel");
            hilton.setStars(4);
            hilton.setTakesCreditCards(true);
            hilton.setStartDate(new Date());
            hilton.setType(Hotel.Type.BUSINESS);
            Address hiltonAddr = new Address();
            hiltonAddr.setStreet("Some street 44");
            hiltonAddr.setPostCode("101");
            hilton.setAddress(hiltonAddr);

            hotelDAO.save(hilton);

            List<Hotel> allHotels = hotelDAO.findAll(0, 10);
            assertEquals(2, allHotels.size());

            assertEquals(1, hotelDAO.findAll(1,10).size());
            assertEquals(1, hotelDAO.findAll(0,1).size());
            assertTrue(hotelDAO.exists("type", Hotel.Type.BUSINESS));
            assertNotNull(hotelDAO.getByValue("type", Hotel.Type.LEISURE));

            hotelDAO.removeById(borg.getId());
            assertEquals(1, hotelDAO.getCount());

            hotelDAO.dropCollection();
            assertEquals(0, hotelDAO.getCount());

        } finally {
            db.dropDatabase();
        }
    }
}
