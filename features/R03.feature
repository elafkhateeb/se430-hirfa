Feature: Artisan registration and account

Scenario: Register a complete artisan account
  Given an artisan enters valid contact details and selects "Leather"
  When the artisan registration form is submitted
  Then an artisan account is created
  And the artisan is signed in to the product dashboard

Scenario: Reject an unapproved craft
  Given "Printing" is not an approved craft
  When an artisan submits that craft
  Then the account is not created
