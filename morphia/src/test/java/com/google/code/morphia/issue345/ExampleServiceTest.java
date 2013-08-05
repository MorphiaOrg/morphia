package com.google.code.morphia.issue345;


import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bson.types.ObjectId;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.code.morphia.AdvancedDatastore;
import com.google.code.morphia.Datastore;
import com.google.code.morphia.Key;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Version;
import com.google.code.morphia.dao.BasicDAO;
import com.google.code.morphia.logging.MorphiaLoggerFactory;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;


@RunWith(ConcurrentJunitRunner.class)
@Concurrent(threads = 4)
@Ignore
public class ExampleServiceTest {

  // private ExampleService service = new ExampleService();
  // @Autowired
  final BookingDetailService bookingDetailService;

  static final MongoConnectionManager mcm;

  static {
    mcm = new MongoConnectionManager("localhost", 27017);
    MorphiaLoggerFactory.get(ExampleServiceTest.class).debug("starting ...");
  }

  {
    bookingDetailService = new BookingDetailService();
    bookingDetailService.setMongoConnectionManager(mcm);
  }

  @Test
  public void test1() {

    final Customer customer = new Customer();
    customer.setName("Jill P");
    customer.setPreferredNumber("0421761183");

    bookingDetailService.book(new Key<BookingDetail>(BookingDetail.class, "24-11-2011"), "09:00 am", customer);
  }

  @Test
  public void test2() {

    final Customer customer = new Customer();
    customer.setName("Sam D");
    customer.setPreferredNumber("0421761183");

    bookingDetailService.book(new Key<BookingDetail>(BookingDetail.class, "24-11-2011"), "09:00 am", customer);
  }

  @Test
  public void test3() {

    final Customer customer = new Customer();
    customer.setName("Jenny B");
    customer.setPreferredNumber("0421761183");

    bookingDetailService.book(new Key<BookingDetail>(BookingDetail.class, "24-11-2011"), "09:00 am", customer);
  }

  @Test
  public void test4() {

    final Customer customer = new Customer();
    customer.setName("Janet T");
    customer.setPreferredNumber("0421761183");

    bookingDetailService.book(new Key<BookingDetail>(BookingDetail.class, "24-11-2011"), "09:00 am", customer);
  }

  @Embedded
  private static class Customer {
    String name;
    String preferredNumber;

    public final String getName() {
      return name;
    }

    public final void setName(final String name) {
      this.name = name;
    }

    public final void setPreferredNumber(final String preferredNumber) {
      this.preferredNumber = preferredNumber;
    }
  }

  @Embedded
  private static class Consultant {
    private String name;

    public final void setName(final String name) {
      this.name = name;
    }
  }

  @Entity("BookingDetail")
  //@Indexes(@Index(unique=true, value="date"))
  private static class BookingDetail {
    @Id
    private String            date;
    @Version
    private Long              version;
    @Embedded
    private final List<BookingSlot> bookingSlot;

    public BookingDetail() {
      bookingSlot = new ArrayList<BookingSlot>();
    }

    public final void setDate(final String date) {
      this.date = date;
    }

    public final Long getVersion() {
      return version;
    }

    public final List<BookingSlot> getBookingSlot() {
      return bookingSlot;
    }
  }

  @Embedded
  private static class BookingSlot {
    // No id because this class is embedded.
    private String     startTime;
    private String     endTime;
    @Embedded
    private Consultant consultant;
    private boolean    enabled;
    @Embedded
    private Customer   customer;
    private Date       dateCreated;

    public final String getStartTime() {
      return startTime;
    }

    public final void setStartTime(final String startTime) {
      this.startTime = startTime;
    }

    public final void setEndTime(final String endTime) {
      this.endTime = endTime;
    }

    public final void setConsultant(final Consultant consultant) {
      this.consultant = consultant;
    }

    public final void setEnabled(final boolean enabled) {
      this.enabled = enabled;
    }

    public final Customer getCustomer() {
      return customer;
    }

    public final void setCustomer(final Customer customer) {
      this.customer = customer;
    }
  }

  private static class MongoConnectionManager {
    private final Datastore db;
    public static final String DB_NAME = "cal_dev";

    public MongoConnectionManager(final String host, final int port) {
      try {
        final MongoClient m = new MongoClient(host, port);

        db = new Morphia().map(BookingDetail.class).createDatastore(m, DB_NAME);
        db.ensureIndexes();
      } catch (Exception e) {
        throw new RuntimeException("Error initializing mongo db", e);
      }
    }

    public Datastore getDb() {
      return db;
    }
  }


