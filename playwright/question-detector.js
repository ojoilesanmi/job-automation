async function detectQuestions(page) {
    const questions = [];

    const formFields = await page.$$eval('input, select, textarea', (elements) => {
        return elements.map(el => ({
            type: el.tagName.toLowerCase() === 'select' ? 'select' : el.type || 'text',
            name: el.name || el.id || '',
            label: el.getAttribute('aria-label') || el.getAttribute('placeholder') || '',
            required: el.required || el.hasAttribute('aria-required'),
            options: el.tagName.toLowerCase() === 'select'
                ? Array.from(el.options).map(o => ({ value: o.value, label: o.text }))
                : undefined
        }));
    });

    for (const field of formFields) {
        if (field.name || field.label) {
            questions.push({
                fieldName: field.name,
                fieldType: field.type,
                label: field.label,
                required: field.required,
                options: field.options || null
            });
        }
    }

    const radioGroups = await page.$$eval('input[type="radio"]', (elements) => {
        const groups = {};
        elements.forEach(el => {
            const name = el.name;
            if (!groups[name]) groups[name] = [];
            groups[name].push({ value: el.value, label: el.getAttribute('aria-label') || el.value });
        });
        return Object.entries(groups).map(([name, options]) => ({
            fieldName: name,
            fieldType: 'radio',
            label: name,
            required: true,
            options
        }));
    });

    const allQuestions = [...questions, ...radioGroups];
    const uniqueByName = {};
    allQuestions.forEach(q => { if (q.fieldName) uniqueByName[q.fieldName] = q; });

    return Object.values(uniqueByName);
}

async function fillForm(page, answers) {
    for (const [fieldName, value] of Object.entries(answers)) {
        try {
            const selector = `input[name="${fieldName}"], input[id="${fieldName}"], textarea[name="${fieldName}"], textarea[id="${fieldName}"]`;
            const element = await page.$(selector);
            if (element) {
                const tagName = await element.evaluate(el => el.tagName.toLowerCase());
                const type = await element.evaluate(el => el.type);

                if (tagName === 'select') {
                    await element.selectOption(value);
                } else if (type === 'radio') {
                    await page.click(`input[name="${fieldName}"][value="${value}"]`);
                } else if (type === 'checkbox') {
                    if (value === 'true' || value === true) {
                        await element.check();
                    } else {
                        await element.uncheck();
                    }
                } else {
                    await element.fill(value);
                }
                console.log(`Filled field: ${fieldName}`);
            }
        } catch (e) {
            console.warn(`Failed to fill field ${fieldName}: ${e.message}`);
        }
    }
}

module.exports = { detectQuestions, fillForm };
