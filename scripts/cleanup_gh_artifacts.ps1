# cleanup_gh_artifacts.ps1
# Deletes the last 50 workflow runs to free up storage space on GitHub.

Write-Host "Fetching last 50 workflow runs..."
$runs = gh run list --limit 50 --json databaseId --jq '.[].databaseId'

if ($runs) {
    foreach ($run in $runs) {
        Write-Host "Deleting run ID: $run"
        gh api -X DELETE "repos/:owner/:repo/actions/runs/$run"
    }
    Write-Host "Cleanup complete."
} else {
    Write-Host "No runs found to delete."
}
