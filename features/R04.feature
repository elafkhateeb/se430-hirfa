Feature: Add a handmade product

Scenario: Add a valid product
  Given an artisan enters "Copper Ring", a price of 12.50 JOD, and stock of 10
  When the artisan submits the product
  Then the product is saved in the artisan's craft
  And the product appears in the storefront

Scenario: Reject a product with a zero price
  Given an artisan enters a price of 0 JOD
  When the artisan submits the product
  Then the product is not saved
