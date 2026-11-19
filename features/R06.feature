Feature: Search the product catalog

Scenario: Filter products by craft
  Given the catalog contains products in several crafts
  When a visitor selects "Soap"
  Then only soap products are displayed

Scenario: Filter products by maximum price
  Given the catalog contains products at different prices
  When a visitor sets the maximum price to 10 JOD
  Then products above 10 JOD are excluded
