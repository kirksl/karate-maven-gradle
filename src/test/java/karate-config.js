function fn()
{
   var env = karate.env;
   karate.log('karate.env system property was:', env);

   if (!env)
   {
      env = 'dev';
   }

   karate.log('karate environment set to:', env);

   var config = karate.read('classpath:application.yml');
   karate.log('baseOrderUrl configured for tests: ' + config.baseOrderUrl);

   if (env == 'dev')
   {
      var Factory = Java.type('MockSpringMvcServlet');
      karate.configure('httpClientInstance', Factory.getMock());
   }
   else if (env == 'stg')
   {
      // customize
   }
   else if (env == 'prd')
   {
      // customize
   }

   karate.configure('logPrettyRequest', true);
   karate.configure('logPrettyResponse', true);

   return config;
}