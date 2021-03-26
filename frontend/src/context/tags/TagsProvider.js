import React, {useReducer} from 'react';
import {getGetOptions} from "../../utils/MaterialTableHelper";
import {convertNullValuesInCollection} from "../../utils/JsonHelper";
import TagsContext from "./TagsContext";
import TagsReducer from "./TagsReducer";
import {handleErrors} from "../../utils/FetchHelper";
import {getTagMapper} from "../../utils/NullValueMappers";
import {getBaseUrl} from "../../utils/UrlBuilder";

const TagsProvider = props => {
    const initialState = {
        tags: null
    }

    const [state, dispatch] = useReducer(TagsReducer, initialState);

    const fetchTags = () => {
        fetch(getBaseUrl('tags'), getGetOptions())
            .then(handleErrors)
            .then(tags => dispatch({
                type: "FETCH_TAGS",
                payload: convertNullValuesInCollection(tags, getTagMapper())
            }))
            .catch(err => console.log(err));
    }

    const addTag = (tag) => {
        dispatch({
            type: "ADD_TAG",
            payload: tag
        });
    }

    const editTag = (tag) => {
        dispatch({
            type: "EDIT_TAG",
            payload: tag
        });
    }

    const removeTag = (id) => {
        dispatch({
            type: "REMOVE_TAG",
            payload: id
        });
    }

    const notifyTags = (sourceName, sourceObjectId, sourceObjectItemsIds) => {
        console.log(`notifyTags: ${sourceName}(${sourceObjectId}) -> ${sourceObjectItemsIds}`);
        let itemsTargetFieldName;
        switch (sourceName) {
            case "BUCKET":
                itemsTargetFieldName = "bucketsIds";
                break;
            default:
                console.log("Undefined notification source!");
                return;
        }
        dispatch({
            type: "NOTIFY_TAGS",
            payload: {itemsTargetFieldName: itemsTargetFieldName, sourceObjectId: sourceObjectId, sourceObjectItemsIds: sourceObjectItemsIds}
        });
    }

    return (
        <TagsContext.Provider value={{tags: state.tags, fetchTags, addTag, editTag, removeTag, notifyTags}}>
            {props.children}
        </TagsContext.Provider>
    );
}

export default TagsProvider;