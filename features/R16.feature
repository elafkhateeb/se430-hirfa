Feature: Enforce the minimum order

Scenario: Accept an order at the minimum
  Given a customer cart has a subtotal of 5 JOD
  When the customer confirms the order
  Then checkout may continue

Scenario: Reject an order below the minimum
  Given a customer cart has a subtotal of 4.99 JOD
  When the customer confirms the order
  Then no order is created
