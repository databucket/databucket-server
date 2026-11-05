// Generates the "user" part of the DEMO dataset (src/main/resources/datasets/demo.json): the class, enums, icons,
// columns, filters and views of the users buckets, their data (dev/int/prd-users, 1074 rows each), the demo users,
// the history of 30 days and the descriptions of buckets and tags. The rest of the dataset (scheduler, config, jobs)
// is kept as it is in the file, except the data id of the scheduler rows (a random user).
//
//   node tools/demo-dataset/generate.mjs
//
// Deterministic (a seeded random generator): running it again gives the same file, so changes are visible in a diff.
// Dates in the data are relative to GENERATED_AT; the times of the history are relative to the start of the day of
// the import (0 = midnight), so the member and the admin work during the day.

import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const FILE = path.join(ROOT, 'src/main/resources/datasets/demo.json');

const GENERATED_AT = Date.parse('2026-11-05T12:00:00Z');
const ROWS_PER_BUCKET = 1074;
const DAYS = 30;
const DAY = 24 * 3600 * 1000;
const USER_BUCKETS = ['dev-users', 'int-users', 'prd-users'];

// --- ids in the dataset owned by this generator (replaced on every run) ---------------------------------------
const CLASS_USER = 1;
const ENUM = {
    eyeColor: 1, height: 2, gender: 7, country: 8, hairColor: 9, industry: 10, companySize: 11, channel: 12,
    paymentMethod: 13, cardBrand: 14, currency: 15, paymentStatus: 16, accountType: 17, accountStatus: 18, authRole: 19
};
const TAG = {active: 1, trash: 2, deleted: 3, scheduled: 4, analysis: 5};
const COLUMNS_USER = 7;
const FIRST_OWN_COLUMNS = 12;   // columns of the user class from older versions of the generator, removed
const FIRST_OWN_FILTER = 12;
const FIRST_OWN_VIEW = 13;
// the views of users: all features (ViewFeatures.js) but Removal (5) and Duplicate (11)
const USER_FEATURES = [1, 2, 3, 4, 6, 7, 8, 9, 10, 12, 13, 14, 15, 16];
const USER_SORT = 'desc(id)';

// --- random -------------------------------------------------------------------------------------------------
function mulberry32(seed) {
    return () => {
        seed |= 0;
        seed = seed + 0x6D2B79F5 | 0;
        let t = Math.imul(seed ^ seed >>> 15, 1 | seed);
        t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t;
        return ((t ^ t >>> 14) >>> 0) / 4294967296;
    };
}

let random = mulberry32(1);
const int = (min, max) => min + Math.floor(random() * (max - min + 1));
const pick = list => list[Math.floor(random() * list.length)];
const chance = p => random() < p;
const digits = n => Array.from({length: n}, () => int(0, 9)).join('');
const round2 = x => Math.round(x * 100) / 100;
const weighted = entries => {
    const total = entries.reduce((sum, [, w]) => sum + w, 0);
    let r = random() * total;
    for (const [value, w] of entries)
        if ((r -= w) < 0)
            return value;
    return entries[entries.length - 1][0];
};
const uuid = key => {
    const h = crypto.createHash('md5').update('databucket-demo:' + key).digest('hex');
    return `${h.slice(0, 8)}-${h.slice(8, 12)}-4${h.slice(13, 16)}-a${h.slice(17, 20)}-${h.slice(20, 32)}`;
};
const iso = ms => new Date(ms).toISOString();
const day = ms => iso(ms).slice(0, 10);

// --- icons --------------------------------------------------------------------------------------------------
const FRAME = '<rect x="0.25" y="0.25" width="23.5" height="15.5" fill="none" stroke="#9e9e9e" stroke-width="0.5"/>';
const svg = body => `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 16">${body}${FRAME}</svg>`;
const usStripes = Array.from({length: 6}, (_, i) =>
    `<rect y="${(1.23 * (2 * i + 1)).toFixed(2)}" width="24" height="1.23" fill="#ffffff"/>`).join('');
const usStars = Array.from({length: 4}, (_, r) => Array.from({length: 5}, (_, c) =>
    `<circle cx="${(1.1 + c * 1.95).toFixed(2)}" cy="${(1.05 + r * 2.05).toFixed(2)}" r="0.42" fill="#ffffff"/>`).join('')).join('');
const FLAGS = {
    US: svg(`<rect width="24" height="16" fill="#b22234"/>${usStripes}<rect width="10" height="8.62" fill="#3c3b6e"/>${usStars}`),
    PL: svg('<rect width="24" height="8" fill="#ffffff"/><rect y="8" width="24" height="8" fill="#dc143c"/>'),
    DE: svg('<rect width="24" height="5.34" fill="#000000"/><rect y="5.33" width="24" height="5.34" fill="#dd0000"/><rect y="10.66" width="24" height="5.34" fill="#ffce00"/>'),
    GB: svg('<rect width="24" height="16" fill="#012169"/><path d="M0 0L24 16M24 0L0 16" stroke="#ffffff" stroke-width="3"/>'
        + '<path d="M0 0L24 16M24 0L0 16" stroke="#c8102e" stroke-width="1"/><path d="M12 0V16M0 8H24" stroke="#ffffff" stroke-width="5"/>'
        + '<path d="M12 0V16M0 8H24" stroke="#c8102e" stroke-width="3"/>'),
    FR: svg('<rect width="8" height="16" fill="#002395"/><rect x="8" width="8" height="16" fill="#ffffff"/><rect x="16" width="8" height="16" fill="#ed2939"/>')
};
// a payment card (the shape of the Material "credit_card" icon); without its own fill it takes the color of the item
const CARD_SVG = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path d="M20 4H4c-1.11 0-1.99.89-1.99 2L2 18c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V6c0-1.11-.89-2-2-2zm0 14H4v-6h16v6zm0-10H4V6h16v2z"/></svg>';

const icon = (name, color) => ({svg: null, name, color});
const svgIcon = (markup, color = null) => ({svg: markup, name: null, color});
const item = (value, text, itemIcon = null) => ({icon: itemIcon, text, value});

