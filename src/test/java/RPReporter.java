import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.intuit.karate.Results;
import com.intuit.karate.core.*;
import io.reactivex.Maybe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rp.com.google.common.base.Supplier;
import rp.com.google.common.base.Suppliers;
import rp.com.google.common.base.Strings;
import java.util.*;
import static rp.com.google.common.base.Strings.isNullOrEmpty;

class RPReporter
{
    private static final Logger logger = LoggerFactory.getLogger(RPReporter.class);
    private final Map<String, Date> featureStartDate = Collections.synchronizedMap(new HashMap<String, Date>());
    private Supplier<Launch> launch;
    private static final String INFO_LEVEL = "INFO";
    private static final String ERROR_LEVEL = "ERROR";
    private static final String TEST_TYPE = "TEST";
    private static final String STEP_TYPE = "STEP";
    private static final String PASSED = "passed";
    private static final String FAILED = "failed";
    private static final String SKIPPED = "skipped";
    private static final String SKIPPED_ISSUE_KEY = "skippedIssue";
    
    RPReporter()
    {
    }

    void startLaunch()
    {
        this.launch = Suppliers.memoize(new Supplier<Launch>()
        {
            // should not be lazy
            private final Date startTime = getTime();
            
            @Override
            public Launch get()
            {
                final ReportPortal reportPortal = ReportPortal.builder().build();
                ListenerParameters parameters = reportPortal.getParameters();

                StartLaunchRQ startLaunchRq = new StartLaunchRQ();
                startLaunchRq.setName(parameters.getLaunchName());
                startLaunchRq.setStartTime(startTime);
                startLaunchRq.setMode(parameters.getLaunchRunningMode());
                startLaunchRq.setAttributes(parameters.getAttributes());

                if (!isNullOrEmpty(parameters.getDescription()))
                {
                    startLaunchRq.setDescription(parameters.getDescription());
                }

                startLaunchRq.setRerun(parameters.isRerun());

                if (!isNullOrEmpty(parameters.getRerunOf()))
                {
                    startLaunchRq.setRerunOf(parameters.getRerunOf());
                }

                if (parameters.getSkippedAnIssue() != null)
                {
                    ItemAttributesRQ skippedIssueAttribute = new ItemAttributesRQ();
                    skippedIssueAttribute.setKey(SKIPPED_ISSUE_KEY);
                    skippedIssueAttribute.setValue(parameters.getSkippedAnIssue().toString());
                    skippedIssueAttribute.setSystem(true);
                    startLaunchRq.getAttributes().add(skippedIssueAttribute);
                }

                return reportPortal.newLaunch(startLaunchRq);
            }
        });

        this.launch.get().start();
    }

    void finishLaunch(Results results)
    {
        FinishExecutionRQ finishLaunchRq = new FinishExecutionRQ();
        finishLaunchRq.setEndTime(getTime());
        finishLaunchRq.setStatus(getLaunchStatus(results));

        launch.get().finish(finishLaunchRq);
    }

    synchronized void startFeature(Feature feature)
    {
        featureStartDate.put(getUri(feature), getTime());
    }

