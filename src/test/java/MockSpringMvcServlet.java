import com.intuit.karate.mock.servlet.MockHttpClient;
import com.intuit.karate.core.ScenarioEngine;
import com.intuit.karate.http.HttpClient;
import com.intuit.karate.http.HttpClientFactory;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public class MockSpringMvcServlet implements HttpClientFactory
{
    private final Servlet servlet;
    private final ServletContext servletContext;

    public MockSpringMvcServlet()
    {
        this.servletContext = new MockServletContext();
        this.servlet = initServlet();
    }

    private Servlet initServlet()
    {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.register(MockConfig.class);
        context.setServletContext(this.servletContext);

        DispatcherServlet servlet = new DispatcherServlet(context);
        ServletConfig servletConfig = new MockServletConfig();

        try
        {
            servlet.init(servletConfig);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        return servlet;
    }

    public static MockSpringMvcServlet getMock()
    {
        return new MockSpringMvcServlet();
    }

    @Override
    public HttpClient create(ScenarioEngine engine)
    {
        return new MockHttpClient(engine, servlet, servletContext);
    }
}