// --- enums --------------------------------------------------------------------------------------------------
const enums = [
    {id: ENUM.eyeColor, name: 'Eye colors', description: 'Most popular eye colors', iconsEnabled: false, items: [
        item('blue', 'Blue'), item('brown', 'Brown'), item('green', 'Green'), item('grey', 'Grey'), item('hazel', 'Hazel')]},
    {id: ENUM.height, name: 'Height', description: 'Height ranges', iconsEnabled: false, items: [
        item('h<1.6m', 'Below 1.6 m'), item('1.6m<h<1.8m', 'Between 1.6 m and 1.8 m'), item('1.8m<h<2m', 'Between 1.8 m and 2 m'), item('h>2m', 'Above 2 m')]},
    {id: ENUM.gender, name: 'Gender', description: null, iconsEnabled: false, items: [
        item('female', 'Female'), item('male', 'Male'), item('other', 'Other')]},
    {id: ENUM.country, name: 'Country', description: 'Countries, with flags (SVG icons)', iconsEnabled: true, items: [
        item('US', 'United States', svgIcon(FLAGS.US)), item('PL', 'Poland', svgIcon(FLAGS.PL)),
        item('DE', 'Germany', svgIcon(FLAGS.DE)), item('GB', 'United Kingdom', svgIcon(FLAGS.GB)),
        item('FR', 'France', svgIcon(FLAGS.FR))]},
    {id: ENUM.hairColor, name: 'Hair color', description: null, iconsEnabled: false, items: [
        item('black', 'Black'), item('brown', 'Brown'), item('blond', 'Blond'), item('red', 'Red'), item('grey', 'Grey'), item('none', 'None')]},
    {id: ENUM.industry, name: 'Industry', description: null, iconsEnabled: false, items: [
        item('it', 'IT & software'), item('finance', 'Finance & insurance'), item('retail', 'Retail & e-commerce'),
        item('manufacturing', 'Manufacturing'), item('logistics', 'Transport & logistics'), item('healthcare', 'Healthcare'),
        item('education', 'Education'), item('media', 'Media & printing')]},
    {id: ENUM.companySize, name: 'Company size', description: 'Number of employees', iconsEnabled: false, items: [
        item('1-10', '1-10'), item('11-50', '11-50'), item('51-250', '51-250'), item('251-1000', '251-1000'), item('1000+', 'More than 1000')]},
    {id: ENUM.channel, name: 'Contact channel', description: 'Preferred contact channel', iconsEnabled: true, items: [
        item('email', 'E-mail', icon('mail', '#1976d2')), item('sms', 'SMS', icon('sms', '#7b1fa2')),
        item('phone', 'Phone', icon('call', '#2e7d32')), item('none', 'No contact', icon('do_not_disturb_on', '#9e9e9e'))]},
    {id: ENUM.paymentMethod, name: 'Payment method', description: 'The card is an SVG icon', iconsEnabled: true, items: [
        item('card', 'Card', svgIcon(CARD_SVG, '#1565c0')), item('transfer', 'Bank transfer', icon('request_quote', '#00897b')),
        item('blik', 'BLIK', icon('smartphone', '#e65100')), item('cash', 'Cash on delivery', icon('monetization_on', '#558b2f'))]},
    {id: ENUM.cardBrand, name: 'Card brand', description: null, iconsEnabled: false, items: [
        item('visa', 'Visa'), item('mastercard', 'Mastercard'), item('amex', 'American Express')]},
    {id: ENUM.currency, name: 'Currency', description: null, iconsEnabled: false, items: [
        item('USD', 'US dollar'), item('PLN', 'Polish zloty'), item('EUR', 'Euro'), item('GBP', 'Pound sterling')]},
    {id: ENUM.paymentStatus, name: 'Payment status', description: null, iconsEnabled: true, items: [
        item('paid', 'Paid', icon('task_alt', '#2e7d32')), item('pending', 'Pending', icon('hourglass_top', '#f9a825')),
        item('failed', 'Failed', icon('error', '#c62828')), item('refunded', 'Refunded', icon('undo', '#1565c0'))]},
    {id: ENUM.accountType, name: 'Account type', description: null, iconsEnabled: true, items: [
        item('standard', 'Standard', icon('person_outline', '#757575')), item('premium', 'Premium', icon('star', '#f9a825')),
        item('vip', 'VIP', icon('workspace_premium', '#8e24aa'))]},
    {id: ENUM.accountStatus, name: 'Account status', description: null, iconsEnabled: true, items: [
        item('active', 'Active', icon('check_circle', '#5eb748')), item('suspended', 'Suspended', icon('remove_circle_outline', '#ef6c00')),
        item('locked', 'Locked', icon('lock', '#c62828')), item('closed', 'Closed', icon('cancel', '#9e9e9e'))]},
    {id: ENUM.authRole, name: 'Role', description: 'Role of a test account in the tested application', iconsEnabled: true, items: [
        item('ADMIN', 'Admin', icon('admin_panel_settings', '#c62828')), item('MEMBER', 'Member', icon('person', '#1976d2'))]}
];

