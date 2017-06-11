// Package name.
package com.zol_dossierfotos;

// Imports from official sources.
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Creates a patient object **/
class Patient {

    private String status;
    private String visitId;
    private String patientId;
    private String firstName;
    private String lastName;
    private String birthDate;
    private String age;
    private String sex;

    Patient(JSONObject patientInfo, String status) {

        try {
            // Process the patient his values from the service.
            this.visitId = patientInfo.getString("visitId");

            this.patientId = patientInfo.getString("patientId");

            int i;
            String[] partsStr1 = patientInfo.getString("givenName").split("\\s+");
            this.firstName = "";
            for (i = 0 ; i < partsStr1.length ; i++) {
                this.firstName += partsStr1[i].substring(0, 1).toUpperCase() +
                        partsStr1[i].substring(1).toLowerCase();
                if ((i+1) < partsStr1.length) {
                    this.firstName += " ";
                }
            }

            int j;
            String[] partsStr2 = patientInfo.getString("lastName").split("\\s+");
            this.lastName = "";
            for (j = 0 ; j < partsStr2.length ; j++) {
                this.lastName += partsStr2[j].substring(0, 1).toUpperCase() +
                        partsStr2[j].substring(1).toLowerCase();
                if ((j+1) < partsStr2.length) {
                    this.lastName += " ";
                }
            }

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            Date date = format.parse(patientInfo.getString("dob"));
            Calendar dob = Calendar.getInstance();
            dob.setTime(date);

            this.birthDate = dob.get(Calendar.DAY_OF_MONTH) + "/" + (dob.get(Calendar.MONTH)+1)
                    + "/" + dob.get(Calendar.YEAR) ;

            Calendar today = Calendar.getInstance();
            int ageInt = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)){
                ageInt--;
            }

            this.age = String.valueOf(ageInt);

            if (patientInfo.getString("sex").equals("M")) {
                this.sex = "Man";
            } else if (patientInfo.getString("sex").equals("F")) {
                this.sex = "Vrouw";
            }

            this.status = status;

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    Patient(String status, String visitId) {
        this.status = status;
        this.visitId = visitId;
    }

    String getStatus() {
        return this.status;
    }

    String getVisitId() {
        return this.visitId;
    }

    String getPatientId() {
        return this.patientId;
    }

    String getFirstName() {
        return this.firstName;
    }

    String getLastName() {
        return this.lastName;
    }

    String getBirthDate() {
        return this.birthDate;
    }

    String getAge() {
        return this.age;
    }

    String getSex() {
        return this.sex;
    }

}
