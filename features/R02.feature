Feature: Customer login

Scenario: Sign in with valid customer credentials
  Given a customer account exists for "amal@example.com"
  When the customer submits the correct password
  Then the customer is signed in
  And customer navigation is displayed

Scenario: Reject an incorrect password
  Given a customer account exists for "amal@example.com"
  When the customer submits an incorrect password
  Then sign-in is rejected
