Feature: Sample Cucumber Test

  Scenario: A simple passing scenario
    Given a variable is set to 1
    When I increment the variable by 1
    Then the variable should be 2

  Scenario: A simple failing scenario
    Given a variable is set to 1
    When I increment the variable by 1
    Then the variable should be 3