  private static class BookingDetailService {
    private static final Logger logger = Logger.getLogger(BookingDetailService.class.getSimpleName());
    MongoConnectionManager mongoConnectionManager;

    public BookingDetail loadOrCreate(final Key<BookingDetail> key) {
      BookingDetail bookingDetail = null;
      final int tries = 3;
      boolean success = false;
      for (int i = 0; i < tries; i++) {
        try {
          bookingDetail = load(key);
          if (bookingDetail == null) {
            create(key); // Many threads can enter here.
            bookingDetail = load(key);
            if (bookingDetail != null) {
              success = true;
              break;
            }
          } else {
            success = true;
            break;
          }
        } catch (MongoException e) {
          // Duplicate key.
          logger.log(Level.FINE, "loadOrCreate attempt: " + i + " another user beat us to it.", e);
        }
      }

      if (!success) {
        logger.warning("Could not loadOrCreate at this time, please try again later");
        // TODO change to service exception.
        throw new RuntimeException("Could not loadOrCreate at this time, please try again later");
      }
      return bookingDetail;
    }

    public Key<BookingDetail> create(final Key<BookingDetail> key) {
      final BookingDetail bookingDetail = new BookingDetail();
      bookingDetail.setDate((String) key.getId());
      createSlots(bookingDetail.getBookingSlot());
      final BasicDAO<BookingDetail, ObjectId> dao = new BasicDAO<BookingDetail, ObjectId>(BookingDetail.class, mongoConnectionManager.getDb());
      final Key<BookingDetail> result = ((AdvancedDatastore) dao.getDatastore()).insert(bookingDetail);
      dao.ensureIndexes();
      return result;
    }

    public BookingDetail load(final Key<BookingDetail> key) {
      final BasicDAO<BookingDetail, ObjectId> dao = new BasicDAO<BookingDetail, ObjectId>(BookingDetail.class, mongoConnectionManager.getDb());
      return dao.getDatastore().getByKey(BookingDetail.class, key);

    }

    public Key<BookingDetail> update(final BookingDetail bd) {

      final BasicDAO<BookingDetail, ObjectId> dao = new BasicDAO<BookingDetail, ObjectId>(BookingDetail.class, mongoConnectionManager.getDb());

      return dao.getDatastore().save(bd);
    }

    public void book(final Key<BookingDetail> key, final String startTime, final Customer customer) {

      final int tries = 3;
      boolean success = false;

      for (int i = 0; i < tries; i++) {

        try {

          final BookingDetail loadedBookingDetail = loadOrCreate(key);
          final List<BookingSlot> availableSlots = new ArrayList<BookingSlot>();
          for (final BookingSlot slot : loadedBookingDetail.getBookingSlot()) {

            if (slot.getStartTime().equals(startTime) && slot.getCustomer() == null) {
              availableSlots.add(slot);
            }
          }

          if (availableSlots.isEmpty()) {
            // No available slots left, another user must have beaten us to it.
            // TODO change to service exception.
            throw new RuntimeException("No available slots for xxx");
          }

          // TODO Logic to choose consultant.
          availableSlots.get(0).setCustomer(customer);
          logger.log(Level.FINE, "Book for customer: " + customer.getName() + " version: " + loadedBookingDetail.getVersion() + " ...");
          update(loadedBookingDetail);
          logger.log(Level.FINE, "Booked.");

          success = true;
          break;

        } catch (ConcurrentModificationException e) {
          logger.log(Level.FINE, "Book attempt: " + i + " failed, another user beat us to it.", e);
        }

      }

      if (!success) {

        logger.severe("Could not make a booking at this time, please try again later.");

        throw new RuntimeException("Could not make booking at this time, please try again later");
      }
    }

    public final void setMongoConnectionManager(final MongoConnectionManager manager) {
      this.mongoConnectionManager = manager;
    }

    // Creates two slots.
    private void createSlots(final List<BookingSlot> bookingSlots) {

      BookingSlot bookingSlot = new BookingSlot();

      bookingSlot.setEnabled(true);
      bookingSlot.setStartTime("09:00 am");
      bookingSlot.setEndTime("10:00 am");

      Consultant consultant = new Consultant();
      consultant.setName("Peter X");
      bookingSlot.setConsultant(consultant);

      bookingSlots.add(bookingSlot);

      bookingSlot = new BookingSlot();

      bookingSlot.setEnabled(true);
      bookingSlot.setStartTime("09:00 am");
      bookingSlot.setEndTime("10:00 am");

      consultant = new Consultant();
      consultant.setName("Tom X");
      bookingSlot.setConsultant(consultant);

      bookingSlots.add(bookingSlot);

    }

  }

}
