Feature: Advance an order status

Scenario: Advance to the next valid state
  Given an order is in the new state
  When the order advances
  Then its state becomes preparing

Scenario: Do not advance a terminal order
  Given an order is delivered
  When the order advances
  Then its state remains delivered
