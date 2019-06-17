# The Podcast API 

This service is the clearing house for new podcast packages uploaded 
using the Podcast shell. Here's the basic workflow. 

*   shell submits all the files in a `.zip` file with a manifest (collectively known as 
    a podcast package or just "package") to an HTTP endpoint 
*   the handler for the HTTP endpoint launches an integration flow that...
*   unzips the package
*   stores the files in S3
*   stores metadata about the podcast in the SQL database
*   sends a message on RabbitMQ to the (Python-based) processor
*   when the response comes from the processor, the resulting file is uploaded to whatever Podcast API we settle on
