@tests=order
Feature: order

    Background:
        * url baseOrderUrl

    @test=order1
    Scenario: order 1
        * print 'order 1'

    @test=order2
    Scenario: order 2
        * print 'order 2'

    @test=order3
    Scenario: order 3
        Given path '/order'
        When method get
        Then status 200
    