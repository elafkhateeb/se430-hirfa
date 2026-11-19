Feature: Add a product to the cart

Scenario: Add an in-stock item
  Given a product has 12 items in stock
  When a customer adds 2 items to the cart
  Then the cart contains 2 of that product

Scenario: Reject a quantity above the per-product limit
  Given a product has 12 items in stock
  When a customer tries to add 6 items
  Then the cart is unchanged
