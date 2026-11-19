Feature: Change a cart quantity

Scenario: Update a quantity within stock
  Given a cart contains 1 item and 8 are in stock
  When the customer changes the quantity to 4
  Then the cart contains 4 items

Scenario: Reject a quantity above current stock
  Given a cart contains 1 item and 3 are in stock
  When the customer changes the quantity to 4
  Then the cart remains unchanged
