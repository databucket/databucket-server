import {notifierChangeAdapter} from "../../utils/JsonHelper";

export default (state, action) => {
    switch (action.type) {
        case "FETCH_BUCKETS":
            return {
                ...state,
                buckets: action.payload
            };
        case "ADD_BUCKET":
            return {
                ...state,
                buckets: [...state.buckets, action.payload]
            };
        case "EDIT_BUCKET":
            const updatedBucket = action.payload;
            const updatedBuckets = state.buckets.map(bucket => {
                if (bucket.id === updatedBucket.id)
                    return updatedBucket;
                return bucket;
            });
            return {
                ...state,
                buckets: updatedBuckets
            };
        case "REMOVE_BUCKET":
            return {
                ...state,
                buckets: state.buckets.filter(bucket => bucket.id !== action.payload)
            };
        case "NOTIFY_BUCKETS":
            return {
                ...state,
                buckets: notifierChangeAdapter(state.buckets, action.payload)
            };
        default:
            return state;
    }
}