# The Podcast API 

![API](https://github.com/bootiful-podcast/api/workflows/API/badge.svg)

This service is the clearing house for new podcast packages uploaded 
using the Podcast shell. Here's the basic workflow. 

*   when the response comes from the processor, the resulting file is uploaded to whatever Podcast API we settle on
*   when the response comes from the processor, the resulting files are recorded in the PostgreSQL database. 
    This will in turn feed into the website's enumeration of new podcasts.
*   once the podcast has been uploaded, there should be an email sent using Sendgrid    

