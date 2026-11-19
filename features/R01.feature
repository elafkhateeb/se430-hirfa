Feature: Customer registration

Scenario: Register with valid details
  Given a visitor enters "Nour Ayada", "0771112223", "nour@example.com", and "NourPass2026"
  When the customer registration form is submitted
  Then a customer account is created
  And the new customer is signed in

Scenario: Reject an email address already in use
  Given an account already uses "amal@example.com"
  When a visitor registers with the same email address
  Then registration is rejected with a duplicate-email message
