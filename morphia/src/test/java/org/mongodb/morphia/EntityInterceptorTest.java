package org.mongodb.morphia;

import com.mongodb.DBObject;
import org.junit.Test;
import org.mongodb.morphia.mapping.Mapper;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class EntityInterceptorTest extends TestBase {
    @Test
    public void shouldFireEventsInInterceptorInCorrectOrder() {
        // Given
        final TestEntityInterceptor entityInterceptor = new TestEntityInterceptor();
        getMorphia().getMapper().addInterceptor(entityInterceptor);

        final Entity entity = new Entity();

        // When
        getDs().save(entity);

        // Then
        assertThat(entityInterceptor.testResults.get("prePersist").count, is(1));
        assertThat(entityInterceptor.testResults.get("prePersist").index, is(0));

        assertThat(entityInterceptor.testResults.get("preSave").count, is(1));
        assertThat(entityInterceptor.testResults.get("preSave").index, is(1));

        assertThat(entityInterceptor.testResults.get("postPersist").count, is(1));
        assertThat(entityInterceptor.testResults.get("postPersist").index, is(2));

        assertThat(entityInterceptor.testResults.get("preLoad"), is(nullValue()));

        assertThat(entityInterceptor.testResults.get("postLoad"), is(nullValue()));

        // When
        final Entity loadedEntity = getDs().find(Entity.class).get();

        // Then
        assertThat(entityInterceptor.testResults.get("prePersist").count, is(1));

        assertThat(entityInterceptor.testResults.get("preSave").count, is(1));

        assertThat(entityInterceptor.testResults.get("postPersist").count, is(1));

        assertThat(entityInterceptor.testResults.get("preLoad").count, is(1));
        assertThat(entityInterceptor.testResults.get("preLoad").index, is(3));

        assertThat(entityInterceptor.testResults.get("postLoad").count, is(1));
        assertThat(entityInterceptor.testResults.get("postLoad").index, is(4));
    }

    private static class Entity {
    }

    private static class TestEntityInterceptor implements EntityInterceptor {
        private Map<String, Result> testResults = new HashMap<String, Result>();
        private int index;

        @Override
        public void postLoad(final Object ent, final DBObject dbObj, final Mapper mapper) {
            updateResults("postLoad");
        }

        @Override
        public void postPersist(final Object ent, final DBObject dbObj, final Mapper mapper) {
            updateResults("postPersist");
        }

        @Override
        public void preLoad(final Object ent, final DBObject dbObj, final Mapper mapper) {
            updateResults("preLoad");
        }

        @Override
        public void prePersist(final Object ent, final DBObject dbObj, final Mapper mapper) {
            updateResults("prePersist");
        }

        @Override
        public void preSave(final Object ent, final DBObject dbObj, final Mapper mapper) {
            updateResults("preSave");
        }

        private void updateResults(final String methodName) {
            Result result = testResults.get(methodName);
            if (result == null) {
                result = new Result();
                testResults.put(methodName, result);
            }
            result.incrementCount();
            result.setIndex(index);
            index++;
        }

        private class Result {
            private int count;
            private int index;

            public void incrementCount() {
                count++;
            }

            public void setIndex(final int index) {
                this.index = index;
            }
        }
    }

}
