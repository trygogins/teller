package ru.ovsyannikov.clustering.model;

import java.util.List;

/**
 * @author Georgii Ovsiannikov
 * @since 5/17/15
 */
public class DistanceKey {

    List<String> collection1;
    List<String> collection2;
    String type;

    public DistanceKey(String type, List<String> collection1, List<String> collection2) {
        this.type = type;
        this.collection1 = collection1;
        this.collection2 = collection2;
    }

    public List<String> getCollection1() {
        return collection1;
    }

    public void setCollection1(List<String> collection1) {
        this.collection1 = collection1;
    }

    public List<String> getCollection2() {
        return collection2;
    }

    public void setCollection2(List<String> collection2) {
        this.collection2 = collection2;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DistanceKey that = (DistanceKey) o;

        if (collection1 != null ? !collection1.equals(that.collection1) : that.collection1 != null) return false;
        if (collection2 != null ? !collection2.equals(that.collection2) : that.collection2 != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = collection1 != null ? collection1.hashCode() : 0;
        result = 31 * result + (collection2 != null ? collection2.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}
