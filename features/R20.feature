Feature: Calculate customer loyalty tier

Scenario: Qualify for a higher tier
  Given a customer has completed enough orders for the next tier
  When the loyalty tier is calculated
  Then the customer receives the matching tier and discount

Scenario: Keep the base tier below the threshold
  Given a customer has fewer completed orders than the first threshold
  When the loyalty tier is calculated
  Then the customer remains in the base tier
