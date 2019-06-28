<!DOCTYPE html>
<html lang="en">
<body>
<p>
    Hello, ${destinationName}. There are still ${bookmarkCount} outstanding CFPs in ${year}.
</P>
<ol>
    <#list bookmarks as b>
        <li>
            <a href="${b.href}"> ${b.description} </a> <br/>
            (<span style="font-size: smaller"><a href="${cfpStatusFunctionUrl}?id=${b.hash}"> Mark as Processed </a></span>)
        </li>
    </#list>
</ol>
</body>
</html>