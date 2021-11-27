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
import com.intuit.karate.Suite;
import com.intuit.karate.core.*;
import io.reactivex.Maybe;
import rp.com.google.common.base.Supplier;
import rp.com.google.common.base.Suppliers;
import rp.com.google.common.base.Strings;
import java.io.File;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import static rp.com.google.common.base.Strings.isNullOrEmpty;

class RPReporter
{
    private final Map<String, Date> featureStartDate = Collections.synchronizedMap(new HashMap<String, Date>());
    private Supplier<Launch> launch;
    private static final String INFO_LEVEL = "INFO";
    private static final String ERROR_LEVEL = "ERROR";
    private static final String TEST_TYPE = "TEST";
    private static final String STEP_TYPE = "STEP";
    private static final String PASSED = "passed";
    private static final String FAILED = "failed";
    private static final String SKIPPED = "SKIPPED";
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

    void finishLaunch(Suite suite)
    {
        try
        {
            Results.of(suite);
            File reportDir = new File(suite.reportDir);
            File[] files = reportDir.listFiles();
            for (File f : files)
            {
                if (f.isFile() && f.getAbsolutePath().endsWith("karate-timeline.html"))
                {
                    sendLaunchLog(f.getName(), INFO_LEVEL, getTime(), f);
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        FinishExecutionRQ finishLaunchRq = new FinishExecutionRQ();
        finishLaunchRq.setEndTime(getTime());
        finishLaunchRq.setStatus(getLaunchStatus(suite));

        this.launch.get().finish(finishLaunchRq);
    }

    synchronized void startFeature(Feature feature)
    {
        featureStartDate.put(getUri(feature), getTime());
    }
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    synchronized void finishFeature(FeatureResult featureResult)
    {
        Feature feature = featureResult.getFeature();
        String featureUri = getUri(feature);

        StartTestItemRQ startFeatureRq = new StartTestItemRQ();
        startFeatureRq.setName(!Strings.isNullOrEmpty(feature.getName()) ? feature.getName() : featureUri);
        startFeatureRq.setType(TEST_TYPE);
        startFeatureRq.setDescription(featureUri);

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
            startScenarioRq.setStartTime(new Date(scenarioResult.getStartTime()));
            startScenarioRq.setType(STEP_TYPE);

            Maybe<String> scenarioId = launch.get().startTestItem(featureId, startScenarioRq);

             if (getScenarioStatus(scenarioResult) != PASSED)
             {
                List<Map<String, Map>> steps = (List<Map<String, Map>>) scenarioResult.toCucumberJson().get("steps");
                for (Map<String, Map> step : steps)
                {
                    String stepName = "STEP: " + step.get("name");
                    Map stepResult = step.get("result");
                    String logLevel = PASSED.equals(stepResult.get("status")) ? INFO_LEVEL : ERROR_LEVEL;

                    String stepDocString = "";
                    if (step.containsKey("doc_string") && step.get("doc_string") != null)
                    {
                        stepDocString = "\n-----------------DOC_STRING-----------------\n" + step.get("doc_string");
                    }

                    if (step.containsKey("embeddings") && step.get("embeddings") != null)
                    {
                        ArrayList<HashMap<Object, Object>> embeddings = (ArrayList<HashMap<Object, Object>>) step.get("embeddings");
                        for (int i = 0; i < embeddings.size(); i++)
                        {
                            try
                            {
                                String message = stepName + " Screenshot " + (i + 1) + stepDocString;
                                HashMap<Object, Object> embed = embeddings.get(i);
                                SaveLogRQ.File rpFile = new SaveLogRQ.File();
                                rpFile.setName(String.valueOf(step.get("name")) + "_" + String.valueOf(i));
                                rpFile.setContentType((String) embed.get("mime_type"));
                                rpFile.setContent(java.util.Base64.getDecoder().decode(embed.get("data").toString()));
                            
                                sendLog(message, logLevel, scenarioId.blockingGet(), rpFile);
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                    else
                    {
                        String message = stepName + stepDocString;
                        sendLog(message, logLevel, scenarioId.blockingGet(), null);
                    }
                }
            }

            FinishTestItemRQ finishScenarioRq = new FinishTestItemRQ();
            finishScenarioRq.setEndTime(new Date(scenarioResult.getEndTime()));
            finishScenarioRq.setStatus(getScenarioStatus(scenarioResult));

            launch.get().finishTestItem(scenarioId, finishScenarioRq);
        }
        FinishTestItemRQ finishFeatureRq = new FinishTestItemRQ();
        finishFeatureRq.setEndTime(getTime());
        finishFeatureRq.setStatus(getFeatureStatus(featureResult));

        launch.get().finishTestItem(featureId, finishFeatureRq);
    }

    private String getLaunchStatus(Suite suite)
    {
        String launchStatus = SKIPPED;

        try
        {
            Stream<FeatureResult> featureResults = suite.getFeatureResults();

            if (featureResults.count() > 0)
            {
                Long failedCount = featureResults
                    .filter(s -> { return s.getScenarioCount() > 0; })
                    .filter(s -> { return s.getFailedCount() > 0; })
                    .collect(Collectors.counting());
    
                launchStatus = (failedCount > 0) ? FAILED : PASSED;
            }
        }
        catch(Exception e)
        {
            // do nothing
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
        return feature.getResource().getRelativePath();
    }

    private void sendLog(final String message, final String level, final String itemUuid, final SaveLogRQ.File file)
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
            
            if (file != null)
            {
                saveLogRq.setFile(file);
            }

            return saveLogRq;
        });
    }

    private void sendLaunchLog(final String message, final String level, final Date time, final File file)
    {
        if (file != null)
        {
            ReportPortal.emitLaunchLog(message, level, time, file);
        }
        else
        {
            ReportPortal.emitLaunchLog(message, level, time);
        }
    }
}