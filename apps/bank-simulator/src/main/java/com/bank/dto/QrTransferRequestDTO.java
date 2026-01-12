package com.bank.dto;

public class QrTransferRequestDTO {
    private String receiverAccount; // Račun Web Shopa (izvučen iz QR koda)
    private Double amount;          // Iznos (izvučen iz QR koda)
    private String email;           // <--- NOVO: Korisnik unosi email (npr. jana@gmail.com)
    private String pin;

    // Getteri i Setteri
    public String getReceiverAccount() { return receiverAccount; }
    public void setReceiverAccount(String receiverAccount) { this.receiverAccount = receiverAccount; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
}