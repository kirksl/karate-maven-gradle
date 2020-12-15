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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KarateHook implements ExecutionHook
{
    private RPReporter rpReporter;
    private static final Logger logger = LoggerFactory.getLogger(KarateHook.class);

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
        try
        {
            this.rpReporter.startFeature(feature);
        }
        catch (Exception e)
        {
            logger.error("beforeFeature exception: {}", e.getMessage(), e);
        }
    
        return true;
    }

    @Override
    public void afterFeature(FeatureResult result, ExecutionContext context)
    {
        try
        {
            this.rpReporter.finishFeature(result);
        }
        catch (Exception e)
        {
            logger.error("afterFeature exception: {}", e.getMessage(), e);
        }
    }    

    @Override
    public void beforeAll(Results results)
    {
        try
        {
            this.rpReporter.startLaunch();
        }
        catch (Exception e)
        {
            logger.error("beforeAll exception: {}", e.getMessage(), e);
        }
    }

    @Override
    public void afterAll(Results results)
    {
        try
        {
            this.rpReporter.finishLaunch(results);
        }
        catch (Exception e)
        {
            logger.error("afterAll exception: {}", e.getMessage(), e);
        }
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