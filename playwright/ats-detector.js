function detectAtsProvider(pageUrl, pageTitle, htmlContent = '') {
    const combined = (pageUrl + ' ' + pageTitle + ' ' + htmlContent).toLowerCase();
    const detectors = [
        { name: 'greenhouse', patterns: ['greenhouse.io', 'boards.greenhouse'] },
        { name: 'lever', patterns: ['lever.co', 'jobs.lever'] },
        { name: 'workable', patterns: ['workable.com', 'jobs.workable'] },
        { name: 'ashby', patterns: ['ashbyhq.com', 'jobs.ashby'] },
        { name: 'bamboohr', patterns: ['bamboohr.com'] },
        { name: 'icims', patterns: ['icims.com'] },
        { name: 'taleo', patterns: ['taleo.net', 'oracle.com/taleo'] },
        { name: 'successfactors', patterns: ['successfactors.com', 'sap.com'] },
        { name: 'smartrecruiters', patterns: ['smartrecruiters.com'] },
        { name: 'jobvite', patterns: ['jobvite.com'] },
    ];

    for (const detector of detectors) {
        if (detector.patterns.some(p => combined.includes(p))) {
            return detector.name;
        }
    }
    return 'unknown';
}

module.exports = { detectAtsProvider };
