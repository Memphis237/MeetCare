package org.example.meetcare.model;

import java.time.LocalDate;

public class AvailabilityBlock {
    private int id;
    private int doctorUserId;
    private LocalDate blockDate;
    private String startTime;
    private String endTime;
    private boolean wholeDay;
    private String reason;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDoctorUserId() {
        return doctorUserId;
    }

    public void setDoctorUserId(int doctorUserId) {
        this.doctorUserId = doctorUserId;
    }

    public LocalDate getBlockDate() {
        return blockDate;
    }

    public void setBlockDate(LocalDate blockDate) {
        this.blockDate = blockDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public boolean isWholeDay() {
        return wholeDay;
    }

    public void setWholeDay(boolean wholeDay) {
        this.wholeDay = wholeDay;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