// --- the user class -----------------------------------------------------------------------------------------
const prop = (path, title, type = 'string', enumId = null) => ({path, type, uuid: uuid(path), title, enumId});
const userClass = {
    id: CLASS_USER, name: 'user', description: 'Tested application user',
    configuration: [
        prop('$.firstName', 'First name'),
        prop('$.lastName', 'Last name'),
        prop('$.gender', 'Gender', 'select', ENUM.gender),
        prop('$.birthDate', 'Birth date', 'date'),
        prop('$.contact.email', 'E-mail'),
        prop('$.contact.phone', 'Phone'),
        prop('$.contact.mobile', 'Mobile'),
        prop('$.contact.preferredChannel', 'Contact channel', 'select', ENUM.channel),
        prop('$.address.street', 'Street'),
        prop('$.address.city', 'City'),
        prop('$.address.postCode', 'Post code'),
        prop('$.address.state', 'State / region'),
        prop('$.address.country', 'Country', 'select', ENUM.country),
        prop('$.appearance.eyeColor', 'Eye color', 'select', ENUM.eyeColor),
        prop('$.appearance.hairColor', 'Hair color', 'select', ENUM.hairColor),
        prop('$.appearance.height.value', 'Height [cm]', 'numeric'),
        prop('$.appearance.height.range', 'Height range', 'select', ENUM.height),
        prop('$.appearance.weight', 'Weight [kg]', 'numeric'),
        prop('$.appearance.features.glasses', 'Glasses', 'boolean'),
        prop('$.appearance.features.tattoo', 'Tattoo', 'boolean'),
        prop('$.company.name', 'Company'),
        prop('$.company.web', 'Web'),
        prop('$.company.vatId', 'VAT ID'),
        prop('$.company.industry', 'Industry', 'select', ENUM.industry),
        prop('$.company.size', 'Company size', 'select', ENUM.companySize),
        prop('$.company.position', 'Position'),
        prop('$.company.department', 'Department'),
        prop('$.company.address.street', 'Company street'),
        prop('$.company.address.city', 'Company city'),
        prop('$.company.address.postCode', 'Company post code'),
        prop('$.company.address.country', 'Company country', 'select', ENUM.country),
        prop('$.payment.method', 'Payment method', 'select', ENUM.paymentMethod),
        prop('$.payment.currency', 'Currency', 'select', ENUM.currency),
        prop('$.payment.balance', 'Balance', 'numeric'),
        prop('$.payment.creditLimit', 'Credit limit', 'numeric'),
        prop('$.payment.card.brand', 'Card brand', 'select', ENUM.cardBrand),
        prop('$.payment.card.number', 'Card number'),
        prop('$.payment.card.holder', 'Card holder'),
        prop('$.payment.card.expiry', 'Card expiry'),
        prop('$.payment.iban', 'IBAN'),
        prop('$.payment.lastPayment.date', 'Last payment', 'datetime'),
        prop('$.payment.lastPayment.amount', 'Last payment amount', 'numeric'),
        prop('$.payment.lastPayment.status', 'Last payment status', 'select', ENUM.paymentStatus),
        prop('$.account.type', 'Account type', 'select', ENUM.accountType),
        prop('$.account.status', 'Account status', 'select', ENUM.accountStatus),
        prop('$.account.registeredAt', 'Registered at', 'datetime'),
        prop('$.account.lastLoginAt', 'Last login', 'datetime'),
        prop('$.account.verified', 'Verified', 'boolean'),
        prop('$.account.newsletter', 'Newsletter', 'boolean'),
        prop('$.account.loyaltyPoints', 'Loyalty points', 'numeric'),
        prop('$.auth.login', 'Login'),
        prop('$.auth.password', 'Password'),
        prop('$.auth.role', 'Role', 'select', ENUM.authRole)
    ]
};
const P = Object.fromEntries(userClass.configuration.map(p => [p.path, p]));
// the copy of the class properties kept by columns, filters and tasks (without a null enumId, as the UI saves them)
const propertiesCopy = () => userClass.configuration.map(({path, type, uuid, title, enumId}) =>
    enumId == null ? {path, type, uuid, title} : {path, type, uuid, title, enumId});

// --- columns, filters, views --------------------------------------------------------------------------------
const col = (uuidOrPath, editable = 'always', enabled = true) => ({
    uuid: uuidOrPath.startsWith('$') ? P[uuidOrPath].uuid : uuidOrPath, enabled, sorting: true, editable, filtering: true
});
// one set for all views of users: the most important columns shown, all other properties defined but hidden
const SHOWN = [col('uuid_data_id', 'never'), col('uuid_tag_id'), col('uuid_reserved'), col('uuid_owner', 'never'),
    col('$.address.country'), col('$.firstName'), col('$.lastName'), col('$.account.type'), col('$.auth.role'),
    col('$.account.status'), col('$.auth.login'), col('$.payment.method'), col('uuid_modified_at', 'never'),
    col('uuid_modified_by', 'never'), col('uuid_created_at', 'never'), col('uuid_created_by', 'never')];
const shownUuids = new Set(SHOWN.map(c => c.uuid));
const columns = [{
    id: COLUMNS_USER, name: 'user columns', classId: CLASS_USER, description: 'The main columns shown, the other properties hidden',
    configuration: {
        columns: [...SHOWN, ...userClass.configuration.filter(p => !shownUuids.has(p.uuid)).map(p => col(p.path, 'always', false))],
        properties: propertiesCopy()
    }
}];

// a property in the logic of a filter, as the rich filter writes it
const v = path => ({var: 'prop.' + path.replace(/\./g, '*')});
const filterOf = (id, name, description, logic) =>
    ({id, name, classId: CLASS_USER, description, configuration: {logic, properties: propertiesCopy()}});
const ownFilters = [
    filterOf(12, 'VIP with a failed payment', 'Account type VIP and the last payment failed',
        {and: [{'==': [v('$.account.type'), 'vip']}, {'==': [v('$.payment.lastPayment.status'), 'failed']}]}),
    filterOf(13, 'companies from Poland', 'Company address in Poland',
        {and: [{'==': [v('$.company.address.country'), 'PL']}]}),
    filterOf(14, 'tall with glasses', 'Taller than 190 cm and wearing glasses',
        {and: [{'>': [v('$.appearance.height.value'), 190]}, {'==': [v('$.appearance.features.glasses'), true]}]}),
    filterOf(15, 'negative balance', 'Balance below zero',
        {and: [{'<': [v('$.payment.balance'), 0]}]}),
    filterOf(16, 'locked or suspended', 'Account locked or suspended',
        {and: [{'in': [v('$.account.status'), ['locked', 'suspended']]}]}),
    filterOf(17, 'admin accounts', 'Test accounts with the role ADMIN',
        {and: [{'==': [v('$.auth.role'), 'ADMIN']}]}),
    filterOf(18, 'premium cards in EUR', 'Premium or VIP accounts paying by card in EUR',
        {and: [{'in': [v('$.account.type'), ['premium', 'vip']]}, {'==': [v('$.payment.method'), 'card']}, {'==': [v('$.payment.currency'), 'EUR']}]})
];
const viewOf = (id, name, description, filterId) => ({
    id, name, roleId: 3, filterId, teamsIds: [], columnsId: COLUMNS_USER, bucketsIds: [], classesIds: [CLASS_USER],
    featuresIds: USER_FEATURES, description, sort: USER_SORT
});
const ownViews = [
    viewOf(13, 'VIP failed payments', 'VIP accounts whose last payment failed', 12),
    viewOf(14, 'companies from Poland', 'Company address in Poland', 13),
    viewOf(15, 'tall with glasses', 'Taller than 190 cm, with glasses', 14),
    viewOf(16, 'negative balance', 'Balance below zero', 15),
    viewOf(17, 'locked or suspended', 'Account locked or suspended', 16),
    viewOf(18, 'admin accounts', 'Test accounts with the role ADMIN', 17),
    viewOf(19, 'premium cards in EUR', 'Premium or VIP, paying by card in EUR', 18)
];

