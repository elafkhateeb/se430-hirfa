Feature: Place an order

Scenario: Place an order with valid details
  Given a signed-in customer has a valid cart and delivery distance
  When the customer confirms the order with cash on delivery
  Then the order is stored with its items and calculated total
  And the purchased stock is reserved

Scenario: Reject an empty cart
  Given a signed-in customer has an empty cart
  When the customer confirms checkout
  Then no order is created
