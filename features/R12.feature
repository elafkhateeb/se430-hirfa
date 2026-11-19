Feature: Cancel an order

Scenario: Cancel a new order within two hours
  Given an order is new and was placed 30 minutes ago
  When the customer cancels the order
  Then the order is cancelled
  And any wallet-funded amount is refunded

Scenario: Reject cancellation after two hours
  Given an order was placed more than two hours ago
  When the customer cancels the order
  Then the order remains unchanged
