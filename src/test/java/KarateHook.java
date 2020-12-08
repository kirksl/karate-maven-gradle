import com.intuit.karate.Results;
import com.intuit.karate.http.HttpRequestBuilder;
import com.intuit.karate.core.ExecutionHook;
import com.intuit.karate.core.Feature;
import com.intuit.karate.core.FeatureResult;
import com.intuit.karate.core.Scenario;
import com.intuit.karate.core.ScenarioContext;
import com.intuit.karate.core.ScenarioResult;
import com.intuit.karate.core.ExecutionContext;
import com.intuit.karate.core.Step;
import com.intuit.karate.core.StepResult;
import com.intuit.karate.core.PerfEvent;

public class KarateHook implements ExecutionHook
{
    private RPReporter rpReporter;

    public KarateHook()
    {
        this.rpReporter = new RPReporter();
    }

    @Override
    public boolean beforeScenario(Scenario scenario, ScenarioContext context)
    {
        return true;
    }

    @Override
    public void afterScenario(ScenarioResult result, ScenarioContext context)
    {
    }    

    @Override
    public boolean beforeFeature(Feature feature, ExecutionContext context)
    {
        this.rpReporter.startFeature(context.result);
        return true;
    }

    @Override
    public void afterFeature(FeatureResult result, ExecutionContext context)
    {
        this.rpReporter.finishFeature(context.result);
    }    

    @Override
    public void beforeAll(Results results)
    {
        this.rpReporter.startLaunch();
    }

    @Override
    public void afterAll(Results results)
    {
        this.rpReporter.finishLaunch();
    }        

    @Override
    public boolean beforeStep(Step step, ScenarioContext context)
    {
        return true;
    }

    @Override
    public void afterStep(StepResult result, ScenarioContext context)
    {
    }        
        
    @Override
    public String getPerfEventName(HttpRequestBuilder req, ScenarioContext context)
    {
        return null;
    }    
    
    @Override
    public void reportPerfEvent(PerfEvent event)
    {
    }
}