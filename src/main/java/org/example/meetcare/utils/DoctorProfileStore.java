package org.example.meetcare.utils;

import org.example.meetcare.model.DoctorProfileData;

public final class DoctorProfileStore {
    private static DoctorProfileData currentDoctor;

    private DoctorProfileStore() {
    }

    public static void setCurrentDoctor(DoctorProfileData doctor) {
        currentDoctor = doctor;
    }

    public static DoctorProfileData getCurrentDoctor() {
        return currentDoctor;
    }
}
