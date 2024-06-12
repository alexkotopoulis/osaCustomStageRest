# Oracle GoldenGate Stream Analytics Custom Stage Example for REST Call

Example for building a JAR file to be used in a custom stage in Oracle GoldenGate Stream Analytics (GGSA)
Written to be used with Oracle GoldenGate Stream Analytics (Installer or OCI Marketplace) version 19.1.0.0.7+
 
## Requirements
- Oracle GoldenGate Stream Analytics 19.1.0.0.7+ environment (Cannot be used with OCI GoldenGate cloud service)
- JDK 1.8 and Maven build environment

The custom Java stage uses the [REST call example from the GGSA documentation](https://docs.oracle.com/en/middleware/fusion-middleware/osa/19.1/using/adding-custom-functions-and-custom-stages.html#GUID-B04169F3-6BD0-4C93-B3AD-FFE1DDD28665). It uses the Google Books API to lookup title and publishing year using a book ISBN number. This requires Internet access from your GGSA environment. If your environment requires proxy configuration, change the CustomStageRest.properties file accordingly.  

## Steps
1. Clone GIT repository to a local directory in build environment
2. Copy the following file from the GGSA environment onto the local directory created in Step 1:  
`$OSA_HOME/osa-base/extensibility-api/osa.spark-cql.extensibility.api.jar`
3. Run the script to create an entry for the extensibility API jar in your local Maven repository  
`sh createLocalMavenOsa.sh`
4. Run Maven install on command line or using development environment  
`mvn install`
 This command will create a Custom Stage JAR ar `target/osaCustomStageRest-0.0.1-SNAPSHOT-jar-with-dependencies.jar`
5. In the GGSA Console, [create a custom JAR object](https://docs.oracle.com/en/middleware/fusion-middleware/osa/19.1/using/adding-custom-functions-and-custom-stages.html#UGOSA-GUID-263756AC-339A-4E38-8C9F-8C310CDD2D34).

6. Create a pipeline that has ISBN strings as one of its fields. You can use the `books.csv` file in the project as input for a file stream. [Add a Custom JAR stage](https://docs.oracle.com/en/middleware/fusion-middleware/osa/19.1/using/adding-custom-functions-and-custom-stages.html#UGOSA-GUID-23080F34-B9F0-4AC9-AFA5-0056AA765C5D) into the pipeline with the ISBN string as input.

7. When running the pipeline, the output of the Custom JAR Stage should show the Title of the book as well as published date. 