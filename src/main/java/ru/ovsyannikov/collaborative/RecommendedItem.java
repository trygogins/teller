package ru.ovsyannikov.collaborative;

import java.util.Comparator;

/**
 * @author Georgii Ovsiannikov
 * @since 5/23/15
 */
public class RecommendedItem {

    private Long kinopoiskId;
    private Double estimatedMark;

    public RecommendedItem(Long kinopoiskId, Double estimatedMark) {
        this.kinopoiskId = kinopoiskId;
        this.estimatedMark = estimatedMark;
    }

    public Long getKinopoiskId() {
        return kinopoiskId;
    }

    public void setKinopoiskId(Long kinopoiskId) {
        this.kinopoiskId = kinopoiskId;
    }

    public Double getEstimatedMark() {
        return estimatedMark;
    }

    public void setEstimatedMark(Double estimatedMark) {
        this.estimatedMark = estimatedMark;
    }

    public static class ItemsComparator implements Comparator<RecommendedItem> {

        @Override
        public int compare(RecommendedItem o1, RecommendedItem o2) {
            double value1 = o1.getEstimatedMark();
            double value2 = o2.getEstimatedMark();
            return value1 > value2 ? -1 : value1 < value2 ? 1 : 0;
        }
    }
}
