/**
 * GitHub Action script to duplicate issues across multiple milestones
 * Triggered by issue comments starting with "/duplicate"
 */

module.exports = async ({github, context, core}) => {
    const comment = context.payload.comment.body;
    const issueNumber = context.payload.issue.number;
    const commenterLogin = context.payload.comment.user.login;

    console.log(`Processing comment from ${commenterLogin}: "${comment}"`);

    try {
        // Parse the comment to extract milestones
        const milestones = parseComment(comment);
        if (!milestones) {
            await postErrorComment(github, context, issueNumber,
                '❌ Invalid format. Please use: `/duplicate milestone1,milestone2,milestone3`\n\nExample: `/duplicate v2.0.0,v3.0.0,v4.0.0`');
            return;
        }

        if (milestones.length === 0) {
            await postErrorComment(github, context, issueNumber,
                '❌ No milestones specified. Please provide at least one milestone.');
            return;
        }

        console.log(`Duplicating issue #${issueNumber} to milestones: ${milestones.join(', ')}`);

        // Get original issue
        const originalIssue = await getOriginalIssue(github, context, issueNumber);
        console.log(`Original issue: "${originalIssue.title}"`);

        // Get milestone mapping
        const milestoneMap = await getMilestoneMap(github, context);

        // Create duplicate issues
        const results = await createDuplicateIssues(
            github,
            context,
            originalIssue,
            milestones,
            milestoneMap,
            issueNumber
        );

        // Post summary comment
        await postSummaryComment(github, context, issueNumber, results, commenterLogin, comment);

        console.log('Done! Posted summary comment.');
    } catch (error) {
        console.error('Error:', error);
        await postErrorComment(github, context, issueNumber,
            `❌ An error occurred while duplicating the issue: ${error.message}`);
    }
};

/**
 * Parse the comment to extract milestone names
 */
function parseComment(comment) {
    const duplicateMatch = comment.match(/^\/duplicate\s+(.+)$/m);
    if (!duplicateMatch) {
        return null;
    }

    const milestonesStr = duplicateMatch[1].trim();
    return milestonesStr.split(',').map(m => m.trim()).filter(m => m.length > 0);
}

/**
 * Get the original issue details
 */
async function getOriginalIssue(github, context, issueNumber) {
    const { data: originalIssue } = await github.rest.issues.get({
        owner: context.repo.owner,
        repo: context.repo.repo,
        issue_number: issueNumber
    });
    return originalIssue;
}

/**
 * Create a map of milestone names to IDs
 */
async function getMilestoneMap(github, context) {
    const { data: openMilestones } = await github.rest.issues.listMilestones({
        owner: context.repo.owner,
        repo: context.repo.repo,
        state: 'open'
    });

    const milestoneMap = {};
    openMilestones.forEach(milestone => {
        milestoneMap[milestone.title] = milestone.number;
    });

    return milestoneMap;
}

/**
 * Create duplicate issues for each milestone
 */
async function createDuplicateIssues(github, context, originalIssue, milestones, milestoneMap, issueNumber) {
    const results = [];

    for (const milestoneName of milestones) {
        const milestoneId = milestoneMap[milestoneName];

        if (!milestoneId) {
            let message = `Milestone "${milestoneName}" not found in ${JSON.stringify(milestoneMap)}, skipping...`;
            console.log(`⚠️  ${message}`);
            results.push(`❌ ${message}`);
            continue;
        }

        const newIssueBody = createNewIssueBody(originalIssue, milestoneName, issueNumber);

        try {
            const { data: newIssue } = await github.rest.issues.create({
                owner: context.repo.owner,
                repo: context.repo.repo,
                title: originalIssue.title,
                body: newIssueBody,
                milestone: milestoneId,
                labels: [ "forward-port" ].concat(originalIssue.labels.map(label => label.name)),
                assignees: originalIssue.assignees.map(assignee => assignee.login)
            });

            console.log(`✓ Created issue #${newIssue.number} for milestone "${milestoneName}"`);
            results.push(`✅ [#${newIssue.number}](${newIssue.html_url}) created for milestone "${milestoneName}"`);
        } catch (error) {
            console.log(`❌ Failed to create issue for milestone "${milestoneName}": ${error.message}`);
            results.push(`❌ Failed to create issue for milestone "${milestoneName}": ${error.message}`);
        }
    }

    return results;
}

/**
 * Create the body text for a new duplicate issue
 */
function createNewIssueBody(originalIssue, milestoneName, issueNumber) {
    return `Duplicate of #${issueNumber} for milestone ${milestoneName}

---

${originalIssue.body || ''}`;
}

/**
 * Post an error comment
 */
async function postErrorComment(github, context, issueNumber, message) {
    await github.rest.issues.createComment({
        owner: context.repo.owner,
        repo: context.repo.repo,
        issue_number: issueNumber,
        body: message
    });
}

/**
 * Post the summary comment with results
 */
async function postSummaryComment(github, context, issueNumber, results, commenterLogin, originalComment) {
    const summaryComment = `## Issue Duplication Results

Processed by @${commenterLogin}

${results.join('\n')}

---
*Triggered by comment: \`${originalComment.split('\n')[0]}\`*`;

    await github.rest.issues.createComment({
        owner: context.repo.owner,
        repo: context.repo.repo,
        issue_number: issueNumber,
        body: summaryComment
    });
}