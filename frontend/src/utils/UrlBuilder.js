export const getOrigin = () => {
    const origin = window.location.origin;
    // const origin = 'http://localhost:8080';

    return origin + getContextPath();
}

export const getContextPath = () => {
    if (document.getElementById("context-path") != null) {
        const contextPath = document.getElementById("context-path").innerHTML;
        if (contextPath !== "/")
            return contextPath;
    }
    return "";
}

export const getDataUrl = (bucket) => {
    return `${getOrigin()}/api/bucket/${bucket.name}`;
}

export const getDataReserveUrl = (bucket, number, random) => {
    return random ? `${getOrigin()}/api/bucket/${bucket.name}/reserve?limit=${number}&sort=random` : `${getOrigin()}/api/bucket/${bucket.name}/reserve?limit=${number}`;
}

export const getDataByIdUrl = (bucket, id) => {
    return `${getOrigin()}/api/bucket/${bucket.name}/${id}`;
}

export const getDataByIdUrl2 = (bucketName, id) => {
    return `${getOrigin()}/api/bucket/${bucketName}/${id}`;
}

export const getDataHistoryUrl = (bucket, id) => {
    return `${getOrigin()}/api/bucket/${bucket.name}/${id}/history`;
}

export const getDataHistoryPropertiesUrl = (bucket, id, idA, idB) => {
    return `${getOrigin()}/api/bucket/${bucket.name}/${id}/history/${idA},${idB}`;
}

export const getBaseUrl = (endpoint) => {
    return `${getOrigin()}/api/${endpoint}`;
}

export const getSessionUrl = (endpoint) => {
    return `${getOrigin()}/api/session/${endpoint}`;
}

export const getSessionUrlWithIds = (endpoint, ids) => {
    return `${getOrigin()}/api/session/${endpoint}/${ids}`;
}

export const getSwaggerDocPath = () => {
    return `/swagger-ui/#/`;
}
