package g2.g2_gp_project.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "online_retail")
public class RetailTransaction {

    @Id
    private String id;

    @Field("InvoiceNo")
    private String invoiceNo;

    @Field("StockCode")
    private String stockCode;

    @Field("Description")
    private String description;

    @Field("Quantity")
    private int quantity; // 🔹 sửa kiểu dữ liệu: nên dùng int thay vì String

    @Field("UnitPrice")
    private double unitPrice; // 🔹 sửa kiểu dữ liệu: nên dùng double thay vì String

    @Field("CustomerID")
    private String customerID;

    @Field("Country")
    private String country;

    @Field("InvoiceDate")
    private String invoiceDate;

    // ✅ Constructors (nên thêm để dễ thao tác)
    public RetailTransaction() {}

    public RetailTransaction(String invoiceNo, String stockCode, String description, int quantity,
                             double unitPrice, String customerID, String country, String invoiceDate) {
        this.invoiceNo = invoiceNo;
        this.stockCode = stockCode;
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.customerID = customerID;
        this.country = country;
        this.invoiceDate = invoiceDate;
    }

    // ✅ Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }

    public String getStockCode() { return stockCode; }
    public void setStockCode(String stockCode) { this.stockCode = stockCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public String getCustomerID() { return customerID; }
    public void setCustomerID(String customerID) { this.customerID = customerID; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(String invoiceDate) { this.invoiceDate = invoiceDate; }
}
