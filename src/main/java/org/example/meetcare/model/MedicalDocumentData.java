package org.example.meetcare.model;

public class MedicalDocumentData {
    private final String title;
    private final String type;
    private final String description;
    private final String filePath;

    public MedicalDocumentData(String title, String type, String description, String filePath) {
        this.title = title;
        this.type = type;
        this.description = description;
        this.filePath = filePath;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getFilePath() {
        return filePath;
    }
}
