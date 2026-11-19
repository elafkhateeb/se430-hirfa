Feature: Report artisan sales

Scenario: Report sales for a valid period
  Given an artisan has completed orders in the selected period
  When a report is requested for that artisan and period
  Then the report includes order count and revenue

Scenario: Reject an invalid date range
  Given the report start date is after its end date
  When the report is requested
  Then an invalid-period result is returned
