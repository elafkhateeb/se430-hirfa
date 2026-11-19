Feature: Top up a wallet

Scenario: Add a valid top-up
  Given a customer wallet has a balance of 20 JOD
  When the customer adds 25 JOD
  Then the wallet balance becomes 45 JOD

Scenario: Reject an amount above the limit
  Given a customer requests a top-up of 1001 JOD
  When the top-up is processed
  Then the wallet balance is unchanged
