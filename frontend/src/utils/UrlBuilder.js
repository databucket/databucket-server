// const origin = window.location.origin;
const origin = 'http://localhost:8080';

export const getPageableUlr = (endpoint, query, enableFilters) => {

    let url = `${origin}/${endpoint}?size=${query.pageSize}&page=${query.page}`;

    if (query.orderBy != null && query.orderBy.field != null)
        url += `&sort=${query.orderBy.field}`;
    if (query.orderDirection === 'desc')
        url += ',desc';

    if (enableFilters && query.filters.length > 0)
        for (const filter of query.filters)
            if (filter.column.type === 'boolean')
                url += `&${filter.column.field}=${filter.value === 'checked'}`;
            else
                url += `&${filter.column.field}=${escape(filter.value)}`;

    return url;
}

export const getBaseUrl = (endpoint) => {
    return `${origin}/api/${endpoint}`;
}

export const getBaseUrlWithIds = (endpoint, ids) => {
    return `${origin}/api/${endpoint}/${ids}`;
}