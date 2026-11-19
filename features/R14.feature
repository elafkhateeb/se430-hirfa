Feature: Rate an artisan

Scenario: Submit a valid rating
  Given an artisan profile is available
  When a customer submits 5 stars
  Then the rating count and average are updated

Scenario: Reject a rating outside the supported range
  Given an artisan profile is available
  When a customer submits 6 stars
  Then the rating is rejected
