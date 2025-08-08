package io.github.mov2day.unifiedtest.cucumber;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CucumberStepDefinitions {
    private int a;

    @Given("a variable is set to {int}")
    public void a_variable_is_set_to(Integer int1) {
        a = int1;
    }

    @When("I increment the variable by {int}")
    public void i_increment_the_variable_by(Integer int1) {
        a = a + int1;
    }

    @Then("the variable should be {int}")
    public void the_variable_should_be(Integer int1) {
        assertEquals(int1, a);
    }
}
