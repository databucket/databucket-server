import React, {useMemo, useReducer} from 'react';
import BucketsContext from "./BucketsContext";
import BucketsReducer from "./BucketsReducer";
import {getGetOptions} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";
import {convertNullValuesInCollection} from "../../utils/JsonHelper";
import {getBucketMapper} from "../../utils/NullValueMappers";
import {getBaseUrl} from "../../utils/UrlBuilder";

const BucketsProvider = props => {

    const initialState = {
        buckets: null
    }

    const [state, dispatch] = useReducer(BucketsReducer, initialState);

    const fetchBuckets = () => {
        fetch(getBaseUrl('buckets'), getGetOptions())
            .then(handleErrors)
            .then(buckets => dispatch({
                type: "FETCH_BUCKETS",
                payload: convertNullValuesInCollection(buckets, getBucketMapper())
            }))
            .catch(err => console.log(err));
    }

    const addBucket = (bucket) => {
        dispatch({
            type: "ADD_BUCKET",
            payload: bucket
        });
    }

    const editBucket = (bucket) => {
        dispatch({
            type: "EDIT_BUCKET",
            payload: bucket
        });
    }

    const removeBucket = (id) => {
        dispatch({
            type: "REMOVE_BUCKET",
            payload: id
        });
    }

    const notifyBuckets = (sourceName, sourceObjectId, sourceObjectItemsIds) => {
        // console.log(`notifyBuckets: ${sourceName}(${sourceObjectId}) -> ${sourceObjectItemsIds}`);
        let itemsTargetFieldName;
        switch (sourceName) {
            case "USER":
                itemsTargetFieldName = "usersIds";
                break;
            case "GROUP":
                itemsTargetFieldName = "groupsIds";
                break;
            case "TAG":
                itemsTargetFieldName = "tagsIds";
                break;
            case "VIEW":
                itemsTargetFieldName = "viewsIds";
                break;
            case "TEAM":
                itemsTargetFieldName = "teamsIds";
                break;
            default:
                console.log("BucketsProvider - Undefined notification source! " + sourceName);
                return;
        }
        dispatch({
            type: "NOTIFY_BUCKETS",
            payload: {
                itemsTargetFieldName: itemsTargetFieldName,
                sourceObjectId: sourceObjectId,
                sourceObjectItemsIds: sourceObjectItemsIds
            }
        });
    }

    const buckets = useMemo(() => {
        return {buckets: state.buckets, fetchBuckets, addBucket, editBucket, removeBucket, notifyBuckets};
    }, [state.buckets]);
    return (
        <BucketsContext.Provider value={buckets}>
            {props.children}
        </BucketsContext.Provider>
    );
}

export default BucketsProvider;
