export const FEATURE_SEARCH = 'Search';
export const FEATURE_FILTER = 'Filter';
export const FEATURE_RICH_FILTER = 'Rich filter';
export const FEATURE_DETAILS = 'Details';
export const FEATURE_CREATION = 'Creation';
export const FEATURE_MODIFYING = 'Modifying';
export const FEATURE_REMOVAL = 'Removal';
export const FEATURE_HISTORY = 'History';
export const FEATURE_DUPLICATE = 'Duplicate';
export const FEATURE_TASKS = 'Tasks';
export const FEATURE_RESERVATION = 'Reservation';
export const FEATURE_IMPORT = 'Import';
export const FEATURE_EXPORT = 'Export';
export const FEATURE_AVAILABLE_TAGS = 'Available tags';

export const features = [
    {id: 1, name: FEATURE_SEARCH},
    {id: 2, name: FEATURE_DETAILS},
    {id: 3, name: FEATURE_CREATION},
    {id: 4, name: FEATURE_MODIFYING},
    {id: 5, name: FEATURE_REMOVAL},
    {id: 6, name: FEATURE_HISTORY},
    {id: 7, name: FEATURE_TASKS},
    {id: 8, name: FEATURE_RESERVATION},
    {id: 9, name: FEATURE_IMPORT},
    {id: 10, name: FEATURE_EXPORT},
    {id: 11, name: FEATURE_DUPLICATE},
    {id: 12, name: FEATURE_FILTER},
    {id: 13, name: FEATURE_RICH_FILTER},
    {id: 14, name: FEATURE_AVAILABLE_TAGS}
];

export const isFeatureEnabled = (name, activeView) => {
    if (activeView != null && activeView.featuresIds != null && activeView.featuresIds.length > 0) {
        const filteredFeatures = features.filter(item => item.name === name);
        if (filteredFeatures.length > 0) {
            return activeView.featuresIds.includes(filteredFeatures[0].id);
        } else {
            console.error("Missing given feature name: " + name);
            return false;
        }
    }
    return false;
}