    synchronized void finishFeature(FeatureResult featureResult)
    {
        Feature feature = featureResult.getFeature();
        String featureUri = getUri(feature);

        StartTestItemRQ startFeatureRq = new StartTestItemRQ();
        startFeatureRq.setName(!Strings.isNullOrEmpty(feature.getName()) ? feature.getName() : featureResult.getDisplayUri());
        startFeatureRq.setType(TEST_TYPE);
        startFeatureRq.setDescription(featureResult.getDisplayUri());

        if (featureStartDate.containsKey(featureUri))
        {
            startFeatureRq.setStartTime(featureStartDate.get(featureUri));
        }
        else
        {
            startFeatureRq.setStartTime(getTime());
        }

        if (feature.getTags() != null && !feature.getTags().isEmpty())
        {
            List<Tag> tags = feature.getTags();
            Set<ItemAttributesRQ> attributes = new HashSet<ItemAttributesRQ>();

            tags.forEach((tag) ->
            {
                attributes.add(new ItemAttributesRQ(null, tag.getName()));
            });

            startFeatureRq.setAttributes(attributes);
        }

        Maybe<String> featureId = launch.get().startTestItem(null, startFeatureRq);

        for (ScenarioResult scenarioResult : featureResult.getScenarioResults())
        {
            StartTestItemRQ startScenarioRq = new StartTestItemRQ();
            startScenarioRq.setDescription(scenarioResult.getScenario().getDescription());
            startScenarioRq.setName(scenarioResult.getScenario().getName());

            if (featureStartDate.containsKey(featureUri))
            {
                startScenarioRq.setStartTime(new Date(scenarioResult.getStartTime() + featureStartDate.get(featureUri).getTime()));
            }
            else
            {
                startScenarioRq.setStartTime(getTime());
            }

            startScenarioRq.setType(STEP_TYPE);

            Maybe<String> scenarioId = launch.get().startTestItem(featureId, startScenarioRq);

            if (getScenarioStatus(scenarioResult) != PASSED)
            {
                List<Map<String, Map>> stepResultsToMap = (List<Map<String, Map>>) scenarioResult.toMap().get("steps");
                
                for (Map<String, Map> step : stepResultsToMap)
                {
                    Map stepResult = step.get("result");
                    String logLevel = PASSED.equals(stepResult.get("status")) ? INFO_LEVEL : ERROR_LEVEL;
                    if (step.get("doc_string") != null)
                    {
                        sendLog("STEP: " + step.get("name") +
                                "\n-----------------DOC_STRING-----------------\n" + step.get("doc_string"), logLevel, scenarioId.blockingGet());
                    }
                    else
                    {
                        sendLog("STEP: " + step.get("name"), logLevel, scenarioId.blockingGet());
                    }
                }
            }

            FinishTestItemRQ finishScenarioRq = new FinishTestItemRQ();

            if (featureStartDate.containsKey(featureUri))
            {
                finishScenarioRq.setEndTime(new Date(scenarioResult.getEndTime() + featureStartDate.get(featureUri).getTime()));
            }
            else
            {
                finishScenarioRq.setEndTime(getTime());
            }

            finishScenarioRq.setStatus(getScenarioStatus(scenarioResult));

            launch.get().finishTestItem(scenarioId, finishScenarioRq);
        }

        FinishTestItemRQ finishFeatureRq = new FinishTestItemRQ();
        finishFeatureRq.setEndTime(getTime());
        finishFeatureRq.setStatus(getFeatureStatus(featureResult));

        launch.get().finishTestItem(featureId, finishFeatureRq);
    }

    private String getLaunchStatus(Results results)
    {
        String launchStatus = SKIPPED;

        if (results.getScenarioCount() > 0)
        {
            if (results.getFailCount() > 0)
            {
                launchStatus = FAILED;
            }
            else
            {
                launchStatus = PASSED;
            }
        }

        return launchStatus;
    }

    private String getFeatureStatus(FeatureResult featureResult)
    {
        String featureStatus = SKIPPED;

        if (featureResult.getScenarioCount() > 0)
        {
            if (featureResult.isFailed())
            {
                featureStatus = FAILED;
            }
            else
            {
                featureStatus = PASSED;
            }
        }

        return featureStatus;
    }

    private String getScenarioStatus(ScenarioResult scenarioResult)
    {
        String scenarioStatus = SKIPPED;

        if (scenarioResult.getStepResults() != null && scenarioResult.getStepResults().size() > 0)
        {
            if (scenarioResult.getFailedStep() != null)
            {
                scenarioStatus = FAILED;
            }
            else
            {
                scenarioStatus = PASSED;
            }
        }

        return scenarioStatus;
    }

    private Date getTime()
    {
        return Calendar.getInstance().getTime();
    }

    private String getUri(Feature feature)
    {
        return feature.getResource().getPath().toString();
    }

    private void sendLog(final String message, final String level, final String itemUuid)
    {
        ReportPortal.emitLog(itemId ->
        {
            SaveLogRQ saveLogRq = new SaveLogRQ();
            saveLogRq.setMessage(message);
            //saveLogRq.setTestItemId(itemId); // rp < v5
            //saveLogRq.setUuid(itemId);       // rp < v5
            saveLogRq.setItemUuid(itemUuid);   // rp >= v5
            saveLogRq.setLevel(level);
            saveLogRq.setLogTime(getTime());

            return saveLogRq;
        });
    }
}