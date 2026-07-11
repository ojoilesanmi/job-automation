const { chromium } = require('playwright');

async function submitApplication(url, method = 'browser', formAnswers = {}) {
    const browser = await chromium.launch({ headless: true });
    const context = await browser.newContext();
    const page = await context.newPage();

    try {
        console.log(`Navigating to: ${url}`);
        await page.goto(url, { waitUntil: 'networkidle', timeout: 30000 });

        const title = await page.title();
        console.log(`Page title: ${title}`);

        const screenshotPath = `./screenshots/${Date.now()}_pre_submit.png`;
        await page.screenshot({ path: screenshotPath, fullPage: true });
        console.log(`Screenshot saved: ${screenshotPath}`);

        const ats = detectAts(page.url(), title);
        console.log(`Detected ATS: ${ats}`);

        return {
            success: true,
            ats: ats,
            pageTitle: title,
            screenshotPath: screenshotPath,
            url: page.url()
        };
    } catch (error) {
        console.error(`Submission failed: ${error.message}`);
        return { success: false, error: error.message };
    } finally {
        await browser.close();
    }
}

function detectAts(url, title) {
    const lower = (url + ' ' + title).toLowerCase();
    if (lower.includes('greenhouse')) return 'greenhouse';
    if (lower.includes('lever')) return 'lever';
    if (lower.includes('workable')) return 'workable';
    if (lower.includes('ashby')) return 'ashby';
    if (lower.includes('bamboohr')) return 'bamboohr';
    if (lower.includes('icims')) return 'icims';
    if (lower.includes('taleo')) return 'taleo';
    if (lower.includes('successfactors')) return 'successfactors';
    return 'unknown';
}

if (require.main === module) {
    const args = process.argv.slice(2);
    const urlIdx = args.indexOf('--url');
    const methodIdx = args.indexOf('--method');
    const url = urlIdx >= 0 ? args[urlIdx + 1] : process.env.SUBMISSION_URL;
    const method = methodIdx >= 0 ? args[methodIdx + 1] : 'browser';

    if (!url) {
        console.error('Usage: node submit.js --url <url> [--method browser|api]');
        process.exit(1);
    }

    submitApplication(url, method).then(result => {
        console.log(JSON.stringify(result, null, 2));
        process.exit(result.success ? 0 : 1);
    });
}

module.exports = { submitApplication, detectAts };
