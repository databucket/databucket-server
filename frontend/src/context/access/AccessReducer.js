import {getLastActiveBucket, getLastActiveGroup, getLastOpenedBuckets, setLastActiveBucket, setLastActiveGroup, setLastOpenedBuckets} from "../../utils/ConfigurationStorage";

const AccessReducer = (state, action) => {
    switch (action.type) {
        case "FETCH_ACCESS_TREE":
            const accessTree = action.payload;
            let activeGroup = null;
            if (accessTree.groups != null && accessTree.groups.length > 0) {
                const lastActiveGroup = accessTree.groups.find(group => group.id === getLastActiveGroup());
                activeGroup = lastActiveGroup != null ? lastActiveGroup : accessTree.groups[0];
            }

            let lastOpenedBucketIds = getLastOpenedBuckets();

            // the following code could be shorter and easier by this command:
            // const bucketsTabs = accessTree.buckets.filter(bucket => lastOpenedBucketIds.includes(bucket.id));
            // But, the above code misses the tab order

            let bucketsTabs = [];
            lastOpenedBucketIds.forEach(bucketId => {
                const bucket = accessTree.buckets.find(bucket => bucket.id === bucketId);
                if (bucket != null)
                    bucketsTabs = [...bucketsTabs, bucket];
            });


            setLastOpenedBuckets(bucketsTabs);

            let activeBucket = null;
            if (bucketsTabs.length > 0) {
                const lastActiveBucketId = getLastActiveBucket();
                const lastActiveBucket = bucketsTabs.find(bucket => bucket.id === lastActiveBucketId);
                activeBucket = lastActiveBucket != null ? lastActiveBucket : bucketsTabs[0];
            }

            return {
                ...state,
                activeGroup: activeGroup,
                activeBucket: activeBucket,
                projects: accessTree.projects,
                groups: accessTree.groups,
                buckets: accessTree.buckets,
                views: accessTree.views,
                bucketsTabs: bucketsTabs
            };
        case "FETCH_SESSION_COLUMNS":
            return {
                ...state,
                columns: action.payload
            };
        case "FETCH_SESSION_FILTERS":
            return {
                ...state,
                filters: action.payload
            };
        case "FETCH_SESSION_TASKS":
            return {
                ...state,
                tasks: action.payload
            };
        case "FETCH_SESSION_CLASSES":
            return {
                ...state,
                classes: action.payload
            };
        case "FETCH_SESSION_TAGS":
            return {
                ...state,
                tags: action.payload
            };
        case "FETCH_SESSION_SVGS":
            return {
                ...state,
                svgs: action.payload
            };
        case "FETCH_SESSION_ENUMS":
            return {
                ...state,
                enums: action.payload
            };
        case "FETCH_SESSION_USERS":
            return {
                ...state,
                users: action.payload
            };
        case "SET_ACTIVE_GROUP":
            setLastActiveGroup(action.payload.id);
            return {
                ...state,
                activeGroup: action.payload
            };
        case "SET_ACTIVE_BUCKET":
            setLastActiveBucket(action.payload.id);
            return {
                ...state,
                activeBucket: action.payload
            };
        case "ADD_TAB":
            const newBucket = action.payload;
            setLastActiveBucket(newBucket.id);
            if (state.bucketsTabs.find(bucket => bucket.id === newBucket.id) == null) {
                const updatedTabs = [...state.bucketsTabs, newBucket];
                setLastOpenedBuckets(updatedTabs);
                return {
                    ...state,
                    activeBucket: newBucket,
                    bucketsTabs: updatedTabs
                };
            } else
                return {
                    ...state,
                    activeBucket: newBucket
                };
        case "REMOVE_TAB":
            const activeTabIndex = state.bucketsTabs.indexOf(state.activeBucket);
            const updatedTabs = state.bucketsTabs.filter(bucket => bucket.id !== action.payload.id);
            setLastOpenedBuckets(updatedTabs);

            // change active tab when removing active tab
            if (state.activeBucket.id === action.payload.id) {
                if (activeTabIndex > updatedTabs.length - 1) {
                    const newActiveBucket = updatedTabs[updatedTabs.length - 1];
                    setLastActiveBucket(newActiveBucket != null ? newActiveBucket.id : -1);
                    return {
                        ...state,
                        activeBucket: newActiveBucket,
                        bucketsTabs: updatedTabs
                    };
                } else {
                    const newActiveBucket = updatedTabs[activeTabIndex];
                    setLastActiveBucket(newActiveBucket != null ? newActiveBucket.id : -1);
                    return {
                        ...state,
                        activeBucket: newActiveBucket,
                        bucketsTabs: updatedTabs
                    }
                }
            } else {
                return {
                    ...state,
                    bucketsTabs: updatedTabs
                }
            }
        default:
            return state;
    }
};

export default AccessReducer;