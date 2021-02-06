import com.intuit.karate.Constants;
import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class KarateRunner
{
    @Test
    void testParallel()
    {
        String env = System.getProperty(Constants.KARATE_ENV, "dev").trim();
        Boolean rp = Boolean.parseBoolean(System.getProperty("reportportal", "false"));

        Runner.Builder rb = Runner.builder();
        rb.path("classpath:org/company");
        rb.tags("~@ignore");

        if (rp)
        {
            rb.hook(new KarateHook());
        }

        if (env.isEmpty() || env.toLowerCase() == "dev")
        {
            rb.clientFactory(MockSpringMvcServlet.getMock());
        }

        Results results = rb.parallel(1);

        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}