// --- people -------------------------------------------------------------------------------------------------
const COUNTRIES = {
    US: {
        weight: 35, currency: 'USD', phone: () => `+1 ${int(201, 989)}-${digits(3)}-${digits(4)}`,
        female: ['Emma', 'Olivia', 'Ava', 'Sophia', 'Isabella', 'Mia', 'Charlotte', 'Amelia', 'Harper', 'Evelyn', 'Abigail', 'Emily', 'Madison', 'Grace', 'Chloe', 'Lily'],
        male: ['Liam', 'Noah', 'William', 'James', 'Benjamin', 'Lucas', 'Henry', 'Alexander', 'Michael', 'Daniel', 'Jacob', 'Logan', 'Jackson', 'Ethan', 'Mason', 'David'],
        last: ['Smith', 'Johnson', 'Williams', 'Brown', 'Jones', 'Garcia', 'Miller', 'Davis', 'Rodriguez', 'Martinez', 'Wilson', 'Anderson', 'Taylor', 'Thomas', 'Moore', 'Jackson', 'Martin', 'Lee', 'Thompson', 'White', 'Harris', 'Clark', 'Lewis', 'Walker', 'Young'],
        cities: [['New York', 'NY', '100'], ['Los Angeles', 'CA', '900'], ['Chicago', 'IL', '606'], ['Houston', 'TX', '770'], ['Phoenix', 'AZ', '850'],
            ['Seattle', 'WA', '981'], ['Boston', 'MA', '021'], ['Denver', 'CO', '802'], ['Atlanta', 'GA', '303'], ['Miami', 'FL', '331'], ['Anchorage', 'AK', '995']],
        postCode: c => c[2] + digits(2),
        streets: ['Main St', 'Oak Ave', 'Maple Dr', 'Park Ave', 'Cedar Ln', 'Washington Blvd', 'Lake St', 'Hill Rd', 'Elm St', 'Sunset Blvd'],
        street: s => `${int(1, 9999)} ${s}`,
        companySuffix: ['Inc.', 'LLC', 'Corp.', 'Group'], vat: () => `US${digits(9)}`, domain: 'com',
        iban: () => null
    },
    PL: {
        weight: 25, currency: 'PLN', phone: () => `+48 ${int(500, 799)} ${digits(3)} ${digits(3)}`,
        female: ['Anna', 'Maria', 'Katarzyna', 'Małgorzata', 'Agnieszka', 'Barbara', 'Ewa', 'Magdalena', 'Joanna', 'Zofia', 'Aleksandra', 'Natalia', 'Julia', 'Monika'],
        male: ['Piotr', 'Krzysztof', 'Andrzej', 'Tomasz', 'Paweł', 'Michał', 'Marcin', 'Jakub', 'Adam', 'Łukasz', 'Mateusz', 'Kamil', 'Bartosz', 'Wojciech'],
        last: ['Nowak', 'Kowalski', 'Wiśniewski', 'Wójcik', 'Kowalczyk', 'Kamiński', 'Lewandowski', 'Zieliński', 'Szymański', 'Woźniak', 'Dąbrowski', 'Kozłowski', 'Jankowski', 'Mazur', 'Krawczyk'],
        lastFemale: l => l.endsWith('ski') ? l.slice(0, -1) + 'a' : l,
        cities: [['Warszawa', 'mazowieckie', '0'], ['Kraków', 'małopolskie', '3'], ['Wrocław', 'dolnośląskie', '5'], ['Poznań', 'wielkopolskie', '6'],
            ['Gdańsk', 'pomorskie', '8'], ['Łódź', 'łódzkie', '9'], ['Katowice', 'śląskie', '4'], ['Lublin', 'lubelskie', '2']],
        postCode: c => `${c[2]}${digits(1)}-${digits(3)}`,
        streets: ['ul. Marszałkowska', 'ul. Długa', 'ul. Polna', 'ul. Leśna', 'ul. Słoneczna', 'ul. Kościuszki', 'ul. Mickiewicza', 'ul. Ogrodowa', 'al. Jerozolimskie'],
        street: s => `${s} ${int(1, 180)}${chance(0.4) ? '/' + int(1, 60) : ''}`,
        companySuffix: ['Sp. z o.o.', 'S.A.', 'sp. j.'], vat: () => `PL${digits(10)}`, domain: 'pl',
        iban: () => `PL${digits(2)} ${digits(4)} ${digits(4)} ${digits(4)} ${digits(4)} ${digits(4)} ${digits(4)}`
    },
    DE: {
        weight: 15, currency: 'EUR', phone: () => `+49 ${int(151, 179)} ${digits(7)}`,
        female: ['Hannah', 'Lena', 'Lea', 'Anna', 'Laura', 'Lara', 'Sophie', 'Marie', 'Johanna', 'Katharina', 'Clara'],
        male: ['Lukas', 'Leon', 'Finn', 'Jonas', 'Paul', 'Felix', 'Maximilian', 'Elias', 'Tim', 'Jan', 'Matthias'],
        last: ['Müller', 'Schmidt', 'Schneider', 'Fischer', 'Weber', 'Meyer', 'Wagner', 'Becker', 'Schulz', 'Hoffmann', 'Koch', 'Richter'],
        cities: [['Berlin', 'Berlin', '1'], ['Hamburg', 'Hamburg', '2'], ['München', 'Bayern', '8'], ['Köln', 'Nordrhein-Westfalen', '5'],
            ['Frankfurt am Main', 'Hessen', '6'], ['Stuttgart', 'Baden-Württemberg', '7'], ['Leipzig', 'Sachsen', '0']],
        postCode: c => c[2] + digits(4),
        streets: ['Hauptstraße', 'Schulstraße', 'Gartenstraße', 'Bahnhofstraße', 'Dorfstraße', 'Bergstraße', 'Lindenstraße'],
        street: s => `${s} ${int(1, 120)}`,
        companySuffix: ['GmbH', 'AG', 'KG'], vat: () => `DE${digits(9)}`, domain: 'de',
        iban: () => `DE${digits(2)} ${digits(4)} ${digits(4)} ${digits(4)} ${digits(4)} ${digits(2)}`
    },
    GB: {
        weight: 15, currency: 'GBP', phone: () => `+44 7${digits(3)} ${digits(6)}`,
        female: ['Olivia', 'Amelia', 'Isla', 'Ava', 'Ivy', 'Freya', 'Lily', 'Florence', 'Poppy', 'Evie', 'Ella'],
        male: ['Oliver', 'George', 'Arthur', 'Noah', 'Muhammad', 'Leo', 'Harry', 'Oscar', 'Archie', 'Jack', 'Freddie'],
        last: ['Smith', 'Jones', 'Taylor', 'Brown', 'Williams', 'Wilson', 'Johnson', 'Davies', 'Patel', 'Robinson', 'Wright', 'Thompson', 'Evans', 'Walker'],
        cities: [['London', 'Greater London', 'SW'], ['Manchester', 'Greater Manchester', 'M'], ['Birmingham', 'West Midlands', 'B'],
            ['Leeds', 'West Yorkshire', 'LS'], ['Glasgow', 'Scotland', 'G'], ['Bristol', 'Bristol', 'BS'], ['Liverpool', 'Merseyside', 'L']],
        postCode: c => `${c[2]}${int(1, 20)} ${int(1, 9)}${pick(['AB', 'DE', 'HJ', 'LN', 'PQ', 'RT', 'WX'])}`,
        streets: ['High Street', 'Station Road', 'Church Lane', 'Victoria Road', 'Green Lane', 'Manor Road', 'Park Road'],
        street: s => `${int(1, 250)} ${s}`,
        companySuffix: ['Ltd', 'plc', 'LLP'], vat: () => `GB${digits(9)}`, domain: 'co.uk',
        iban: () => `GB${digits(2)} ${pick(['BARC', 'LOYD', 'HBUK', 'NWBK'])} ${digits(4)} ${digits(4)} ${digits(4)} ${digits(2)}`
    },
    FR: {
        weight: 10, currency: 'EUR', phone: () => `+33 6 ${digits(2)} ${digits(2)} ${digits(2)} ${digits(2)}`,
        female: ['Louise', 'Jade', 'Emma', 'Alice', 'Chloé', 'Léa', 'Manon', 'Camille', 'Inès', 'Juliette'],
        male: ['Gabriel', 'Raphaël', 'Louis', 'Arthur', 'Jules', 'Hugo', 'Lucas', 'Adam', 'Nathan', 'Théo'],
        last: ['Martin', 'Bernard', 'Dubois', 'Thomas', 'Robert', 'Richard', 'Petit', 'Durand', 'Leroy', 'Moreau', 'Simon', 'Laurent'],
        cities: [['Paris', 'Île-de-France', '75'], ['Lyon', 'Auvergne-Rhône-Alpes', '69'], ['Marseille', "Provence-Alpes-Côte d'Azur", '13'],
            ['Toulouse', 'Occitanie', '31'], ['Nantes', 'Pays de la Loire', '44'], ['Lille', 'Hauts-de-France', '59']],
        postCode: c => c[2] + digits(3),
        streets: ['rue de la Paix', 'rue Victor Hugo', 'avenue Jean Jaurès', 'boulevard Voltaire', 'rue Pasteur', 'rue de la République'],
        street: s => `${int(1, 150)} ${s}`,
        companySuffix: ['SAS', 'SARL', 'SA'], vat: () => `FR${digits(11)}`, domain: 'fr',
        iban: () => `FR${digits(2)} ${digits(4)} ${digits(4)} ${digits(4)} ${digits(4)} ${digits(4)} ${digits(3)}`
    }
};
const COMPANY_WORDS = {
    it: ['Soft', 'Data', 'Cloud', 'Byte', 'Code', 'Net'], finance: ['Capital', 'Invest', 'Finance', 'Assurance', 'Credit'],
    retail: ['Shop', 'Market', 'Store', 'Trade', 'Mart'], manufacturing: ['Steel', 'Works', 'Industries', 'Machines', 'Tools'],
    logistics: ['Cargo', 'Logistics', 'Transport', 'Freight', 'Express'], healthcare: ['Med', 'Care', 'Pharma', 'Clinic', 'Health'],
    education: ['Academy', 'Learning', 'School', 'Campus'], media: ['Media', 'Print', 'Studio', 'Press', 'Graphics']
};
const COMPANY_PREFIX = ['North', 'Blue', 'Green', 'Prime', 'Smart', 'Nova', 'Alpha', 'Euro', 'Global', 'Silver', 'Bright', 'Star', 'Next', 'Delta', 'Vista'];
const POSITIONS = ['QA Engineer', 'Test Lead', 'Developer', 'Product Owner', 'Business Analyst', 'Project Manager', 'Accountant',
    'Sales Manager', 'Consultant', 'Designer', 'DevOps Engineer', 'HR Specialist', 'CEO', 'CTO', 'Support Specialist'];
