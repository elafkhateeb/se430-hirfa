Feature: Apply a promotion

Scenario: Apply HIRFA10 to an eligible subtotal
  Given the subtotal is at least 10 JOD
  When the customer enters "HIRFA10"
  Then a 10 percent discount is applied

Scenario: Reject HIRFA10 below its minimum
  Given the subtotal is below 10 JOD
  When the customer enters "HIRFA10"
  Then no discount is applied and an eligibility message is shown
