package dev.morphia.test.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity("authors")
public class Author {
    @Id
    public String name;

    @Reference(idOnly = true)
    public List<Book> list;
    @Reference(idOnly = true)
    public Set<Book> set;

    public Author() {
    }

    public Author(String name) {
        this.name = name;
    }

    public List<Book> getList() {
        return list;
    }

    public Author setList(List<Book> list) {
        this.list = list;
        return this;
    }

    public Set<Book> getSet() {
        return set;
    }

    public Author setSet(Set<Book> set) {
        this.set = set;
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, list, set);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Author)) {
            return false;
        }
        Author author = (Author) o;
        return Objects.equals(name, author.name) && Objects.equals(list, author.list) &&
               Objects.equals(set, author.set);
    }
}
