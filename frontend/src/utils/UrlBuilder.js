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

export const getDataUrl = (bucket) => {
    return `${origin}/api/bucket/${bucket.name}/data`;
}

export const getDataReserveUrl = (bucket, number, random) => {
    return random ? `${origin}/api/bucket/${bucket.name}/data/reserve?limit=${number}&sort=random` : `${origin}/api/bucket/${bucket.name}/data/reserve?limit=${number}`;
}

export const getDataByIdUrl = (bucket, id) => {
    return `${origin}/api/bucket/${bucket.name}/data/${id}`;
}

export const getDataHistoryUrl = (bucket, id) => {
    return `${origin}/api/bucket/${bucket.name}/data/${id}/history`;
}

export const getDataHistoryPropertiesUrl = (bucket, id, idA, idB) => {
    return `${origin}/api/bucket/${bucket.name}/data/${id}/history/${idA},${idB}`;
}

export const getBaseUrl = (endpoint) => {
    return `${origin}/api/${endpoint}`;
}

export const getSessionUrl = (endpoint) => {
    return `${origin}/api/session/${endpoint}`;
}

export const getSessionUrlWithIds = (endpoint, ids) => {
    return `${origin}/api/session/${endpoint}/${ids}`;
}

export const getBaseUrlWithIds = (endpoint, ids) => {
    return `${origin}/api/${endpoint}/${ids}`;
}