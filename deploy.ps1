$env:HTTP_PROXY="http://127.0.0.1:7890"
$env:HTTPS_PROXY="http://127.0.0.1:7890"
$response = Invoke-RestMethod 'https://www.google.com' -Method 'GET'
if ($response.Length -gt 0) {
    Write-Output "set proxy success"
}
mvn clean deploy -Prelease