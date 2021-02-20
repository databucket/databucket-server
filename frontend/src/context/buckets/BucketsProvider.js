import React, {useReducer} from 'react';
import BucketsContext from "./BucketsContext";
import BucketsReducer from "./BucketsReducer";
import {getBaseUrl, getGetOptions} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";
import {convertNullValuesInCollection} from "../../utils/JsonHelper";
import {getBucketMapper} from "../../utils/NullValueMappers";

const BucketsProvider = props => {

    const initialState = {
        buckets: null
    }

    const [state, dispatch] = useReducer(BucketsReducer, initialState);

    const fetchBuckets = () => {
        fetch(getBaseUrl('buckets'), getGetOptions())
            .then(handleErrors)
            .then(users => dispatch({
                type: "FETCH_BUCKETS",
                payload: convertNullValuesInCollection(users, getBucketMapper())
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
        console.log(`notifyBuckets: ${sourceName}(${sourceObjectId}) -> ${sourceObjectItemsIds}`);
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
            default:
                console.log("Undefined notification source!");
                return;
        }
        dispatch({
            type: "NOTIFY_BUCKETS",
            payload: {itemsTargetFieldName: itemsTargetFieldName, sourceObjectId: sourceObjectId, sourceObjectItemsIds: sourceObjectItemsIds}
        });
    }

    return (
        <BucketsContext.Provider value={{buckets: state.buckets, fetchBuckets, addBucket, editBucket, removeBucket, notifyBuckets}}>
            {props.children}
        </BucketsContext.Provider>
    );
}

export default BucketsProvider;