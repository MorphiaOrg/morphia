package dev.morphia.testmodel;


import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;

import java.util.HashMap;
import java.util.Map;


@Entity("articles")
@SuppressWarnings("unchecked")
public class Article extends TestEntity {
    private Map<String, Translation> translations;
    @Property
    private Map<String, Object> attributes;
    @Reference
    private Map<String, Article> related;

    public Article() {
        translations = new HashMap<>();
        attributes = new HashMap<>();
        related = new HashMap<>();
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Article> getRelated() {
        return related;
    }

    public void setRelated(Map<String, Article> related) {
        this.related = related;
    }

    public Article getRelated(String name) {
        return related.get(name);
    }

    public Translation getTranslation(String langCode) {
        return translations.get(langCode);
    }

    public Map<String, Translation> getTranslations() {
        return translations;
    }

    public void setTranslations(Map<String, Translation> translations) {
        this.translations = translations;
    }

    public void putRelated(String name, Article a) {
        related.put(name, a);
    }

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    public void setTranslation(String langCode, Translation t) {
        translations.put(langCode, t);
    }
}
