# The Podcast API 

This service is the clearing house for new podcast packages uploaded 
using the Podcast shell. A package consists of a manifest and 
the interview and introduction audio. Once uploaded, the packages are unzipped. 
Once unzipped entries are recorded in the database, and then sent 
using RabbitMQ to the Podcast processor. When those files are returned, eventually,
the resulting podcast is uploaded to Soundcloud and the Github pages site is updated.