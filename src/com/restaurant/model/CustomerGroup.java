package com.restaurant.model;

public class CustomerGroup {
    private String groupId;
    private int groupSize;
    private int preferredTableSize;
    private boolean isVip;
    private int arrivalTime;
    private int diningDuration;
    private int seatedTime;
    private int waitTime;
    private boolean seated;
    private boolean departed;

    public CustomerGroup(String groupId, int groupSize, int preferredTableSize,
                         boolean isVip, int arrivalTime, int diningDuration) {
        this.groupId = groupId;
        this.groupSize = groupSize;
        this.preferredTableSize = preferredTableSize;
        this.isVip = isVip;
        this.arrivalTime = arrivalTime;
        this.diningDuration = diningDuration;
        this.seatedTime = -1;
        this.waitTime = 0;
        this.seated = false;
        this.departed = false;
    }

    public String getGroupId()         { return groupId; }
    public int getGroupSize()          { return groupSize; }
    public int getPreferredTableSize() { return preferredTableSize; }
    public boolean isVip()             { return isVip; }
    public int getArrivalTime()        { return arrivalTime; }
    public int getDiningDuration()     { return diningDuration; }
    public int getSeatedTime()         { return seatedTime; }
    public int getWaitTime()           { return waitTime; }
    public boolean isSeated()          { return seated; }
    public boolean isDeparted()        { return departed; }

    public void seat(int currentTime) {
        this.seatedTime = currentTime;
        this.waitTime = currentTime - arrivalTime;
        this.seated = true;
    }

    public void depart() {
        this.departed = true;
        this.seated = false;
    }

    public CustomerGroup copy() {
        CustomerGroup g = new CustomerGroup(groupId, groupSize, preferredTableSize,
                isVip, arrivalTime, diningDuration);
        g.seatedTime = this.seatedTime;
        g.waitTime = this.waitTime;
        g.seated = this.seated;
        g.departed = this.departed;
        return g;
    }

    @Override
    public String toString() {
        return "Group[" + groupId + ", size=" + groupSize + (isVip ? ", VIP" : "")
                + ", arr=" + arrivalTime + ", dur=" + diningDuration + "]";
    }
}