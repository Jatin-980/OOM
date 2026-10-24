package Banking_system.Code;

import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;



public class Customer {
    private int customerID;
    private String name;
    private LocalDate dobDate;
    private LocalDate registrationDate;
    private String phoneNo;
    private String emailId;
    private String pin;
    private int failedLoginAttempts = 0;
    private boolean isLocked = false;
    private List <Integer> ownedAccountList = new ArrayList<>();
    
    
    public Customer( String name, LocalDate dobDate, String phoneNo, String emailId, String pin) {
        this.name = name;
        this.dobDate = dobDate;
        this.registrationDate = LocalDate.now();
        this.phoneNo = phoneNo;
        this.emailId = emailId;
        this.pin = pin;
    }
    
    // ADDED THIS NEW 7-parameter constructor for BankStorage.java
    public Customer(int customerID, String name, LocalDate dob, String phoneNo, String emailId, String pin, LocalDate registrationDate) {
        this.customerID = customerID;
        this.name = name;
        this.dobDate = dob;
        this.phoneNo = phoneNo;
        this.emailId = emailId;
        this.pin = pin;
        this.registrationDate = registrationDate;
    }
    
    public void addAccountNo(int accountNo){
        this.getOwnedAccountList().add(accountNo);
    }
    
    
    public void setCustomerID(int customerID) {
        this.customerID = customerID;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setDobDate(LocalDate dobDate) {
        this.dobDate = dobDate;
    }
    public void setRegistrationDate(LocalDate registrationDate) {
        this.registrationDate = registrationDate;
    }
    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }
    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }
    public void setPin(String pin) {
        this.pin = pin;
    }
    public void setOwnedAccountList(List<Integer> owned_accounts) {
        this.ownedAccountList = owned_accounts;
    }
    
    public int getCustomerID() {
        return customerID;
    }
    public String getName() {
        return name;
    }
    public LocalDate getDobDate() {
        return dobDate;
    }
    public LocalDate getRegistrationDate() {
        return registrationDate;
    }
    public String getPhoneNo() {
        return phoneNo;
    }
    public String getEmailId() {
        return emailId;
    }
    public String getPin() {
        return pin;
    }
    public List<Integer> getOwnedAccountList() {
        return ownedAccountList;
    }
    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }
    
    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }
    
    public boolean isLocked() {
        return isLocked;
    }
    
    public void setLocked(boolean isLocked) {
        this.isLocked = isLocked;
    }
}






