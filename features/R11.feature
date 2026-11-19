Feature: Process payment

Scenario: Pay with a valid card
  Given a customer has a valid card number
  When the customer submits a card payment
  Then the payment is confirmed

Scenario: Reject an invalid card number
  Given a customer submits a card number with fewer than 16 digits
  When the payment is processed
  Then the payment is rejected
