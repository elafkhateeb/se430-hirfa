Feature: Estimate delivery time

Scenario: Estimate a standard delivery
  Given the delivery distance is 5 kilometers and the current load is 10 orders
  When the estimate is calculated
  Then the estimate includes preparation and travel time

Scenario: Add the high-load allowance
  Given the delivery distance is 5 kilometers and the current load is 16 orders
  When the estimate is calculated
  Then the high-load allowance is included
