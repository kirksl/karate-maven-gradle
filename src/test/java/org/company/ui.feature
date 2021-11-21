Feature: ui

Scenario: ui 1
    * configure driver = { type: 'chrome' }
    * driver 'https://github.com/login'
    * screenshot()
    * screenshot()
    * screenshot()
    * assert 1 == 0