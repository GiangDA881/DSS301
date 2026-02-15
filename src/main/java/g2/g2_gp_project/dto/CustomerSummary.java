package g2.g2_gp_project.dto;

public class CustomerSummary {
    private String customerID;
    private String country;
    private double totalSpent;
    private int totalOrders;

    public CustomerSummary(String customerID, String country, double totalSpent, int totalOrders) {
        this.customerID = customerID;
        this.country = country;
        this.totalSpent = totalSpent;
        this.totalOrders = totalOrders;
    }

    // Getters & setters
    public String getCustomerID() { return customerID; }
    public void setCustomerID(String customerID) { this.customerID = customerID; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(double totalSpent) { this.totalSpent = totalSpent; }

    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }
}
