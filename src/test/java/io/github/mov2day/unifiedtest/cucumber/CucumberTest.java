package io.github.mov2day.unifiedtest.cucumber;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(features = "src/test/resources/io/github/mov2day/unifiedtest/cucumber")
public class CucumberTest {
}