const DEPARTMENTS = ['IT', 'QA', 'Finance', 'Sales', 'Marketing', 'HR', 'Operations', 'Management', 'Support'];
const ASCII = {'ą': 'a', 'ć': 'c', 'ę': 'e', 'ł': 'l', 'ń': 'n', 'ó': 'o', 'ś': 's', 'ź': 'z', 'ż': 'z', 'ä': 'ae', 'ö': 'oe', 'ü': 'ue', 'ß': 'ss', 'é': 'e', 'è': 'e', 'ë': 'e', 'ï': 'i', 'î': 'i', 'ô': 'o', 'ç': 'c'};
const ascii = text => text.toLowerCase().replace(/[^\x00-\x7f]/g, ch => ASCII[ch] ?? '').replace(/[^a-z0-9]/g, '');
const passwordOf = () => pick(['Test', 'Qa', 'Demo', 'Pass', 'Secret']) + pick(['!', '#', '$', '%', '@']) + digits(4) + pick(['a', 'x', 'q', 'z']);
const cardNumber = brand => brand === 'amex' ? `3782 ****** *${digits(4)}` : `${brand === 'visa' ? '4' : '5'}${digits(3)} **** **** ${digits(4)}`;

function address(c) {
    const country = COUNTRIES[c];
    const city = pick(country.cities);
    return {street: country.street(pick(country.streets)), city: city[0], postCode: country.postCode(city), state: city[1], country: c};
}

