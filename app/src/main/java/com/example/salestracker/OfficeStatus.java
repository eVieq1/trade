package com.example.salestracker;

public class OfficeStatus {
    private int officeId;
    private String officeName;
    private boolean isOpen;
    private String openedBy;
    private String openedAt;

    public OfficeStatus(int officeId, String officeName, boolean isOpen, String openedBy, String openedAt) {
        this.officeId = officeId;
        this.officeName = officeName;
        this.isOpen = isOpen;
        this.openedBy = openedBy;
        this.openedAt = openedAt;
    }

    public int getOfficeId() { return officeId; }
    public String getOfficeName() { return officeName; }
    public boolean isOpen() { return isOpen; }
    public String getOpenedBy() { return openedBy; }
    public String getOpenedAt() { return openedAt; }
}