package dev.morphia.critter;

import java.lang.reflect.Constructor;
import java.util.Date;
import java.util.List;
import java.util.Set;

import dev.morphia.MorphiaDatastore;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.critter.CritterEntityModel;
import dev.morphia.test.TestBase;
import dev.morphia.test.config.ManualMorphiaTestConfig;
import dev.morphia.test.models.Address;
import dev.morphia.test.models.Hotel;
import dev.morphia.test.models.PhoneNumber;
import dev.morphia.test.models.User;

import org.bson.types.ObjectId;
import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.eq;
import static java.time.LocalDate.of;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Verifies that critter-generated entity models work end-to-end with Morphia's
 * mapping pipeline: model registration, CRUD operations, and embedded object
 * serialization/deserialization.
 */
public class CritterCrudTest extends TestBase {

    private static final String[][] CRITTER_MODELS = {
            { "dev.morphia.test.models.Hotel",
                    "dev.morphia.test.models.__morphia.hotel.HotelEntityModel" },
            { "dev.morphia.test.models.User",
                    "dev.morphia.test.models.__morphia.user.UserEntityModel" },
    };

    private MorphiaDatastore createCritterDatastore() throws Exception {
        MorphiaConfig config = new ManualMorphiaTestConfig()
                .database(TEST_DB_NAME)
                .packages(List.of());

        MorphiaDatastore ds = new MorphiaDatastore(getMongoClient(), config);
        Mapper mapper = ds.getMapper();

        for (String[] entry : CRITTER_MODELS) {
            Class<?> modelClass = Class.forName(entry[1]);
            Constructor<?> ctor = modelClass.getConstructor(Mapper.class);
            EntityModel model = (EntityModel) ctor.newInstance(mapper);
            mapper.register(model);
        }

        return ds;
    }

    @Test
    public void testEntityModelIsCritterGenerated() throws Exception {
        MorphiaDatastore ds = createCritterDatastore();
        Mapper mapper = ds.getMapper();

        EntityModel hotelModel = mapper.getEntityModel(Hotel.class);
        assertNotNull(hotelModel, "Hotel entity model should be registered");
        assertTrue(hotelModel instanceof CritterEntityModel,
                "Hotel model should be a CritterEntityModel but was " + hotelModel.getClass().getName());

        EntityModel userModel = mapper.getEntityModel(User.class);
        assertNotNull(userModel, "User entity model should be registered");
        assertTrue(userModel instanceof CritterEntityModel,
                "User model should be a CritterEntityModel but was " + userModel.getClass().getName());
    }

    @Test
    public void testHotelCrud() throws Exception {
        MorphiaDatastore ds = createCritterDatastore();

        Hotel hotel = new Hotel();
        hotel.setName("Grand Budapest");
        hotel.setStars(5);
        hotel.setStartDate(new Date());
        hotel.setTags(Set.of("luxury", "historic"));
        hotel.setType(Hotel.Type.LEISURE);

        Address address = new Address();
        address.setStreet("123 Main St");
        address.setPostCode("12345");
        hotel.setAddress(address);

        hotel.getPhoneNumbers().add(new PhoneNumber(1, 5551234, PhoneNumber.Type.PHONE));
        hotel.getPhoneNumbers().add(new PhoneNumber(1, 5555678, PhoneNumber.Type.FAX));

        ds.save(hotel);
        ObjectId id = hotel.getId();
        assertNotNull(id, "ID should be assigned after save");

        Hotel loaded = ds.find(Hotel.class).filter(eq("_id", id)).first();
        assertNotNull(loaded, "Hotel should be found by ID");
        assertEquals(loaded.getName(), "Grand Budapest");
        assertEquals(loaded.getStars(), 5);
        assertEquals(loaded.getType(), Hotel.Type.LEISURE);
        assertEquals(loaded.getTags(), Set.of("luxury", "historic"));

        // Modify and replace
        loaded.setName("Grand Budapest Updated");
        loaded.setStars(4);
        ds.save(loaded);

        Hotel reloaded = ds.find(Hotel.class).filter(eq("_id", id)).first();
        assertNotNull(reloaded);
        assertEquals(reloaded.getName(), "Grand Budapest Updated");
        assertEquals(reloaded.getStars(), 4);
    }

    @Test
    public void testUserCrud() throws Exception {
        MorphiaDatastore ds = createCritterDatastore();

        User user = new User("Alice", of(2024, 1, 15), "hiking", "reading");
        user.setAge(25);

        ds.save(user);
        ObjectId id = user.getId();
        assertNotNull(id, "ID should be assigned after save");

        User loaded = ds.find(User.class).filter(eq("_id", id)).first();
        assertNotNull(loaded, "User should be found by ID");
        assertEquals(loaded.getName(), "Alice");
        assertEquals(loaded.getAge(), 25);
        assertEquals(loaded.getLikes(), List.of("hiking", "reading"));

        // Modify and replace
        loaded.setAge(26);
        loaded.setLikes(List.of("hiking", "reading", "coding"));
        ds.save(loaded);

        User reloaded = ds.find(User.class).filter(eq("_id", id)).first();
        assertNotNull(reloaded);
        assertEquals(reloaded.getAge(), 26);
        assertEquals(reloaded.getLikes(), List.of("hiking", "reading", "coding"));
    }

    @Test
    public void testHotelEmbeddedObjects() throws Exception {
        MorphiaDatastore ds = createCritterDatastore();

        Hotel hotel = new Hotel();
        hotel.setName("Embassy Suites");
        hotel.setStars(3);

        Address address = new Address();
        address.setStreet("456 Oak Ave");
        address.setPostCode("67890");
        hotel.setAddress(address);

        hotel.getPhoneNumbers().add(new PhoneNumber(44, 2071234, PhoneNumber.Type.PHONE));
        hotel.getPhoneNumbers().add(new PhoneNumber(44, 2075678, PhoneNumber.Type.FAX));

        ds.save(hotel);

        Hotel loaded = ds.find(Hotel.class).filter(eq("_id", hotel.getId())).first();
        assertNotNull(loaded);

        Address loadedAddr = loaded.getAddress();
        assertNotNull(loadedAddr, "Address should be deserialized");
        assertEquals(loadedAddr.getStreet(), "456 Oak Ave");
        assertEquals(loadedAddr.getPostCode(), "67890");

        List<PhoneNumber> phones = loaded.getPhoneNumbers();
        assertNotNull(phones);
        assertEquals(phones.size(), 2);
        assertEquals(phones.get(0).getCountryCode(), 44);
        assertEquals(phones.get(0).getLocalExtension(), 2071234);
        assertEquals(phones.get(0).getType(), PhoneNumber.Type.PHONE);
        assertEquals(phones.get(1).getType(), PhoneNumber.Type.FAX);
    }

    @Test
    public void testSaveAndDelete() throws Exception {
        MorphiaDatastore ds = createCritterDatastore();

        User user = new User("Bob", of(2024, 6, 1));
        user.setAge(30);
        ds.save(user);

        assertEquals(ds.find(User.class).count(), 1, "Should have 1 user after save");

        ds.delete(user);

        assertEquals(ds.find(User.class).count(), 0, "Should have 0 users after delete");
        assertNull(ds.find(User.class).filter(eq("_id", user.getId())).first(),
                "Deleted user should not be found");
    }
}
