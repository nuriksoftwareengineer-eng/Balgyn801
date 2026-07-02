package com.nurba.java.tracking;

import com.nurba.java.domain.ParcelTracking;
import lombok.Value;

import java.util.List;

@Value
public class TrackingResult {
    String lastStatus;
    String statusDetail;
    List<ParcelTracking.TrackingEvent> events;
}