function heightRange(cm) {
    return cm < 160 ? 'h<1.6m' : cm < 180 ? '1.6m<h<1.8m' : cm <= 200 ? '1.8m<h<2m' : 'h>2m';
}

function person(env, index, logins) {
    const c = weighted(Object.entries(COUNTRIES).map(([code, def]) => [code, def.weight]));
    const country = COUNTRIES[c];
    const gender = weighted([['female', 48], ['male', 48], ['other', 4]]);
    const firstName = pick(gender === 'male' ? country.male : gender === 'female' ? country.female : [...country.female, ...country.male]);
    const lastBase = pick(country.last);
    const lastName = gender === 'female' && country.lastFemale ? country.lastFemale(lastBase) : lastBase;

    let login = `${ascii(firstName)[0]}${ascii(lastName)}`;
    while (logins.has(login))
        login = `${ascii(firstName)[0]}${ascii(lastName)}${int(1, 999)}`;
    logins.add(login);

    const birth = GENERATED_AT - int(18 * 365, 75 * 365) * DAY;
    const heightCm = Math.round(gender === 'male' ? 165 + random() * 35 : 152 + random() * 30) + (chance(0.02) ? 20 : 0);
    const registeredAt = GENERATED_AT - int(40, 6 * 365) * DAY - int(0, DAY);

    const row = {
        firstName, lastName, gender, birthDate: day(birth),
        contact: {
            email: `${ascii(firstName)}.${ascii(lastName)}${chance(0.3) ? int(1, 99) : ''}@${pick(['gmail.com', 'outlook.com', 'yahoo.com', 'proton.me', 'example.' + country.domain])}`,
            phone: country.phone(),
            preferredChannel: weighted([['email', 50], ['sms', 20], ['phone', 20], ['none', 10]])
        },
        address: address(c),
        appearance: {
            eyeColor: weighted([['brown', 40], ['blue', 30], ['green', 10], ['grey', 10], ['hazel', 10]]),
            hairColor: weighted([['brown', 35], ['black', 25], ['blond', 20], ['red', 5], ['grey', 10], ['none', 5]]),
            height: {value: heightCm, range: heightRange(heightCm)},
            weight: Math.round(heightCm - 100 + (random() - 0.4) * 30),
            features: {glasses: chance(0.35), tattoo: chance(0.2)}
        }
    };
    if (chance(0.6))
        row.contact.mobile = country.phone();

    if (chance(0.8)) {
        const industry = pick(Object.keys(COMPANY_WORDS));
        const name = chance(0.5) ? `${pick(COMPANY_PREFIX)}${pick(COMPANY_WORDS[industry])}` : `${pick(country.last)} ${pick(COMPANY_WORDS[industry])}`;
        // most work in their own country
        const companyCountry = chance(0.85) ? c : pick(Object.keys(COUNTRIES));
        row.company = {
            name: `${name} ${pick(COUNTRIES[companyCountry].companySuffix)}`,
            web: `https://www.${ascii(name)}.${COUNTRIES[companyCountry].domain}`,
            vatId: COUNTRIES[companyCountry].vat(),
            industry,
            size: weighted([['1-10', 20], ['11-50', 30], ['51-250', 25], ['251-1000', 15], ['1000+', 10]]),
            position: pick(POSITIONS),
            department: pick(DEPARTMENTS),
            address: (({street, city, postCode, country: cc}) => ({street, city, postCode, country: cc}))(address(companyCountry))
        };
    }

    const method = weighted([['card', 55], ['transfer', 20], ['blik', c === 'PL' ? 30 : 0], ['cash', 5]]);
    const accountType = weighted([['standard', 70], ['premium', 22], ['vip', 8]]);
    const payment = {
        method,
        currency: chance(0.9) ? country.currency : pick(['USD', 'PLN', 'EUR', 'GBP']),
        balance: round2(chance(0.12) ? -random() * 800 : random() * (accountType === 'vip' ? 50000 : 5000)),
        creditLimit: accountType === 'standard' ? pick([500, 1000, 2000]) : accountType === 'premium' ? pick([5000, 10000]) : pick([25000, 50000, 100000])
    };
    if (method === 'card') {
        const brand = weighted([['visa', 50], ['mastercard', 40], ['amex', 10]]);
        const expiry = GENERATED_AT + int(-60, 4 * 365) * DAY;
        payment.card = {brand, number: cardNumber(brand), holder: `${firstName} ${lastName}`.toUpperCase(), expiry: iso(expiry).slice(0, 7)};
    }
    if (method === 'transfer' && country.iban() != null)
        payment.iban = country.iban();
    if (chance(0.85))
        payment.lastPayment = {
            date: iso(GENERATED_AT - DAYS * DAY - int(0, 120) * DAY - int(0, DAY)),
            amount: round2(5 + random() * (accountType === 'standard' ? 200 : 2000)),
            status: weighted([['paid', 85], ['pending', 5], ['failed', 7], ['refunded', 3]])
        };
    row.payment = payment;

    row.account = {
        type: accountType,
        status: weighted([['active', 88], ['suspended', 5], ['locked', 4], ['closed', 3]]),
        registeredAt: iso(registeredAt),
        lastLoginAt: iso(GENERATED_AT - DAYS * DAY - int(0, 200) * DAY - int(0, DAY)),
        verified: chance(0.85),
        newsletter: chance(0.4),
        loyaltyPoints: int(0, accountType === 'vip' ? 20000 : 3000)
    };
    row.auth = {login, password: passwordOf(), role: weighted([['MEMBER', 90], ['ADMIN', 10]])};
    return row;
}

// --- history ------------------------------------------------------------------------------------------------
const ADMIN = 'demo-admin', MEMBER = 'demo-member', ROBOT = 'demo-robot';
const ago = ms => {
    const total = Math.max(0, Math.round(ms / 60000));
    const d = Math.floor(total / 1440), h = Math.floor(total % 1440 / 60), m = total % 60;
    return `P${d > 0 ? d + 'D' : ''}T${h}H${m}M`;
};
const HOUR = 3600000;

