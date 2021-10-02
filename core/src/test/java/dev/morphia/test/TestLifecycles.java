package dev.morphia.test;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostPersist;
import dev.morphia.mapping.DateStorage;
import dev.morphia.mapping.MapperOptions;
import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Calendar;
import java.util.Date;

import static java.lang.String.format;

public class TestLifecycles extends TestBase {
    @Test
    public void ensureDateConfigurationIsAppliedEverywhere() {

        withOptions(MapperOptions.builder().dateStorage(DateStorage.SYSTEM_DEFAULT).build(), () -> {
            getDs().getMapper().map(SimpleBean.class);
            SimpleBean simpleBean = new SimpleBean();
            simpleBean.id = ObjectId.get();
            simpleBean.ldt = LocalDateTime.of(2021, Month.APRIL, 15, 18, 0, 0, 0);

            Calendar instance = Calendar.getInstance();
            instance.set(2021, Calendar.APRIL, 15, 18, 0, 0);
            simpleBean.date = instance.getTime();

            getDs().save(simpleBean);

            SimpleBean reloaded = getDs().find(SimpleBean.class).first();

            Assert.assertEquals(reloaded.ldt.getHour(), simpleBean.ldt.getHour(), format("'%s' vs '%s'", reloaded.ldt, simpleBean.ldt));
            Assert.assertEquals(reloaded.date, simpleBean.date, format("'%s' vs '%s'", reloaded.date, simpleBean.date));
        });

    }

    @Entity
    private static class SimpleBean {
        @Id
        private ObjectId id;
        private LocalDateTime ldt;
        private Date date;

        @PostPersist
        void postPersist() {
            // This @PostPersist method ensures DocumentWriter is used during persistance.
            // The method dev.morphia.mapping.codec.DocumentWriter.writeDateTime(long) uses the hardcoded Zone UTC
        }
    }
}

