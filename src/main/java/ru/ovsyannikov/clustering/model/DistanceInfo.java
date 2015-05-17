package ru.ovsyannikov.clustering.model;

import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * @author Georgii Ovsiannikov
 * @since 5/11/15
 */
public class DistanceInfo<T> {

    private List<T> collection1;
    private List<T> collection2;
    private String type;
    private double distance;

    public DistanceInfo() {
        // nothing to do here
    }

    public DistanceInfo(List<T> collection1, List<T> collection2, String type, double distance) {
        this.collection1 = collection1;
        this.collection2 = collection2;
        this.type = type;
        this.distance = distance;
    }

    public List<T> getCollection1() {
        return collection1;
    }

    public void setCollection1(List<T> collection1) {
        if (!collection1.isEmpty()) {
            this.collection1 = Arrays.asList((T[]) StringUtils.split(String.valueOf(collection1.get(0)), ","));
        } else {
            this.collection1 = collection1;
        }

    }

    public List<T> getCollection2() {
        return collection2;
    }

    public void setCollection2(List<T> collection2) {
        if (!collection2.isEmpty()) {
            this.collection2 = Arrays.asList((T[]) StringUtils.split(String.valueOf(collection2.get(0)), ","));
        } else {
            this.collection2 = collection2;
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DistanceInfo that = (DistanceInfo) o;

        if (collection1 != null ? !collection1.equals(that.collection1) : that.collection1 != null) return false;
        if (collection2 != null ? !collection2.equals(that.collection2) : that.collection2 != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = collection1 != null ? collection1.hashCode() : 0;
        result = 31 * result + (collection2 != null ? collection2.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}