/**
 * 30 days, hour by hour (from the oldest): the robot's test runs reserve users and release them when they end (the
 * test logged in and paid; a failed payment goes to analysis), the member edits users by hand during the day, and once
 * a week the admin cleans up (analysed users back to active, some suspended or locked, trash to deleted, a few more
 * to the trash). The runs still going at the import keep their users reserved.
 */
function makeHistory(bucket, rows, changes) {
    const state = rows.map(row => ({tag: row.tagId, reserved: row.reserved, data: structuredClone(row.properties)}));
    const free = i => !state[i].reserved && state[i].tag !== TAG.trash && state[i].tag !== TAG.deleted;
    const anyFree = () => {
        for (let tries = 0; tries < 50; tries++) {
            const i = int(0, rows.length - 1);
            if (free(i))
                return i;
        }
        return null;
    };
    const add = (i, when, user, change) => {
        changes.push({bucket, row: i, ago: ago(when), user, ...change});
        if (change.tagId !== undefined)
            state[i].tag = change.tagId === 0 ? null : change.tagId;
        if (change.reserved !== undefined)
            state[i].reserved = change.reserved;
    };
    const running = new Map();  // hour (ago) the run ends -> rows

    for (let hour = DAYS * 24 - 1; hour >= 0; hour--) {
        const at = minutes => hour * HOUR - minutes * 60000;   // a moment in this hour
        const hourOfDay = 23 - hour % 24;
        const working = hourOfDay >= 8 && hourOfDay < 18;
        const weekday = Math.floor(hour / 24) % 7 >= 2;

        for (const i of running.get(hour) ?? []) {
            const status = weighted([['paid', 80], ['failed', 12], ['pending', 8]]);
            const amount = round2(5 + random() * 300);
            const when = at(int(1, 59));
            const properties = {
                account: {lastLoginAt: iso(GENERATED_AT - when)},
                payment: {lastPayment: {date: iso(GENERATED_AT - when), amount, status}}
            };
            if (status === 'paid')
                properties.payment.balance = state[i].data.payment.balance = round2(state[i].data.payment.balance - amount);
            add(i, when, ROBOT, {reserved: false, tagId: status === 'failed' ? TAG.analysis : TAG.active, properties});
        }
        running.delete(hour);

        // a run every few hours, also at night
        if (chance(working ? 0.35 : 0.1)) {
            const ends = hour - int(1, 30);
            const when = at(int(0, 5));
            for (let k = int(3, 8); k > 0; k--) {
                const i = anyFree();
                if (i == null)
                    continue;
                add(i, when, ROBOT, {reserved: true, tagId: TAG.scheduled});
                if (ends >= 0)
                    running.set(ends, [...(running.get(ends) ?? []), i]);
            }
        }

        if (working && weekday && chance(0.8))
            for (let e = int(1, 3); e > 0; e--) {
                const i = anyFree();
                if (i != null)
                    memberEdit(i, at(int(6, 59)));
            }

        if (hourOfDay === 10 && Math.floor(hour / 24) % 7 === 3)
            cleanUp(at);
    }

    function memberEdit(i, when) {
        const p = state[i].data;
        const country = COUNTRIES[p.address.country];
        const kind = weighted([['phone', 3], ['email', 2], ['address', 3], ['card', 2], ['channel', 2], ['appearance', 1], ['password', 2], ['analysis', 2]]);
        if (kind === 'phone')
            add(i, when, MEMBER, {properties: {contact: {phone: country.phone()}}});
        else if (kind === 'email')
            add(i, when, MEMBER, {properties: {contact: {email: `${ascii(p.firstName)}.${ascii(p.lastName)}${int(100, 999)}@${pick(['gmail.com', 'outlook.com', 'proton.me'])}`}}});
        else if (kind === 'address') {
            const {street, city, postCode, state: region} = address(p.address.country);
            add(i, when, MEMBER, {properties: {address: {street, city, postCode, state: region}}});
        } else if (kind === 'card' && p.payment.card != null)
            add(i, when, MEMBER, {properties: {payment: {card: {expiry: iso(GENERATED_AT + int(365, 5 * 365) * DAY).slice(0, 7), number: cardNumber(p.payment.card.brand)}}}});
        else if (kind === 'channel') {
            const channel = pick(['email', 'sms', 'phone', 'none'].filter(x => x !== p.contact.preferredChannel));
            p.contact.preferredChannel = channel;
            add(i, when, MEMBER, {properties: {contact: {preferredChannel: channel}}});
        } else if (kind === 'appearance') {
            p.appearance.features.glasses = !p.appearance.features.glasses;
            add(i, when, MEMBER, {properties: {appearance: {hairColor: pick(['black', 'brown', 'blond', 'red', 'grey']), features: {glasses: p.appearance.features.glasses}}}});
        } else if (kind === 'password')
            add(i, when, MEMBER, {properties: {auth: {password: passwordOf()}}});
        else if (state[i].tag !== TAG.analysis)
            add(i, when, MEMBER, {tagId: TAG.analysis});
    }

    function cleanUp(at) {
        state.forEach((s, i) => {
            if (s.tag === TAG.analysis && !s.reserved && chance(0.7)) {
                const failed = s.data.payment.lastPayment?.status === 'failed';
                if (failed && chance(0.5)) {
                    s.data.account.status = pick(['suspended', 'locked']);
                    add(i, at(int(0, 20)), ADMIN, {tagId: TAG.active, properties: {account: {status: s.data.account.status}}});
                } else
                    add(i, at(int(0, 20)), ADMIN, {tagId: TAG.active});
            } else if (s.tag === TAG.trash && !s.reserved && chance(0.6))
                add(i, at(int(20, 40)), ADMIN, {tagId: TAG.deleted});
        });
        for (let k = int(3, 8); k > 0; k--) {
            const i = anyFree();
            if (i != null) {
                state[i].data.account.status = 'closed';
                add(i, at(int(40, 50)), ADMIN, {tagId: TAG.trash, properties: {account: {status: 'closed'}}});
            }
        }
        for (let k = int(2, 5); k > 0; k--) {
            const i = anyFree();
            if (i != null && state[i].data.account.status !== 'active') {
                state[i].data.account.status = 'active';
                add(i, at(int(50, 59)), ADMIN, {properties: {account: {status: 'active'}}});
            }
        }
    }
}

