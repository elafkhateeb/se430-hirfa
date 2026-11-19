Feature: Restock a product

Scenario: Restock an available product
  Given an artisan owns a product with 4 items in stock
  When the artisan adds 6 items
  Then the stock becomes 10

Scenario: Reject restocking above the stock limit
  Given a product has 499 items in stock
  When the artisan adds 2 items
  Then the stock remains unchanged
