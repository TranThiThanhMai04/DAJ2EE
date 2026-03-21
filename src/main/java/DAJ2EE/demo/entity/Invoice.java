package DAJ2EE.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "invoices")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private User tenant;

    private int month;
    private int year;

    private double rentAmount;
    private double electricUsage;
    private double electricCost;
    private double waterUsage;
    private double waterCost;
    
    // totalAmount should be calculated from rent + electricCost + waterCost usually
    private double totalAmount;

    private String status; // "PAID" hoặc "UNPAID"

    public Invoice() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getTenant() { return tenant; }
    public void setTenant(User tenant) { this.tenant = tenant; }
    
    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }
    
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    
    public double getRentAmount() { return rentAmount; }
    public void setRentAmount(double rentAmount) { this.rentAmount = rentAmount; }
    
    public double getElectricUsage() { return electricUsage; }
    public void setElectricUsage(double electricUsage) { this.electricUsage = electricUsage; }
    
    public double getElectricCost() { return electricCost; }
    public void setElectricCost(double electricCost) { this.electricCost = electricCost; }
    
    public double getWaterUsage() { return waterUsage; }
    public void setWaterUsage(double waterUsage) { this.waterUsage = waterUsage; }
    
    public double getWaterCost() { return waterCost; }
    public void setWaterCost(double waterCost) { this.waterCost = waterCost; }
    
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