// --- the dataset --------------------------------------------------------------------------------------------
const dataset = JSON.parse(fs.readFileSync(FILE, 'utf8'));

dataset.format = 2;
dataset.description = 'Demo project: users of a tested application in three environments (' + ROWS_PER_BUCKET + ' each, with a history of '
    + '30 days made by the demo users), scheduler, configuration and jobs. Demo users: demo-admin, demo-member, '
    + 'demo-robot (password: test).';

const ownEnums = new Set(Object.values(ENUM));
dataset.enums = [...dataset.enums.filter(e => !ownEnums.has(e.id)), ...enums].sort((a, b) => a.id - b.id);
dataset.classes = [userClass, ...dataset.classes.filter(c => c.id !== CLASS_USER)].sort((a, b) => a.id - b.id);

dataset.columns = [...dataset.columns.filter(c => c.id !== COLUMNS_USER && c.id < FIRST_OWN_COLUMNS), ...columns]
    .sort((a, b) => a.id - b.id);

// the tag filters of the user class keep their logic, with the new properties
dataset.filters = dataset.filters.filter(f => f.id < FIRST_OWN_FILTER);
dataset.filters.filter(f => f.classId === CLASS_USER).forEach(f => f.configuration.properties = propertiesCopy());
dataset.filters.push(...ownFilters);

dataset.views = dataset.views.filter(view => view.id < FIRST_OWN_VIEW);
dataset.views.filter(view => view.classesIds.includes(CLASS_USER)).forEach(view => {
    view.columnsId = COLUMNS_USER;
    view.featuresIds = USER_FEATURES;
    view.sort = USER_SORT;
});
dataset.views.find(view => view.id === 1).description = 'All users';
dataset.views.push(...ownViews);

dataset.tasks.filter(t => t.classId === CLASS_USER).forEach(t => t.configuration.properties = propertiesCopy());

// robots read and change test data too: the buckets are open from the role ROBOT (id 4) up
dataset.buckets.forEach(bucket => bucket.roleId = 4);
const BUCKET_DESCRIPTIONS = {users: 'users', scheduler: 'test schedule', jobs: 'jobs'};
dataset.buckets.forEach(bucket => {
    const [env, kind] = bucket.name.split('-');
    bucket.description = kind == null ? 'Global config' : env.toUpperCase() + ' environment ' + BUCKET_DESCRIPTIONS[kind];
});

const GROUP_NAMES = {DEV: 'Development env', INT: 'Integration env', PRD: 'Production env'};
dataset.groups.forEach(group => {
    group.name = GROUP_NAMES[group.shortName] ?? group.name;
    group.description = 'Group of buckets defined for the ' + group.shortName + ' environment';
});

const TAG_DESCRIPTIONS = {
    active: 'Active users, ready for tests', trash: 'Trashed users, waiting for deletion', deleted: 'Deleted users',
    scheduled: 'Scheduled for a test', analysis: 'Under analysis by a person', todo: 'Task to do', done: 'Task done'
};
dataset.tags.forEach(tag => tag.description = TAG_DESCRIPTIONS[tag.name] ?? tag.description);

const flagIcons = Object.entries(FLAGS).map(([code, markup]) => ({name: 'flag-' + code.toLowerCase(), structure: markup}));
dataset.icons = [...flagIcons, {name: 'payment-card', structure: CARD_SVG}].map((i, k) => ({id: k + 1, ...i}));

dataset.users = [
    {username: ADMIN, password: 'test', role: 'ADMIN', description: 'Demo administrator (DEMO dataset)'},
    {username: MEMBER, password: 'test', role: 'MEMBER', description: 'Demo member (DEMO dataset)'},
    {username: ROBOT, password: 'test', role: 'ROBOT', description: 'Demo robot, e.g. automated tests (DEMO dataset)'}
];

const changes = [];
USER_BUCKETS.forEach((bucket, b) => {
    random = mulberry32(1000 + b);
    const logins = new Set();
    const rows = Array.from({length: ROWS_PER_BUCKET}, (_, i) => ({
        tagId: weighted([[null, 35], [TAG.active, 50], [TAG.scheduled, 3], [TAG.analysis, 5], [TAG.trash, 5], [TAG.deleted, 2]]),
        reserved: false,
        properties: person(bucket, i, logins)
    }));
    // a few rows reserved by the importing user
    for (let k = 0; k < 15; k++)
        rows[int(0, rows.length - 1)].reserved = true;
    dataset.data[bucket] = rows;
    makeHistory(bucket, rows, changes);
});

// the scheduler rows point at users: the data id is the id of a row of the users bucket (1..ROWS_PER_BUCKET)
random = mulberry32(55);
['dev-scheduler', 'int-scheduler', 'prd-scheduler'].forEach(bucket =>
    dataset.data[bucket].forEach(row => row.properties.data = {...row.properties.data, id: int(1, ROWS_PER_BUCKET)}));

// int-jobs was empty: jobs like those of dev
random = mulberry32(77);
dataset.data['int-jobs'] = dataset.data['dev-jobs'].map(job => ({
    ...job, properties: {...job.properties, status: weighted([['passed', 80], ['warning', 10], ['failed', 10]]), userId: int(10000, 99999)}
}));

dataset.history = {since: `P${DAYS}D`, changes};

// --- writing: objects indented, data rows and history changes one per line (as DatasetWriter) ------------------
const {data, history, ...objects} = dataset;
let text = JSON.stringify({...objects, history: {since: history.since, changes: []}, data: {}}, null, 2);
const lines = list => list.map(x => '      ' + JSON.stringify(x)).join(',\n');
text = text.replace('"changes": []', `"changes": [\n${lines(history.changes)}\n    ]`);
text = text.replace('"data": {}', '"data": {\n' + Object.entries(data)
    .map(([bucket, rows]) => `    ${JSON.stringify(bucket)}: [${rows.length ? '\n' + lines(rows) + '\n    ]' : ']'}`).join(',\n') + '\n  }');
fs.writeFileSync(FILE, text + '\n');

const count = Object.values(data).reduce((sum, rows) => sum + rows.length, 0);
console.log(`${FILE}: ${count} rows, ${changes.length} changes, ${(text.length / 1024 / 1024).toFixed(1)} MB`);
