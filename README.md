# commonLogger
This project contains only a LoggingAspect class that can generate and write logs into a file for every method's entry and exit points where this piece of code is present.
## How to implement this logging in a particular java project?
### Option 1
- Copy the LoggingAspect class from this project to your Java project

### Option 2
- Convert the commonLogger project into a JAR and add it as a dependency to your Java project

***** Properties to be added to application.properties file *****
- Add the following property do the logs do contain the application name
  - ****spring.application.name=your_app_name****
- If logging to a file is needed add the following property, else the default is false
  - ****logging.aspectLogging.logFileEnabled=true****
