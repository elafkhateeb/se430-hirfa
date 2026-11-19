Feature: Calculate delivery fees

Scenario: Apply a distance-based fee
  Given an order subtotal is below the free-delivery threshold
  When the delivery distance is 7 kilometers
  Then the fee for the matching distance band is charged

Scenario: Waive the fee for a qualifying order
  Given an order subtotal is at least 100 JOD
  When delivery fees are calculated
  Then the delivery fee is 0 JOD
