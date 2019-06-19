package dev.morphia;


import dev.morphia.mapping.Mapper;
import org.bson.Document;


public class AbstractEntityInterceptor implements EntityInterceptor {

    @Override
    public void postLoad(final Object ent, final Document document, final Mapper mapper) {
    }

    @Override
    public void postPersist(final Object ent, final Document document, final Mapper mapper) {
    }

    @Override
    public void preLoad(final Object ent, final Document document, final Mapper mapper) {
    }

    @Override
    public void prePersist(final Object ent, final Document document, final Mapper mapper) {
    }

    @Override
    public void preSave(final Object ent, final Document document, final Mapper mapper) {
    }
}
