package Banking_system.Code;


import java.util.Map;



import java.util.HashMap;


public class Bank {
    // Singleton instance
    private static Bank instance;

    // customer id mapped to customer
    private Map<Integer, Customer> customersMap = new HashMap<>();
    private Map<Integer, Account> accountMap = new HashMap<>();
    
    private int nextAccountNumber = 2000;
    private int nextCustomerId = 1;

    // Private constructor prevents direct external instantiation (Singleton Pattern)
    private Bank() {}

    // Global access point to the Singleton instance
    public static synchronized Bank getInstance() {
        if (instance == null) {
            instance = new Bank();
        }
        return instance;
    }

    // Optional reset method for unit testing / test harnesses
    public static synchronized void resetInstance() {
        instance = null;
    }
    
    public void addCustomer(Customer customer){
        int customerId = nextCustomerId++;
        customer.setCustomerID(customerId);
        customersMap.put(customerId, customer);
    }
    
    // 2. New method specifically for opening accounts
    public void openAccountForCustomer(int customerId, Account account) {
        if (!customersMap.containsKey(customerId)) {
            System.out.println("Customer not found.");
            return;
        }
        
        int accountNo = nextAccountNumber++;
        account.setAccountNo(accountNo);
        account.setCustomerId(customerId);
        
        // Save account in the bank's master ledger
        accountMap.put(accountNo, account);
        
        // Link the account number to the customer's profile
        Customer customer = customersMap.get(customerId);
        customer.addAccountNo(accountNo);
    }
    
    
    public void setCustomersMap(Map<Integer, Customer> customers) {
        if (customers != null && !customers.isEmpty()) {
            this.customersMap = customers;
            
            // Restore counters based on highest existing ID to prevent duplicates
            int maxId = 0;
            for (Integer id : customers.keySet()) {
                if (id > maxId) {
                    maxId = id;
                }
            }
            this.nextCustomerId = maxId + 1;
            
        }
    }
    public void setAccountMap(Map<Integer, Account> accounts) {
        if (accounts != null && !accounts.isEmpty()) {
            this.accountMap = accounts;

            int maxAccNo = 1999;
            for (Integer accNo : accounts.keySet()) {
                if (accNo > maxAccNo) {
                    maxAccNo = accNo;
                }
            }
            this.nextAccountNumber = maxAccNo + 1;
        }
    }
        
    public Map<Integer, Account> getAccountMap() {
        return accountMap;
    }
    public Map<Integer, Customer> getCustomersMap() {
        return customersMap;
    }
    
    public boolean findCustomer(int id){
        return customersMap.containsKey(id);
    }


    public Customer getCustomer(int id){
        return customersMap.get(id);
    }

    public boolean deleteCustomer(int id){
        return customersMap.remove(id) != null;
    }





}
