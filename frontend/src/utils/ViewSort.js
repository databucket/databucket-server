// The default sort of a view, as the sort parameter of the API: asc(<source>) or desc(<source>), e.g. desc(modifiedAt)
// or asc($.lastName); the source is a field of the data or the path of a property, as in the columns of the data table

const FIELDS = {
    uuid_data_id: {source: 'id', title: 'Id'},
    uuid_tag_id: {source: 'tagId', title: 'Tag'},
    uuid_reserved: {source: 'reserved', title: 'Reserved'},
    uuid_owner: {source: 'owner', title: 'Owner'},
    uuid_created_at: {source: 'createdAt', title: 'Created at'},
    uuid_created_by: {source: 'createdBy', title: 'Created by'},
    uuid_modified_at: {source: 'modifiedAt', title: 'Modified at'},
    uuid_modified_by: {source: 'modifiedBy', title: 'Modified by'}
};

export const parseViewSort = (sort) => {
    const match = /^(asc|desc)\((.+)\)$/.exec(sort ?? '');
    return match != null ? {ord: match[1], source: match[2]} : null;
}

export const formatViewSort = (source, ord) => source != null && source !== '' ? `${ord}(${source})` : null;

// a column of a columns definition as {source, title}
const describeColumn = (column, properties) => {
    if (FIELDS[column.uuid] != null)
        return FIELDS[column.uuid];
    const property = (properties ?? []).find(p => p.uuid === column.uuid);
    return property != null ? {source: property.path, title: property.title} : null;
}

// the columns of a columns definition the data can be sorted by (enabled, sorting allowed), in their order
export const getSortableColumns = (columnsDef) => {
    if (columnsDef?.configuration?.columns == null)
        return [];
    return columnsDef.configuration.columns
        .filter(column => column.enabled && column.sorting !== false)
        .map(column => describeColumn(column, columnsDef.configuration.properties))
        .filter(column => column != null);
}

// how a view's default sort reads in the settings, e.g. "Modified at ↓"
export const describeViewSort = (sort, columnsDef) => {
    const parsed = parseViewSort(sort);
    if (parsed == null)
        return '';
    const column = getSortableColumns(columnsDef).find(c => c.source === parsed.source);
    return `${column != null ? column.title : parsed.source} ${parsed.ord === 'desc' ? '↓' : '↑'}`;
}

/**
 * The sort the data table starts with, as {colId, ord} (colId: the index among the enabled columns): the user's own
 * sort when one is saved (also no sort at all), otherwise the default sort of the view.
 */
export const getStartOrder = (savedState, view, columnsDef) => {
    if (savedState != null && savedState.order !== undefined)
        return savedState.order;
    const parsed = parseViewSort(view?.sort);
    if (parsed == null || columnsDef?.configuration?.columns == null)
        return null;
    const colId = columnsDef.configuration.columns
        .filter(column => column.enabled)
        .findIndex(column => describeColumn(column, columnsDef.configuration.properties)?.source === parsed.source);
    return colId >= 0 ? {colId, ord: parsed.ord} : null;
}
