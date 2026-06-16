package com.project.ieum.service.geocoding;

/**
 * 두 WGS84 좌표 사이의 직선(대권) 거리 계산 및 근사 표시 라벨 생성.
 *
 * <p>거리는 발견/정렬 보조용이며, 화면 표시는 정확한 거리 대신 0.5km 버킷으로 흐린다.
 * 정확한 거리를 노출하면 caregiver가 본 거리로 요청자의 실제 위치를 역산할 수 있어,
 * 프라이버시 보호를 위해 의도적으로 근사값("약 2.5km")만 표시한다.
 */
public final class GeoDistance {

    private static final double EARTH_RADIUS_KM = 6371.0088;
    private static final double BUCKET_KM = 0.5;

    private GeoDistance() {
    }

    /** 두 좌표(위도, 경도) 사이의 대권 거리(km). 하버사인 공식. */
    public static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * 거리를 0.5km 버킷으로 반올림한 근사 라벨. "약 2.5km"는 실제로 ±250m 범위를 뜻한다.
     * 0.25km 미만은 버킷이 0이 되어 "약 0km"가 어색하므로 "500m 이내"로 표시한다.
     */
    public static String approxLabel(double km) {
        if (km < BUCKET_KM / 2) {
            return "500m 이내";
        }
        double bucket = Math.round(km / BUCKET_KM) * BUCKET_KM;
        if (bucket == Math.floor(bucket)) {
            return "약 " + (long) bucket + "km";
        }
        return "약 " + bucket + "km";
    }
}
