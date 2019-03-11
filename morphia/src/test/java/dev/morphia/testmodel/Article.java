package dev.morphia.testmodel;


import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;
import dev.morphia.testutil.TestEntity;

import java.util.HashMap;
import java.util.Map;


/**
 * @author Olafur Gauti Gudmundsson
 */
@Entity("articles")
@SuppressWarnings("unchecked")
public class Article extends TestEntity {
    @Embedded
    private Map<String, Translation> translations;
    @Property
    private Map attributes;
    @Reference
    private Map<String, Article> related;

    public Article() {
        translations = new HashMap<String, Translation>();
        attributes = new HashMap<String, Object>();
        related = new HashMap<String, Article>();
    }

    public Object getAttribute(final String name) {
        return attributes.get(name);
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(final Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Article> getRelated() {
        return related;
    }

    public void setRelated(final Map<String, Article> related) {
        this.related = related;
    }

    public Article getRelated(final String name) {
        return related.get(name);
    }

    public Translation getTranslation(final String langCode) {
        return translations.get(langCode);
    }

    public Map<String, Translation> getTranslations() {
        return translations;
    }

    public void setTranslations(final Map<String, Translation> translations) {
        this.translations = translations;
    }

    public void putRelated(final String name, final Article a) {
        related.put(name, a);
    }

    public void setAttribute(final String name, final Object value) {
        attributes.put(name, value);
    }

    public void setTranslation(final String langCode, final Translation t) {
        translations.put(langCode, t);
    }
}
